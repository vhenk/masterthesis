package dblp_dataset.post_processing;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class CollaborationFilter {
	static ArrayList<String> ntfiles = new ArrayList<>();	static String authorskeypath = "";
	static ArrayList<Collaboration> colist = new ArrayList<>();
	
	static class ClusterDirectory {
		String name, root; ArrayList<String> filenames;
		public ClusterDirectory(String name, String root) {
			this.name = name;	this.root = root;	this.filenames = new ArrayList<>();
			File dir = new File(root + "/" + name);
			
			// Get Filenames:
			File[] listtmp = null;
			try { listtmp = dir.listFiles(); } catch(Exception e) {	e.printStackTrace();	}
			if(listtmp != null) {
				for(File f : listtmp) {
					String tmp = f.getName();
					if((!tmp.substring(0, 1).equals(".")) && (tmp.indexOf(".txt") != -1))	filenames.add(tmp);
				}
			}			
		}
	} 
	
	static class Author {
		String id, uri, name;
		public Author(String id) {
			this.id = id;	this.uri = "";	this.name = "";
			
			// Get Name and URI:
			try(BufferedReader br = new BufferedReader(new FileReader(authorskeypath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						String idtmp = line.substring(0, line.indexOf("\t"));
						if(this.id.equals(idtmp)) {
							line = line.substring(line.indexOf("\t")+1);
							this.name = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
							this.uri = line.trim();	break;
						}
					}
				}
				br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	static class Collaboration {
		String author1, author2; boolean value;
		public Collaboration(String author1, String author2, boolean value) {
			this.author1 = author1;	this.author2 = author2; this.value = value;
		}
	}
	
	public static boolean collaborationExists(Author a1, Author a2) {
		boolean collaborated = false;	boolean checked = false;
		// Check if collaboration is already in colist:
		for(Collaboration c : colist) {
			if(((c.author1.equals(a1.id)) && (c.author2.equals(a2.id))) || ((c.author1.equals(a2.id)) && (c.author2.equals(a1.id)))) {
				checked = true;	collaborated = c.value;	break;
			}	
		}
		if(!checked) {
			for(String file : ntfiles) {
				try(BufferedReader br = new BufferedReader(new FileReader(file))) {
					String line;	boolean matcha1 = false;	boolean matcha2 = false;
					while((line = br.readLine()) != null) {
						String authortmp = "";
						if(line.indexOf("#authoredBy") != -1) authortmp = line.substring(line.indexOf("#authoredBy")+14, line.length()-3);
						if(authortmp.equals(a1.uri))	matcha1 = true;	if(authortmp.equals(a2.uri))	matcha2 = true;
						if(line.indexOf("#yearOfPublication") != -1) {
							String yeartmp = line.substring(line.indexOf("#yearOfPublication")+21, line.length()-3);
							int year = 0;	try { year = Integer.parseInt(yeartmp); } catch(Exception e) {}
							if((matcha1) && (matcha2) && (year < 2016)) {	collaborated = true; break;	}
							matcha1 = false; matcha2 = false;
						}
					}
					if(collaborated) break;
					br.close();
				} catch(IOException e) {	e.printStackTrace();	}
			}
			colist.add(new Collaboration(a1.id, a2.id, collaborated));
		}
		System.out.println("\tCo " + a1.id + " + " + a2.id + ": " + collaborated);
		return collaborated;
	}
	
	public static void filterSimAuthors(String path) {
		System.out.println("Filtering Similar-Authors-file ....");	String simauthorsoutputpath = "similarauthors_new.txt";
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;	int idx = 0;	int counter = 0; String authorid = "";
			ArrayList<String> authorids = new ArrayList<>(); ArrayList<String> simvalues = new ArrayList<>();
			while((line = br.readLine()) != null) {
				if(idx == 0) {	authorid = line.trim();	idx++;	}
				else if(idx == 1) {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							authorids.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {	authorids.add(line.trim());	break;	}
					}
					idx++;
				}
				else {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							simvalues.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {	simvalues.add(line.trim());	break;	}
					}
					// Get Entries not corresponding to Collaborations from lists:
					ArrayList<String> authorsnew = new ArrayList<>();	ArrayList<String> valuesnew = new ArrayList<>();
					for(int i = 0; i < authorids.size(); i++) {
						Author a1 = new Author(authorid);	Author a2 = new Author(authorids.get(i));
						if(!collaborationExists(a1, a2)) {
							authorsnew.add(authorids.get(i));	valuesnew.add(simvalues.get(i));
						}
					}
					
					String content = "";
					if(counter != 0)	content += "\n";
					content += authorid + "\n";
					for(int i = 0; i < authorsnew.size(); i++) {
						content += authorsnew.get(i);	if(i != authorsnew.size()-1)	content += "\t";
					}
					content += "\n";
					for(int i = 0; i < valuesnew.size(); i++) {
						content += valuesnew.get(i);	if(i != valuesnew.size()-1)	content += "\t";
					}
					appendToFile(simauthorsoutputpath, content);
					authorids = new ArrayList<>();	simvalues = new ArrayList<>();
					counter++; idx = 0;
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void filterClusters(ClusterDirectory cd, String outputdir) {
		String authorid = "A" + cd.name; System.out.println("Filtering Clusters for " + authorid + " ....");
		File output = new File(outputdir + "/" + cd.name); output.mkdir();	String dirpath = cd.root + "/" + cd.name;	
		for(String file : cd.filenames) {
			String oldpath = dirpath + "/" + file;	String newpath = output + "/" + file;
			try(BufferedReader br = new BufferedReader(new FileReader(oldpath))) {
				String line;	int countadded = 0;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1) {
						String idtmp = line.substring(0, line.indexOf("\t"));
						if(idtmp.equals(authorid))	{
							if(countadded != 0)	line = "\n" + line;
							appendToFile(newpath, line);	countadded++;
						}
						else {
							Author a1 = new Author(authorid);	Author a2 = new Author(idtmp);
							if(!collaborationExists(a1, a2)) {
								if(countadded != 0)	line = "\n" + line;
								appendToFile(newpath, line);	countadded++;
							}
						}
					}	
				}
				br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	public static void cleanClusters(ClusterDirectory cd, String outputdir) {
		String authorid = "A" + cd.name;
		for(String file : cd.filenames) {
			String path = outputdir + "/" + cd.name + "/" + file;
			try(BufferedReader br = new BufferedReader(new FileReader(path))) {
				String line;	int authorcount = 0;	int otherscount = 0;
				while((line = br.readLine()) != null) {
					if(line.indexOf("\t") != -1)	{
						String idtmp = line.substring(0, line.indexOf("\t"));
						if(idtmp.equals(authorid))	authorcount++;
						else	otherscount++;
					}
				}
				br.close();
				
				if((otherscount == 0) || (authorcount == 0)) {	File f = new File(path);	f.delete();	}
			} catch(IOException e) {	e.printStackTrace();	}
		}	
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Process Input:
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory conatining selected clusters: ");	String rootdir = reader.nextLine();	
		System.out.println("Name of Similar-Authors-file: ");	String simauthorpath = reader.nextLine();
		System.out.println("Name of Authors-Key-file: ");	authorskeypath = reader.nextLine();
		System.out.println("Directory with .nt-files: ");	String dirpath = reader.nextLine(); reader.close();
		
		// Checking Input:
		if(authorskeypath.indexOf(".txt") == -1)	authorskeypath += ".txt";
		if(simauthorpath.indexOf(".txt") == -1)	simauthorpath += ".txt";
		File dir = new File(dirpath);	File[] filestmp = dir.listFiles();
		for(File f : filestmp) {
			if((!(f.getName().substring(0, 1).equals(".")))	&& (f.getName().indexOf(".nt") != -1)) 	
				ntfiles.add(dirpath + "/" + f.getName());
		}
		
		boolean isdir = false;	File directory = null;
		try { directory = new File(rootdir);	isdir = directory.isDirectory(); }
		catch(Exception e) {	e.printStackTrace();	}
		if(!isdir) {	System.out.println("Path to clusters is not a directory. Program aborted!"); System.exit(0);	}
		
		// Get subdirectories:
		ArrayList<ClusterDirectory> clusterdirs = new ArrayList<>();	File[] listtmp = null;
		try { listtmp = directory.listFiles(); } catch(Exception e) { e.printStackTrace(); }
		if(listtmp != null) {
			for(File f : listtmp) {	if(f.isDirectory())	clusterdirs.add(new ClusterDirectory(f.getName(), rootdir));	}
		}
		
		// Filter Similar-Authors-file:
		filterSimAuthors(simauthorpath);
		
		// Output-root-directory:
		String outputpath = "filtered-clusters";	File outputdir = new File(outputpath);	outputdir.mkdir();
		
		// Filter clusters to get predictions:
		System.out.println("Filtering Clusters ....");
		for(ClusterDirectory cd : clusterdirs)	filterClusters(cd, outputpath);
		
		// Remove clusters with only 1 node and empty clusters:
		System.out.println("Cleaning Data ....");
		System.out.println("Removing empty Clusters ....");
		for(ClusterDirectory cd : clusterdirs) cleanClusters(cd, outputpath);
	}
}