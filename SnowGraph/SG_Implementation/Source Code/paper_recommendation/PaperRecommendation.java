package scigraph_dataset.paper_recommendation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PaperRecommendation {
	// Input-file:
	static String authorspath = "";
	
	// Output:
	static String suffix = "";
	static String recommendationsjsfile = "";	static String recommendationsfile = "";
	
	// Lists for reference:
	static HashMap<String, Author> authorslist = new HashMap<>();
	static HashMap<Integer, Paper> paperslist = new HashMap<>();
	
	static class Author {
		String id, name;	ArrayList<Integer> papers; ArrayList<String[]> simauthors;
		public Author(String id, ArrayList<String[]> simauthors) {
			this.id = id;	this.simauthors = simauthors;	this.name = "";	this.papers = new ArrayList<>();
		
			// Get name and papers:
			try(BufferedReader br = new BufferedReader(new FileReader(authorspath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						String idtmp = line.substring(0, line.indexOf("\t"));
						if(this.id.equals(idtmp)) {
							line = line.substring(line.indexOf("\t")+1);
							this.name = line.substring(1, line.indexOf("\t")-1);
							line = line.substring(line.indexOf("\t")+2);
							
							// Get list of Papers:
							while(!line.trim().equals("")) {
								if(line.indexOf("\t") != -1) {
									String pnrtmp = line.substring(0, line.indexOf("\t"));	int pnr = -1;
									try { pnr = Integer.parseInt(pnrtmp); } catch(Exception e) {}
									if(pnr > 0)	this.papers.add(pnr);	
									line = line.substring(line.indexOf("\t")+1);
								}
								else {
									String pnrtmp = line.trim();	int pnr = -1;
									try { pnr = Integer.parseInt(pnrtmp); } catch(Exception e) {}
									if(pnr > 0)	this.papers.add(pnr);	break;
								}
							}
						}
					}
				}
				br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			this.name = removeUTFCharacters(this.name);
		}
	}
	
	static class Paper {
		int number, year;	String title;	ArrayList<String> authors;	ArrayList<String> keywords;
		public Paper(int number, String title, int year, ArrayList<String> keywords, ArrayList<String> authors) {
			this.number = number;	this.title = removeUTFCharacters(title);	this.year = year;	
			this.keywords = keywords;	this.authors = authors;
		}
	}
	
	public static String getAuthorName(String id) {
		String name = "";
		try(BufferedReader br = new BufferedReader(new FileReader(authorspath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String idtmp = line.substring(0, line.indexOf("\t"));
					if(id.equals(idtmp)) {
						line = line.substring(line.indexOf("\t")+2);	name = line.substring(0, line.indexOf("\t")-1);
					}
				}
			}
			br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		return removeUTFCharacters(name);
	}
	
	public static double getSimValue(String author1, String author2) {
		boolean author1inlist = authorslist.keySet().contains(author1);
		boolean author2inlist = authorslist.keySet().contains(author2);
		double value = 0;
		
		if((!author1inlist) || (!author2inlist))	return -1;
		else {
			Author atmp = authorslist.get(author1);
			for(String[] sa : atmp.simauthors) {	
				if(sa[0].equals(author2))	{	try { value = Double.parseDouble(sa[1]);	} catch(Exception e)	{}	}
			}
		}
		return value;
	}
	
	public static void retrieveRecommendations(ArrayList<String> authors) {
		int counter = 0;
		for(String a : authors) {
			ArrayList<Double[]> recommendations = new ArrayList<>();
			Author author = authorslist.get(a);	ArrayList<String[]> simauthors = author.simauthors;

			// Get Papers of similar authors and maximum similarity value:
			ArrayList<Integer> allpapers = new ArrayList<>();	double maximum = 0;
			for(String[] sa : simauthors) {
				Author atmp = authorslist.get(sa[0]);
				for(int p : atmp.papers) {	if(!allpapers.contains(p))	allpapers.add(p);	}
			
				double weight = 0;
				try { weight = Double.parseDouble(sa[1]); } catch(Exception e) {}
				maximum = maximum + weight;
			}
			
			// Calculate similarity value of each paper:
			for(int pnr : allpapers) {
				Paper paper = paperslist.get(pnr);	ArrayList<String[]> candidate_p = new ArrayList<>();
				for(String atmp : paper.authors) {
					for(String[] satmp : simauthors) {
						if(satmp[0].equals(atmp))	{	
							String[] tmp = new String[2];	tmp[0] = satmp[0];	tmp[1] = satmp[1];	
							candidate_p.add(tmp);
						}
					}
				}
				
				double value = 0;
				for(String[] c : candidate_p) {
					double tmp = 0;
					try { tmp = Double.parseDouble(c[1]); } catch(Exception e) {}
					value += tmp;
				}
				
				// Normalize and Store similarity value:
				Double[] recom = new Double[2];	recom[0] = (double) pnr;	recom[1] = value / maximum;	
				recommendations.add(recom);
			}
			
			// Remove author's own papers from recommendations:
			ArrayList<Double[]> rectmp = recommendations;	recommendations = new ArrayList<>();
			ArrayList<Integer> plist = author.papers;
			for(int i = 0; i < rectmp.size(); i++) {
				boolean check = false;
				for(int x : plist) 	if(x == rectmp.get(i)[0])	{	check = true; break;	}
				if(!check)	recommendations.add(rectmp.get(i));
			}

			// Sort/Filter Recommendations:
			ArrayList<Paper> papers = sortRecommendations(recommendations); 
			
			// Add to Recommendations-file:
			recommendationsfile += author.id + "\n";
			for(int i = 0; i < papers.size(); i++) {
				recommendationsfile += papers.get(i).number;	if(i != papers.size()-1)	recommendationsfile += "\t";
			}
			recommendationsfile += "\n";
			for(int i = 0; i < papers.size(); i++) {
				double value = 0;
				for(Double[] tmp : recommendations) {	if(tmp[0] == papers.get(i).number)	value = tmp[1];	}
				recommendationsfile += value;	if(i != papers.size()-1)	recommendationsfile += "\t";
			}
			if(counter != authors.size()-1)	recommendationsfile += "\n";
			
			// Add to Recommendations-JS-File:
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add("id", author.id).add("numrecommendations", papers.size());
			// Add Paper-Recommendations:
			JsonArrayBuilder recbuilder = Json.createArrayBuilder();
			for(Paper p : papers) {
				double value = 0;
				for(Double[] tmp : recommendations) {	if(tmp[0] == p.number)	value = tmp[1];	 	}
				JsonObjectBuilder paperobjbuilder = Json.createObjectBuilder();
				paperobjbuilder.add("number", p.number).add("year", p.year).add("title", p.title).add("weight", value);
			
				// Add Authors:
				JsonArrayBuilder authorbuilder = Json.createArrayBuilder();
				for(String aID : p.authors) {
					String aname = getAuthorName(aID);	double weight = getSimValue(author.id, aID);
					JsonObject objtmp = Json.createObjectBuilder().add("id", aID).add("name", aname).add("weight", weight).build();
					authorbuilder.add(objtmp);
				}
				JsonArray authorarray = authorbuilder.build();
				paperobjbuilder.add("authors", authorarray);
				
				// Add Keywords:
				JsonArrayBuilder keybuilder = Json.createArrayBuilder();
				for(String key : p.keywords) {
					JsonObject objtmp = Json.createObjectBuilder().add("keyword", key).build();
					keybuilder.add(objtmp);
				}
				JsonArray keyarray = keybuilder.build();
				paperobjbuilder.add("topics", keyarray);
				
				JsonObject paperobj = paperobjbuilder.build();
				recbuilder.add(paperobj);
			}
			JsonArray recarray = recbuilder.build();
			builder.add("papers", recarray);
			JsonObject obj = builder.build();
			
			recommendationsjsfile += "var PRec" + author.id + suffix + " = " + obj.toString() + ";";
			if(counter != authors.size()-1)	recommendationsjsfile += "\n";
			
			counter++;	
		}	
	}
	
	public static ArrayList<Paper> sortRecommendations(ArrayList<Double[]> papers) {
		if(papers.size() == 0)	return new ArrayList<>();
		
		// Sort by year:
		Paper[] tmpsort = new Paper[papers.size()];
		for(int i = 0; i < papers.size(); i++) {
			if(i == 0)	{
				double pidx_double = papers.get(0)[0];	int pidx = (int) pidx_double;
				tmpsort[0] = paperslist.get(pidx);
			}
			else {
				double pidx_double = papers.get(i)[0];	int pidx = (int) pidx_double;
				Paper ptmp = paperslist.get(pidx);	int idx = 0;
				while((idx < tmpsort.length) && (tmpsort[idx] != null)) {
					if(ptmp.year >= tmpsort[idx].year) {
						Paper ptmp2 = tmpsort[idx];	tmpsort[idx] = ptmp;	ptmp = ptmp2;
					}
					idx++;
				}
				if(idx != tmpsort.length)	tmpsort[idx] = ptmp;
			}
		}
		ArrayList<Paper> sorted = new ArrayList<>();
		for(Paper x : tmpsort)	sorted.add(x);
		
		//Sort by paper similarity value:
		ArrayList<Paper> sortedweight = new ArrayList<>();
		ArrayList<Paper> yeargroup = new ArrayList<>();
		int yeartmp = sorted.get(0).year;	
				
		for(int i = 0; i < sorted.size(); i++) {
			boolean exists = false;	for(Paper x : sortedweight)	{	if(x.year == yeartmp)	exists = true;	}
					
			if(!exists) {
				for(Paper p : sorted) {	if(p.year == yeartmp)	yeargroup.add(p);	}		
							
				Paper[] ytmpsort = new Paper[yeargroup.size()];
				for(int j = 0; j < yeargroup.size(); j++) {
					if(j == 0)	ytmpsort[0] = yeargroup.get(0);
					else {
						Paper ptmp = yeargroup.get(j); int idx = 0;
						while((idx < ytmpsort.length) && (ytmpsort[idx] != null)) {
							double ptmp_value = 0;
							for(Double[] p : papers) {	if(p[0] == ptmp.number)	ptmp_value = p[1];	}
							double ytmpsort_value = 0;
							for(Double[] p : papers) {	if(p[0] == ytmpsort[idx].number)	ytmpsort_value = p[1];	}
							if(ptmp_value >= ytmpsort_value) {
								Paper ptmp2 = ytmpsort[idx];	ytmpsort[idx] = ptmp;	ptmp = ptmp2;
							}	
							idx++;
						}
						if(idx != ytmpsort.length)	ytmpsort[idx] = ptmp;
					}
				}
					
				// Reduce recommendations per year to 5:
				int limit = 5;	if(ytmpsort.length < limit)	limit = ytmpsort.length;
				for(int p = 0; p < limit; p++) {
					sortedweight.add(ytmpsort[p]);
				}
				yeargroup.clear();
			}
			if(i != sorted.size()-1)	yeartmp = sorted.get(i+1).year;
		}
		return sortedweight;
	}
	
	public static String removeUTFCharacters(String text){
		Pattern pattern = Pattern.compile("\\\\u(\\p{XDigit}{4})");
		Matcher matcher = pattern.matcher(text);
		StringBuffer buffer = new StringBuffer(text.length());
		while (matcher.find()) {
			String s = String.valueOf((char) Integer.parseInt(matcher.group(1), 16));
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(s));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
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
		System.out.println("Name of Authors-file: ");	authorspath = reader.nextLine();
		System.out.println("Name of Papers-Keywords-file: ");	String pkeywordspath = reader.nextLine();
		System.out.println("Name of Similar-Authors-file: ");	String simauthorspath = reader.nextLine();
		System.out.println("Suffix for Json-Objects: ");	suffix = reader.nextLine();	reader.close();
		if(authorspath.indexOf(".txt") == -1)	authorspath += ".txt";
		if(pkeywordspath.indexOf(".txt") == -1)	pkeywordspath += ".txt";
		if(simauthorspath.indexOf(".txt") == -1)	simauthorspath += ".txt";
				
		// Output-files:
		String recommendationspath = "p_recommendations_" + suffix + ".txt";	
		String recommendationsjspath = "p_recommendations_" + suffix + ".js";	
		
		// Read similar-authors-file and obtain Authors-list:
		System.out.println("Obtaining Authors-Reference-List ....");
		ArrayList<String> relevantAuthors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(simauthorspath))) {
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
		
		// Obtain Papers-list:
		System.out.println("Obtaining Papers-Reference-List ....");
		try(BufferedReader br = new BufferedReader(new FileReader(pkeywordspath))) {
			String line;	int lineidx = 0;	
			int number = -1; int year = -1;	String title = "";	
			ArrayList<String> keywords = new ArrayList<>();	ArrayList<String> authors = new ArrayList<>();
			while((line = br.readLine()) != null) {
				if(lineidx == 0) {
					if(line.indexOf("\t") != -1) {
						number = -1;	year = -1;
						String numtmp = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+2);
						title = line.substring(0, line.indexOf("\t")-1);	line = line.substring(line.indexOf("\t")+1);
						String yeartmp = "";
						if(line.indexOf("\t") != -1) {
							yeartmp = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	yeartmp = line.trim();	line = "";	}
						try {	year = Integer.parseInt(yeartmp);	number = Integer.parseInt(numtmp);	} catch(Exception e)	{}
						
						// Get Keywords:
						keywords = new ArrayList<>();
						while(!line.trim().equals("")) {
							if(line.indexOf("\t") != -1) {
								String ktmp = line.substring(1, line.indexOf("\t")-1);
								keywords.add(removeUTFCharacters(ktmp));	line = line.substring(line.indexOf("\t")+1);	
							}
							else {	keywords.add(line.substring(1, line.indexOf("]")));	break;	}
						}			
					}
					lineidx++;
				}
				else {
					authors = new ArrayList<>();
					while(!line.trim().equals("")) {
						// Get Authors:
						String aID = "";
						if(line.indexOf("\t") != -1) {
							aID = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	aID = line.trim();	line = "";	}
						authors.add(aID);
					}
					// Add Paper to reference list:
					Paper paper = new Paper(number, title, year, keywords, authors);	paperslist.put(number, paper);	
					lineidx = 0;
				}
			}
			br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		
		// Compute and store Recommendations:
		retrieveRecommendations(relevantAuthors);
		writeToNewFile(recommendationspath, recommendationsfile);
		writeToNewFile(recommendationsjspath, recommendationsjsfile);		
	}
}
