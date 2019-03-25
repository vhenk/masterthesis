package dblp_dataset.kg_embedding_models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class KGAuthors {	// 2)
static String stopwordspath;
	
	static class Paper {
		String title, description; ArrayList<String> keywords; int year, number;
		public Paper(int number, String title, int year, ArrayList<String> keywords) {
			this.number = number;	this.title = title;	this.keywords = keywords; this.year = year;
			this.description = " " + title;
			for(String k : keywords) {
				String ktmp = k.replaceAll("[^a-zA-Z ]", "");	ktmp = ktmp.replaceAll(" ", "_");	description += " " + ktmp;
			}
			this.description = removeStopWords(description);
			// Remove duplicates in description:
			String[] words = description.split(" ");	ArrayList<String> wordsfiltered = new ArrayList<>();
			Set<String> cleanup = new HashSet<>();	for(String w : words)	cleanup.add(w);	wordsfiltered.addAll(cleanup);
			this.description = "";	for(String w : wordsfiltered)	description += " " + w;
		}		
	}
	
	public static ArrayList<Paper> retrievePapers(String pkeywordspath) {
		System.out.println("Retrieving Papers ....");
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(pkeywordspath))) {
			String line;	int i = 0;
			while((line = br.readLine()) != null)	{
				if(i == 0) {
					if(line.indexOf("\t") != -1) {
						int number = -1;
						String num = line.substring(0, line.indexOf("\t"));	try { number = Integer.parseInt(num); } catch(Exception e) {}
						line = line.substring(line.indexOf("\t") + 1);
						String title = line.substring(1, line.indexOf("\t")-1);	title = title.replaceAll("[^a-zA-Z ]", "");
						line = line.substring(line.indexOf("\t") + 1);
						String yeartmp;
						if(line.indexOf("[") != -1)	yeartmp = line.substring(0, line.indexOf("[")).trim();	else	yeartmp = line.trim();
						int year = -1;	try { year = Integer.parseInt(yeartmp); } catch(Exception e) {}
						if(year < 2016) {
							ArrayList<String> kwords = new ArrayList<>();
							if(line.indexOf("\t") != -1) {
								line = line.substring(line.indexOf("\t") + 1);
								while(!line.trim().equals("")) {
									String tmp = "";
									if(line.indexOf("\t") != -1) {
										tmp = line.substring(1, line.indexOf("]"));
										line = line.substring(line.indexOf("\t") + 1);
									}
									else {
										tmp = line.substring(1, line.length() - 1);	line = "";
									}
									kwords.add(tmp);
								}
							}
							papers.add(new Paper(number, title, year, kwords));
						}						
					}
					i++;
				}
				else	i = 0;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return papers;
	}
	
	public static void retrieveAuthors(ArrayList<Paper> papers, String authorspath, String kgauthorspath, String kgauthorsppath) {
		System.out.println("Retrieving Authors ....");
		String output_authors_p = "";	String output_authors = "";
		try(BufferedReader br = new BufferedReader(new FileReader(authorspath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String name = line.substring(1, line.indexOf("\t")-1);	line = line.substring(line.indexOf("\t")+1);
					ArrayList<Integer> auth_papers = new ArrayList<>();
					while(!line.trim().equals("")) {
						int p = -1;
						if(line.indexOf("\t") != -1) {
							String ptmp = line.substring(0, line.indexOf("\t"));
							try {	p = Integer.parseInt(ptmp);	}	catch(Exception e) {}
							line = line.substring(line.indexOf("\t")+1);
						}
						else {
							String ptmp = line.trim();
							try {	p = Integer.parseInt(ptmp);	}	catch(Exception e) {}
							line = "";
						}
						if(paperInList(p, papers))	auth_papers.add(p);
					}
					
					if(auth_papers.size() > 0) {
						// Content of kg_authors_p.txt
						output_authors_p += id + "\t" + name;
						for(Integer n : auth_papers)	output_authors_p += "\t" + n;
						output_authors_p += "\n";
						
						// Content of kg_authors.txt
						String description = "";
						for(Integer n : auth_papers) {
							int idx = getPaper(n, papers);	description += " " + papers.get(idx).description;
						}
						// Clean up description:
						description = description.replaceAll("\\d", "");
						String[] words = description.split(" ");	ArrayList<String> wordsfiltered = removeDuplicateKeywords(words);
						description = "";	for(String w : wordsfiltered)	description += " " + w;
						
						String ntmp = name.replaceAll("[^a-zA-ZÄäÖöÜüßéèáà ]", "") + description;
						output_authors += id + "\t";	int x = ntmp.indexOf("  ");
						if(x != -1)	output_authors += ntmp.substring(0, x) + ntmp.substring(x+1) + "\n";
						else	output_authors += ntmp + "\n";
					}
				}
			} br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		writeToNewFile(kgauthorsppath, output_authors_p);	writeToNewFile(kgauthorspath, output_authors);
	}
	
	// ------------------------------------------------------------------

	public static boolean entryExists(String[] x, ArrayList<String[]> list) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i)[0].equals(x[0])) {
				if(list.get(i)[1].equals(x[1]))	return true;
			}
		}
		return false;
	}
	
	public static boolean paperInList(int num, ArrayList<Paper> papers) {
		boolean check = false;
		for(Paper p : papers) {	if(p.number == num)	{ check = true;	break; }	}
		return check;
	}
	
	public static int getPaper(int num, ArrayList<Paper> papers) {
		int index = -1;
		for(int i = 0; i < papers.size(); i++) {	if(papers.get(i).number == num)	index = i;	}
		return index;
	}
	
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
	
	public static ArrayList<String[]> convertFileTo2DimList(String path, boolean removeDuplicates) {	
		ArrayList<String[]> list = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String[] tmp = new String[]{line.substring(0, line.indexOf("\t")), line.substring(line.indexOf("\t")+1)};
					if(removeDuplicates) {
						if(!entryExists(tmp, list))	list.add(tmp);
					}
					else	list.add(tmp);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return list;
	}
	
	public static String removeStopWords(String text) {
		ArrayList<String> stopwords = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(stopwordspath))) {
			String line;
			while((line = br.readLine()) != null)	if(!line.trim().equals(""))	stopwords.add(line.trim());	br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		for(String w : stopwords)	{
			String tmp = " " + w + " ";	text = text.replaceAll("(?i)" + tmp, " ");
		}
		return text;
	}
	
	public static ArrayList<String> removeDuplicateKeywords(String[] input) {
		ArrayList<String> output = new ArrayList<>();
		Set<String> cleanup = new HashSet<>();	for(String i : input)	cleanup.add(i);	output.addAll(cleanup);
		return output;		
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String authorspath = "data_dblp/paperrecommendations/authors.txt";	stopwordspath = "data_dblp/embd/input/stopwords-en.txt";
		String pkeywordspath = "data_dblp/paperrecommendations/papers_keywords.txt";	
		String kgauthorsppath = "data_dblp/embd/kg_authors_p.txt";	String kgauthorspath = "data_dblp/embd/kg_authors.txt";
			
		// Retrieve list with Papers:
		ArrayList<Paper> papers = retrievePapers(pkeywordspath);
				
		// Retrieve Authors:
		retrieveAuthors(papers, authorspath, kgauthorspath, kgauthorsppath);
	}
}
