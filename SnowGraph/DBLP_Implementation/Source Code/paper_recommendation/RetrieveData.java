package dblp_dataset.paper_recommendation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class RetrieveData {
	static String paperspath = "";	static String s2datapath = "";
	
	static class Author {
		String id, name; ArrayList<Integer> papers;
		public Author(String id, String name) {
			this.id = id;	this.name = name;	papers = new ArrayList<>();
			
			// Retrieve papers:
			try(BufferedReader br = new BufferedReader(new FileReader(paperspath))) {
				String line;	System.out.println("\tRetrieving Papers for Author " + this.id + " ....");
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						String numtmp = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						int number = -1;
						try {number = Integer.parseInt(numtmp);} catch(Exception e) {}
						line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
						// Get part of line with Author-IDs:
						line = line.substring(line.indexOf("\t")+1);
						int pos = line.indexOf(this.id);
						if(pos != -1) {
							line = line.substring(pos);
							if(line.indexOf("\t") != -1) {
								line = line.substring(0, line.indexOf("\t"));
								if((line.equals(this.id)) && (number != -1))	papers.add(number);
							}
							else {
								if((line.trim().equals(this.id)) && (number != -1))	papers.add(number);	
							}	
						}
					}					
				}
				br.close();
			} catch(IOException e)	{	e.printStackTrace();	}
		}	
	}
	
	static class Paper {
		int number, year;	String title; ArrayList<String> keywords; ArrayList<String> authors;
		public Paper(int number, String title, int year, ArrayList<String> authors) {
			this.number = number;	this.title = title;	this.year = year;	
			this.keywords = new ArrayList<>();	this.authors = authors;
			
			// Retrieve Keywords:
			System.out.println("\tRetrieving Keywords for Paper " + this.number + " ....");
			// Get S2-ID and DOI:
			String s2ID = ""; String doi = ""; String titlecheck = this.title.replaceAll("[^a-zA-Z0-9]", "");
			try(BufferedReader br = new BufferedReader(new FileReader(s2datapath))) {
				String line;
				while((line = br.readLine()) != null) {
					try(JsonReader reader = Json.createReader(new StringReader(line));) {
						JsonObject obj = reader.readObject();	reader.close();	String titletmp = obj.getString("title");
						String titletmpcheck = titletmp.replaceAll("[^a-zA-Z0-9]", "");
						if(titletmpcheck.toUpperCase().equals(titlecheck.toUpperCase())) {
							s2ID = obj.getString("id");	doi = obj.getString("doi");	break;
						}
					} catch(Exception e)	{	e.printStackTrace();	}
				}
				br.close();
			} catch(IOException e)	{	e.printStackTrace();	}
			
			// Query S2-API for Keywords:
			if(!s2ID.equals("")) {
				URL url = null;	URL urldoi = null;
				try {
					url = new URL("https://api.semanticscholar.org/v1/paper/" + s2ID);
					urldoi = new URL("https://api.semanticscholar.org/v1/paper/" + doi);
				} catch(MalformedURLException e)	{	e.printStackTrace();	}
				
				// Search by S2-ID:
				try(InputStream stream = url.openStream(); JsonReader reader = Json.createReader(stream)) {
					JsonObject obj = reader.readObject();	reader.close();
					JsonArray topics = obj.getJsonArray("topics");
					for(JsonObject item : topics.getValuesAs(JsonObject.class)) {
						this.keywords.add(item.getString("topic"));
					}					
				} catch(Exception e)	{	
					// If Paper is not found, search by DOI:
					if(!doi.equals("")) {
						try(InputStream stream = urldoi.openStream(); JsonReader reader = Json.createReader(stream)) {
							JsonObject obj = reader.readObject();	reader.close();
							JsonArray topics = obj.getJsonArray("topics");
							for(JsonObject item : topics.getValuesAs(JsonObject.class)) {
								this.keywords.add(item.getString("topic"));
							}					
						} catch(Exception e2)	{}
					}
				}
			}
		}
	}
	
	public static ArrayList<Author> getAuthors(String keypath) {
		System.out.println("Retrieving list of Authors ....");
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(keypath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String name = line.substring(0, line.indexOf("\t"));
					authors.add(new Author(id, name));
				}	
			}
			br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		return authors;
	}
	
	public static ArrayList<Paper> getPapers() {
		System.out.println("Retrieving list of Papers ....");
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(paperspath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String numtmp = line.substring(0, line.indexOf("\t"));		
					line = line.substring(line.indexOf("[")+1);
					String title = line.substring(0, line.indexOf("\t")-1);
					line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
					String yeartmp = line.substring(0, line.indexOf("\t"));
					int number = -1;	int year = -1;
					try {number = Integer.parseInt(numtmp); year = Integer.parseInt(yeartmp);} catch(Exception e) {}
					line = line.substring(line.indexOf("\t") + 1);
					
					// Get Authors:
					ArrayList<String> p_authors = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							p_authors.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {	p_authors.add(line.trim());	break;	}
					}
					papers.add(new Paper(number, title, year, p_authors));
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return papers;
	}
	 
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}

	public static void main(String[] args) {
		// Input-files:
		Scanner reader = new Scanner(System.in);
		System.out.println("Name of Authors-key-file: ");	String authorskeypath = reader.nextLine();
		System.out.println("Name of Papers-file: ");	paperspath = reader.nextLine();
		System.out.println("Name of S2-data-file: ");	s2datapath = reader.nextLine();
		reader.close();
		if(authorskeypath.indexOf(".txt") == -1)	authorskeypath += ".txt";
		if(paperspath.indexOf(".txt") == -1)	paperspath += ".txt";
		if(s2datapath.indexOf(".txt") == -1)	s2datapath += ".txt";
		
		// Output-files:
		String authorspath = "authors.txt";	String authorsfile = "";
		String pkeywordspath = "papers_keywords.txt";	String pkeywordsfile = "";
		
		// Retrieve list of Authors:
		ArrayList<Author> authors = getAuthors(authorskeypath);
		
		// Store Authors-file:
		System.out.println("Processing Data ....");
		for(int i = 0; i < authors.size(); i++) {
			Author a = authors.get(i);	authorsfile += a.id + "\t[" + a.name + "]";
			for(int j = 0; j < a.papers.size(); j++)	authorsfile += "\t" + a.papers.get(j);
			if(i != authors.size()-1)	authorsfile += "\n";
		}
		writeToNewFile(authorspath, authorsfile); 
		
		// Retrieve list of Papers with Keywords:
		ArrayList<Paper> papers = getPapers();
		
		// Store Papers-Keywords-file:
		System.out.println("Processing Data ....");	int gotkeys = 0;
		for(int i = 0; i < papers.size(); i++) {
			Paper p = papers.get(i);	pkeywordsfile += p.number + "\t[" + p.title + "]\t" + p.year;
			for(int j = 0; j < papers.get(i).keywords.size(); j++)	pkeywordsfile += "\t[" + papers.get(i).keywords.get(j) + "]";
			pkeywordsfile += "\n";
			for(int j = 0; j < papers.get(i).authors.size(); j++)	{
				pkeywordsfile += papers.get(i).authors.get(j);	if(j != papers.get(i).authors.size()-1)	pkeywordsfile += "\t";
			}
			if(i != papers.size()-1)	pkeywordsfile += "\n";
			if(papers.get(i).keywords.size() != 0)	gotkeys++;
		}
		
		System.out.println("Found Keywords for " + gotkeys + " of " + papers.size() + " Papers.");
		writeToNewFile(pkeywordspath, pkeywordsfile);	
	}
}
