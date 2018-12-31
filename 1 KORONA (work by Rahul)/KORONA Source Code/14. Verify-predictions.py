#!/usr/bin/env python3
#./Filter-predictions.py <author-key-file> <author-url-file> <cluster-directory> <output-directory>

import os
import pathlib
import openpyxl
import sys

excel_data = []
prediction_file = "output/author-clusters/metis/95-percentile/Maria-Esther-Vidal-predictions.txt"
excel_file = "metis.xlsx"
wb = openpyxl.load_workbook(excel_file)
sh = wb.get_active_sheet()
counter =0
predictions = []
fp = open(prediction_file,'r',encoding="utf-8")
for line in fp:
    author1 =line.split("\t")[0].split("(")[1].strip("\n").strip(")")
    author2 =line.split("\t")[1].split("(")[1].strip("\n").strip(")")
    predictions.append([author1,author2])
for row in sh.rows:  # loop over each line of input file
    eachrow = []
    counter += 1  # point to the next line of the input file
    if (counter == 1):  # First row is header row, so skip.
        continue
    num_author = int(row[2].value)  # Get number of authors for the paper represented in that row.
    if (num_author > 0):  # If there is atleast one entry of authorship for that paper.
        for num in range(1, num_author + 1):  # For each author of that paper
            auth_link = str(row[3 + num].value)  # get the author's URI.
            eachrow.append(auth_link)
    excel_data.append(eachrow)
wb.close()
for item in excel_data:
    for author_pairs in predictions:
        if (set(author_pairs) & set(item) == set(author_pairs)):
#        if author_pairs in excel_data[item]:
            print("Authors %s found in excel file line number %s and prediction file line number %s"%(author_pairs,excel_data.index(item),predictions.index(author_pairs)))
print("If nothing was printed it means it was a pure prediction")
