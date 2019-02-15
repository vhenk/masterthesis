import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class Test {

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
		
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
	
	public static ArrayList<Collaboration> getAllCollaborations() {
		ArrayList<Collaboration> collaborations = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("data/papers_keywords.txt"))) {
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
	
	public static boolean publishedPreviously(String authorid, ArrayList<Collaboration> coOther) {
		boolean published = false;
		for(Collaboration c : coOther) {
			if((c.author1.equals(authorid)) || (c.author2.equals(authorid)))	published = true;
			if(published)	break;
		}
		return published;
	}
	
	public static void main(String[] args) {
		ArrayList<Collaboration> collaborations = getAllCollaborations();

		// Get collaborations before 2016:
		ArrayList<Collaboration> coOther = new ArrayList<>();
		for(Collaboration c : collaborations)	if(!c.year.equals("2016"))	coOther.add(c);
		//for(Collaboration c : coOther)	System.out.println(c.author1 + "\t" + c.author2 + "\t" + c.year);
		//System.out.println(coOther.size());
		
		// Get collaborations in 2016 and NOT in coOther:
		ArrayList<Collaboration> co2016 = new ArrayList<>();
		for(Collaboration c : collaborations) {
			if((c.year.equals("2016")) && (!c.existsIn(coOther, false)))	co2016.add(c);
		}
		// for(Collaboration c : co2016)	System.out.println(c.author1 + "\t" + c.author2 + "\t" + c.year);
		// System.out.println(co2016.size());
		/*
		ArrayList<String> authors = new ArrayList<>();
		for(Collaboration c : co2016) {	authors.add(c.author1);	authors.add(c.author2);	}
		Set<String> cleanup = new HashSet<>();	cleanup.addAll(authors);	authors.clear(); authors.addAll(cleanup);
		int counter = 0;
		for(String a : authors) {
			if(publishedPreviously(a, coOther))	System.out.println(a);	counter++;
		}
		System.out.println(counter);*/
		
		ArrayList<String> aWithK = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("dkrl_data/entityWords.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	aWithK.add(line.substring(0, line.indexOf("\t")));
			}
		} catch(IOException e) {	e.printStackTrace();	}
		
		ArrayList<String> triples = new ArrayList<>();
		for(Collaboration c : co2016) {	
			if((publishedPreviously(c.author1, coOther)) && (publishedPreviously(c.author2, coOther)))	{
				String testdata = c.author1 + "\t/author/collaboration/coauthor\t" + c.author2;
				triples.add(testdata);
			}
		}
		// Shuffle triples:
		Integer[] helplist = new Integer[triples.size()];
		for(int i = 0; i < triples.size(); i++)	helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		for(int num : listtmp)	appendToFile("test_new.txt", triples.get(num) + "\n");
		
		// Training-Set:
		ArrayList<String> training = new ArrayList<>();
		for(Collaboration c : coOther) {
			String trainingdata = c.author1 + "\t/author/collaboration/coauthor\t" + c.author2;
			training.add(trainingdata);
		}
		// Shuffle triples:
		Integer[] helplist2 = new Integer[training.size()];
		for(int i = 0; i < training.size(); i++)	helplist2[i] = i;
		List<Integer> listtmp2 = Arrays.asList(helplist2);	Collections.shuffle(listtmp2);
		for(int num : listtmp2)	appendToFile("training_new.txt", training.get(num) + "\n");
		
	}
 }
