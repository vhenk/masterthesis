package scigraph_dataset.venue_recommendation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class RetrieveData {
	static String sgconfpath, sgbookpath, conferencepath; static int ccount = 0;
	static HashMap<String, Conference> confdict;	static HashMap<String, Venue>	venuedict;
	
	static class Conference {
		String sg_id, name, venue, id;	int year;
		public Conference(String sg_id) {
			this.sg_id = sg_id;	this.name = "";	this.year = -1;	String venuetmp = "";
			
			try(BufferedReader br = new BufferedReader(new FileReader(sgconfpath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("/things/conferences/" + sg_id + ">") != -1) {
						if(line.indexOf("/ontologies/core/name>") != -1) {
							int pos = line.indexOf("/ontologies/core/name") + 24;
							this.name = line.substring(pos, line.length() - 3);
						}
						else if(line.indexOf("ontologies/core/year>") != -1) {
							int pos = line.indexOf("/ontologies/core/year") + 24;
							String ytmp = line.substring(pos, pos + 4);
							try {	year = Integer.parseInt(ytmp);	}	catch(Exception e) {} 
						}
						else if(line.indexOf("ontologies/core/hasConferenceSeries>") != -1) {
							int pos = line.indexOf("/things/conference-series/") + 26;
							venuetmp = line.substring(pos, line.length() - 3);
						}
					}
					if((year != -1) && (!this.name.equals("") && (!venuetmp.equals(""))))	break;
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			
			String checkconf = isContainedInList(confdict);
			if(checkconf.equals(""))	{
				int num = confdict.size() + 1;	this.id = "CE" + num;	confdict.put(this.id, this);	
			}
			else	this.id = checkconf;
			
			Venue vtmp = new Venue(venuetmp);	this.venue = vtmp.id;
			
			if(checkconf.equals("")) {
				String cline = this.id + "\t" + this.sg_id + "\t" + venue + "\t" + this.year + "\t[" + this.name + "]\t[" + vtmp.acronym + "]\n";
				appendToFile(conferencepath, cline);	
				if((ccount % 15) == 0)	System.out.print("\t" + this.id + "\n");	else System.out.print("\t" + this.id);	ccount++;
			}
		}
		
		private String isContainedInList(HashMap<String, Conference> list) {
			String contained = "";
			for(String key : list.keySet()) {
				Conference ctmp = list.get(key);
				if(ctmp.sg_id.equals(this.sg_id))	{	contained = key;	break;	}
			}
			return contained;
		}
	}
	
	static class Venue {
		String sg_id, id, acronym, name;
		public Venue(String sg_id) {
			this.sg_id = sg_id;	this.acronym = "";	this.name = "";
			try(BufferedReader br = new BufferedReader(new FileReader(sgconfpath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("/things/conference-series/" + sg_id + ">") != -1) {
						if(line.indexOf("/ontologies/core/name>") != -1) {
							int pos = line.indexOf("/ontologies/core/name") + 24;
							this.name = line.substring(pos, line.length() - 3);
						}
						else if(line.indexOf("ontologies/core/dblpId>") != -1) {
							int pos = line.indexOf("/ontologies/core/dblpId") + 26;
							this.acronym = line.substring(pos, line.length() - 3);
						}
					}
					if((!this.name.equals("") && (!this.acronym.equals(""))))	break;
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			
			String checkvenue = isContainedInList(venuedict);
			
			if(checkvenue.equals("")) {
				int num = venuedict.size() + 1;	this.id = "V" + num;	venuedict.put(this.id, this);
			}
			else	this.id = checkvenue;
		}
		
		private String isContainedInList(HashMap<String, Venue> list) {
			String contained = "";
			for(String key : list.keySet()) {
				Venue vtmp = list.get(key);
				if(vtmp.sg_id.equals(this.sg_id))	{	contained = key;	break;	}
			}
			return contained;
		}
	}
	
	private static HashMap<String, ArrayList<String>> retrieveAuthBooks(String path) {
		System.out.println("Retrieving Book-IDs ....");
		HashMap<String, ArrayList<String>> authbooks = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String atmp = line.substring(0, line.indexOf("\t"));
					String booktmp = line.substring(line.indexOf("\t")+1);
					if(authbooks.containsKey(atmp)) {
						ArrayList<String> list = authbooks.get(atmp);
						if(!list.contains(booktmp)) {
							list.add(booktmp);	authbooks.replace(atmp, list);
						}
					}
					else {
						ArrayList<String> list = new ArrayList<>();	list.add(booktmp);
						authbooks.put(atmp, list);
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authbooks;
	}
	
	private static HashMap<String, Conference> retrieveBookConfs(ArrayList<String> booklist) {
		System.out.println("Retrieving Conferences ....\n\t(this may take a while!)\n");
		HashMap<String, Conference> bookconfs = new HashMap<>();
		for(String book : booklist) {
			String confID = getConfID(book);
			if(!confID.equals("")) bookconfs.put(book, new Conference(confID));
		}
		return bookconfs;
	}
	
	private static String getConfID(String bookID) {
		String confID = "";
		try(BufferedReader br = new BufferedReader(new FileReader(sgbookpath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("/core/hasConference>") != -1) {	
					if(line.indexOf("/things/books/" + bookID + ">") != -1) {
						int pos = line.indexOf("/things/conferences/") + 20;
						confID = line.substring(pos, line.length() - 3);	break;
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return confID;
	}
	
	private static void cleanUpFile(String path) {
		HashMap<String, String>	file = new HashMap<>();	String prefix = "";
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String key = line.substring(0, line.indexOf("\t"));
					if(prefix.equals(""))	prefix = key.replaceAll("[^A-Z]", "");
					if(!file.containsKey(key))	file.put(key, line);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		File rmfile = new File(path);	rmfile.delete();
		
		// Get the maximum key-value in file:
		int maximum = 0;
		for(String key : file.keySet()) {
			String tmp = key.replaceAll("[^0-9]", "");
			int keynum = -1;
			try {	keynum = Integer.parseInt(tmp);	} catch(Exception e) {}
			if(keynum > maximum)	maximum = keynum;
		}
		
		// Store data of HashMap in file:
		for(int i = 1; i <= maximum; i++) {
			String testkey = prefix + i;
			if(file.containsKey(testkey))	appendToFile(path, file.get(testkey) + "\n");
		}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Process Input-files:
		Scanner reader = new Scanner(System.in);
		System.out.println("SG_Authors-Books-file: ");	String authbookspath = reader.nextLine();
		System.out.println("SG_conference data: ");	sgconfpath = reader.nextLine();
		System.out.println("SG_book data: ");	sgbookpath = reader.nextLine();
		reader.close();
		if(authbookspath.indexOf(".txt") == -1)	authbookspath += ".txt";
		if(sgconfpath.indexOf(".nt") == -1)	sgconfpath += ".nt";
		if(sgbookpath.indexOf(".nt") == -1)	sgbookpath += ".nt";
		
		// Output-files:
		conferencepath = "conferences.txt";	String venuepath = "venues.txt";	String authconfspath = "authorconfs.txt";

		venuedict = new HashMap<>();	confdict = new HashMap<>();
		
		// Get books of each author:
		HashMap<String, ArrayList<String>> auth_books = retrieveAuthBooks(authbookspath);
		
		// Get Conference of each book:
		ArrayList<String> booklist = new ArrayList<>();
		for(String key : auth_books.keySet()) {
			for(String b : auth_books.get(key))	if(!booklist.contains(b))	booklist.add(b);
		}
		HashMap<String, Conference> book_confs = retrieveBookConfs(booklist);
		
		// Print venues-file:
		for(String key : venuedict.keySet()) {
			String line = "";
			ArrayList<String> confs = new ArrayList<>();
			for(String c : confdict.keySet()) 	if(confdict.get(c).venue.equals(key))	confs.add(c);
			Venue venue = venuedict.get(key);
			line = key + "\t" + venue.acronym + "\t[" + venue.name + "]";
			for(String c : confs)	line += "\t" + c;
			appendToFile(venuepath, line + "\n");
		}
		
		// Print authors-file:
		for(String author : auth_books.keySet()) {
			String line = "";
			ArrayList<String> conferences = new ArrayList<>();
			for(String book : auth_books.get(author)) {
				if(book_confs.containsKey(book)) {
					Conference ctmp = book_confs.get(book);
					conferences.add(ctmp.id);
				}
			}
			if(conferences.size() > 0) {
				line += author;
				for(String c : conferences)	line += "\t" + c;
				appendToFile(authconfspath, line + "\n");
			}
		}
		
		// Clean-up/sort output-files:
		cleanUpFile(conferencepath);	cleanUpFile(venuepath);	cleanUpFile(authconfspath);
		
		
		
		
	}
}
