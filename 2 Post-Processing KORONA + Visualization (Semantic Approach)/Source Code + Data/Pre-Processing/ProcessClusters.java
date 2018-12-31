import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

public class ProcessClusters {
	static ArrayList<Author> authors = new ArrayList<>();	static String newdir = "selectedpredictions";
	static String authorsmatrixpath = "";
	static String clusterdirpath = "";	static ArrayList<String> clusterfiles = new ArrayList<>();	

	static class Author {
		String id; ArrayList<SimAuthor> simauthors;	
		public Author(String id) {
			this.id = id;	this.simauthors = new ArrayList<>();
			int number = Integer.parseInt(this.id.substring(1));	ArrayList<String> filenames = new ArrayList<>();
			
			// Create sub-directory for this Author:
			File subdir = new File(newdir + "/" + number);	subdir.mkdir();
			for(String f : clusterfiles) {
				String pathtmp = clusterdirpath + "/" + f;
				if(containsAuthor(pathtmp, this.id)) {
					File src = new File(pathtmp);	File dest = new File(newdir + "/" + number + "/" + number + f);	filenames.add(number + f);
					try {	Files.copy(src.toPath(), dest.toPath());	} 	catch(IOException e)	{	e.printStackTrace();	}
				}
			}
			
			// Get similar Authors:
			System.out.println("Getting similar authors for " + this.id + " ....");
			for(String s : filenames) {
				try(BufferedReader br = new BufferedReader(new FileReader(newdir + "/" + number + "/" + s))) {
					String line;
					while((line = br.readLine()) != null) {
						String idtmp = line.substring(0, line.indexOf("\t"));
						if(!idtmp.equals(this.id)) {
							boolean exists = false;
							for(SimAuthor sa : this.simauthors) {	if(sa.id.equals(idtmp))	exists = true;	}
							if(!exists)	this.simauthors.add(new SimAuthor(idtmp, 0));
						}	
					}
					br.close();
				}
				catch(IOException e) {	e.printStackTrace();	}
			}
			
			// Assign similarity values to similar Authors:
			System.out.println("\t Assigning similarity values ..");
			String allIDs = "";
			try(BufferedReader br = new BufferedReader(new FileReader(authorsmatrixpath))) {
				String line;	int idx = 0;
				while((line = br.readLine()) != null) {
					if(idx == number)	{	allIDs = line;	break;	}	idx++;
				}
				br.close();
			}
			catch(IOException e)	{	e.printStackTrace();	}
			for(SimAuthor sa : this.simauthors) {
				int idtmp = Integer.parseInt(sa.id.substring(1));	String line = allIDs;	int idx = idtmp - 1;	int j = 0;
				System.out.println("\t  Searching for A" + idtmp);
				while(j < idx) {	line = line.substring(line.indexOf(" ")+1);	j++;	}
				double value = Double.parseDouble(line.substring(0, line.indexOf(" ")));
				sa.weight = value;
			}
			System.out.println("");
		}
	}

	static class SimAuthor {
		String id; double weight;
		public SimAuthor(String id, double weight) {
			this.id = id;	this.weight = weight;
		}
	}
	
	public static boolean containsAuthor(String filepath, String author) {
		boolean contained = false;	boolean containsother = false;
		try(BufferedReader br = new BufferedReader(new FileReader(filepath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String tmp = line.substring(0, line.indexOf("\t"));
					if(tmp.equals(author)) contained = true;	else	containsother = true;
				}		
			}
			br.close();		if(!containsother)	contained = false;
		}
		catch(IOException e) { e.printStackTrace(); }
		return contained;
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
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory containing cluster-files: ");	clusterdirpath = reader.nextLine();
		System.out.println("Name of Authors-Matrix-file: ");	authorsmatrixpath = reader.nextLine();
		System.out.println("Numbers of authors to retrieve - separated by single spaces (i.e., 2 87 138): ");
		String authorsinput = reader.nextLine().trim();	reader.close();
		if(authorsmatrixpath.indexOf(".txt") == -1)	authorsmatrixpath += ".txt";
		
		// Checking input:
		boolean isdir = false;	File clusterdir = null;
		try { clusterdir = new File(clusterdirpath);	isdir = clusterdir.isDirectory();	}
		catch(Exception e)	{	e.printStackTrace();	}
		if(!isdir)	{	System.out.println("Path to clusters is not a directory. Program aborted!");	System.exit(0);	}
		
		// Get list of files in cluster-directory:
		File[] listtmp = null;
		try {	listtmp = clusterdir.listFiles();	} 	catch(Exception e)	{	e.printStackTrace();	}
		if(listtmp != null) {
			for(File f : listtmp) {
				String tmp = f.getName();
				if((!tmp.substring(0, 1).equals(".")) &&(tmp.indexOf(".txt") != -1))	clusterfiles.add(tmp);
			}
		}
		
		// Process Authors' numbers:
		File dir = new File(newdir);	dir.mkdir();
		while(authorsinput.length() > 0) {
			if(authorsinput.indexOf(" ") != -1) {
				int pos = authorsinput.indexOf(" ");
				try {	int atmp = Integer.parseInt(authorsinput.substring(0, pos));	authors.add(new Author("A" + atmp));	}	
				catch(Exception e)	{	System.out.println("Invalid input for authors' numbers. Program aborted!");	System.exit(1);	}
				authorsinput = authorsinput.substring(pos + 1);
			}
			else {
				try {	int atmp = Integer.parseInt(authorsinput);	authors.add(new Author("A" + atmp));	break;	}
				catch(Exception e)	{	System.out.println("Invalid input for authors' numbers. Program aborted!");	System.exit(1);	}
			}
		}
		System.out.println("Filtering of Cluster-files completed!");
		
		// Creating Similar Authors Output-file:
		String simauthorsfilepath = "similarauthors.txt";	String simauthorsfile = "";
		for(int i = 0; i < authors.size(); i++) {
			simauthorsfile += authors.get(i).id + "\n";
			for(int j = 0; j < authors.get(i).simauthors.size(); j++) {
				if(j != authors.get(i).simauthors.size()-1)	simauthorsfile += authors.get(i).simauthors.get(j).id + "\t";
				else	simauthorsfile += authors.get(i).simauthors.get(j).id;
			}
			simauthorsfile += "\n";
			for(int j = 0; j < authors.get(i).simauthors.size(); j++) {
				if(j != authors.get(i).simauthors.size()-1)	simauthorsfile += authors.get(i).simauthors.get(j).weight + "\t";
				else	simauthorsfile += authors.get(i).simauthors.get(j).weight;
			}
			if(i != authors.size()-1)	simauthorsfile += "\n";	
		}
		writeToNewFile(simauthorsfilepath, simauthorsfile);
	}
}