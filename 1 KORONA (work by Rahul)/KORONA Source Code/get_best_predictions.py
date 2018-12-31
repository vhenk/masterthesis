
import sys, os,re
import collections
import glob
# -*- coding: utf-8 -*-
input_author_clusters = "output/author-clusters/"
author_keys_file = "output/author-key-map.txt"
author_url_file = "output/author-list.txt"
author_keys = []

def get_prediction_list(path):
    predictions = []
    prediction_filename = path + "-predictions.txt"
    fp = open(prediction_filename, "r", encoding="utf-8")
    next(fp)
    for line in fp:
        predictions.append([line.split("\t")[2],line.split("\t")[3]])
    return predictions


def filter_best_predictions(cluster_dir):
    all_clusters = os.listdir(cluster_dir)
    all_predictions = []

    for file in all_clusters:
        if ".txt" not in file:
            continue
        current_cluster_file = os.path.join(cluster_dir, file)
        cluster_fp = open(current_cluster_file, 'r', encoding="utf-8")
        for line in cluster_fp:
            current_author_in_predictions = 0
            current_author_number = line.split("\t")[0].strip("A")
            if (current_author_number == get_author_index(os.path.basename(os.path.dirname(current_cluster_file)))):
                continue
            current_author_similarity= line.split("\t")[2]
            for items in all_predictions:
                if current_author_number in items:
                    current_author_in_predictions = 1
                    if (all_predictions[all_predictions.index(items)][1] < current_author_similarity):
                        all_predictions[all_predictions.index(items)][1] = current_author_similarity
            if current_author_in_predictions == 0:
                all_predictions.append([current_author_number,current_author_similarity])
    #print(all_predictions)
    all_predictions.sort(key=lambda x: x[1])
    all_predictions.reverse()
    return all_predictions


def input_author_keys(author_name_file, author_url_file):
    urlfile = open(author_url_file, 'r', encoding="utf-8")
    temp_list = []
    for line in urlfile:
        temp_list.append(line.strip('\n'))
    urlfile.close()
    urlfile = open(author_name_file, 'r', encoding="utf-8")
    global author_keys
    counter = 0
    for line in urlfile:
        author_keys.append([line.split("\t")[0],line.split("\t")[1].strip(" \n"),temp_list[counter]])
        counter+=1
    print (author_keys)
    return

def get_author_index(current_author_name):
    global author_keys
    current_author_key = 0
    for item in author_keys:
        if (re.sub(r"[^A-Za-z]+",'',current_author_name) in re.sub(r"[^A-Za-z]+",'',item[1])):
            current_author_key = item[0].strip("A")
            #print (current_author_key)
            return current_author_key
    print("could not find author link %s in author dictionary" % (current_author_name))
    return 0

def save_file(current_file_path,results_top_fifteen,predicted_list):
    filtered_file_path = current_file_path + "-topten_predictions.txt"
    cytoscape_file_path = current_file_path + "-cytoscape-predictions.txt"
    cyto_fp = open(cytoscape_file_path,"w",encoding="utf-8")
    fp = open(filtered_file_path,"w",encoding="utf-8")
    counter = 0
    line_written = 0
    name1 = author_keys[int(get_author_index(os.path.basename(current_file_path)))-1][1]
    link1 = author_keys[int(get_author_index(os.path.basename(current_file_path)))-1][2]
    for each_result in results_top_fifteen:
        name = author_keys[int(results_top_fifteen[counter][0]) - 1][1]
        link = author_keys[int(results_top_fifteen[counter][0]) - 1][2]
        weight = results_top_fifteen[counter][1]
        cyto_fp.write(name1 + "\t" + link1 + "\t" + name + "\t" + link + "\t" + weight + "\n")
        counter+=1
    cyto_fp.close()
    counter = 0
    for each_result in results_top_fifteen:
        name = author_keys[int(results_top_fifteen[counter][0]) - 1][1]
        link = author_keys[int(results_top_fifteen[counter][0]) - 1][2]
        for items in predicted_list:
            if name in items[0]:
                fp.write(name + "\t" + link + "\n")
                line_written+=1
                if (line_written==10):
                    print("holla")
                    fp.close()
                    return
        counter+=1
    fp.close()
    return


#######################$$$$$$############
dir_names = []

for filename in glob.iglob(input_author_clusters+'**/*.txt', recursive=True):
    if "predictions" in filename:
        continue
    current_dirname= os.path.dirname(filename)
    if current_dirname not in dir_names:
        dir_names.append(current_dirname)

input_author_keys(author_keys_file,author_url_file)



for current_file_path in dir_names:
    print (current_file_path)
    current_prediction_list = get_prediction_list(current_file_path)
    results_top_fifteen = filter_best_predictions(current_file_path)
    save_file(current_file_path,results_top_fifteen,current_prediction_list)
            

