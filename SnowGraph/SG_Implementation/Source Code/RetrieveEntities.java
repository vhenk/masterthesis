package scigraph_dataset;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RetrieveEntities {
	static HashMap<String, Integer> authordict;
	
	static class Paper {
		String title; int year, id; ArrayList<String> authors;
		public Paper(int id, String title, int year, ArrayList<String> authors) {
			this.id = id;	this.title = title;	this.year = year;	this.authors = authors;
		}
	}
	
	static class Conference {
		int year; ArrayList<String> authors;
		public Conference(int year, ArrayList<Paper> papers) {
			this.year = year;	authors = new ArrayList<>();
			for(Paper p : papers) {
				if(p.year == this.year)	for(String a : p.authors)	if(!this.authors.contains(a))	this.authors.add(a);
			}
		}
	}
	
	private static ArrayList<Paper> retrievePapers(String path) {
		System.out.println("Retrieving papers from Excel-file ....");
		ArrayList<Paper> papers = new ArrayList<>();
		try {
			InputStream excelFile = new FileInputStream(path);
			XSSFWorkbook wb = new XSSFWorkbook(excelFile);	XSSFSheet sheet = wb.getSheetAt(0);
			XSSFRow row;	XSSFCell cell;	Iterator<Row> rows = sheet.rowIterator();
			int rownum = 1;
			while(rows.hasNext()) {
				row = (XSSFRow) rows.next();	Iterator<Cell> cells = row.cellIterator();	int counter = 0;
				if(rownum != 1) {
					int number = -1;	int year = -1;	String title = "";
					ArrayList<String> authors = new ArrayList<>();
					
					while(cells.hasNext()) {
						cell = (XSSFCell) cells.next();
						if(counter == 0)	number = (int) cell.getNumericCellValue();
						else if(counter == 1)	title = cell.getStringCellValue();
						else if(counter == 3)	year = (int) cell.getNumericCellValue();
						else if(counter > 3)	authors.add(cell.getStringCellValue());
						counter++;
					}
					if((number != -1) && (year != -1) && (!title.equals("")))	papers.add(new Paper(number, title, year, authors));
				}
				rownum++;
			}
			wb.close();
		} catch(Exception e) {	e.printStackTrace();	}
		return papers;
	}
	
	private static void storeAuthorLists(ArrayList<Paper> papers, String dirpath) {
		System.out.println("Generating Author-files ....");
		authordict = new HashMap<>();
		String authorpath = dirpath + "Author.txt";	String authormap = dirpath + "author-key-map.txt";
		ArrayList<String> authors = new ArrayList<>();
		for(Paper p : papers) 	for(String a : p.authors)	if(!authors.contains(a))	authors.add(a);
		appendToFile(authorpath, authors.size() + "\n");
		for(int i = 0; i < authors.size(); i++) {
			int j = i + 1;
			authordict.put(authors.get(i), j);
			appendToFile(authorpath, "A" + j + "\n");
			appendToFile(authormap, "A" + j + "\t" + authors.get(i) + "\n");
		}
	}

	private static double getConfSimilarity(ArrayList<Conference> conferences, int c1, int c2) {
		double similarity = -1;
		if(c1 == c2)	return 1.0;
		ArrayList<String> authors1 = conferences.get(c1).authors;
		ArrayList<String> authors2 = conferences.get(c2).authors;
		ArrayList<String> inBothConfs = new ArrayList<>();	ArrayList<String> inEitherConf = new ArrayList<>();
		for(String a : authors1) {
			if(authors2.contains(a))	if(!inBothConfs.contains(a))	inBothConfs.add(a);
		}
		inEitherConf = authors1;
		for(String a : authors2)	if(!inEitherConf.contains(a))	inEitherConf.add(a);
		if(inEitherConf.size() == 0)	return 0.0;
		similarity = (double) inBothConfs.size() / inEitherConf.size();
		return similarity;
	}
	
	private static void storeConferenceLists(ArrayList<Paper> papers, String dirpath) {
		System.out.println("Generating Conference-files ....");
		ArrayList<Integer> conferences = new ArrayList<>();		ArrayList<Conference> confs = new ArrayList<>();
		String confpath = dirpath + "Conf.txt";	String confmatrixpath = dirpath + "Conf_matrix.txt";
		for(Paper p : papers)	if(!conferences.contains(p.year))	conferences.add(p.year);
		Collections.sort(conferences);
		for(int c : conferences)	confs.add(new Conference(c, papers));
		appendToFile(confpath, conferences.size() + "\n");
		for(int c : conferences)	appendToFile(confpath, "C" + c + "\n");

		// Retrieve Conference Similarity Matrix:
		double[][] confmatrix = new double[confs.size()][confs.size()];
		for(int i = 0; i < confs.size(); i++) {
			for(int j = 0; j < confs.size(); j++) {
				if(i <= j) {
					double similarity = getConfSimilarity(confs, i, j);
					confmatrix[i][j] = similarity;	confmatrix[j][i] = similarity;
				}
			}
		}
		
		// Store Conference Similarity Matrix:
		appendToFile(confmatrixpath, confs.size() + "\n");
		for(int i = 0; i < confs.size(); i++) {
			String printline = "";
			for(int j = 0; j < confs.size(); j++) {
				if(j != 0)	printline += " ";
				printline += confmatrix[i][j];
			}
			appendToFile(confmatrixpath, printline + "\n");
		}
	}
	
	private static void storeGraphFile(ArrayList<Paper> papers, String dirpath) {
		System.out.println("Generating Graph-file ....");
		String graphpath = dirpath + "Auth-Conf_graph.txt";
		ArrayList<Integer> years = new ArrayList<>();	ArrayList<String> authors = new ArrayList<>();
		for(Paper p : papers)	{
			if(!years.contains(p.year))	years.add(p.year);
			for(String a : p.authors)	if(!authors.contains(a))	authors.add(a);
		}
		int[][] confcounter = new int[years.size()][authors.size()];
		for(int i = 0; i < years.size(); i++) {
			for(int j = 0; j < authors.size(); j++)	confcounter[i][j] = 0;
		}
		
		int max_value = 0;
		for(Paper p : papers) {
			// Get idx for year:
			int idx_y = -1;	for(int i = 0; i < years.size(); i++)	if(years.get(i) == p.year) {	idx_y = i;	break;	}

			for(String a : p.authors) {
				// Get idx for each author and increase counters:
				int idx_a = -1;
				for(int i = 0; i < authors.size(); i++) {
					if(authors.get(i).equals(a))	{	
						idx_a = i;	confcounter[idx_y][idx_a]++;	
						if(confcounter[idx_y][idx_a] > max_value)	max_value = confcounter[idx_y][idx_a];
					}
				}
			}
		}
		
		// count edges (where counter is > 0):
		int edgecounter = 0;
		for(int i = 0; i < years.size(); i++) {
			for(int j = 0; j < authors.size(); j++) {
				if(confcounter[i][j] > 0)	edgecounter++;
			}
		}
		
		// generate + print graph:
		appendToFile(graphpath, edgecounter + "\n");	String line = "";
		for(int j = 0; j < authors.size(); j++) {
			for(int i = 0; i < years.size(); i++) {		
				int value = confcounter[i][j];
				if(value > 0) {
					double weight = (double) value / max_value;
					line = "A" + authordict.get(authors.get(j)) + "\tC" + years.get(i) + "\tedge\t" + weight + "\n";
					appendToFile(graphpath, line);
				}
			}
		}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String outputdir = "data/output/";	String paperpath = outputdir + "papers.xlsx";	
		ArrayList<Paper> papers = retrievePapers(paperpath);
		storeAuthorLists(papers, outputdir);	storeConferenceLists(papers, outputdir);	storeGraphFile(papers, outputdir);
	}
	
}
