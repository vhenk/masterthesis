import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class RetrieveAffiliations {	// 2)
	static String stopwordspath = "stopwords-en.txt";
	
	public static ArrayList<String[]> readOrgFile(ArrayList<String[]> affil, String sgOrgpath) {
		ArrayList<String[]> organizations = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(sgOrgpath))) {
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
	
	public static void retrieveGRIDData(ArrayList<String> orgIDs, String gridpath, String orglistgridpath) {
		ArrayList<String[]> orgList = new ArrayList<>();	ArrayList<String> nodoubles = new ArrayList<>();
		System.out.println("Processing GRID-Data ....");
		try(InputStream fis = new FileInputStream(gridpath);	JsonReader reader = Json.createReader(fis);) {
			JsonObject gridObject = reader.readObject();	reader.close();
			JsonArray array = gridObject.getJsonArray("institutes");
			for(JsonObject entry : array.getValuesAs(JsonObject.class)) {
				String id = entry.getString("id");
				if((orgIDs.contains(id)) && (!nodoubles.contains(id))) {
					String name = ""; String city = ""; String state = ""; String country = "";
					try { name = entry.getString("name"); } catch(Exception e) {}
					JsonArray addresses = entry.getJsonArray("addresses");	int i = 0;
					if(addresses != null) {
						for(JsonObject x : addresses.getValuesAs(JsonObject.class)) {
							i++;
							try {
								city = x.getString("city");	state = x.getString("state");	country = x.getString("country");
							} catch(Exception e) {};
							if(i > 0) break;
						}
					}
					orgList.add(new String[] { id, name, city, state, country });	nodoubles.add(id);
				}			
			}
		} catch(Exception e) { e.printStackTrace(); }
		
		// Store Organization-List:
		System.out.println("Store Organizations-List....");
        String output = "";
        for(String o[] : orgList) {
        	String keywords = " " + o[1] + " " + o[2] + " " + o[3] + " " + o[4] + " ";
        	keywords = keywords.replaceAll("[^a-zA-Z ]", "");	keywords = removeStopWords(keywords);
        	// Remove duplicates in description:
        	String[] words = keywords.split(" ");	ArrayList<String> wordsfiltered = removeDuplicateKeywords(words);
        	keywords = "";	for(String w : wordsfiltered)	keywords += " " + w;
        	if(!keywords.trim().equals("")) output += o[0] + "\t" + keywords + "\n";
        }
		appendToFile(orglistgridpath, output);
	}
	
	public static String retrieveKeywords(String affID, String afLabelpath) {
		String keywords = "";	String label = "";
		try(BufferedReader br = new BufferedReader(new FileReader(afLabelpath))) {
			String line;
			while((line = br.readLine()) != null) {
				if((line.indexOf("rdf-schema#label>") != -1) && (line.indexOf("things/affiliations/"+affID) != -1)) {
					label = line.substring(line.indexOf("\"Affiliation:")+14, line.length()-3);	break;	
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		if((label.substring(0, 1).equals("[")) && (label.substring(label.length()-1).equals("]")))	return "";
		keywords = prepareKeywords(label);
		// Remove duplicates in description:
		String[] words = keywords.split(" ");	ArrayList<String> wordsfiltered = removeDuplicateKeywords(words);
		keywords = "";	for(String w : wordsfiltered)	keywords += " " + w;
		return keywords;
	}
	
	// -------------------------------------------------------------------------------------

	public static String prepareKeywords(String label) {
		String keywords = removeUTFCharacters(label).toString();	keywords = " " + keywords.replaceAll("[^a-zA-Z ]", " ");
		keywords = removeStopWords(keywords);	return keywords;
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
	
	public static ArrayList<String> removeDuplicateKeywords(String[] input) {
		ArrayList<String> output = new ArrayList<>();
		Set<String> cleanup = new HashSet<>();	for(String i : input)	cleanup.add(i);	output.addAll(cleanup);
		return output;		
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
	
	public static int isInList(String x, ArrayList<String[]> list) {
		int result = -1;
		for(int i = 0; i < list.size(); i++)	if(list.get(i)[1].equals(x))	result = i;
		return result;
	}
	
	public static boolean entryExists(String[] x, ArrayList<String[]> list) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i)[0].equals(x[0])) {
				if(list.get(i)[1].equals(x[1]))	return true;
			}
		}
		return false;
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
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String authorgpath = "auth_org.txt";	String kgorgpath = "kg_organizations.txt";	String kgauthppath = "kg_authors_p.txt";
		String orgpath = "organizations.txt";	String orglistpath = "org-list_grid.txt";	String sgOrgpath = "scigraphOrganization.txt";
		String kgorgcompletepath = "kg_organizations_complete.txt";	String afpath = "affiliations.txt";	String gridpath = "grid.json";
		String kgauthorgcompletepath = "kg_auth_orgs_complete.txt";	String afnewpath = "affiliations_new.txt"; String afLabelpath = "afLabels.txt";
		
		// Retrieve Organizations
		ArrayList<String[]> affil = getFileContent(afpath, 2);
		System.out.println("Retrieve Oranization-IDs....");
		ArrayList<String[]> organizations = readOrgFile(affil, sgOrgpath);
		System.out.println("Retrieve IDs of Affiliations without Organization ....");
		for(String[] a : affil) {
			boolean match = false;	for(String[] o : organizations)	if(o[2].equals(a[1]))	match = true;
			if(!match)	appendToFile(afnewpath, a[0] + "\t" + a[1] + "\n");
		}
		System.out.println("Store Organization-IDs ....");
		ArrayList<String[]> org_filtered = new ArrayList<>();
		for(String[] o : organizations) {
			boolean match = false;	// Check for duplicates
			for(String[] o2 : org_filtered)	if((o[0].equals(o2[0])) && (o[1].equals(o2[1])))	match = true;
			if(!match)	{
				org_filtered.add(new String[] { o[0], o[1] });	appendToFile(orgpath, o[0] + "\t" + o[1] + "\n");
			}
		}
		
		// Retrieve Organization-IDs and corresponding data
		ArrayList<String[]> atmp = getFileContent(kgauthppath, 3);
		ArrayList<String> authornames = new ArrayList<>();	for(String[] a : atmp)	authornames.add(a[1]);
		ArrayList<String[]> otmp = getFileContent(orgpath, 2);
		ArrayList<String> orgIDs = new ArrayList<>();	System.out.println("Reading Organizations-File ....");
		for(String[] org : otmp) 	if((authornames.contains(org[0])) && (!orgIDs.contains(org[1])))	orgIDs.add(org[1]);
		retrieveGRIDData(orgIDs, gridpath, orglistpath);
		
		// Retrieve Organization-Information from SG
		ArrayList<String[]> aorg = getFileContent(afnewpath, 2);
		for(String[] a : aorg) {
			String keywords = retrieveKeywords(a[1], afLabelpath);
			if(!keywords.trim().equals("")) {
				int idx = isInList(keywords, organizations);
				if(idx == -1)	{
					int counter = organizations.size() + 1;	
					organizations.add(new String[] { "Org" + counter, keywords });	idx = counter-1;
					appendToFile(kgorgpath, "Org" + counter + "\t" + keywords + "\n");
				}
				appendToFile(authorgpath, a[0] + "\t" + organizations.get(idx)[0] + "\n");
			}
		}
		
		// Merge Affiliation-Data & Remove duplicates from auth_org
		ArrayList<String[]> authors = convertFileTo2DimList(authorgpath, true);
		ArrayList<String[]> orgs = convertFileTo2DimList(kgorgpath, false);
		ArrayList<String[]> grid = convertFileTo2DimList(orglistpath, false);
		ArrayList<String[]> authgrid = convertFileTo2DimList(orgpath, false);
		for(String[] g : grid) {
			int counter = orgs.size() + 1;	orgs.add(new String[] { "Org" + counter, g[1] });
			for(String[] ag : authgrid)	if(ag[1].equals(g[0]))	authors.add(new String[] { ag[0], "Org" + counter });
		}
		for(String[] org : orgs)	appendToFile(kgorgcompletepath, org[0] + "\t" + org[1] + "\n");
		for(String[] auth : authors)	appendToFile(kgauthorgcompletepath, auth[0] + "\t" + auth[1] + "\n");
	}
}
