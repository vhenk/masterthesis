package dblp_dataset.venue_recommendation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class RetrieveData {
	static String authorkeyfile = "";	static String simfile = "";	
	static String ntdirpath_p = "";	static ArrayList<String> ntfiles_p = new ArrayList<>();
	static String ntdirpath_c = "";	static ArrayList<String> ntfiles_c = new ArrayList<>();
	
	static class Author {
		String id, uri;
		public Author(String id) {
			this.id = id;	this.uri = "";
			// Retrieve URI:
			try(BufferedReader br = new BufferedReader(new FileReader(authorkeyfile))) {
				String line;
				while((line = br.readLine()) != null) {
					if(!line.trim().equals("")) {
						int pos = line.indexOf("\t");	String idtmp = line.substring(0, pos);
						if(this.id.equals(idtmp)) {
							line = line.substring(pos+1);	line = line.substring(line.indexOf("\t"));
							this.uri = line.trim();	break;
						}
					}
				}
				br.close();
			} catch(IOException e)	{	e.printStackTrace();	}			
		}
	}
	
	static class Conference {
		String id, uri, venue, title, series;	int year;
		public Conference(String id, String uri) {
			this.id = id;	this.uri = uri;	this.venue = "";	// venue is assigned later
			this.title = "";	this.series = "";	this.year = -1;
			
			// Retrieve title, series, and year from DBLP data set:
			System.out.println("\t Loading Conference Edition " + this.id + " ....");
			boolean gotmatch = false;	boolean matchnomore = false;	int counter = 1;
			for(String f : ntfiles_c) {
				System.out.println("\t\t " + this.id + " (File " + counter + ") ..");
				try(BufferedReader br = new BufferedReader(new FileReader(f))) {
					String line;
					while((line = br.readLine()) != null) {
						if(!line.trim().equals("")) {	
							String yeartmp = "";
							// Extract subject, predicate and object in each line:
							String subject = line.substring(1, line.indexOf(">"));
							line = line.substring(line.indexOf(">")+1).trim();
							String predicate = line.substring(1, line.indexOf(">"));
							line = line.substring(line.indexOf(">")+1).trim();
							String object = line.substring(0, line.lastIndexOf(".")).trim();
							if(object.indexOf(">") != -1) object = object.substring(1, object.indexOf(">"));
							if(subject.equals(this.uri)) {
								gotmatch = true;
								if(predicate.indexOf("#title") != -1)	this.title = object;
								else if((predicate.indexOf("#publishedInSeries") != -1) && (predicate.indexOf("#publishedInSeriesVolume") == -1))	this.series = object;
								else if(predicate.indexOf("#yearOfPublication") != -1)	yeartmp = object;
								yeartmp = yeartmp.replaceAll("\"", "");	if(!yeartmp.equals(""))	this.year = Integer.parseInt(yeartmp);
							}
							else {	if(gotmatch)	matchnomore = true;	}
							if(matchnomore)	break;
						}
					}
					br.close();
				} catch(IOException e)	{	e.printStackTrace();	}
				if(matchnomore)	break;
				counter++;
			}
			this.title = this.title.replaceAll("\"", "");	this.series = this.series.replaceAll("\"", "");		
		}
	}
	
	static class Venue {
		String id, uri, name; ArrayList<Conference> conferences;
		public Venue(String id, String uri) {
			this.id = id;	this.uri = uri;	this.conferences = new ArrayList<>();	int value = 10;
			while(this.name == null)	{	this.name = setName(value);	value *= 10;	}
		}
		
		private String setName(int count) {
			String resultname = null;
			URL url = null;
			try {
				url = new URL("https://dblp2.uni-trier.de/search/venue/api?q=" + this.uri + "&h=" + count + "&format=json");
				System.out.println("Retrieving Name of Venue " + id + "\t" + url.toString() + "....");
			} catch (MalformedURLException e1) {	e1.printStackTrace();	}
			try(
				InputStream is = url.openStream();	JsonReader reader = Json.createReader(is)) {
				JsonObject obj = reader.readObject();
				JsonArray hits = obj.getJsonObject("result").getJsonObject("hits").getJsonArray("hit");
				if(hits != null) {
					for(JsonObject result : hits.getValuesAs(JsonObject.class)) {
						JsonObject info = result.getJsonObject("info");
						String urltmp = info.getString("url");
						if(urltmp.equals("https://dblp.org/db/conf/" + this.uri + "/"))	{
							resultname = info.getString("venue");	break;
						}
					}
				}
				else resultname = "";
			} catch(Exception e) {	e.printStackTrace();	}
			return resultname;
		}
	}
	
	public static ArrayList<Author> getAuthors() {
		System.out.println("Retrieving relevant authors ....");
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(simfile))) {
			String line;	int idx = 0;
			while((line = br.readLine()) != null) {
				boolean match = false;
				if(idx == 0)	{	
					match = false; String idtmp = line.trim();
					for(Author a : authors) {	if(a.id.equals(idtmp))	match = true;	}
					if(!match)	authors.add(new Author(idtmp));
					idx++;	
				}
				else if(idx == 1) {
					while(!line.trim().equals("")) {
						int pos = line.indexOf("\t");
						if(pos == -1) {
							match = false; String idtmp = line.trim();
							for(Author a : authors) {	if(a.id.equals(idtmp))	match = true;	}
							if(!match)	authors.add(new Author(idtmp));	line = "";
						}
						else {
							match = false; String idtmp = line.substring(0, pos);
							for(Author a : authors) {	if(a.id.equals(idtmp))	match = true;	}
							if(!match)	authors.add(new Author(idtmp));	line = line.substring(pos+1);
						}
					}
					idx++;
				}
				else	idx = 0;
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authors;
	}
		
	public static String[] getConfURIs(Author author) {
		System.out.println("Retrieving conferences for author " + author.id + " ....");
		ArrayList<String> tmp = new ArrayList<>();	int anz = ntfiles_p.size();
		if(author.uri.equals(""))	return null;
		for(int f = 0; f < anz; f++) {
			int ftmp = f+1;	System.out.println("\t" + author.id + "\tProcessing File " + ftmp + "/" + anz + " ....");
			try(BufferedReader br = new BufferedReader(new FileReader(ntfiles_p.get(f)))) {
				String line;	String conftmp = "";
				while((line = br.readLine()) != null) {
					if(!line.trim().equals("")) {
						// Extract subject, predicate and object in each line:
						String subject = line.substring(1, line.indexOf(">"));
						line = line.substring(line.indexOf(">")+1).trim();
						String predicate = line.substring(1, line.indexOf(">"));
						line = line.substring(line.indexOf(">")+1).trim();
						String object = line.substring(0, line.lastIndexOf(".")).trim();
						if(object.indexOf(">") != -1) object = object.substring(1, object.indexOf(">"));
						
						if((object.equals(author.uri)) && (predicate.indexOf("#authoredBy") != -1)) conftmp = subject;
						if((subject.equals(conftmp)) && (predicate.indexOf("#publishedAsPartOf") != -1))	tmp.add(object);
					}
				}
				br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
		Set<String> cleanup = new HashSet<>();	cleanup.addAll(tmp);
		if(cleanup.size() == 0)	return null;
		String[] confURIs = new String[cleanup.size()];	int i = 0;
		for(String s : cleanup) {	confURIs[i] = s;	i++;	}		
		return confURIs;
	}
	
	public static ArrayList<Venue> retrieveVenues(ArrayList<Conference> conferences) {
		ArrayList<Venue> venues = new ArrayList<>();	int counter = 1;
		for(Conference c : conferences) {
			String tmp = c.uri.substring(0, c.uri.lastIndexOf("/"));	tmp = tmp.substring(tmp.lastIndexOf("/")+1);
			boolean exists = false;
			for(Venue v : venues) {	if(v.uri.equals(tmp))	{	exists = true;	c.venue = v.id;	}	}
			if(!exists) {	String vid = "V" + counter;	counter++;	venues.add(new Venue(vid, tmp));	c.venue = vid;	}
			
			// Add conference to conference list of venue:
			for(Venue v : venues)	{	if(v.id.equals(c.venue))	v.conferences.add(c);	}
		}
		return venues;
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
		System.out.println("Name of file with author keys: ");	authorkeyfile = reader.nextLine();
		System.out.println("Name of authors similarity file: ");	simfile = reader.nextLine();
		System.out.println("Name of directory with .nt-files (papers): ");	ntdirpath_p = reader.nextLine();	
		System.out.println("Name of directory with .nt-files (conferences): ");	ntdirpath_c = reader.nextLine();	reader.close();
		if(authorkeyfile.indexOf(".txt") == -1)	authorkeyfile += ".txt";	if(simfile.indexOf(".txt") == -1)	simfile += ".txt";

		// Get list of .nt-files (papers):
		File ntdir = new File(ntdirpath_p);	
		if(!ntdir.isDirectory())	{	System.out.println(ntdirpath_p + " is not a directory. Program exits.");	System.exit(0);	}
		File[] list = ntdir.listFiles();
		for(File f : list)	{
			if(!f.getName().substring(0, 1).equals("."))	ntfiles_p.add(ntdirpath_p + "/" + f.getName());
		}
		
		// Get list of .nt-files(conferences):
		File ntdir2 = new File(ntdirpath_c);	
		if(!ntdir2.isDirectory())	{	System.out.println(ntdirpath_c + " is not a directory. Program exits.");	System.exit(0);	}
		File[] list2 = ntdir2.listFiles();
		for(File f : list2)	{
			if(!f.getName().substring(0, 1).equals("."))	ntfiles_c.add(ntdirpath_c + "/" + f.getName());
		}
		
		// Output-files:
		String authorconfspath = "authorconfs.txt"; String authorconfsfile = "";
		String conferencepath = "conferences.txt";	String conferencefile = "";
		String venuespath = "venues.txt";	String venuesfile = "";
		
		// Association Authors - Conference Editions (URIs):
		ArrayList<Author> authors = getAuthors();
		HashMap<String, String[]> authorConfURIs = new HashMap<>();	ArrayList<String> allconfs = new ArrayList<>();
		for(Author a : authors)	{
			String[] confstmp = getConfURIs(a);	authorConfURIs.put(a.id, confstmp);	
			if(confstmp != null)	{for(String s : confstmp)	allconfs.add(s);	}
		}
		
		// Retrieve list of conferences:
		Set<String> cleanup = new HashSet<>();	cleanup.addAll(allconfs);	allconfs.clear();
		allconfs.addAll(cleanup);	System.out.println("Number of conference editions: " + allconfs.size());
		Collections.sort(allconfs);
		ArrayList<Conference> conferences = new ArrayList<>();	int counter = 1;
		for(String c : allconfs) {	conferences.add(new Conference("CE"+counter, c));	counter++;	}
		
		// Creating Authors + Conference Editions Output-file:
		for(int i = 0; i < authors.size(); i++) {
			String[] conf = authorConfURIs.get(authors.get(i).id);
			authorconfsfile += authors.get(i).id;
			for(String c : conf) {
				String cid = "";
				for(Conference ce : conferences) {	if(ce.uri.equals(c))	{	cid = ce.id;	break;	}	}
				authorconfsfile += "\t" + cid;
			}
			if(i != authors.size()-1)	authorconfsfile += "\n";
		}
		writeToNewFile(authorconfspath, authorconfsfile);
		
		// Creating Venues Output-file:
		ArrayList<Venue> venues = retrieveVenues(conferences);
		for(int i = 0; i < venues.size(); i++) {
			venuesfile += venues.get(i).id + "\t" + venues.get(i).uri + "\t[" + venues.get(i).name + "]";
			for(Conference c : venues.get(i).conferences)	venuesfile += "\t" + c.id;
			if(i != venues.size()-1)	venuesfile += "\n";
		}
		writeToNewFile(venuespath, venuesfile);
	
		// Creating Conferences Output-file:
		for(int i = 0; i < conferences.size(); i++) {
			conferencefile += conferences.get(i).id + "\t" + conferences.get(i).uri + "\t" + conferences.get(i).venue + "\t" + conferences.get(i).year + "\t[" + conferences.get(i).title + "]\t[" + conferences.get(i).series + "]";
			if(i != conferences.size()-1)	conferencefile += "\n";
		}
		writeToNewFile(conferencepath, conferencefile);
	}
}
