package dblp_dataset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class RemoveDuplicatesNTFiles {

	private static ArrayList<String[]> getDuplicates(String dpath) {
		ArrayList<String[]> duplicates = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(dpath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String name = line.substring(0, line.indexOf("\t"));
					String duplicate = line.substring(line.indexOf("\t")+1);
					duplicates.add(new String[] {"<" + name + ">", "<" + duplicate + ">"});
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return duplicates;
	}
	
	private static void filterDuplicatesNTFiles(ArrayList<String> ntfiles, ArrayList<String[]> duplicates) {
		int counter = 0; int size = ntfiles.size();
		for(String file : ntfiles) {
			counter++;	System.out.println("Processing file " + counter + "/" + size + " ....");
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while((line = br.readLine()) != null) {
					int pos = line.indexOf("#authoredBy");
					if(pos != -1) {
						String obj = line.substring(pos + 13, line.length() - 2);
						int idx = exists(duplicates, obj);
						if(idx != -1) 	line.replace(obj, duplicates.get(idx)[0]);
					}
					appendToFile("f_" + file, line + "\n");
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	private static int exists(ArrayList<String[]> list, String item) {
		int idx = -1;
		for(int i = 0; i < list.size(); i++) 	if(list.get(i)[1].equals(item))	idx = i;
		return idx;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Processing Input:
		Scanner reader = new Scanner(System.in);
		System.out.println("Directory containing .nt-files: ");	String ntpath = reader.nextLine();
		System.out.println("de-duplicate file: ");	String duppath = reader.nextLine();
		reader.close();
		if(duppath.indexOf(".txt") == -1)	duppath += ".txt";
		boolean isdir = false;	File ntdir = null;
		try {	ntdir = new File(ntpath);	isdir = ntdir.isDirectory();	}
		catch(Exception e)	{	e.printStackTrace();	}
		if(!isdir)	{	System.out.println("Path is not a directory. Program exits!");	System.exit(0);	}
		File outputdir = new File("f_" + ntpath);	outputdir.mkdir();
				
		// Get list of files in NT-files directory:
		ArrayList<String> ntfiles = new ArrayList<>();	File[] listtmp = null;
		try {	listtmp = ntdir.listFiles();	}	catch(Exception e) {	e.printStackTrace();	}
		if(listtmp != null) {
			for(File f : listtmp) {
				String tmp = f.getName();	
				if((!tmp.substring(0, 1).equals(".")) && (tmp.indexOf(".nt") != -1))	ntfiles.add(ntpath + "/" + tmp);
			}
		}
		
		ArrayList<String[]> duplicates = getDuplicates(duppath);
		filterDuplicatesNTFiles(ntfiles, duplicates);
	}
	
}
