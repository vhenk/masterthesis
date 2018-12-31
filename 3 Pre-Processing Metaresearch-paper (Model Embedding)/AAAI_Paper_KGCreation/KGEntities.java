import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class KGEntities {	// 3)
	
	public static ArrayList<String>	getEntityWords(String path, String keywordspath) {
		ArrayList<String> list = new ArrayList<>();	System.out.println("Processing Keywords in File " + path + " ....");
		ArrayList<String[]> file = getFileContent(path, 2);	String keywords = "";
		for(String[] f : file) {
			// Count Keywords:
			String[] words = f[1].split(" "); int counter = 0;	for(String x : words)	if(!x.trim().equals(""))	counter++;
			String tmp = f[1].replaceAll("  ", " ");	list.add(f[0] + "\t" + counter + "\t" + tmp + "\n");
			keywords += tmp + " ";
		}
		appendToFile(keywordspath, keywords);
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
		/*
		String keywordspath = "keywords.txt";	String kgauthpath = "kg_authors.txt";	String kgorgpath = "kg_organizations.txt";
		String kgpaperspath = "kg_papers.txt";	String kgeventspath = "kg_events.txt";	String ewpath = "entityWords.txt";
		String e2idpath = "entity2id.txt";	
		*/
		String vocabpath = "dim50/vocab.txt";	String vecpath = "dim50/vec.txt";
		String vecoutputpath = "dim50/vectorfile.txt";	String vocaboutputpath = "dim50/word2id.txt";
		/*
		// Generate entity2id.txt:
		ArrayList<String> complete = new ArrayList<>();	
		complete.addAll(getFirstColumn(getFileContent(kgauthpath, 2)));	complete.addAll(getFirstColumn(getFileContent(kgpaperspath, 2)));
		complete.addAll(getFirstColumn(getFileContent(kgeventspath, 2)));	complete.addAll(getFirstColumn(getFileContent(kgorgpath, 2)));
		int counter = 0; String output = "";
		for(String c : complete)	{		output += c + "\t" + counter + "\n";	counter++;	}
		writeToNewFile(e2idpath, output);
		
		// Generate entityWords.txt and keywords.txt:
		ArrayList<String> entitywords = new ArrayList<>();
		entitywords.addAll(getEntityWords(kgauthpath, keywordspath));	entitywords.addAll(getEntityWords(kgpaperspath, keywordspath));
		entitywords.addAll(getEntityWords(kgeventspath, keywordspath));	entitywords.addAll(getEntityWords(kgorgpath, keywordspath));
		output = "";	for(String e : entitywords)	output += e;	writeToNewFile(ewpath, output);
		*/
		// Tranform Word2Vec-Output:
		transformWord2Vec(vocabpath, vecpath, vocaboutputpath, vecoutputpath);
	}
}
