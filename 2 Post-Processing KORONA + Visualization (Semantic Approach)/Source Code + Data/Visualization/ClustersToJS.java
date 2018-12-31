import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ClustersToJS {
	// Input-files:
	static String authorkeyspath = "";	static String suffix = "";
	
	static class Author {
		String id;	ArrayList<Cluster> clusters;
		public Author(String id, ArrayList<Cluster> clusters) {	this.id = id;	this.clusters = clusters;	}
	}
	
	static class Cluster {
		ArrayList<Node> nodes;	ArrayList<Link> links;
		public Cluster(ArrayList<Node> nodes, ArrayList<Link> links) {	this.nodes = nodes; this.links = links;	}
	}
	
	static class Node {
		String id, name; int group;
		public Node(String id, int group) {
			this.id = id;	this.group = group;	this.name = "";
			if(id.substring(0, 1).equals("A")) {
				// Retrieve Author's Name:
				try(BufferedReader br = new BufferedReader(new FileReader(authorkeyspath))) {
					String line;
					while((line = br.readLine()) != null) {
						if(line.indexOf("\t") != -1) {
							String idtmp = line.substring(0, line.indexOf("\t"));
							if(idtmp.equals(this.id)) {
								line = line.substring(line.indexOf("\t")+1);
								this.name = line.substring(0, line.indexOf("\t")).trim();	break;
							}
						}
					}
					br.close();
				} catch(IOException e)	{	e.printStackTrace();	}
			}
			else	this.name = "ISW" + this.id;
		}
	}
	
	static class Link {
		String source, target; double weight;
		public Link(String source, String target, double weight) {
			this.source = source;	this.target = target;	this.weight = weight;
		}		
	}
	
	public static ArrayList<Cluster> readSubDir(String dirpath, String subdir) {
		String authorid = "A" + subdir;
		ArrayList<Cluster> clusters = new ArrayList<>();	ArrayList<String> filenames = new ArrayList<>();
		// Get Files in subdir:
		File[] list = null;
		try {
			File dir = new File(dirpath + "/" + subdir);	list = dir.listFiles();
			for(File f : list)	{
				String tmp = f.getName();
				if((!tmp.substring(0, 1).equals(".")) && (tmp.indexOf(".txt") != -1))	filenames.add(tmp);
			}
		} catch(Exception e)	{	e.printStackTrace();	}
		
		// Read each file and create the corresponding cluster:
		for(String file : filenames) {
			try(BufferedReader br = new BufferedReader(new FileReader(dirpath + "/" + subdir + "/" + file))) {
				String line;	ArrayList<Link> links = new ArrayList<>();	ArrayList<Node> nodes = new ArrayList<>();
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						// Retrieve Link-list:
						String source = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						String target = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						String wtmp = line.substring(0, line.indexOf("\t"));	double weight = -1;
						try {	weight = Double.parseDouble(wtmp);	}	catch(Exception e) {}
						links.add(new Link(source, target, weight));
					}
				}
				// Retrieve Node-list:
				for(Link l : links) {
					// Check if target is already in Node-list:
					boolean exists = false;
					for(Node n : nodes)	{	if(n.id.equals(l.target))	exists = true;	}
					if(!exists)	nodes.add(new Node(l.target, 3));
					// Check if source already exists in Node-list:
					exists = false;
					for(Node n : nodes ) {	if(n.id.equals(l.source))	exists = true;	}
					if(!exists) {
						if(l.source.equals(authorid))	nodes.add(new Node(l.source, 1));
						else	nodes.add(new Node(l.source, 2));
					}
				}
				// Add Cluster to List:
				clusters.add(new Cluster(nodes, links));
			} catch(IOException e)	{	e.printStackTrace();	}
		}
		
		return clusters;
	}

	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Process Input-files:
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory with filtered clusters: ");	String dirpath = reader.nextLine();
		System.out.println("Name of author-keys-file: ");	authorkeyspath = reader.nextLine();
		System.out.println("Suffix for Json-Objects: ");	suffix = reader.nextLine();
		reader.close();
		if(authorkeyspath.indexOf(".txt") == -1)	authorkeyspath += ".txt";
		
		// Get Subdirectories:
		ArrayList<String> subdirs = new ArrayList<>();
		File[] list = null;
		try {
			File dir = new File(dirpath);	list = dir.listFiles();
			for(File f : list) {	if(f.isDirectory())	subdirs.add(f.getName());	}
		} catch(Exception e)	{	e.printStackTrace();	}
		
		// Convert Subdirectories to Js-Objects:
		ArrayList<String> jsObjects = new ArrayList<>();
		for(String author : subdirs) {
			ArrayList<Cluster> clusters = readSubDir(dirpath, author);
			// Create JS-Object:
			JsonArrayBuilder cbuilder = Json.createArrayBuilder();
			for(Cluster c : clusters) {
				JsonArrayBuilder nbuilder = Json.createArrayBuilder();
				for(Node n : c.nodes) {
					JsonObject node = Json.createObjectBuilder().add("id", n.id).add("name", n.name).add("group", n.group).build();
					nbuilder.add(node);
				}
				JsonArray nodelist = nbuilder.build();
				JsonArrayBuilder lbuilder = Json.createArrayBuilder();
				for(Link l : c.links) {
					JsonObject link = Json.createObjectBuilder().add("source", l.source).add("target", l.target)
							.add("value", l.weight).build();
					lbuilder.add(link);
				}
				JsonArray linklist = lbuilder.build();
				JsonObject cluster = Json.createObjectBuilder().add("nodes", nodelist).add("links", linklist).build();
				cbuilder.add(cluster);
			}
			JsonArray clusterlist = cbuilder.build();
			JsonObject obj = Json.createObjectBuilder().add("id", "A" + author).add("clusters", clusterlist).build();
			jsObjects.add("var A" + author + suffix + " = " + obj.toString() + ";");
		}
		
		// Create and store Output-file:
		String outputpath = "network_" + suffix + ".js";	String outputfile = "";
		for(int i = 0; i < jsObjects.size(); i++) {
			outputfile += jsObjects.get(i);
			if(i != jsObjects.size()-1)	outputfile += "\n";
		}
		writeToNewFile(outputpath, outputfile);
	}
}
