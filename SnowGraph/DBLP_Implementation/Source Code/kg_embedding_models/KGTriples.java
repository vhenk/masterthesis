package dblp_dataset.kg_embedding_models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KGTriples {	// 5)
	static ArrayList<String[]> trainingSet, testSet;
	static class Collaboration {
		String author1, author2, year;
		public Collaboration(String author1, String author2, String year) {
			this.author1 = author1;	this.author2 = author2; this.year = year;
		}
			
		public boolean existsIn(ArrayList<Collaboration> list, boolean considerYear) {
			boolean exists = false;
			if(considerYear) {
				for(Collaboration c : list) {
					if((c.author1.equals(this.author1)) && (c.author2.equals(this.author2)) && (c.year.equals(this.year))) exists = true;
					if((c.author2.equals(this.author1)) && (c.author1.equals(this.author2)) && (c.year.equals(this.year)))	exists = true;
					if(exists) break;
				}
			}
			else {
				for(Collaboration c : list) {
					if((c.author1.equals(this.author1)) && (c.author2.equals(this.author2))) exists = true;
					if((c.author2.equals(this.author1)) && (c.author1.equals(this.author2)))	exists = true;
					if(exists) break;
				}
			}
			return exists;
		}
	}
	
	public static ArrayList<Collaboration> getAllCollaborations(String paperkeywordspath) {
		ArrayList<Collaboration> collaborations = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(paperkeywordspath))) {
			String line; int idx = 0;	String year = "";
			while((line = br.readLine()) != null) {
				if(idx == 0) {
					if(line.indexOf("\t") != -1) {
						line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
						if(line.indexOf("\t") != -1)	year = line.substring(0, line.indexOf("\t"));
						else	year = line.trim();
					}
					idx++;
				}
				else {	// Line with Authors
					ArrayList<String> authors = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							authors.add(line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	authors.add(line.trim());	line = "";	}
					}
					for(String a : authors) {
						for(String a2 : authors) {
							if(!a.equals(a2)) {
								Collaboration ctmp = new Collaboration(a, a2, year);
								if(!ctmp.existsIn(collaborations, true))	collaborations.add(ctmp);
							}
						}
					}
					idx = 0;
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return collaborations;
	}
	
	public static ArrayList<String[]> getTrainingTriples(String kgauthorsppath, String paperkeywordspath, String kgtriplespath) {
		ArrayList<String[]> training = new ArrayList<>();
		/* Auth-Entity	/author/collaboration/coauthor	Auth-Entiy */
		ArrayList<Collaboration> collaborations = getAllCollaborations(paperkeywordspath);
		// Collaborations BEFORE 2016:
		ArrayList<Collaboration> coOther = new ArrayList<>();
		for(Collaboration c : collaborations)	if(!c.year.equals("2016"))	coOther.add(c);
		// Collaboration IN 2016 and NOT IN coOther:
		ArrayList<Collaboration> co2016 = new ArrayList<>();
		for(Collaboration c : collaborations) {
			if((c.year.equals("2016")) && (!c.existsIn(coOther, false)))	co2016.add(c);
		}
	
		// Get collaborations for Training:
		for(Collaboration c : coOther) {
			training.add(new String[] { c.author1, "/author/collaboration/coauthor", c.author2 });
		}
		System.out.println("Triples for /author/collaboration/coauthor Relationship retrieved!");
		
		/* Auth-Entity	/author/publication/paper	Paper-Entity
		   Auth-Entity /author/publication/paper/publication_location/venue	Venue-Entity */
		HashMap<String, String> paperyears = retrieveYears(paperkeywordspath);
		training.addAll(getPaperVenueTriples(paperyears, kgauthorsppath));
		System.out.println("Triples for /author/publication/paper Relationship retrieved!");
		System.out.println("Triples for /author/publication/paper/publication_location/venue Relationship retrieved!");
		
		/* Auth-Entity	/author/affiliation/organization	Organization-Entity */
		training.addAll(getFileContent(kgtriplespath, 3));
		System.out.println("Triples for /author/affiliation/organization Relationship retrieved!");

		return training;
	}
	
	public static boolean publishedPreviously(String authorid, ArrayList<Collaboration> coOther) {
		boolean published = false;
		for(Collaboration c : coOther) {
			if((c.author1.equals(authorid)) || (c.author2.equals(authorid)))	published = true;
			if(published)	break;
		}
		return published;
	}
	
	public static HashMap<String, String> retrieveYears(String paperkeywordspath) {
		System.out.println("Retrieving Years of Papers ....");
		HashMap<String, String> papers = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(paperkeywordspath))) {
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
	
	public static ArrayList<String[]> getPaperVenueTriples(HashMap<String, String> paperyears, String kgauthorsppath) {
		System.out.println("Retrieving Triples for Papers and Conferences ....");
		ArrayList<String[]> pvTriples = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(kgauthorsppath))) {
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
						pvTriples.add(new String[] { aID, "/author/publication/paper", "P" + p });	conf_years.add(paperyears.get(p));
					}
					Set<String> cleanup = new HashSet<>();	cleanup.addAll(conf_years);	conf_years.clear();	conf_years.addAll(cleanup);
					for(String c : conf_years) 
						pvTriples.add(new String[] { aID, "/author/publication/paper/publication_location/venue", "ISWC" + c });
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return pvTriples;
	}
	
	
	public static ArrayList<String[]> shuffleTriples(ArrayList<String[]> triples) {
		ArrayList<String[]> shuffled = new ArrayList<>();
		Integer[] helplist = new Integer[triples.size()];	for(int i = 0; i < triples.size(); i++) helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		for(int num : listtmp) {
			shuffled.add(new String[] { triples.get(num)[0], triples.get(num)[1], triples.get(num)[2] });
		}
		return shuffled;
	}
	
	public static void splitTriples(ArrayList<String[]> triples, String trainoutput, String testoutput) {			
		ArrayList<String[]> shuffled = shuffleTriples(triples);
		testSet = new ArrayList<>();	trainingSet = new ArrayList<>();
		
		for(int i = 0; i < 6000; i++)	testSet.add(shuffled.get(i));	
		System.out.println("Test-Set: " + testSet.size());
		
		for(int i = 6000; i < shuffled.size(); i++)	trainingSet.add(shuffled.get(i));	
		System.out.println("Train-Set: " + trainingSet.size());
		
		storeTriplesFile(trainingSet, trainoutput);	storeTriplesFile(testSet, testoutput);	
	}
	
	public static boolean isIdentic(String[] item1, String[] item2) {
		int size = item1.length;	if(item2.length != size)	return false;
		boolean same = true;
		for(int i = 0; i < size; i++) {	if(!item1[i].equals(item2[i]))	same = false;	}
		return same;
	}
	
	public static boolean isInList(ArrayList<String[]> list, String[] item) {
		boolean exists = false;
		for(String[] l : list) {	if(isIdentic(item, l))	{	exists = true; break;	}	}
		return exists;
	}
	
	public static ArrayList<String[]> removeDuplicates(ArrayList<String[]> list) {
		ArrayList<String[]> unique = new ArrayList<>(); int counter = 1;
		for(int i = 0; i < list.size(); i++) {	
			if((counter % 1000) == 0)	System.out.print("...");
			for(int j = 0; j < list.size(); j++) {
				if(i != j) {
					if(isIdentic(list.get(i), list.get(j)))	if(!isInList(unique, list.get(i)))	unique.add(list.get(i));
				}
			}
			counter++;
		}
		for(String[] l : list)	if(!isInList(unique, l))	unique.add(l);
		return unique;
	}
	
	public static ArrayList<String[]> getValidData(ArrayList<String[]> training) {
		ArrayList<String[]> tmp = shuffleTriples(training);
		ArrayList<String[]> valid = new ArrayList<>();
		for(int i = 0; i < 2000; i++)	valid.add(tmp.get(i));
		return valid;
	}
	
	public static ArrayList<String[]> convert2DKRL(ArrayList<String[]> triples) {
		ArrayList<String[]> converted = new ArrayList<>();
		for(String[] t : triples)	converted.add(new String[] { t[0], t[2], t[1] });
		return converted;
	}
	
	// ----------------------------------------------------
	
	public static ArrayList<String[]> getFileContent(String path, int columns) {
		ArrayList<String[]> list = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				int counter = 0;	String[] item = new String[columns];
				while((line.indexOf("\t") != -1) && (counter < columns)) {
					item[counter] = line.substring(0, line.indexOf("\t"));
					counter++;	line = line.substring(line.indexOf("\t")+1);
				}
				if((line.indexOf("\t") == -1) && (counter < columns))	item[counter] = line.trim();
				list.add(item);
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return list;	
	}
	
	public static void storeTriplesFile(ArrayList<String[]> triples, String filename) {
		String output = "";
		for(String[] t : triples) 	output += t[0] + "\t" + t[1] + "\t" + t[2] + "\n";
		writeToNewFile(filename, output);
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
		String paperkeywordspath = "data_dblp/paperrecommendations/papers_keywords.txt"; 
		String kgauthorsppath = "data_dblp/embd/kg_authors_p.txt";	String kgtriplespath = "data_dblp/embd/kg_triplesorg.txt";

		// Output-files:
		String trainpath = "data_dblp/embd/triples/train.tsv";	String testpath = "data_dblp/embd/triples/test.tsv";
		String dkrltrainpath = "data_dblp/embd/triples/dkrl_train.txt";	String dkrltestpath = "data_dblp/embd/triples/dkrl_test.txt";
		String dkrlvalidpath = "data_dblp/embd/triples/dkrl_valid.txt";

		// Generate Triples:
		ArrayList<String[]> triplesTraining = getTrainingTriples(kgauthorsppath, paperkeywordspath, kgtriplespath);
		System.out.println("Triples in Training: " + triplesTraining.size());
		ArrayList<String[]> unique = removeDuplicates(triplesTraining);
		System.out.println("Training after removing duplicates: " + unique.size());
		
		// Split into Training and Test-Sets:
		splitTriples(unique, trainpath, testpath);
		
		// Generate DKRL-Triple-Sets:
		ArrayList<String[]> traindata = convert2DKRL(trainingSet);	storeTriplesFile(traindata, dkrltrainpath);
		ArrayList<String[]> testdata = convert2DKRL(testSet);	storeTriplesFile(testdata, dkrltestpath);
		ArrayList<String[]> validdata = getValidData(traindata);	storeTriplesFile(validdata, dkrlvalidpath);
	}
}
