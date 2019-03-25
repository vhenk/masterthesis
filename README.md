# masterthesis
The settings and results of the Metaresearch Recommendations paper are contained in the directoy 'SG4MR [metaresearch paper]'.

The corresponding source code is stored along with the previous implemented parts in the directory 'SnowGraph'.

The directory 'SnowGraph' contains three direct sub-directories:

* DBLP_Implementation:
    * Contains the improved implementation of KORONA (done by Rahul, errors removed by me)
    * Contains the extended parts of the KG (paper, venue recommendations, etc.)
    * Contains the data corresponding to this data source
    * Contains the metaresearch implementation, but this source code can also be applied to the SciGraph-based KG. 
	
* SG_Implementation:
    * Contains the KG-creation implementation based on SN SciGraph
    * Contains the extended parts of the KG (paper, venue recommendations, etc.)
    * Contains the data corresponding to this data source
	
* Visualization_Source Code:
    * Contains the java tools to convert clusters and the author list to JS-objects. They are identic for both main data sources.
	