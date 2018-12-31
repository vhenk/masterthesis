import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ProcessSemanticScholarData {

	public static boolean filterPapers(ArrayList<String> inputfiles, String[] names, ArrayList<String> exactnames, String outputpath) {
		boolean success = false;
		for(int i = 0; i < inputfiles.size(); i++) {
			int j = i+1;	System.out.println("Processing File " + j + "/" + inputfiles.size() + " ....");
			ArrayList<String> filteredlines = new ArrayList<>();
			try(BufferedReader br = new BufferedReader(new FileReader(inputfiles.get(i)))) {
				String line;	
				while((line = br.readLine()) != null) {
					try(JsonReader reader = Json.createReader(new StringReader(line));) {
						JsonObject obj = reader.readObject();	reader.close();
						String venue = obj.getString("venue");
						// Compare with exact names:
						for(String v : exactnames) {
							if(venue.equals(v)) {
								filteredlines.add(line);	
								// Store result:
								appendToFile(outputpath, line+"\n");	break;
							}
						}
						// Compare with contained names:
						for(String v : names) {
							if(venue.indexOf(v) != -1)	{	filteredlines.add(line);	
							// Store result:
							appendToFile(outputpath, line+"\n");	break;	}
						}
					} catch(Exception e)	{	e.printStackTrace();	}
				}
				br.close();	success = true;
			} catch(IOException e)	{	e.printStackTrace();	}
		}		
		return success;
	}

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Input-file:
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory containing Semantic Scholar-data: ");	String s2dirpath = reader.nextLine();
		reader.close();
	
		// Checking input:
		boolean isdir = false;	File s2dir = null;
		try {	s2dir = new File(s2dirpath);	isdir = s2dir.isDirectory();	}
		catch(Exception e)	{	e.printStackTrace();	}
		if(!isdir)	{	System.out.println("Path is not a directory. Program exits!");	System.exit(0);	}
		
		// Get list of files in Semantic Scholar directory:
		ArrayList<String> s2files = new ArrayList<>();	File[] listtmp = null;
		try {	listtmp = s2dir.listFiles();	}	catch(Exception e) {	e.printStackTrace();	}
		if(listtmp != null) {
			for(File f : listtmp) {
				String tmp = f.getName();	if(!tmp.substring(0, 1).equals("."))	s2files.add(s2dirpath + "/" + tmp);
			}
		}
		
		// Venue-name-parts for comparison when filtering:
		String[] venuenameparts = new String[2];	venuenameparts[0] = "ISWC";	venuenameparts[1] = "Semantic Web";
		
		// Venue-names for exact comparison when filtering:
		ArrayList<String> venuenames = new ArrayList<>();
		venuenames.add("Artificial Intelligence and Law");	venuenames.add("BDA");	venuenames.add("BioData Mining");	
		venuenames.add("CKC");	venuenames.add("CLA");	venuenames.add("COLD");	venuenames.add("CrowdSem");	
		venuenames.add("Description Logics");	venuenames.add("EKAW");	venuenames.add("ESOE");	
		venuenames.add("Extreme Markup Languages®");	venuenames.add("FIRST");	venuenames.add("ICBO");	
		venuenames.add("Journal of Automated Reasoning");	venuenames.add("LISC");	venuenames.add("MSW");	
		venuenames.add("NatuReS");	venuenames.add("OBI");	venuenames.add("OM");	venuenames.add("Ontology Matching");
		venuenames.add("OWLED");	venuenames.add("PEAS");	venuenames.add("PSSS");	venuenames.add("RuleML");
		venuenames.add("RuleML Challenge");	venuenames.add("SATBI+SWIM");	venuenames.add("SEBD");	venuenames.add("SEBIZ");
		venuenames.add("Semantic Desktop Workshop");	venuenames.add("SemDesk");	venuenames.add("SEMPS");
		venuenames.add("SemWeb");	venuenames.add("SeRSy");	venuenames.add("SMRR");	venuenames.add("SPIM");	
		venuenames.add("SSN");	venuenames.add("SWCS");	venuenames.add("SWDB");	venuenames.add("SWPM");	venuenames.add("SWSWPC");
		venuenames.add("SWW 2.0");	venuenames.add("SWWS");	venuenames.add("UniDL");	venuenames.add("URSW");
		venuenames.add("WoMO");	venuenames.add("WOP");
		venuenames.add("2009 IEEE/WIC/ACM International Joint Conference on Web Intelligence and Intelligent Agent Technology");	
		venuenames.add("2015 IEEE/WIC/ACM International Conference on Web Intelligence and Intelligent Agent Technology (WI-IAT)");	
		venuenames.add("2016 IEEE Tenth International Conference on Semantic Computing (ICSC)");	
		venuenames.add("21st International Conference on Data Engineering (ICDE'05)");

		// Output-file:
		String s2datapath = "s2_papers.txt";
		
		// Filter data:
		boolean filtered = filterPapers(s2files, venuenameparts, venuenames, s2datapath);
		if(filtered)	System.out.println("S2-Data was filtered successfully! Remaining data was stored in " + s2datapath + ".");

	}	
}
