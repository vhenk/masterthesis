#!/usr/bin/env python3
#./Filter-visualization.py <input-cluster-directory> <output-cluster-directory>

import sys, os,re
from shutil import copyfile

input_clusters = []
output_clusters = []

def load_cluster_filenames(dirname):
    for filename in sorted(os.listdir(dirname)):
        if filename.endswith(".txt"):
            tok = filename.split("-")
            key = int(tok[2])
            fpath = os.path.join(dirname, filename)
            input_clusters.append(fpath)
        else:
            print("Fatal filename unknown "+filename)
    return

def trim_clusters(file):
    f_cluster_file = open(file, 'r', encoding="utf-8")
    author_count = 0
    author_list = []
    for line in f_cluster_file:
        eachline = line.split()
        author = eachline[0].strip("A")
        if (author not in author_list):
            author_count+=1
            author_list.append(author)
        if (author_count>1):
            output_clusters.append(file)
            break
    f_cluster_file.close()
    return

def filter_clusters_with_authors(file, author_filter_list):
    author_filter_present = [0] * len(author_filter_list)
    f_cluster_file = open(file, 'r', encoding="utf-8")
    for line in f_cluster_file:
        eachline = line.split()
        current_author = eachline[0].strip("A")
        if(current_author in author_filter_list):
            #print(current_author)
            author_filter_present[author_filter_list.index(current_author)] = 1
            if(sum(author_filter_present)==len(author_filter_list)):
                #print(author_filter_list)
                output_clusters.append(file)
                break
    f_cluster_file.close()
    return

######################################################################################
if __name__ == "__main__":
    load_cluster_filenames(sys.argv[1])
    #load_cluster_filenames("output/semEP-clusters/95-percentile/")
    author_filter_list = []
    output_folder = sys.argv[2]
    #output_folder = "output/author-clusters/semEP/95-percentile/Abraham-Bernstein/"
    directory = os.path.dirname(output_folder)
    try:
        os.stat(directory)
    except:
        os.mkdir(directory)
    #output_folder = "output/filtered_clusters/"
    author_filter_flag = input("Do you want to Filter with Author(s): (Press Y for yes or N for no\n")
    while True:
        if(author_filter_flag == "Y")  or (author_filter_flag == "y"):
            author_filter_var = input("Enter Author Number. For eg. 92 \n")
            author_filter_list.append(author_filter_var.strip("A"))
            author_filter_flag = input("Do you want to Enter More Author(s) into the Filter: (Press Y for yes or N for no\n")
            continue
        elif(author_filter_flag == "N") or (author_filter_flag == "n") :
            break
        else:
            print ("Not valid input. Press Y or N please.")
            author_filter_flag = input("Do you want to Filter with Author(s): (Press Y for yes or N for no\n")
            continue

    for var in range(len(input_clusters)):
        trim_clusters(input_clusters[var])
    
    #print(output_clusters)
    if(author_filter_list != []):
        input_clusters = output_clusters
        output_clusters = []
        #temp_filter_list = author_filter_list
        #print (author_filter_list)
        #print(*author_filter_list, sep=', ')
        print(len(input_clusters))
        for var in range(len(input_clusters)):
            #print (input_clusters[var])
            filter_clusters_with_authors(input_clusters[var],author_filter_list)
            #quit()
    print (len(output_clusters))
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    for file in output_clusters:
        head, tail = os.path.split(file)
        copyfile(file, output_folder+tail)
    sys.exit(1)
#    print("Done! output filename: "+sys.argv[2])
