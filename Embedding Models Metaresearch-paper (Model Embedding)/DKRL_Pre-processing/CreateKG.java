import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateKG {
	static HashMap<String, String> pyears;

	public static HashMap<String, String> retrieveYears() {
		System.out.println("Retrieving Years of Papers ....");
		HashMap<String, String> papers = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader("data/papers_keywords.txt"))) {
			String line;	int idx = 0;
			while((line = br.readLine()) != null) {
				if(idx == 0) {
					if(line.indexOf("\t") != -1) {
						String pnr = line.substring(0, line.indexOf("\t"));
						line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
						String year = "";
						if(line.indexOf("\t") == -1)	year = line.trim();	
						else	year = line.substring(0, line.indexOf("\t"));
						papers.put(pnr, year);
					}
					idx++;
				}
				else	idx = 0;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return papers;
	}
	
	public static String getAuthPapers() {
		System.out.println("Retrieving Triples for Papers and Conferences ....");
		String triples_auth_p_c = "";
		try(BufferedReader br = new BufferedReader(new FileReader("kg_authors_p.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				ArrayList<String> conf_years = new ArrayList<>();
				if(line.indexOf("\t") != -1) {
					String aID = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
					ArrayList<String> papers = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							papers.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	papers.add(line.trim());	line = "";	}
					}
					for(String p : papers)	{
						triples_auth_p_c += aID + "\t/author/publication/paper\tP" + p + "\n";	conf_years.add(pyears.get(p));
					}
					Set<String> cleanup = new HashSet<>();	cleanup.addAll(conf_years);	conf_years.clear();	conf_years.addAll(cleanup);
					for(String c : conf_years) 
						triples_auth_p_c += aID + "\t/author/publication/paper/publication_location/venue\tISWC" + c + "\n";	
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return triples_auth_p_c;
	}
	
	public static String getCoAuthors() {
		String triples_co_auth = "";	System.out.println("Retrieving Co-Authors ....");
		HashMap<String, ArrayList<String>> authors = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_authors_p.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
					ArrayList<String> papers = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							papers.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	papers.add(line.trim());	line = "";	}
					}
					authors.put(id, papers);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		for(String k1 : authors.keySet()) {
			ArrayList<String> k1papers = authors.get(k1);
			ArrayList<String> coauthors = new ArrayList<>();
			for(String k2 : authors.keySet()) {
				if(!k1.equals(k2)) {
					ArrayList<String> k2papers = authors.get(k2);
					for(String p : k1papers)	if(k2papers.contains(p))	coauthors.add(k2);	
				}
				Set<String> cleanup = new HashSet<>();	cleanup.addAll(coauthors); coauthors.clear();	coauthors.addAll(cleanup);
			}	
			for(String ca : coauthors)	triples_co_auth += k1 + "\t/author/collaboration/coauthor\t" + ca + "\n";
		}		
		return triples_co_auth;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		/*
		pyears = retrieveYears();
		// Author-Entity	/author/publication/paper	Paper-Entity
		// Author-Entity	/author/publication/paper/publication_location/venue	Venue-Entity
		String triples_auth_p_c = getAuthPapers(); 	appendToFile("kg_triples.txt", triples_auth_p_c);
		System.out.println("Triples for /author/publication/paper Relationship stored!");
		System.out.println("Triples for /author/publication/paper/publication_location/venue Relationship stored!");
		
		// Author-Entity	/author/collaboration/coauthor	Author-Entity
		String triples_co_auth = getCoAuthors();	appendToFile("kg_triples.txt", triples_co_auth);
		System.out.println("Tripes for /author/collaboration/coauthor Relationship stored!");
		*/
		
		/*
		// Randomize Triple-Order
		ArrayList<String[]> triples = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_triples.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String subject = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String relation = line.substring(0, line.indexOf("\t"));	String object = line.substring(line.indexOf("\t")+1).trim();
					triples.add(new String[] {subject, relation, object});
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		Integer[] helplist = new Integer[triples.size()];	for(int i = 0; i < triples.size(); i++)	helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		for(int num : listtmp) {
			appendToFile("train.txt", triples.get(num)[0] + "\t" + triples.get(num)[1] + "\t" + triples.get(num)[2] + "\n");
		}
		*/
	}
	
}
