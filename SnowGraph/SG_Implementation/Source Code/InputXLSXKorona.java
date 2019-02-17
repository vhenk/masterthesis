package scigraph_dataset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class InputXLSXKorona {
	static String cpath = "";
	static class Paper {
		String paperID, title, year, doi; ArrayList<String> authors;
		public Paper(String paperID, String title, String year, String doi, ArrayList<String> contributions) {
			this.paperID = paperID;	this.title = title;	this.year = year;	this.doi = doi;
			this.authors = new ArrayList<>();
			for(String c : contributions)	{
				String nametmp = getAuthorName(c);	if(!nametmp.equals(""))	authors.add(nametmp);
			}
		}
		
		private String getAuthorName(String contributionID) {
			String name = "";
			try(BufferedReader br = new BufferedReader(new FileReader(cpath))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("/things/contributions/" + contributionID + ">") != -1) {
						int idx = line.indexOf("core/publishedName>");
						if(idx != -1)	name = line.substring(idx+21, line.length()-3);
						break;
					}	
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
			return name;
		}
	}
	
	static class Duplicate {
		String name; ArrayList<String> aliases;
		public Duplicate(String name, ArrayList<String> aliases) {
			this.name = name;	this.aliases = aliases;
		}
	}
	
	private static ArrayList<Paper> readFile(String path) {
		ArrayList<Paper> papers = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int counter = 0;
			while((line = br.readLine()) != null) {
				counter++; System.out.println("Processing Paper " + counter + " ...");
				if(line.indexOf("\t") != -1) {
					// SG-ID \t Title \t Year \t DOI \t Author1 \t Author2 ... 
					String id = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);
					String title = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);
					String year = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);
					String doi = line.substring(0, line.indexOf("\t"));
					line = line.substring(line.indexOf("\t")+1);
					
					// Get authors:
					ArrayList<String> authors = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							authors.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {	authors.add(line.trim());	line = "";	}
					}
					papers.add(new Paper(id, title, year, doi, authors));
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return papers;
	}
	
	private static void writeXLSXFile(ArrayList<Paper> papers, String path) {
		String sheetname = "Sheet";
		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet(sheetname);
		int rownumbers = papers.size() + 1;
		
		// First Row
		XSSFRow row_first = sheet.createRow(0);
		XSSFCell c1 = row_first.createCell(0); c1.setCellValue("Paper Number");
		XSSFCell c2 = row_first.createCell(1); c2.setCellValue("Title");
		XSSFCell c3 = row_first.createCell(2); c3.setCellValue("Number of Authors");
		XSSFCell c4 = row_first.createCell(3); c4.setCellValue("Year");
		
		// Other Rows
		for(int i = 0; i < rownumbers - 1; i++) {
			int r_num = i + 1;	XSSFRow row = sheet.createRow(r_num);
			// Number	Title	Num_Authors	 Year	Authors...
			
			// Cells
			XSSFCell cell1 = row.createCell(0); int num = i + 1;	cell1.setCellValue(num);
			XSSFCell cell2 = row.createCell(1); cell2.setCellValue(papers.get(i).title);
			XSSFCell cell3 = row.createCell(2);	int num_authors = papers.get(i).authors.size();	cell3.setCellValue(num_authors);
			int yearint = -1;	try {	yearint = Integer.parseInt(papers.get(i).year);	} catch(Exception e) {};
			XSSFCell cell4 = row.createCell(3);	cell4.setCellValue(yearint);
			for(int j = 0; j < papers.get(i).authors.size(); j++) {
				int tmp = 4 + j;	XSSFCell cellX = row.createCell(tmp);	cellX.setCellValue(papers.get(i).authors.get(j));
			}
		}
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			wb.write(fileOut);	wb.close();	fileOut.flush();	fileOut.close();
		} catch(Exception e)	{	e.printStackTrace();	}
	}
	
	private static void filterContributorNames(ArrayList<String> filenames, String outputpath) {
		System.out.println("Filtering Contribution-Data:");	int counter = 0; int num = filenames.size();
		for(String path : filenames) {
			counter++;	System.out.println("\tProcessing file " + counter + "/" + num + " ...");
			try(BufferedReader br = new BufferedReader(new FileReader(path))) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.indexOf("springernature.com/things/contributions/") != -1) {
						if(line.indexOf("/ontologies/core/publishedName") != -1) {
							appendToFile(outputpath, line + "\n");
						}
					}
				} br.close();
			} catch(IOException e) {	e.printStackTrace();	}
		}
	}
	
	private static void removeDuplicates(String dup_path, String inputpath, String outputpath) {
		System.out.println("Removing duplicates from author names ...");
		// Retrieve list of duplicates from manually constructed file:
		ArrayList<Duplicate> duplicates = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(dup_path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String name = line.substring(0, line.indexOf("\t"));	ArrayList<String> aliases = new ArrayList<>();
					line = line.substring(line.indexOf("\t")+1);
					while(!line.equals("")) {
						if(line.indexOf("\t") != -1) {
							aliases.add(line.substring(0, line.indexOf("\t")));
							line = line.substring(line.indexOf("\t")+1);
						}
						else {
							aliases.add(line.trim());	line = "";
						}
					}
					duplicates.add(new Duplicate(name, aliases));
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Remove aliases from contributors-file:
		try(BufferedReader br = new BufferedReader(new FileReader(inputpath))) {
			String line;
			while((line = br.readLine()) != null) {
				boolean next = false;
				for(Duplicate d : duplicates) {
					for(String a : d.aliases)	if(line.indexOf(a) != -1)	{
						line = line.replace(a, d.name);	next = true;	break;
					}
					if(next)	break;
				}
				appendToFile(outputpath, line + "\n");
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		File tmpdir = new File("data/tmp");	tmpdir.mkdir();	String path = "data/SciGraph-Data/";
		String contpath = "data/tmp/contributors.nt"; cpath = "data/data_filtered/sg_contributors_filtered.nt";
		String duppath = "data/duplicates.txt";	
		String paperpath = "data/output/sg_papers.txt";	String xlsxpath = "data/output/papers.xlsx";
		
		ArrayList<String> filenames = new ArrayList<>();	
		filenames.add(path + "scigraph2002.nt");	filenames.add(path + "scigraph2006.nt");	filenames.add(path + "scigraph2009.nt");
		filenames.add(path + "scigraph2011.nt");	filenames.add(path + "scigraph2012.nt");	filenames.add(path + "scigraph2013.nt");
		filenames.add(path + "scigraph2014.nt");	filenames.add(path + "scigraph2015.nt");
		filterContributorNames(filenames, contpath);
		removeDuplicates(duppath, contpath, cpath);
		
		ArrayList<Paper> papers = readFile(xlsxpath);
		writeXLSXFile(papers, paperpath);
		
		System.out.println("Removing temporary data ....");
		File tmp1 = new File(contpath);	tmp1.delete();	tmpdir.delete();
	}
}
