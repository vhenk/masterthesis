package scigraph_dataset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ExtractPapers {
	static class Source {
		String sourceyear; String[] editions;
		public Source(String sourceyear, String[] editions) {
			this.sourceyear = sourceyear; this.editions = editions;
		}
	}
	
	static class Paper {
		String paperID, title, year, doi; ArrayList<String> authors;
		public Paper(String paperID, String sourceyear) {
			this.paperID = paperID;		this.year = "";
			this.title = "";	this.doi = "";	this.authors = new ArrayList<>();
			
			// Retrieving missing properties:
			String path = "data/SciGraph-Data/scigraph" + sourceyear + ".nt"; 
			try(BufferedReader br = new BufferedReader(new FileReader(path))) {
				String line;
				while((line = br.readLine()) != null) {
					String idtmp = line.substring(line.indexOf("book-chapters/")+14, line.indexOf(">"));
					if(idtmp.equals(paperID)) {
						if(line.indexOf("ontologies/core/title>") != -1) {
							line = line.substring(line.indexOf("/ontologies/core/title")+25);
							this.title = line.substring(0, line.length()-3);
							if(this.title.indexOf("\n") != -1)	this.title = this.title.replaceAll("\n", "");
							this.title = this.title.trim();
						}
						else if(line.indexOf("ontologies/core/copyrightYear>") != -1) {
							line = line.substring(line.indexOf("/ontologies/core/copyrightYear")+33);
							this.year = line.substring(0, 4);
						}
						else if(line.indexOf("ontologies/core/doi>") != -1) {
							line = line.substring(line.indexOf("/ontologies/core/doi")+23);
							this.doi = line.substring(0, line.length()-3);
						}
						else if(line.indexOf("ontologies/core/hasContribution>") != -1) {
							line = line.substring(line.indexOf("/things/contributions/")+22);
							String tmp = line.substring(0, line.length()-3); authors.add(tmp);
						}
					}					
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	private static ArrayList<String> retrievePaperIDs(String sourceyear, String[] editions) {
		ArrayList<String> paperIDs = new ArrayList<>();	String path = "data/SciGraph-Data/scigraph" + sourceyear + ".nt";
		System.out.println("Retrieving PaperIDs " + sourceyear + " ...");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;	int counter = 0;
			while((line = br.readLine()) != null) {
				for(String e : editions) {
					String s = "/book-editions/" + e;
					if(line.indexOf(s) != -1) {
						String paperID = line.substring(line.indexOf("book-chapters/")+14, line.indexOf(">"));
						paperIDs.add(paperID);	counter++;
					}
				}
			} br.close();
			System.out.println(counter + " matches found!");
		} catch(IOException e) {	e.printStackTrace();	}
		return paperIDs;
	}
	
	private static ArrayList<String> getAuthorNames(ArrayList<String> contributions, ArrayList<Source> files) {
		ArrayList<String> names = new ArrayList<>();
		for(Source source : files) {
			String path = "data/SciGraph-Data/scigraph" + source.sourceyear + ".nt"; 
			try(BufferedReader br = new BufferedReader(new FileReader(path))) {
				String line;
				while((line = br.readLine()) != null) {
					int pos = line.indexOf("/things/contributions/");
					if(pos != -1) {
						line = line.substring(pos);
						String cID = line.substring(pos + 22, line.indexOf(">"));
						if(contributions.contains(cID)) {
							int idx = line.indexOf("core/publishedName>");
							if(idx != -1)	names.add(line.substring(idx + 21, line.length() - 3));
						}
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
		// remove duplicates:
		Set<String> cleanup = new HashSet<>();	cleanup.addAll(names);	names.clear();	names.addAll(cleanup);
		
		return names;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		ArrayList<Source> ntfiles = new ArrayList<>();
		ntfiles.add(new Source("2009", new String[] {"321c0ea9af37adb9d7547dcff1d6fa46", "3a083dd1dd5c9b99870c6b91e7c18a21", "753e0a3161a02abaf632e0a3435dab9f"}));
		ntfiles.add(new Source("2006", new String[] {"c7169dfebde5497295f894d6f4a06e61", "519537007a8f221bc546afdf4d902ab1", "92f42d142a3ea2c666b2f6a414156278"}));
		ntfiles.add(new Source("2002", new String[] {"ebd3a6767c7630330a58e62481897f34", "c49d2b70e92922b83af474e9ca529454", "6b461ac2dadea7381f21bb35df012b5d", "5f46d0824aca74226a059cecb075f753"}));
		
		File dir = new File("data/output");	dir.mkdir();	ArrayList<String> contributions = new ArrayList<>();
		String authornamespath = "data_sg/output/authornames.txt";
		
		for(Source nt : ntfiles) {
			ArrayList<String> paperIDs = retrievePaperIDs(nt.sourceyear, nt.editions);	
			int counter = 0;	int size = paperIDs.size();
			for(String p : paperIDs) {
				counter++;	System.out.println("\tProcessing Paper " + counter + "/" + size + " ...");
				Paper paper = new Paper(p, nt.sourceyear);
				String paperline = paper.paperID + "\t" + paper.title + "\t" + paper.year + "\t" + paper.doi;
				for(String a : paper.authors)	{
					paperline += "\t" + a;
					if(!contributions.contains(a))	contributions.add(a);
				}
				paperline += "\n";
				appendToFile("data/ouput/sgpapers.txt", paperline);
			}
		}
		ArrayList<String> authornames = getAuthorNames(contributions, ntfiles);
		for(String an : authornames)	appendToFile(authornamespath, an + "\n");
	}
}
