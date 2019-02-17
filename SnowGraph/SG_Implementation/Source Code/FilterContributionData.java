package scigraph_dataset;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FilterContributionData {

	private static void filterFile(String path) {
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("hasContribution>") != -1)	appendToFile("data/data_filtered/sgcontributions.nt", line + "\n");
				else if(line.indexOf("hasBook>") != -1)	appendToFile("data/data_filtered/sgbooks.nt", line + "\n");
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	private static void filterNTFile(String phrase, String path, String outputpath) {
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf(phrase) != -1)	appendToFile(outputpath, line + "\n");
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
		String path = "data/SciGraph-Data/";	String fpath = "data/data_filtered/";
		ArrayList<String> filenames = new ArrayList<>();
		filenames.add(path + "scigraph2002.nt");	filenames.add(path + "scigraph2006.nt");
		filenames.add(path + "scigraph2009.nt");	filenames.add(path + "scigraph2011.nt");
		filenames.add(path + "scigraph2012.nt");	filenames.add(path + "scigraph2013.nt");
		filenames.add(path + "scigraph2014.nt");	filenames.add(path + "scigraph2015.nt");
		for(String file : filenames) {
			System.out.println("Processing file " + file + " ....");	filterFile(file);
		}
		
		filterNTFile("hasConference", path + "sg_bookdata.nt",  fpath + "sg_bookdata_filtered.nt");
		filterNTFile("core/acronym", path + "sg_conferencedata.nt", fpath + "sg_conferencedata_filtered.nt");
	}
}
