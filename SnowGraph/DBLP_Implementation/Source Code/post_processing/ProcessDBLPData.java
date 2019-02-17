package dblp_dataset.post_processing;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ProcessDBLPData {

	public static void filterFiles(String dirpath, ArrayList<String> ntfiles, String papersdirpath, String confsdirpath) {
		int total = ntfiles.size();
		File papersdir = new File(papersdirpath);	papersdir.mkdir();
		File confsdir = new File(confsdirpath);	confsdir.mkdir();
		for(int i = 0; i < total; i++) {
			int j = i+1;	System.out.println("Processing File (" + j + "/" + total + ")\n");
			String path_p = papersdirpath + "/" + ntfiles.get(i);	
			String path_c = confsdirpath + "/" + ntfiles.get(i);
			ArrayList<String> subjects_p = new ArrayList<>();
			
			// Go through file to get subjects of relevant papers and conferences:
			try(BufferedReader br = new BufferedReader(new FileReader(dirpath + "/" + ntfiles.get(i)))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf(">") != -1) {
						// Get Subject of this line and check if related to any conference:
						String subject = line.substring(0, line.indexOf(">")+1);
						if(subject.indexOf("dblp.org/rec/conf/") != -1) {
							// Check if line refers to conference edition:
							if(line.substring(line.indexOf(">")+1).indexOf("#authoredBy>") != -1)	subjects_p.add(subject);
						}
					}
				}
				br.close();
				// Remove duplicates from subjects_c:
				Set<String> cleanup = new HashSet<>();	cleanup.addAll(subjects_p);	subjects_p.clear();	subjects_p.addAll(cleanup);
			}	catch(IOException e)	{	e.printStackTrace();	}
			
			// Go through file again:
			try(BufferedReader br = new BufferedReader(new FileReader(dirpath + "/" + ntfiles.get(i)))) {
				// Create new file in both target-directories:
				File file_p = new File(path_p);	file_p.createNewFile();	
				File file_c = new File(path_c);	file_c.createNewFile();
				
				// Buffers to write in new files:
				BufferedWriter out_p = new BufferedWriter(new FileWriter(path_p));
				BufferedWriter out_c = new BufferedWriter(new FileWriter(path_c));

				String line;
				while((line = br.readLine()) != null) {
					if(line.trim().equals(""))	{	out_p.write("");	out_c.write("");	}
					else if(line.indexOf(">") != -1) {
						// Get Subject of this line:
						String subject = line.substring(0, line.indexOf(">")+1);
						
						// Check if subject is refers to a conference or conference paper:
						if(subject.indexOf("dblp.org/rec/conf/") != -1) {
							// Check if it's a conference edition or a paper:
							boolean isPaper = false;
							for(String s : subjects_p)	{	if(s.equals(subject))	isPaper = true;	}
							
							// Write line in corresponding file:
							if(isPaper) 	out_p.write(line + "\n");	else out_c.write(line + "\n");
						}
					}
				}
				br.close();	out_p.close();	out_c.close();
			} catch(IOException e)	{	e.printStackTrace();	}	
		}
		eraseEmptyFiles(papersdirpath, ntfiles);	eraseEmptyFiles(confsdirpath, ntfiles);
	}
	
	public static void eraseEmptyFiles(String directory, ArrayList<String> ntfiles) {
		System.out.println("Deleting empty files in " + directory + "/ ....");
		int delcount = 0;	int total = ntfiles.size();
		for(int i = 0; i < total; i++) {
			try(BufferedReader br = new BufferedReader(new FileReader(directory + "/" + ntfiles.get(i)))) {
				String line;	boolean empty = true;
				while((line = br.readLine()) != null) {	if(!line.trim().equals(""))	empty = false;	}	br.close();
				if(empty)	{	File file = new File(directory + "/" + ntfiles.get(i));	file.delete();	delcount++;	}
			} catch(IOException e)	{	e.printStackTrace();	}
		}
		System.out.println(delcount + " of " + total + " Files deleted!");
	}
	
	public static void main(String[] args) {
		// Process Input-files:
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory with .nt-files: ");	String dirpath = reader.nextLine();	reader.close();
		ArrayList<String> ntfiles = new ArrayList<>();	File dir = new File(dirpath);	File[] filestmp = dir.listFiles();	
		for(File f : filestmp) {	if(!(f.getName().substring(0, 1)).equals("."))	ntfiles.add(f.getName());	}
		
		// Output-files:
		String papersdir = "dblp_papers";	String confsdir = "dblp_confs";
		
		// Filter .nt-data into 2 new directories:
		filterFiles(dirpath, ntfiles, papersdir, confsdir);
	}
}
