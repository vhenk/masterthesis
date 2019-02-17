package scigraph_dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class METIS2semEP {
	static class GraphEdge {
		String author, conference, weight;
		public GraphEdge(String author, String conference, String weight) {
			this.author = author;	this.conference = conference;	this.weight = weight;
		}
	}
	
	private static ArrayList<GraphEdge> loadInteractions(String path) {
		ArrayList<GraphEdge> interactions = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int counter = 0;
			while((line = br.readLine()) != null) {
				if(counter != 0) {
					if(line.indexOf("\t") != -1) {
						String aID = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						String cID = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						String weight = line.substring(line.indexOf("\t")+1);
						interactions.add(new GraphEdge(aID, cID, weight));
					}
				}
				counter++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return interactions;
	}
	
	private static HashMap<String, ArrayList<GraphEdge>> retrieveClusters(ArrayList<GraphEdge> graph, String path) {
		HashMap<String, ArrayList<GraphEdge>> clusters = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int idx = 0;
			while((line = br.readLine()) != null) {
				GraphEdge edge = graph.get(idx);
				String key = line.trim();
				if(!clusters.containsKey(key)) {
					ArrayList<GraphEdge> tmp = new ArrayList<>();	tmp.add(edge);	clusters.put(key, tmp);
				}
				else {
					ArrayList<GraphEdge> tmp = clusters.get(key);	tmp.add(edge);	clusters.replace(key, tmp);
				}
				idx++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return clusters;
	}
	
	private static void storeClusters(HashMap<String, ArrayList<GraphEdge>> clusters, String dirpath) {
		File dir = new File(dirpath);	dir.mkdir();
		for(String key : clusters.keySet()) {
			String filename = dirpath + "/metis-cluster-" + key + "-semEPformat.txt";
			ArrayList<GraphEdge> edges = clusters.get(key);
			for(GraphEdge e : edges)	appendToFile(filename, e.author + "\t" + e.conference + "\t" + e.weight + "\tedge\n");
		}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String graphpath = "data_sg/output/Auth-Conf_graph.txt";	
		String metispath = "data_sg/metisgraphs/metis98.txt.part.2875";
		String outputpath = "metis2semEP";
		ArrayList<GraphEdge> interactions = loadInteractions(graphpath);
		HashMap<String, ArrayList<GraphEdge>> clusters = retrieveClusters(interactions, metispath);
		storeClusters(clusters, outputpath);
	}
}
