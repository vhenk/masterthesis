package scigraph_dataset.post_processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class CollaborationFilter {	
	static class ClusterDirectory {
		String name, root;	ArrayList<String> filenames;
		public ClusterDirectory(String name, String root) {
			this.name = name;	this.root = root;	this.filenames = new ArrayList<>();
			File dir = new File(root + "/" + name);
			
			// Get Filenames:
			File[] listtmp = null;
			try {	listtmp = dir.listFiles();	}	catch(Exception e) {	e.printStackTrace();	}
			if(listtmp != null) {
				for(File f : listtmp) {
					String tmp = f.getName();
					if((!tmp.substring(0, 1).equals(".")) && (tmp.indexOf(".txt") != -1))	filenames.add(tmp);
				}
			}
		}
	}
	
	static class Collaboration {
		String author1, author2;
		
		public Collaboration(String author1, String author2) {
			this.author1 = author1;	this.author2 = author2;
		}
		
		private boolean equalsC(Collaboration c) {
			boolean same = false;
			if((this.author1.equals(c.author1)) && (this.author2.equals(c.author2)))	same = true;
			else if((this.author2.equals(c.author1)) && (this.author1.equals(c.author2)))	same = true;
			return same;
		}
	}
	
	private static boolean containsCollaboration(ArrayList<Collaboration> colist, Collaboration c) {
		boolean exists = false;
		for(Collaboration ctmp : colist)	if(ctmp.equalsC(c))	{	exists = true; break;	}
		return exists;
	}
	
	private static boolean matchingEntry(ArrayList<String> list1, ArrayList<String> list2) {
		boolean match = false;
		for(String s1 : list1)
			for(String s2 : list2)
				if(s1.equals(s2)) {	match = true;	break;	}
		return match;
	}
	
	private static ArrayList<Collaboration> getCollaborations(String path) {
		System.out.println("Retrieving existing collaborations ....");
		HashMap<String, ArrayList<String>> authors = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String atmp = line.substring(0, line.indexOf("\t"));
					String idtmp = line.substring(line.indexOf("\t")+1);
					if(!authors.containsKey(atmp)) {
						ArrayList<String> listtmp = new ArrayList<>();	listtmp.add(idtmp);
						authors.put(atmp, listtmp);
					}
					else {
						ArrayList<String> listtmp = authors.get(atmp);
						if(!listtmp.contains(idtmp))	{
							listtmp.add(idtmp);	authors.replace(atmp, listtmp);
						}
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		ArrayList<Collaboration> clist = new ArrayList<>();
		for(String key1 : authors.keySet()) {
			for(String key2 : authors.keySet()) {
				if(!key1.equals(key2)) {
					ArrayList<String> papers1 = authors.get(key1);	ArrayList<String> papers2 = authors.get(key2);
					if(matchingEntry(papers1, papers2)) {
						Collaboration ctmp = new Collaboration(key1, key2);
						if(!containsCollaboration(clist, ctmp))	clist.add(ctmp);
					}
				}				
			}
		}
		return clist;
	}
	
	private static void filterSimAuthors(ArrayList<Collaboration> colist, String path, String outputpath) {
		System.out.println("Filtering Similar-Authors-file ....");
		String simauthorsoutputpath = outputpath + "similarauthors_new.txt";
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;	int idx = 0;	int counter = 0;	String authorid = "";
			ArrayList<String> authorlist = new ArrayList<>();	ArrayList<String> simvalues = new ArrayList<>();
			while((line = br.readLine()) != null) {
				if(idx == 0)	{	authorid = line.trim(); idx++;	}
				else if(idx == 1) {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							authorlist.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {	authorlist.add(line.trim());	break;	}
					}
					idx++;
				}
				else {
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							simvalues.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						} else {	simvalues.add(line.trim());	break;	}
					}
					// Get Entries not correpsonding to Collaborations from lists:
					ArrayList<String> authorsnew = new ArrayList<>();
					ArrayList<String> valuesnew = new ArrayList<>();
					for(int i = 0; i < authorlist.size(); i++) {
						Collaboration ctest = new Collaboration(authorid, authorlist.get(i));
						if(!containsCollaboration(colist, ctest)) {
							authorsnew.add(authorlist.get(i));	valuesnew.add(simvalues.get(i));
						}
					}
					
					// Print new file:
					String content = "";
					if(counter != 0)	content += "\n";
					content += authorid + "\n";
					for(int i = 0; i < authorsnew.size(); i++) {
						content += authorsnew.get(i);
						if(i != authorsnew.size() - 1)	content += "\t";
					}
					content += "\n";
					for(int i = 0; i < valuesnew.size(); i++) {
						content += valuesnew.get(i);
						if(i != valuesnew.size() - 1) content += "\t";
					}
					appendToFile(simauthorsoutputpath, content);
					authorlist = new ArrayList<>();	simvalues = new ArrayList<>();	counter++;	idx = 0;
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	private static void filterClusters(ArrayList<Collaboration> clist, ClusterDirectory cd, String outputdir) {
		String authorid = "A" + cd.name;	System.out.println("Filtering Clusters for " + authorid + " ....");
		File output = new File(outputdir + "/" + cd.name);	output.mkdir();
		String dirpath = cd.root + "/" + cd.name;
		for(String file : cd.filenames) {
			String oldpath = dirpath + "/" + file;	String newpath = output + "/" + file;
			try(BufferedReader br = new BufferedReader(new FileReader(oldpath))) {
				String line;	int countadded = 0;
				while((line = br.readLine()) != null) {
					String idtmp = line.substring(0, line.indexOf("\t"));
					if(line.indexOf("\t") != -1) {
						if(idtmp.equals(authorid)) {
							if(countadded != 0)	line = "\n" + line;
							appendToFile(newpath, line);	countadded++;
						}
						else {
							Collaboration ctest = new Collaboration(authorid, idtmp);
							if(!containsCollaboration(clist, ctest)) {
								if(countadded != 0)	line = "\n" + line;
								appendToFile(newpath, line);	countadded++;
							}
						}
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	private static void cleanClusters(ClusterDirectory cd, String outputdir) {
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
				} br.close();
				
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
		System.out.println("Directory containing selected clusters: ");	String dir = reader.nextLine();
		System.out.println("Name of Similar-Authors-file: ");	String simauthorpath = reader.nextLine();
		System.out.println("Name of Authors-Publications-file: ");	String authpubpath = reader.nextLine();
		System.out.println("Path to output-directory: ");	String odirpath = reader.nextLine();
		reader.close();
		
		// Checking Input:
		if(simauthorpath.indexOf(".txt") == -1)	simauthorpath += ".txt";
		if(authpubpath.indexOf(".txt") == -1)	authpubpath += ".txt";
		
		boolean isdir = false;	File rootdir = null;
		try {	rootdir = new File(odirpath);	isdir = rootdir.isDirectory();	}
		catch(Exception e) {	e.printStackTrace();	}
		if(!isdir) {	System.out.println("Path to clusters is not a directory. Program aborted!");	System.exit(0);	}
		if(!odirpath.substring(odirpath.length()-1).equals("/"))	odirpath += "/";
		
		isdir = false;	File directory = null;
		try {	directory = new File(dir);	isdir = directory.isDirectory(); }
		catch(Exception e)	{	e.printStackTrace();	}
		if(!isdir) {	System.out.println("Path to clusters is not a directory. Program aborted!");	System.exit(0);	}
		
		// Get Cluster-sub-directories:
		ArrayList<ClusterDirectory> clusterdirs = new ArrayList<>();	File[] listtmp = null;
		try {	listtmp = directory.listFiles();	} catch(Exception e) {	e.printStackTrace();	}
		if(listtmp != null) {
			for(File f : listtmp) {	if(f.isDirectory())	clusterdirs.add(new ClusterDirectory(f.getName(), dir));	}
		}
		
		ArrayList<Collaboration> co_list = getCollaborations(authpubpath);
		
		// Filter Similar-Authors-file:
		filterSimAuthors(co_list, simauthorpath, odirpath);
		
		// Output-root-directory:
		String outputpath = odirpath + "filtered-clusters";	File outputdir = new File(outputpath);	outputdir.mkdir();
		
		// Filter clustes to get predictions:
		System.out.println("Filtering Clusters ....");
		for(ClusterDirectory cd : clusterdirs)	filterClusters(co_list, cd, outputpath);
		
		// Remove clusters with only 1 node and empty clusters:
		System.out.println("Cleaning Data and removing empty Clusters ....");
		for(ClusterDirectory cd : clusterdirs)	cleanClusters(cd, outputpath);
	}
	
}
