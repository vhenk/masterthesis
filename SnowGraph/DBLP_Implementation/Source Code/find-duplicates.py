import sys, os,re
import collections
author_name_file = "output/author-key-map.txt"
author_link_file = "output/author-list.txt"
output_file = "output/Problems/duplicate_links.txt"
output_file_2 = "output/de-duplicate.txt"
if __name__ == "__main__":
    author_name_list = []
    author_link_list = []
    file_pointer = open(author_name_file, 'r', encoding="utf-8")
    for line in file_pointer:
        current_name = line.split("\t")[1].strip("\n")
        author_name_list.append(current_name)
    file_pointer.close()

    file_pointer = open(author_link_file, 'r', encoding ="utf-8")
    for line in file_pointer:
        current_link = line.strip("\n")
        author_link_list.append(current_link)
    file_pointer.close()

    print (len(author_name_list))
    print (len(set(author_name_list)))

    duplicate_list = ([item for item, count in collections.Counter(author_name_list).items() if count > 1])
    print (duplicate_list)

    with open(output_file, 'w', encoding ="utf-8") as file1, open(output_file_2, 'w', encoding ="utf-8") as file2:
        for num in range(len(duplicate_list)):
            file1.write("%s.Duplicated Author Number\n" %(num+1))
            indices = [i for i,x in enumerate(author_name_list) if x == duplicate_list[num]]
            file1.write("Author Name: %s\nDuplicated Links:" %(author_name_list[indices[0]]))
            for items in indices:
                file1.write("\n%s" %(author_link_list[items]))
                file2.write("%s\t" % (author_link_list[items]))
            file2.write("\n")
            file1.write("\n")
            file1.write("\n")
        file1.close()
        file2.close()

