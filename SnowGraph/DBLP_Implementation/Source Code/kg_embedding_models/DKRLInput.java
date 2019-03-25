package dblp_dataset.kg_embedding_models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DKRLInput {	// 6)
	static String a2idpath, ev2idpath, p2idpath, o2idpath;
	
	public static ArrayList<String>	getEntityWords(String path) {
		ArrayList<String> list = new ArrayList<>();	System.out.println("Processing Keywords in File " + path + " ....");
		ArrayList<String[]> file = getFileContent(path, 2);	
		for(String[] f : file) {
			// Count Keywords:
			String[] words = f[1].split(" "); int counter = 0;	for(String x : words)	if(!x.trim().equals(""))	counter++;
			String tmp = f[1].replaceAll("  ", " ");	list.add(f[0] + "\t" + counter + "\t" + tmp + "\n");
		}
		return list;
	}
	
	public static void transformWord2Vec(String vocabfile, String vectorfile, String vocaboutputpath, String vecoutputpath) {
		try(BufferedReader br = new BufferedReader(new FileReader(vocabfile))) {
			String line;	int counter = 0;	String vocab = "";
			while((line = br.readLine()) != null) {
				if((!line.trim().equals("")) && (line.indexOf("</s>") == -1)) {
					int x = line.indexOf(" ");
					if(x != -1) {	vocab += line.substring(0, x) + "\t" + counter + "\n";	counter++;	}					
				}
			} br.close();
			writeToNewFile(vocaboutputpath, vocab);
		} catch(IOException e) {	e.printStackTrace();	}
		
		try(BufferedReader br = new BufferedReader(new FileReader(vectorfile))) {
			String line; int counter = 0;	String vec = "";
			while((line = br.readLine()) != null) {
				if((counter > 1) && (!line.trim().equals(""))) {
					int x = line.indexOf(" ");
					if(x != -1)	vec += line.substring(x+1) + "\n";
				}
				counter++;
			} br.close();
			writeToNewFile(vecoutputpath, vec);
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void filterEntityData(ArrayList<String[]> edata) {
		for(String[] e : edata) {
			String outputpath = "";
			if(e[0].substring(0, 1).equals("A"))	outputpath = a2idpath;
			if(e[0].substring(0, 1).equals("P"))	outputpath = p2idpath;
			if(e[0].substring(0, 1).equals("I"))	outputpath = ev2idpath;
			if(e[0].substring(0, 1).equals("O"))	outputpath = o2idpath;
			String line = e[0] + "\t" + e[1];
			if(!outputpath.equals(""))	appendToFile(outputpath, line + "\n");
		}
	}
	
	// ----------------------------------------------------------------------
	
	public static ArrayList<String[]> getFileContent(String path, int columns) {
		ArrayList<String[]> list = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				int counter = 0;	String[] item = new String[columns];
				while((line.indexOf("\t") != -1) && (counter < columns)) {
					item[counter] = line.substring(0, line.indexOf("\t"));
					counter++;	line = line.substring(line.indexOf("\t")+1);
				}
				if((line.indexOf("\t") == -1) && (counter < columns))	item[counter] = line.trim();
				list.add(item);
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return list;	
	}
	
	public static ArrayList<String> getFirstColumn(ArrayList<String[]> list) {
		ArrayList<String> column = new ArrayList<>();
		for(String[] l : list)	column.add(l[0]);
		return column;
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Input-files:
		String kgauthpath = "data_dblp/embd/kg_authors.txt";	String kgorgpath = "data_dblp/embd/kg_organizations.txt";
		String kgpaperspath = "data_dblp/embd/kg_papers.txt";	String kgeventspath = "data_dblp/embd/kg_events.txt";
		String vocabpath = "data_dblp/embd/word2vec/vocab.txt";	String vecpath = "data_dblp/embd/word2vec/vec.txt";
		
		// Output-files:
		String e2idpath = "data_dblp/embd/dkrl/entity2id.txt";	String ewpath = "data_dblp/embd/dkrl/entityWords.txt";
		String vecoutputpath = "data_dblp/embd/dkrl/vectorfile.txt";	String vocaboutputpath = "data_dblp/embd/dkrl/word2id.txt";
		a2idpath = "data_dblp/embd/dkrl/author2id.txt";	ev2idpath = "data_dblp/embd/dkrl/event2id.txt";
		p2idpath = "data_dblp/embd/dkrl/paper2id.txt";	o2idpath = "data_dblp/embd/dkrl/org2id.txt";
		
		// Generate entity2id.txt:
		ArrayList<String> complete = new ArrayList<>();	ArrayList<String[]> entity2id = new ArrayList<>();
		complete.addAll(getFirstColumn(getFileContent(kgauthpath, 2)));	complete.addAll(getFirstColumn(getFileContent(kgpaperspath, 2)));
		complete.addAll(getFirstColumn(getFileContent(kgeventspath, 2)));	complete.addAll(getFirstColumn(getFileContent(kgorgpath, 2)));
		int counter = 0; String output = "";
		for(String c : complete)	{		
			output += c + "\t" + counter + "\n";	counter++;	
			String[] entity = new String[] { c, counter + "" };	entity2id.add(entity);
		}
		writeToNewFile(e2idpath, output);
		
		// Generate entityWords.txt:
		ArrayList<String> entitywords = new ArrayList<>();
		entitywords.addAll(getEntityWords(kgauthpath));	entitywords.addAll(getEntityWords(kgpaperspath));
		entitywords.addAll(getEntityWords(kgeventspath));	entitywords.addAll(getEntityWords(kgorgpath));
		output = "";	for(String e : entitywords)	output += e;	writeToNewFile(ewpath, output);
		
		// Tranform Word2Vec-Output:
		transformWord2Vec(vocabpath, vecpath, vocaboutputpath, vecoutputpath);
	
		// Generate *2id-files:
		filterEntityData(entity2id);
	}
}
