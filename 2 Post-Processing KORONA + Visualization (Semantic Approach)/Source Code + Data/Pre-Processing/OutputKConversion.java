import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class OutputKConversion {
	// Author-List-File:
	static String listpath = "";
	
	static class Author {
		String id, uri, name;
		public Author(String id, String name) {
			this.id = id;	this.name = name;	this.uri = "";
			
			// Get Author's URI:
			int number = -1;	int idx = 1;
			try { number = Integer.parseInt(this.id.substring(1)); } catch(Exception e) {};
			if(number != -1) {
				try(BufferedReader br = new BufferedReader(new FileReader(listpath))) {
					String line;
					while((line = br.readLine()) != null) {
						if(idx == number) 	{	this.uri = line.trim();	break;}	idx++;
					}
					br.close();
				} catch(IOException e) {	e.printStackTrace();	}
			}
		}
	}
	
	public static ArrayList<Author> getAuthors(String mappath) {
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(mappath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String name = line.trim();
					authors.add(new Author(id, name));
				}
			}
			br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authors;
	}
	
	public static void convertExcelToTxt(String excelpath, String txtpath, ArrayList<Author> authors) {
		System.out.println("Starting Conversion of Excel-File ....");
		// Read Excel-file:
		try {
			InputStream excelFile = new FileInputStream(excelpath);
			XSSFWorkbook wb = new XSSFWorkbook(excelFile);	XSSFSheet sheet = wb.getSheetAt(0);	XSSFRow row;	XSSFCell cell;
			Iterator<Row> rows = sheet.rowIterator();	int rownum = 1;
			while(rows.hasNext()) {
				row = (XSSFRow) rows.next();	Iterator<Cell> cells = row.cellIterator();	int counter = 0;
				String line = "";	if(rownum > 2)	line += "\n";
				while(cells.hasNext()) {
					cell = (XSSFCell) cells.next();
					if(rownum > 1) {
						if(counter == 0)	line += (int) cell.getNumericCellValue();
						if(counter == 1)	{
							String tmp = cell.getStringCellValue();
							line += "\t[" + tmp.substring(0, tmp.length()-1) + "]";
						}
						if((counter == 2) || (counter == 3))	line += "\t" + (int) cell.getNumericCellValue();
						if(counter > 3)	{
							String uritmp = cell.getStringCellValue();	String idtmp = "";
							for(Author a : authors) {	if(a.uri.equals(uritmp)) { idtmp = a.id; break;	}	}
							line += "\t" + idtmp;
						}
					}
					counter++;
				}
				appendToFile(txtpath, line);
				rownum++;
			}
			wb.close();
			System.out.println("Conversion from .xlsx to .txt completed!");
		} catch(Exception e)	{	e.printStackTrace();	}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Process Input-files:
		Scanner reader = new Scanner(System.in);	
		System.out.println("Authors-Map-file: ");	String mappath = reader.nextLine();
		System.out.println("Authors-List-file: ");	listpath = reader.nextLine();
		System.out.println("Papers Excel-file: ");	String excelpath = reader.nextLine();
		reader.close();
		if(mappath.indexOf(".txt") == -1)	mappath += ".txt";	if(listpath.indexOf(".txt") == -1)	listpath += ".txt";
		if(excelpath.indexOf(".xlsx") == -1)	excelpath += ".xlsx";
		
		// Output-files:
		String authormappath = "authorkeys.txt";	 String paperspath = "papers.txt";
		String authormapfile = "";
		
		// Get Author Names:
		ArrayList<Author> authors = getAuthors(mappath);
		
		// Assing Author Names and URIs:
		int counter = 0;
		for(Author a : authors) {
			authormapfile += a.id + "\t" + a.name + "\t" + a.uri;
			if(counter != authors.size()-1)	authormapfile += "\n";	counter++;
		}
		
		// Creating Authormaps Output-file: 
		writeToNewFile(authormappath, authormapfile);
		
		// Creating Papers Output-file:
		File papersfile = new File(paperspath);	
		try {papersfile.createNewFile();	}	catch(IOException e)	{	e.printStackTrace();	}
		convertExcelToTxt(excelpath, paperspath, authors);
	}
}
