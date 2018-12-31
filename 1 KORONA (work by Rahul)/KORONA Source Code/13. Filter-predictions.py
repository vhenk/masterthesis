#!/usr/bin/env python3
#./Filter-predictions.py <author-key-file> <author-url-file> <cluster-directory>
# ./13.\ Filter-predictions.py output/author-key-map.txt output/author-list.txt output/author-clusters/


import os,re
import pathlib
import openpyxl
from collections import Counter

cluster_files_filtered = []
cluster_files_with_single_nodes = []
excel_data = []
authors_list = []
Collaborated_Authors = []

def load_cluster_filenames(cluster_folder):
    for path, subdirs, files in os.walk(cluster_folder):
        for file_name in files:
            if "prediction" in file_name:
                os.remove(pathlib.PurePath(path,file_name))
                continue
            if file_name.endswith(".txt"):
                cluster_files_with_single_nodes.append(pathlib.PurePath(path, file_name))
            else:
                print("Fatal filename unknown " + file_name)
    return

def trim_clusters(cluster_file):
    f_cluster_file = open(cluster_file, 'r', encoding="utf-8")
    line_counter = 0
    for line in f_cluster_file:
        line_counter+=1
        if (line_counter>1):
            cluster_files_filtered.append(cluster_file)
            break
    f_cluster_file.close()
    return

def load_authors(author_url_file,author_name_file):
    temp_auth_list = []
    urlfile = open(author_url_file, 'r', encoding="utf-8")
    temp_list = []
    for line in urlfile:
        temp_list.append(line.strip('\n'))
    urlfile.close()
    namefile = open(author_name_file, 'r', encoding="utf-8")
    counter = 0
    for line in namefile:
        temp_auth_list.append([temp_list[counter], line.split("\t")[1].strip("\n")])
        counter += 1
    namefile.close()
    del temp_list
    #print (author_list)
    return temp_auth_list

def get_author_index(author_link):
    for item in authors_list:
        if (author_link) in item:
            index1 = authors_list.index(item)
            #print (authors_list[index1])
            return index1
    print ("could not find author link %s in author dictionary"%(author_link))
    return 0

def get_authors_from_clusters(file):
    global cluster_files_filtered
    cluster_fp = open(cluster_files_filtered[file], 'r', encoding="utf-8")
    current_auth_list = []
    for line in cluster_fp:
        if line != '\n':
            author_index = int(line.split("\t")[0].strip("A"))-1
            #print (author_index)
            if author_index not in current_auth_list:
                current_auth_list.append(author_index)
    return current_auth_list

def get_paired_lists(input_list):
    Results_In_Pairs = []
    for item_1 in input_list:
        for item_2 in input_list:
            if (item_1 != item_2) and ((item_1,item_2) not in Results_In_Pairs):
                Results_In_Pairs.append((item_1,item_2))
    return Results_In_Pairs

def findPredictions(cluster_authors):
    global Collaborated_Authors
    author_pairs_all = get_paired_lists(cluster_authors)
    prediction_list = list(set(author_pairs_all).difference(set(Collaborated_Authors)))
    collaborated_list = list(set(author_pairs_all).intersection(set(Collaborated_Authors)))
    return (prediction_list,collaborated_list)

def remove_duplicates(Author_Tuples):
    for (author1,author2) in Author_Tuples:
        if (author2,author1) in Author_Tuples:
            Author_Tuples.remove(Author_Tuples[Author_Tuples.index((author2,author1))])
    return Author_Tuples



