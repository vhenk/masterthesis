# -*- coding: utf-8 -*-
from rdflib import Graph, plugin, URIRef, Literal, BNode, Namespace
from rdflib.serializer import Serializer
import glob, time, os, logging,pprint
from rdflib.namespace import XSD, RDF
import sys,re

reload(sys)
sys.setdefaultencoding('UTF-8')

inputpath = os.getcwd() + "/nt-files/" #Path for the imput directory containing triples files.
x = glob.glob("%s*.nt" %inputpath) #Accumulate all files inside the triples directory.
outputpath="ISWC.nt" #Output file with triples related to only ISWC Conferences

f= open(outputpath, "w")    #Open output file

for current_file in x:  #For each N-Triples file in the triples directory.

    g= Graph() #Initialize a Graph
    g.parse(current_file, format="nt") #Parse the Input NT file for SPARQL filtering
    qres = g.query(
                   """SELECT ?a ?b ?c
                    WHERE {
                    ?a ?b ?c .
                    FILTER (regex( str(?a),  "http://dblp.org/rec/conf/semweb/")).
                    }
                    order by asc(str(?a))
                       """,
                initNs=dict(
                            amco=Namespace("http://dblp.org/rec/journals/"),
                            rdfs=Namespace("http://dblp.org/rdf/schema-2017-04-18#")))  #SPARQL query to get all triples related to ISWC. They have the same subject prefix.

    for row in qres.result: #Results would contain triples which belong to ISWC conference.
        f.write("<%s>\t" %row[0].decode('unicode-escape').encode('utf-8')) #Write Subject to file
        f.write("<%s>\t" %row[1].decode('unicode-escape').encode('utf-8')) #write predicate
        if re.match(r'^http', row[2]):
            f.write("<%s>\t.\n" %row[2].decode('unicode-escape').encode('utf-8')) #write object
        else:
            temp = row[2].replace("\"", "'")
            f.write("\"%s\"\t.\n" %temp.decode('unicode-escape').encode('utf-8')) #write object
f.close()   #Output file is ready
