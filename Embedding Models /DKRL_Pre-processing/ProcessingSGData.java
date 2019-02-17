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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessingSGData {
	static ArrayList<String> path, filtered_path, authors;
	
	public static void filterFile(String path, String output) {
		System.out.print("\nProcessing File " + path + " ....");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int counter = 0; int counterkept = 0;
			while((line = br.readLine()) != null) {
				counter++;	boolean keep = false;
				if ((counter % 1000000) == 0)	System.out.print("\t...");
				if(line.contains("contribution"))	keep = true;
				else if(line.contains("affiliation"))	keep = true;
				if(keep) {	appendToFile(output, line + "\n");	counterkept++;	}
			} br.close();
		System.out.println("\nFile " + output + " generated - " + counterkept + " of " + counter + " lines kept!");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static ArrayList<String[]> getContributions(String path) {
		System.out.println("Reading File " + path + " ....");	int counter = 0;
		ArrayList<String[]> contributions = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				// Get subject of line:
				String subject = line.substring(0, line.indexOf(">")+1);
				if(subject.indexOf("<http://scigraph.springernature.com/things/contributions/") != -1) {
					// Get publishedName:
					if(line.indexOf("/ontologies/core/publishedName>") != -1) {
						String name = line.substring(line.indexOf("/publishedName>")+17, line.length()-3);
						String sgID = subject.substring(subject.indexOf("/contributions/")+15, subject.indexOf(">"));
						String nametmp = removeUTFCharacters(name).toString();
						if(authors.contains(nametmp)) {	contributions.add(new String[] {nametmp, sgID});	counter++;	}
					}
				}		
			} br.close();	System.out.println(counter + " Names retrieved!");
		} catch(IOException e) {	e.printStackTrace();	}
		return contributions;	
	}
	
	public static void getAffiliations(String path, ArrayList<String[]> cont) {
		System.out.println("\nProcessing File " + path + " ....");	int found = 0;
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			 String line; int countlines = 0;
			 while((line = br.readLine()) != null) {
				 countlines++;	if((countlines % 500000) == 0)	System.out.print("\t...");
				 if(line.indexOf("hasAffiliation") != -1) {
					// Get subject of line:
					 String subject = line.substring(0, line.indexOf(">")+1);
					 subject = subject.substring(subject.lastIndexOf("/")+1, subject.length()-1);
					 // Check if this line is relevant:
					 int check = isInList(subject, cont);	String aID;
					 if(check != -1) {
						 aID = line.substring(line.indexOf("/things/affiliations/") + 21, line.length()-3);
						 String tmp = cont.get(check)[0] + "\t" + aID + "\n";
						 appendToFile("affiliations.txt", tmp);	found++;
					 }
				 }
			 } br.close();
			 System.out.println("\n" + found + " Entries found!");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void filterData (String path) {
		System.out.println("\nProcessing File " + path + " ....");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			 String line; int counter = 0;
			 while((line = br.readLine()) != null) {
				 counter++;	if ((counter % 1000000) == 0)	System.out.print("\t...");
				 if(line.indexOf("hasOrganization") != -1)	appendToFile("scigraphOrganization.txt", line + "\n");
			 } br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static ArrayList<String[]> readOrgFile(ArrayList<String[]> affil) {
		ArrayList<String[]> organizations = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("scigraphOrganization.txt"))) {
			String line; int counter = 0;	System.out.println("");
			while((line = br.readLine()) != null) {
				counter++;	if ((counter % 10000) == 0)	System.out.print("\t" + counter);
				// Get subject:
				String subject = line.substring(0, line.indexOf(">"));	subject = subject.substring(subject.lastIndexOf("/")+1);
				int check = isInList(subject, affil);
				if(check != -1) {
					String org = line.substring(line.lastIndexOf("grid"), line.lastIndexOf(">"));
					organizations.add(new String[] { affil.get(check)[0], org, affil.get(check)[1] });
				}
			} br.close();
			System.out.println("");
		} catch(IOException e) {	e.printStackTrace();	}
		
		return organizations;
	}
	
	public static void filterDataA(String path, ArrayList<String> affilIDs) {
		System.out.println("Processing File " + path + " ....");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int counter = 0;
			while((line = br.readLine()) != null) {
				counter++;	if ((counter % 1000000) == 0)	System.out.print("\t...");
				if(line.indexOf("/things/affiliations/") != -1) {
					String subject = line.substring(0, line.indexOf(">"));	subject = subject.substring(subject.lastIndexOf("/")+1);
					if(affilIDs.contains(subject))	appendToFile("affilData.txt", line + "\n");
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}	
	}
	
	public static ArrayList<String> readAuthorNamesKG() {
		ArrayList<String> authornames = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_authors_p.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					line = line.substring(line.indexOf("\t")+1);	
					String name = line.substring(0, line.indexOf("\t"));	authornames.add(name);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authornames;
	}
	
	public static void filterAFFile() {
		try(BufferedReader br = new BufferedReader(new FileReader("affilData.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("/rdf-schema#label>") != -1) 	appendToFile("afLabels.txt", line + "\n");
			} br.close();
		} catch(IOException e) {	e.printStackTrace();}
	}
	
	public static String retrieveKeywords(String affID) {
		String keywords = "";	String label = "";
		try(BufferedReader br = new BufferedReader(new FileReader("afLabels.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if((line.indexOf("rdf-schema#label>") != -1) && (line.indexOf("things/affiliations/"+affID) != -1)) {
					label = line.substring(line.indexOf("\"Affiliation:")+14, line.length()-3);	break;	
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		if((label.substring(0, 1).equals("[")) && (label.substring(label.length()-1).equals("]")))	return "";
		keywords = removeUTFCharacters(label).toString();	keywords = " " + keywords.replaceAll("[^a-zA-Z ]", " ");
		keywords = removeStopWords(keywords);
		// Remove duplicates in description:
		String[] words = keywords.split(" ");	ArrayList<String> wordsfiltered = new ArrayList<>();
		Set<String> cleanup = new HashSet<>();	for(String w : words)	cleanup.add(w);	wordsfiltered.addAll(cleanup);
		keywords = "";	for(String w : wordsfiltered)	keywords += " " + w;
		return keywords;
	}
	
	// ---------------------------------------------------------------------------------------------------
	
	public static ArrayList<String[]> convertFileToList(String path, boolean removeDuplicates) {	
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
		try(BufferedReader br = new BufferedReader(new FileReader("stopwords-en.txt"))) {
			String line;
			while((line = br.readLine()) != null)	if(!line.trim().equals(""))	stopwords.add(line.trim());	br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		for(String w : stopwords)	{
			String tmp = " " + w + " ";	text = text.replaceAll("(?i)" + tmp, " ");
		}
		return text;
	}
	
	public static boolean entryExists(String[] x, ArrayList<String[]> list) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i)[0].equals(x[0])) {
				if(list.get(i)[1].equals(x[1]))	return true;
			}
		}
		return false;
	}
	
	public static int isInList(String x, ArrayList<String[]> list) {
		int result = -1;
		for(int i = 0; i < list.size(); i++)	if(list.get(i)[1].equals(x))	result = i;
		return result;
	}
	
	public static StringBuffer removeUTFCharacters(String data){
		Pattern p = Pattern.compile("\\\\u(\\p{XDigit}{4})");	Matcher m = p.matcher(data);
		StringBuffer buf = new StringBuffer(data.length());
		while (m.find()) {
			String ch = String.valueOf((char) Integer.parseInt(m.group(1), 16));
			m.appendReplacement(buf, Matcher.quoteReplacement(ch));
		}
		m.appendTail(buf);
		return buf;
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// 10) Retrieve affiliation-triples and translate authornames to IDs:
		// Author-Entity	/author/affiliation/organization	Organization-Entity
		HashMap<String, String> authorsmap = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader("data/authorkeys.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String name = line.substring(0, line.indexOf("\t"));
					authorsmap.put(name, id);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		ArrayList<String[]> auth_orgs = convertFileToList("kg_auth_orgs_complete.txt", false);
		String triples = "";
		for(String[] ao : auth_orgs)	triples += authorsmap.get(ao[0]) + "\t/author/affiliation/department\t" + ao[1] + "\n";
		appendToFile("kg_triples.txt", triples);
		
		// Create Mapping-Files:
		String authorsorg = "";
		for(String authorname : authorsmap.keySet()) {
			authorsorg += authorsmap.get(authorname);
			for(String[] ao : auth_orgs) 	if(ao[0].equals(authorname))	authorsorg += "\t" + ao[1];
			authorsorg += "\n";
		}
		writeToNewFile("authors_org.txt", authorsorg);
		
		String orgsauth = "";
		ArrayList<String[]> orgs = convertFileToList("kg_organizations.txt", false);
		for(String[] org : orgs) {
			orgsauth += org[0];
			for(String[] ao : auth_orgs)	if(ao[1].equals(org[0]))	orgsauth += "\t" + authorsmap.get(ao[0]);
			orgsauth += "\n";
		}
		writeToNewFile("orgs_author.txt", orgsauth);		
		
		
		
		
	}
}
