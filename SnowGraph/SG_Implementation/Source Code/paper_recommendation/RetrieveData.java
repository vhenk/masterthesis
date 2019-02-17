package scigraph_dataset.paper_recommendation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
	static String sgpaperspath, tmpfile, s2paperspath;
	
	static class Paper {
		String title, sg_id, doi; int number, year;	ArrayList<String> authors, keywords;
		public Paper(int number, String title, int year, ArrayList<String> authors) {
			this.number = number;	this.title = title;	this.year = year;	
			this.authors = authors;	this.keywords = new ArrayList<>();
			
			// Retrieve Keywords:	
			System.out.println("\tRetrieving Keywords for Paper " + this.number + " ....");
						
			// Retrieve Scigraph-ID:
			try(BufferedReader br = new BufferedReader(new FileReader(sgpaperspath))) {
				String line;	int counter = 0;
				while((line = br.readLine()) != null) {
					counter++;
					if(counter == this.number)	{
						this.sg_id = line.substring(0, line.indexOf("\t"));	break;
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			
			// Retrieve DOI:
			try(BufferedReader br = new BufferedReader(new FileReader(tmpfile))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("/things/book-chapters/" + this.sg_id + ">") != -1)	{
						int pos = line.indexOf("/core/doi>") + 12;
						this.doi = line.substring(pos, line.length() - 3);	break;
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			
			// Get S2-ID:
			String s2ID = "";	String titlecheck = this.title.replaceAll("[^a-zA-Z0-9]", "");
			try(BufferedReader br = new BufferedReader(new FileReader(s2paperspath))) {
				String line;
				while((line = br.readLine()) != null) {
					try(JsonReader reader = Json.createReader(new StringReader(line));) {
						JsonObject obj = reader.readObject();	reader.close();
						String titletmp = obj.getString("title");
						String titletmpcheck = titletmp.replaceAll("[^a-zA-Z0-9]", "");
						String doitmp = obj.getString("doi");
						
						if((this.doi.equals(doitmp)) || (titletmpcheck.toUpperCase().equals(titlecheck.toUpperCase()))) {
							s2ID = obj.getString("id");	break;
						}
					} catch(Exception e) {	e.printStackTrace();	}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			
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
	
	static class Author {
		String id, name; ArrayList<Integer> papers;
		public Author(String id, String name, ArrayList<Paper> allpapers) {
			this.id = id;	this.name = name;
		
			// Get papers from list with all papers:
			this.papers = new ArrayList<>();
			for(Paper p : allpapers)	if(p.authors.contains(this.id))	this.papers.add(p.number);
		}
	}

	private static void filterDOIs(ArrayList<String> files, String doifile) {
		System.out.println("Filtering DOIs in SG-files ....");
		for(String file : files) {
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("/core/doi>") != -1)	appendToFile(doifile, line + "\n");
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	private static ArrayList<Paper> retrievePapers(String path) {
		System.out.println("Retrieve list of Papers ....");
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String numtmp = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("[") + 1);	String title = line.substring(0, line.indexOf("\t")-1);
					line = line.substring(line.indexOf("\t")+1); line = line.substring(line.indexOf("\t")+1);
					String yeartmp = line.substring(0, line.indexOf("\t"));	
					int number = -1;	int year = -1;
					try {	number = Integer.parseInt(numtmp);	year = Integer.parseInt(yeartmp);	}	catch(Exception e) {}
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
	
	private static ArrayList<Author> retrieveAuthors(String path, ArrayList<Paper> allpapers) {
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String idtmp = line.substring(0, line.indexOf("\t"));
					String nametmp = line.substring(line.indexOf("\t") + 1);
					authors.add(new Author(idtmp, nametmp, allpapers));
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authors;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	private static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");	
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Input files:
		Scanner reader = new Scanner(System.in);
		System.out.println("Name of Authors-key-map-file: ");	String authorskeypath = reader.nextLine();
		System.out.println("Name of Papers-file: ");	String paperspath = reader.nextLine();
		System.out.println("Name of SG-Papers-file: ");	sgpaperspath = reader.nextLine();
		System.out.println("Name of S2-Papers-file: ");	s2paperspath = reader.nextLine();
		reader.close();
		if(authorskeypath.indexOf(".txt") == -1)	authorskeypath += ".txt";
		if(paperspath.indexOf(".txt") == -1)	paperspath += ".txt";
		if(sgpaperspath.indexOf(".txt") == -1)	sgpaperspath += ".txt";
		if(s2paperspath.indexOf(".txt") == -1)	s2paperspath += ".txt";
		
		ArrayList<String> sgdatapaths = new ArrayList<>();	String path = "data_sg/SciGraph-Data/";
		sgdatapaths.add(path + "scigraph2002.nt");	sgdatapaths.add(path + "scigraph2006.nt");
		sgdatapaths.add(path + "scigraph2009.nt");	sgdatapaths.add(path + "scigraph2011.nt");
		sgdatapaths.add(path + "scigraph2012.nt");	sgdatapaths.add(path + "scigraph2013.nt");
		sgdatapaths.add(path + "scigraph2014.nt");	sgdatapaths.add(path + "scigraph2015.nt");
		
		// Output-files:
		String authorspath = "authors.txt";	String authorsfile = "";
		String pkeywordspath = "papers_keywords.txt";	String pkeywordsfile = "";
		
		tmpfile = "data_sg/tmp/sgdois.txt";	
		filterDOIs(sgdatapaths, tmpfile);
		
		// Retrieve Papers and Authors:
		ArrayList<Paper> papers = retrievePapers(paperspath);
		ArrayList<Author> authors = retrieveAuthors(authorskeypath, papers);
		
		// Store Authors-file:
		System.out.println("Processing Data - generating authors.txt ....");
		for(int i = 0; i < authors.size(); i++) {
			Author a = authors.get(i);	authorsfile += a.id + "\t[" + a.name + "]";
			for(int j = 0; j < a.papers.size(); j++)	authorsfile += "\t" + a.papers.get(j);
			if(i != authors.size() - 1)	authorsfile += "\n";			
		}
		writeToNewFile(authorspath, authorsfile);
		
		// Store Papers-Keywords-file:
		System.out.println("Processing Data  - generating papers_keywords.txt ....");	int gotkeys = 0;
		for(int i = 0; i < papers.size(); i++) {
			Paper p = papers.get(i);	pkeywordsfile += p.number + "\t[" + p.title + "]\t" + p.year;
			for(int j = 0; j < papers.get(i).keywords.size(); j++) 
				pkeywordsfile += "\t[" + papers.get(i).keywords.get(j) + "]";
			pkeywordsfile += "\n";
			for(int j = 0; j < papers.get(i).authors.size(); j++)	{
				pkeywordsfile += papers.get(i).authors.get(j);
				if(j != papers.get(i).authors.size()-1)	pkeywordsfile += "\t";
			}
			if(i != papers.size()-1)	pkeywordsfile += "\n";
			if(papers.get(i).keywords.size() != 0)	gotkeys++;
		}
		System.out.println("Found Keywords for " + gotkeys + " of " + papers.size() + " Papers.");
		writeToNewFile(pkeywordspath, pkeywordsfile);
		
		File tmp1 = new File(tmpfile);	tmp1.delete();
	}
}
