package dblp_dataset.kg_embedding_models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class KGPapers {	// 4)
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
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(pkeywordspath))) {
			String line;	int i = 0;
			while((line = br.readLine()) != null)	{
				if(i == 0) {
					if(line.indexOf("\t") != -1) {
						int number = -1;
						String num = line.substring(0, line.indexOf("\t"));	try { number = Integer.parseInt(num); } catch(Exception e) {}
						line = line.substring(line.indexOf("\t")+1);
						String title = line.substring(1, line.indexOf("\t")-1);	title = title.replaceAll("[^a-zA-Z ]", "");
						line = line.substring(line.indexOf("\t")+1);
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
						if(x != -1)	output_authors += ntmp.substring(0, x) + ntmp.substring(x+1) + "/n";
						else	output_authors += ntmp + "\n";
					}
				}
			} br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		writeToNewFile(kgauthorsppath, output_authors_p);	writeToNewFile(kgauthorspath, output_authors);
	}
	
	public static ArrayList<String>	getEntityWords(String path, String keywordspath) {
		ArrayList<String> list = new ArrayList<>();	System.out.println("Processing Keywords in File " + path + " ....");
		ArrayList<String[]> file = getFileContent(path, 2);	String keywords = "";
		for(String[] f : file) {
			// Count Keywords:
			String[] words = f[1].split(" "); int counter = 0;	for(String x : words)	if(!x.trim().equals(""))	counter++;
			String tmp = f[1].replaceAll("  ", " ");	list.add(f[0] + "\t" + counter + "\t" + tmp + "\n");
			keywords += tmp + " ";
		}
		appendToFile(keywordspath, keywords);
		return list;
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
	
	public static String getHashKey(HashMap<String, String> map, String item) {
		String key = "";
		for(String k : map.keySet()) {
			if(map.get(k).equals(item)) {	key = k;	break;	}
		}
		return key;
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
		// Input-files:
		stopwordspath = "data_dblp/embd/input/stopwords-en.txt";		
		String authorkeyspath = "data_dblp/output/authorkeys.txt";	
		String kgorgpath = "data_dblp/embd/kg_organizations.txt";	String kgauthorgpath = "data_dblp/embd/kg_auth_orgs.txt";
		String kgeventspath = "data_dblp/embd/kg_events.txt";	String kgauthpath = "data_dblp/embd/kg_authors.txt";
		String pkeywordspath = "data_dblp/paperrecommendations/papers_keywords.txt";
			
		// Output-files:
		String triplespath = "data_dblp/embd/kg_triplesorg.txt";	String kgpaperspath = "data_dblp/embd/kg_papers.txt";
		String keywordspath = "data_dblp/embd/word2vec/keywords.txt";
		
		// Temporary files:
		String authorsorgpath = "authors_org.txt";	String orgauthpath = "org_author.txt";
		
		// Retrieve affiliation-triples and translate authornames to IDs:
		// Author-Entity	/author/affiliation/organization	Organization-Entity
		ArrayList<String[]> atmp = getFileContent(authorkeyspath, 3);
		HashMap<String, String> authorsmap = new HashMap<>();	
		for(String[] a : atmp)	authorsmap.put(a[1], a[0]);	
		ArrayList<String[]> auth_orgs = convertFileTo2DimList(kgauthorgpath, false);
		String triples = "";
		for(String[] ao : auth_orgs)	{
			//String aID = getHashKey(authorsmap, ao[0]); 
			triples += authorsmap.get(ao[0]) + "\t/author/affiliation/department\t" + ao[1] + "\n";
		}
		appendToFile(triplespath, triples);
		
		// Create Mapping-Files:
		String authorsorg = "";	String orgsauth = "";
		for(String authorname : authorsmap.keySet()) {
			authorsorg += authorsmap.get(authorname);
			for(String[] ao : auth_orgs) 	if(ao[0].equals(authorname))	authorsorg += "\t" + ao[1];	authorsorg += "\n";
		}
		writeToNewFile(authorsorgpath, authorsorg);
		
		ArrayList<String[]> orgs = convertFileTo2DimList(kgorgpath, false);
		for(String[] org : orgs) {
			orgsauth += org[0];
			for(String[] ao : auth_orgs)	if(ao[1].equals(org[0]))	orgsauth += "\t" + authorsmap.get(ao[0]);	orgsauth += "\n";
		}
		writeToNewFile(orgauthpath, orgsauth);
		
		// Retrieve list with Papers:
		ArrayList<Paper> papers = retrievePapers(pkeywordspath);
		String output_papers = "";
		for(Paper p : papers) {	output_papers += "P" + p.number + "\t" + p.description + " " + p.year + "\n";	}
		writeToNewFile(kgpaperspath, output_papers);
		
		// Generate keywords-file:
		ArrayList<String> keywords = new ArrayList<>();
		keywords.addAll(getEntityWords(kgauthpath, keywordspath));	keywords.addAll(getEntityWords(kgpaperspath, keywordspath));
		keywords.addAll(getEntityWords(kgeventspath, keywordspath));	keywords.addAll(getEntityWords(kgorgpath, keywordspath));
		
		// Remove temporary files:
		File tmp1 = new File(authorsorgpath);	tmp1.delete();	File tmp2 = new File(orgauthpath);	tmp2.delete();
	}
}
