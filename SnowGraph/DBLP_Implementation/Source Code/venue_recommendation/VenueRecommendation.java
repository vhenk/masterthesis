package dblp_dataset.venue_recommendation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class VenueRecommendation {
	// Input-files:
	static String authorkeyspath = "";	static String authorconfspath = "";	static String suffix = "";
	
	// Output-files:
	static String recommendationspath = "";	static String recommendationsfile = "";
	static String recommendationsjspath = "";	static String recommendationsjsfile = "";
	
	// Lists for Reference:
	static HashMap<String, Conference> confslist = new HashMap<>();
	static HashMap<String, Venue> venueslist = new HashMap<>();
	static HashMap<String, Author> authorslist = new HashMap<>();
	
	static class Conference {
		String id, name, venue; int year;
		public Conference(String id, String name, String venue, int year) {
			this.id = id;	this.name = name;	this.venue = venue;	this.year = year;
		}
		public Conference(String id) {
			Conference c = confslist.get(id);
			this.id = id;	this.name = c.name;	this.venue = c.venue;	this.year = c.year;
		}
	}
	
	static class Venue {
		String id, name;	ArrayList<Conference> conferences;
		public Venue(String id, String name, ArrayList<Conference> conferences) {
			this.id = id;	this.name = name;	this.conferences = conferences;
		}
	}
	
	static class Author {
		String id, name;	ArrayList<Conference> conferences;	ArrayList<String[]> simauthors;
		public Author(String id, ArrayList<String[]> simauthors) {
			this.id = id;	this.simauthors = simauthors;
			// Get the author's name:
			try(BufferedReader br = new BufferedReader(new FileReader(authorkeyspath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						if(line.substring(0, line.indexOf("\t")).equals(this.id)) {
							line = line.substring(line.indexOf("\t")+1);	this.name = line.substring(0, line.indexOf("\t"));	break;
						}
					}
				}
				br.close();	
			} catch(IOException e) {	e.printStackTrace();	}
			
			// Get list with conferences:
			this.conferences = new ArrayList<>();
			try(BufferedReader br = new BufferedReader(new FileReader(authorconfspath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						if(line.substring(0, line.indexOf("\t")).equals(this.id)) {
							line = line.substring(line.indexOf("\t")+1);
							ArrayList<String> confIDs = new ArrayList<>();
							while(!line.trim().equals("")) {
								if(line.indexOf("\t") != -1) {
									confIDs.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
								}
								else {	confIDs.add(line.trim());	break;	}
							}
							for(String c : confIDs) {	Conference ctmp = confslist.get(c);	conferences.add(ctmp);	}
							break;
						}
					}
				}
				br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	public static boolean publishedInVenue(String authorid, String venueid) {
		boolean published = false;
		Author a = authorslist.get(authorid);
		for(Conference c : a.conferences) {	if(c.venue.equals(venueid))	published = true;	}
		return published;
	}
	
	public static ArrayList<String[]> publishedInConference(ArrayList<String[]> simauthors, String conferenceid) {
		ArrayList<String[]> authorids = new ArrayList<>();
		for(String[] sa : simauthors) {
			Author author = authorslist.get(sa[0]);
			boolean exists = false;
			for(Conference c : author.conferences) {	if(c.id.equals(conferenceid))	exists = true;	}
			if(exists)	authorids.add(sa);
		}
		return authorids;
	}
	
	public static void retrieveRecommendations(ArrayList<String> authors) {
		int counter = 0;	
		for(String s : authors) {
			Author a = authorslist.get(s);	ArrayList<String> simvenues = new ArrayList<>();	double maximum = 0;
			for(String[] x : a.simauthors) {
				// Get conferences of similar authors of a and maximum similarity value:
				ArrayList<Conference> atmpconf = authorslist.get(x[0]).conferences;
				// Add conferences to simconfs:
				for(Conference c1 : atmpconf) {
					boolean exists = false;
					for(String v : simvenues) {	if(c1.venue.equals(v))	exists = true;	}
					if((!publishedInVenue(a.id, c1.venue)) && (!exists))	simvenues.add(c1.venue);
				}
				double weight = 0;
				try { weight = Double.parseDouble(x[1]); } catch(Exception e) {}
				maximum = maximum + weight;
			}
				
			ArrayList<String[]> recommendations = new ArrayList<>();
			for(String v : simvenues)	{
				// Get list with weights of similar authors who published in this venue:
				ArrayList<Double> weights = new ArrayList<>();
				for(String[] sa : a.simauthors) {
					if(publishedInVenue(sa[0], v))	{
						double w = 0;	try {	w = Double.parseDouble(sa[1]);	} catch(Exception e) {}	weights.add(w);
					}
				}
					
				// Calculate weight for this venue:
				double value = 0; for(double d : weights)	value += d;	
				// Normalize value:
				value = value / maximum;
				// Store recommendation:
				String[] tmp = new String[2];	tmp[0] = v;	tmp[1] = value+""; recommendations.add(tmp);
			}
			
			// Sort recommendations by value:
			recommendations = sortRecommendations(recommendations);
			
			// Get recommendations-text-file:
			recommendationsfile += a.id + "\n";
			for(int i = 0; i < recommendations.size(); i++) {
				recommendationsfile += recommendations.get(i)[0];
				if(i != recommendations.size()-1)	recommendationsfile += "\t";
			}
			recommendationsfile += "\n";
			for(int i = 0; i < recommendations.size(); i++) {
				recommendationsfile += recommendations.get(i)[1];
				if(i != recommendations.size()-1)	recommendationsfile += "\t";
			}
			if(counter != authors.size()-1)	recommendationsfile += "\n";
			
			// Get recommendations-js-file:
			JsonArrayBuilder buildervenues = Json.createArrayBuilder();
			for(String[] r : recommendations) {			
				// Get conferences of this venue where similar authors published:
				ArrayList<Conference> processedconfs = new ArrayList<>();
				JsonArrayBuilder builderconfs = Json.createArrayBuilder();
				for(Conference c : venueslist.get(r[0]).conferences) {
					ArrayList<String[]> simauthorstmp = publishedInConference(a.simauthors, c.id);
					if((!processedconfs.contains(c)) && (simauthorstmp.size() > 0)) {
						processedconfs.add(c);
						JsonArrayBuilder buildersimauthors = Json.createArrayBuilder();
						for(String[] sa : simauthorstmp) {
							// Similar author content:
							double svalue = -1;
							try { svalue = Double.parseDouble(sa[1]); } catch(Exception e) {}
							
							JsonObject sa_js = Json.createObjectBuilder().add("id", sa[0])
									.add("name", authorslist.get(sa[0]).name).add("weight", svalue).build();
							buildersimauthors.add(sa_js);
						}
						JsonArray simauthorsjs = buildersimauthors.build();
						// Conference content:
						JsonObject conf_js = Json.createObjectBuilder().add("id", c.id).add("name", c.name)
								.add("year", c.year).add("simauthors", simauthorsjs).build();
						builderconfs.add(conf_js);
					}	
				}
				// Venue content:
				JsonArray confsjs = builderconfs.build();
				String vname = venueslist.get(r[0]).name;	if(vname.equals(""))	vname = r[0];
				JsonObject venue_js = Json.createObjectBuilder().add("venueid", r[0])
						.add("venuename", vname).add("conferences", confsjs).build();
				buildervenues.add(venue_js);
				
			}
			JsonArray venuesjs = buildervenues.build();
			JsonObject authorjs = Json.createObjectBuilder().add("id", a.id).add("name", a.name)
					.add("recommendations", venuesjs).build(); 
			
			recommendationsjsfile += "var VRec" + a.id + suffix + " = " + authorjs.toString() + ";";
			if(counter != authors.size()-1)	recommendationsjsfile += "\n";
			counter++;
		}
	}
	
	public static ArrayList<String[]> sortRecommendations(ArrayList<String[]> unsorted) {
		Object[] rectmp = new Object[unsorted.size()];
		for(int i = 0; i < unsorted.size(); i++) {
			if(i == 0)	rectmp[0] = unsorted.get(0);
			else {
				String[] rtmp = unsorted.get(i);	int idx = 0;
				while((idx < rectmp.length) && (rectmp[idx] != null)) {
					double weighttmp = 0;
					try {	weighttmp = Double.parseDouble(rtmp[1]);	} catch(Exception e) {}
					double weighttmp2 = 0;	String[] entrytmp = (String[]) rectmp[idx];
					try {	weighttmp2 = Double.parseDouble(entrytmp[1]);	} catch(Exception e) {}
					if(weighttmp >= weighttmp2)	{
						String[] rtmp2 = (String[]) rectmp[idx];	rectmp[idx] = rtmp;	rtmp = rtmp2;
					}
					idx++;	
				}
				if(idx != rectmp.length)	rectmp[idx] = rtmp;
			}
		}
		
		ArrayList<String[]> recommendations = new ArrayList<>();
		for(Object r : rectmp) {	recommendations.add((String[]) r);	}
		return recommendations;
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
		System.out.println("Name of conferences-file: ");	String confsfilepath = reader.nextLine();
		System.out.println("Name of venues-file: ");	String venuesfilepath = reader.nextLine();
		System.out.println("Name of similar authors-file: ");	String simauthorsfilepath = reader.nextLine();
		System.out.println("Name of author-keys-file: ");	authorkeyspath = reader.nextLine();
		System.out.println("Name of author-conferences-file: ");	authorconfspath = reader.nextLine();
		System.out.println("Suffix for Json-Objects: ");	suffix = reader.nextLine();
		reader.close();
		if(confsfilepath.indexOf(".txt") == -1)	confsfilepath += ".txt";
		if(venuesfilepath.indexOf(".txt") == -1)	venuesfilepath += ".txt";
		if(simauthorsfilepath.indexOf(".txt") == -1)	simauthorsfilepath += ".txt";
		if(authorkeyspath.indexOf(".txt") == -1)	authorkeyspath += ".txt";
		if(authorconfspath.indexOf(".txt") == -1)	authorconfspath += ".txt";
		recommendationspath = "v_recommendations_" + suffix + ".txt";	recommendationsjspath = "v_recommendations_" + suffix + ".js";
		
		// Obtain conferences-list:
		try(BufferedReader br = new BufferedReader(new FileReader(confsfilepath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String idtmp = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
					String venuetmp = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String yearstring = line.substring(0, line.indexOf("\t"));
					int yeartmp = -1;	try {	yeartmp = Integer.parseInt(yearstring);	} catch(Exception e) {};
					String titletmp = line.substring(line.indexOf("[")+1, line.indexOf("]"));
					confslist.put(idtmp, new Conference(idtmp, titletmp, venuetmp, yeartmp));
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Obtain venues-list:
		try(BufferedReader br = new BufferedReader(new FileReader(venuesfilepath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String idtmp = line.substring(0, line.indexOf("\t"));
					String titletmp = line.substring(line.indexOf("[")+1, line.indexOf("]"));
					line = line.substring(line.indexOf("]")+2);	
					ArrayList<Conference> confstmp = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							confstmp.add(new Conference(line.substring(0, line.indexOf("\t"))));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	confstmp.add(new Conference(line.trim()));	break;	}
					}
					venueslist.put(idtmp, new Venue(idtmp, titletmp, confstmp));
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Read similar-authors-file and obtain authors-list:
		ArrayList<String> relevantAuthors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(simauthorsfilepath))) {
			String line;	int idx = 0;	
			String aID = "";	ArrayList<String> sim = new ArrayList<>();	ArrayList<String> weights = new ArrayList<>();
			while((line = br.readLine()) != null) {
				if(idx == 0) {	aID = line.trim();	idx++;	}
				else if(idx == 1) {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							sim.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	sim.add(line.trim());	break;	}
					}
					idx++;
				}
				else {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							weights.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	weights.add(line.trim());	break;	}
					}
					ArrayList<String[]> simauthorstmp = new ArrayList<>();
					for(int i = 0; i < sim.size(); i++) {
						String[] tmp = new String[2];	tmp[0] = sim.get(i);	tmp[1] = weights.get(i);
						simauthorstmp.add(tmp);
					}
					authorslist.put(aID, new Author(aID, simauthorstmp));	relevantAuthors.add(aID);
					idx = 0;	sim = new ArrayList<>();	weights = new ArrayList<>();
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Add remaining (similar) authors to authorslist:
		ArrayList<String> authorsrest = new ArrayList<>();
		for(String key : authorslist.keySet()) {
			Author a = authorslist.get(key);	
			for(String[] s : a.simauthors)	{	if(!authorslist.containsKey(s[0]))	authorsrest.add(s[0]);	}
		}
		for(String x : authorsrest) {
			// Obtain similar authors of x:
			ArrayList<String[]> simauthorstmp = new ArrayList<>();
			for(String key : authorslist.keySet()) {
				Author a = authorslist.get(key);
				for(String[] s : a.simauthors) {
					if(s[0].equals(x))	{
						String[] tmp = new String[2];	tmp[0] = a.id;	tmp[1] = s[1];	simauthorstmp.add(tmp);
					}
				}
			}
			authorslist.put(x, new Author(x, simauthorstmp));
		}
		
		// Compute and store Recommendations:
		retrieveRecommendations(relevantAuthors);
		System.out.println("\n" + recommendationsfile + "\n");
		writeToNewFile(recommendationspath,recommendationsfile);
		writeToNewFile(recommendationsjspath, recommendationsjsfile);	
	}
}
