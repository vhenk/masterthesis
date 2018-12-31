
import sys, os,re
import collections
output_file = "output/Filtered-ISWC.nt"
duplicate_file = "output/de-duplicate.txt"
input_NT_file = "ISWC.nt"

duplicate_list = []

def de_duplicate(author):
    author_temp = author.strip("<").strip(">")
    for list in duplicate_list:
        if author_temp in list:
            print (list[0])
            return ("<%s>"%(list[0]))
    return author

file_pointer = open(duplicate_file, 'r', encoding ="utf-8")
for line in file_pointer:
    current_list = re.split(r'\t+', line)
    current_list.remove('\n')
    duplicate_list.append(current_list)
file_pointer.close()

file_pointer = open(input_NT_file, 'r', encoding ="utf-8")
file_writer = open(output_file, 'w', encoding= "utf-8")

line_counter = 0
for line in file_pointer:
    line_counter +=1
    current_list = re.split(r'\t+', line)
    current_list.remove('.\n')
    fixed_author = de_duplicate(current_list[2])
    file_writer.write("%s\t%s\t%s\t.\n"%(current_list[0],current_list[1],fixed_author))

file_pointer.close()
file_writer.close()