def Save_Results(Predicted_Results, Collaborated_Results, file):
    global cluster_files_filtered
    global authors_list

    prediction_filename = os.path.basename(os.path.dirname(cluster_files_filtered[file])) + "-predictions.txt"
    prediction_file_link = os.path.join(os.path.dirname(os.path.dirname(cluster_files_filtered[file])),prediction_filename)

    directory = os.path.dirname(prediction_file_link)
    try:
        os.stat(directory)
    except:
        os.mkdir(directory)

    if os.path.isfile(prediction_file_link):
        fp_prediction = open(prediction_file_link, 'r+', encoding="utf-8")
        next(fp_prediction)
        for line in fp_prediction:
            if line == '\n' :
                continue
            author1 = get_author_index(line.split("\t")[1])
            author2 = get_author_index(line.split("\t")[3])
            prediction_flag = int(line.split("\t")[4].strip("\n"))
            if prediction_flag == 1:
                if(author1, author2) in Predicted_Results:
                    Predicted_Results.remove((author1, author2))
                elif (author2, author1) in Predicted_Results:
                    Predicted_Results.remove((author2, author1))
                else:
                    Predicted_Results.append((author1,author2))
            elif prediction_flag ==0:
                if(author1,author2) in Collaborated_Results:
                    Collaborated_Results.remove((author1,author2))
                elif(author2,author1) in Collaborated_Results:
                    Collaborated_Results.remove((author2,author1))
                else:
                    Collaborated_Results.append((author1,author2))
            else:
                print("error found")

        fp_prediction.close()

    fp_prediction = open(prediction_file_link, 'w', encoding="utf-8")
    fp_prediction.write("Author-1\tAuthor-1-link\tAuthor-2\tAuthor-2-link\tPrediction")
    current_author_name_without_quotes = re.sub(r"[^A-Za-z]+", '',os.path.basename(os.path.dirname(cluster_files_filtered[file])))
    #print (current_author_name_without_quotes)
    current_author_index = 0
    for items in authors_list:
        if  current_author_name_without_quotes in re.sub(r"[^A-Za-z]+", '',items[1]):
            current_author_index = get_author_index(items[1])
            break
    #print(current_author_index)

    Predicted_Results = remove_duplicates(Predicted_Results)
    Collaborated_Results = remove_duplicates(Collaborated_Results)
    if set(Predicted_Results).intersection(set(Collaborated_Results)) != set():
        print("something is wrong")
    for items in range(len(Predicted_Results)):
        current_author_1 = authors_list[Predicted_Results[items][0]]
        current_author_2 = authors_list[Predicted_Results[items][1]]
        if isinstance(current_author_index, int):
            if current_author_index == 0:
                print("error")
            if (current_author_1 == authors_list[current_author_index]) or (current_author_2 == authors_list[current_author_index]):
                if current_author_1 == authors_list[current_author_index]:
                    fp_prediction.write("\n%s\t%s\t%s\t%s\t1"%(current_author_1[1],current_author_1[0],current_author_2[1],current_author_2[0]))
                else:
                    fp_prediction.write("\n%s\t%s\t%s\t%s\t1"%(current_author_2[1],current_author_2[0],current_author_1[1],current_author_1[0]))
    for items in range(len(Collaborated_Results)):
        current_author_1 = authors_list[Collaborated_Results[items][0]]
        current_author_2 = authors_list[Collaborated_Results[items][1]]
        if isinstance(current_author_index, int):
            if (current_author_1 != current_author_index) and (current_author_2 != current_author_index):
                continue
        fp_prediction.write("\n%s\t%s\t%s\t%s\t0"%(current_author_1[1],current_author_1[0],current_author_2[1],current_author_2[0]))
    fp_prediction.close()
    del current_author_name_without_quotes
    del current_author_index
    return

#./Filter-predictions.py <author-key-file> <author-url-file> <cluster-directory> <output-directory>
######################################################################################

if __name__ == "__main__":
    # author_url_file = sys.argv[1]
    # author_key_file = sys.argv[2]
    # clusters_directory = sys.argv[3]
    # excel_file = sys.argv[4]
    # output_file = sys.argv[5]


    author_url_file = "output/author-list.txt"
    author_key_file = "output/author-key-map.txt"
    clusters_directory = "output/author-clusters/"
    excel_file = "metis.xlsx"

    load_cluster_filenames(clusters_directory) #load clusters with links
    for var in range(len(cluster_files_with_single_nodes)):
        trim_clusters(cluster_files_with_single_nodes[var])
    del cluster_files_with_single_nodes

    authors_list = load_authors(author_url_file, author_key_file) #load author links

    #Here we load the ISWC entire data from the excel file onto memory.
    wb = openpyxl.load_workbook(excel_file)
    sh = wb.get_active_sheet()
    counter =0
    for row in sh.rows:  # loop over each line of input file
        eachrow = []
        counter += 1  # point to the next line of the input file
        if (counter == 1):  # First row is header row, so skip.
            continue
        num_author = int(row[2].value)  # Get number of authors for the paper represented in that row.
        if (num_author > 0):  # If there is atleast one entry of authorship for that paper.
            #eachrow.extend([int(row[0].value), row[1].value, num_author, int(row[3].value)])
            for num in range(1, num_author + 1):  # For each author of that paper
                auth_link = str(row[3 + num].value)  # get the author's URI.
                eachrow.append(get_author_index(auth_link))
            Collaborated_Authors.extend(get_paired_lists(eachrow))
        excel_data.append(eachrow)
    wb.close()

#    same_cluster_authors = []
    for file in range(len(cluster_files_filtered)):
        #print("Now processing file number %s named %s"%(file,cluster_files_filtered[file]))
        authorsInaCluster = get_authors_from_clusters(file)
        (Predicted_Results, Collaborated_Results) = findPredictions(authorsInaCluster)
        if Predicted_Results != [] or Collaborated_Results != []:
            Save_Results(Predicted_Results,Collaborated_Results,file)


    #print("Done! output filename: "+sys.argv[2])
