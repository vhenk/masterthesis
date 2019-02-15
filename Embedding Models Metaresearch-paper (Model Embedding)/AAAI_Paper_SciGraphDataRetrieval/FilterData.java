import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterData {	// 1)
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
	
	public static void filterDataOrg(String path, String orgpath) {
		System.out.println("\nProcessing File " + path + " ....");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			 String line; int counter = 0;
			 while((line = br.readLine()) != null) {
				 counter++;	if ((counter % 1000000) == 0)	System.out.print("\t...");
				 if(line.indexOf("hasOrganization") != -1)	appendToFile(orgpath, line + "\n");
			 } br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void getContributions(String path, String contpath) {
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
		
		String output = "";	System.out.println("");
		for(int i = 0; i < contributions.size(); i++) {
			output += contributions.get(i)[0] + "\t" + contributions.get(i)[1] + "\n";
		}
		appendToFile(contpath, output);
	}
	
	public static void getAffiliations(String path, ArrayList<String[]> cont, String affiliationspath) {
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
						 appendToFile(affiliationspath, tmp);	found++;
					 }
				 }
			 } br.close();
			 System.out.println("\n" + found + " Entries found!");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void filterDataAffil(ArrayList<String> path, ArrayList<String> IDs, String afDatapath, String afLabelpath) {
		for(String p : path) {
			System.out.println("Processing File " + p + " ....");
			try(BufferedReader br = new BufferedReader(new FileReader(p))) {
				String line; int counter = 0;
				while((line = br.readLine()) != null) {
					counter++;	if ((counter % 1000000) == 0)	System.out.print("\t...");
					if(line.indexOf("/things/affiliations/") != -1) {
						String subject = line.substring(0, line.indexOf(">"));	subject = subject.substring(subject.lastIndexOf("/")+1);
						if(IDs.contains(subject))	appendToFile(afDatapath, line + "\n");
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
		
		try(BufferedReader br = new BufferedReader(new FileReader(afDatapath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("/rdf-schema#label>") != -1) 	appendToFile(afLabelpath, line + "\n");
			} br.close();
		} catch(IOException e) {	e.printStackTrace();}
	}
	
	// --------------------------------------------------------------------
	
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
	
	public static int isInList(String x, ArrayList<String[]> list) {
		int result = -1;
		for(int i = 0; i < list.size(); i++)	if(list.get(i)[1].equals(x))	result = i;
		return result;
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
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String affiliationspath = "affiliations.txt";	String contpath = "contributions.txt";
		String authorkeyspath = "data/authorkeys.txt";	String afDatapath = "affilData.txt";
		String afLabelpath = "afLabels.txt";	String orgpath = "scigraphOrganization.txt";
		
		ArrayList<String[]> atmp = getFileContent(authorkeyspath, 3);	for(String[] a : atmp)	authors.add(a[1]);
		path = new ArrayList<>();
		path.add("scigraphdata/scigraph2001-2005.nt");	path.add("scigraphdata/scigraph2006-2008.nt");
		path.add("scigraphdata/scigraph2009-2010.nt");	path.add("scigraphdata/scigraph2011.nt");
		path.add("scigraphdata/scigraph2012.nt");	path.add("scigraphdata/scigraph2013.nt");
		path.add("scigraphdata/scigraph2014.nt");	path.add("scigraphdata/scigraph2015.nt");
		
		// Filter SciGraph-Data (keep data with "affiliation" and "contribution")
		filtered_path = new ArrayList<>();
		for(String p : path)	{
			String filtered = "filtered_" + p;	filtered_path.add(filtered);	filterFile(p, filtered);
		}
		
		// Filter SciGraph-Data (keep data with "hasOrganization")
		for(String p : filtered_path)	filterDataOrg(p, orgpath);	// appends to "scigraphOrganization.txt"

		// Get Contributions ({Name, SG-ID})
		for(String p : filtered_path)	getContributions(p, contpath);	// appends to "contributions.txt"
		
		// Get Affiliations
		ArrayList<String[]> cont = getFileContent(contpath, 2);
		for(String p : filtered_path)	getAffiliations(p, cont, affiliationspath);	// appends to "affiliations.txt"
		
		// Filter Affiliation-Data
		ArrayList<String[]> affil = getFileContent(affiliationspath, 2);
		ArrayList<String> affilIDs = new ArrayList<>();	for(String[] a : affil)	affilIDs.add(a[1]);
		filterDataAffil(filtered_path, affilIDs, afDatapath, afLabelpath);
	}
}
