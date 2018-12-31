# -*- coding: utf-8 -*-


from array import array
import urllib2,re
from bs4 import BeautifulSoup
import urllib
from urllib2 import Request, urlopen, URLError
import time
import sys,re

reload(sys)
sys.setdefaultencoding('UTF-8')

Auth_dict_file = "output/author-key-map.txt"
Error_file = "output/Problems/404.txt"
author_dict_link = dict()
author_name = []
error_list = []
def remove_non_alphabets(name):
    new_name = re.sub('[!@#$]=', '', name)
    new_name = re.sub('\d+', '', new_name)
    if (name != new_name):
        print ("old name was %s and new name is %s" %(name,new_name))
    return new_name

def getName(link):
    response = urllib2.Request(link)
    try:
        value=urllib2.urlopen(response)
    except URLError, e:
        print e.reason
        if e.reason == "Not Found":
            error_list.append(link)
            name = str(link.rsplit('/', 1)[-1].split(':')[1]) + " " + str(link.rsplit('/', 1)[-1].split(':')[0])
            print ("link was %s and decoded name is %s" %(link,name))
            return remove_non_alphabets(name)
        if e.reason == "Too Many Requests":
            return "Too Many Requests"
    html = value.read()
    soup = BeautifulSoup(html, "lxml")
    name = ((soup.title.contents[0]).encode('utf-8')).split("dblp: ")[1]
    #print name
    return remove_non_alphabets(name)

def file_save():
    print len(author_name)
    with open(Auth_dict_file, 'w') as f:
        for value in range(len(author_name)):
            f.write("A%s\t%s\n" %(value+1,author_name[value]))
            #print ("A%s\t%s\n" %(value+1,author_name[value]))
    f.close()
    with open(Error_file, 'w') as f:
        for item in error_list:
            f.write("%s\n" %(item))
    f.close()
    quit()
########################################################################################


counter = 0
with open(Auth_dict_file,'r') as inf:
    for line in inf:
        currentline = line.split("\t") # Separate the keys and the values
        author_dict_link.update({currentline[1].strip('\n'):int(currentline[0].strip("A"))}) #All the links added to dictionary
        author_name.append(currentline[1].strip('\n'))
inf.close()

with open(Error_file, 'r') as f:
    for line in f:
        currentline = line.strip("\n")
        error_list.append(currentline)
    f.close()

counter = 0
for item in author_name:
    if "http" in item:
        #print link
        name = getName(item)
        if name == "Too Many Requests":
            file_save()
        else:
                author_name[counter] = name
    else:
        author_name[counter] = remove_non_alphabets(item)
    counter+=1
file_save()

