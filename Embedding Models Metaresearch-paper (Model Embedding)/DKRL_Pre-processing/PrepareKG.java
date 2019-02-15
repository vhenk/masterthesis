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

public class PrepareKG {
	static ArrayList<String> stopwords;
	
	static class Paper {
		String title, description; ArrayList<String> keywords; int year, number;
		public Paper(int number, String title, int year, ArrayList<String> keywords) {
			this.number = number;	this.title = title;	this.keywords = keywords; this.year = year;
			this.description = " " + title;
			for(String k : keywords) {
				String ktmp = k.replaceAll("[^a-zA-Z ]", "");	ktmp = ktmp.replaceAll(" ", "_");	description += " " + ktmp;
			}
			this.description = removeStopWords(description, stopwords);
			// Remove duplicates in description:
			String[] words = description.split(" ");	ArrayList<String> wordsfiltered = new ArrayList<>();
			Set<String> cleanup = new HashSet<>();	for(String w : words)	cleanup.add(w);	wordsfiltered.addAll(cleanup);
			this.description = "";	for(String w : wordsfiltered)	description += " " + w;
		}		
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
	
	public static String removeStopWords(String text, ArrayList<String> stopwords) {
		for(String w : stopwords)	{
			String tmp = " " + w + " ";	text = text.replaceAll("(?i)" + tmp, " ");
		}
		return text;
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
		// Retrieve List with Stop Words
		stopwords = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("stopwords-en.txt"))) {
			String line;
			while((line = br.readLine()) != null)	if(!line.trim().equals(""))	stopwords.add(line.trim());	br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Retrieve List with Papers
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("data/papers_keywords.txt"))) {
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
							while(line.indexOf("\t") != -1) {
								String tmp = line.substring(line.indexOf("\t")+2, line.indexOf("]"));
								kwords.add(tmp);
								line = line.substring(line.indexOf("]"));
								if(line.indexOf("\t") != -1)	line = line.substring(1);
							}
							
							papers.add(new Paper(number, title, year, kwords));
						}
					}
					i++;
				}
				else	i = 0;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		String output_papers = "";
		for(Paper p : papers) {	output_papers += "P" + p.number + "\t" + p.description + " " + p.year + "\n";	}
		writeToNewFile("kg_papers.txt", output_papers);

		
		/*
		// Retrieve / Process Authors
		String output_authors_p = "";
		String output_authors = "";
		try(BufferedReader br = new BufferedReader(new FileReader("data/authors.txt"))) {
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
						String description = name;
						for(Integer n : auth_papers) {
							int idx = getPaper(n, papers);
							description += " " + papers.get(idx).description;
						}
						output_authors += id + "\t" + description + "\n";
					}
				}
			} br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		
		writeToNewFile("kg_authors_p.txt", output_authors_p);
		writeToNewFile("kg_authors.txt", output_authors);
		*/
		
	}
}
