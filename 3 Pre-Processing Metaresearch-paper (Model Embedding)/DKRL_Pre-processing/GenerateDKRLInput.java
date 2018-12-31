import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GenerateDKRLInput {

	public static void changeTripleOrder(String path, String outputpath) {
		ArrayList<String[]> triples = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String subject = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String relation = line.substring(0, line.indexOf("\t"));
					String object = line.substring(line.indexOf("\t")+1).trim();
					triples.add(new String[] { subject, relation, object });
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		for(String[] t : triples) {	appendToFile(outputpath, t[0] + "\t" + t[2] + "\t" + t[1] + "\n");	}
	}
	
	public static ArrayList<String> getFirstColumn(String path) {
		System.out.println("Processing File " + path + " ....");
		ArrayList<String> list = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	list.add(line.substring(0, line.indexOf("\t")));
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return list;
	}
	
	public static void addEntities(String path) {
		String file = "";	System.out.println("Processing Keywords in File " + path + " ....");
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;	
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	String keywords = line.substring(line.indexOf("\t")).trim();
					// count keywords:
					String[] list = keywords.split(" "); int counter = 0;
					for(String x : list)	if(!x.trim().equals(""))	counter++;
					keywords.replaceAll("  ", " ");
					file += id + "\t" + counter + "\t" + keywords + "\n";
				}
			} br.close();
			appendToFile("dkrl_data/entityWords_new.txt", file);
		} catch(IOException e) {		e.printStackTrace();	}		
	}
	
	public static void correctFile(String path) {
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; String file = "";
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	String keywords = line.substring(line.indexOf("\t")+1).trim();
					file += id + "\t";	int x = keywords.indexOf("  ");
					if(x != -1)	file += keywords.substring(0, x) + keywords.substring(x+1);
					else	file += keywords;
					/*
					 String[] list = keywords.split(" ");
					 
					for(int i = 0; i < list.length; i++) {
						if(!list[i].trim().equals(""))	file += list[i].trim();	if(i != list.length-1)	file += " ";
					}*/
					file += "\n";	System.out.println("\t" + id);
				}
			} br.close();
			appendToFile("kg_authors.txt", file);
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void createKeywordsFile() {
		try(BufferedReader br = new BufferedReader(new FileReader("dkrl_data/entityWords.txt"))) {
			String line;	String keywords = "";
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					line = line.substring(line.indexOf("\t")+1);	line = line.substring(line.indexOf("\t")+1);
					keywords += line.trim() + " ";
				}
			} br.close();
			writeToNewFile("dkrl_data/keywords.txt", keywords);
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void transformWord2VecFiles(String vocabpath, String vecpath) {
		try(BufferedReader br = new BufferedReader(new FileReader(vocabpath))) {
			String line;	int counter = 0;	String vocab = "";
			while((line = br.readLine()) != null) {
				if((!line.trim().equals("")) && (line.indexOf("</s>") == -1)) {
					int x = line.indexOf(" ");
					if(x != -1) {	vocab += line.substring(0, x) + "\t" + counter + "\n";	counter++;	}					
				}
			} br.close();
			writeToNewFile("dkrl_data/word2id.txt", vocab);
		} catch(IOException e) {	e.printStackTrace();	}
		
		try(BufferedReader br = new BufferedReader(new FileReader(vecpath))) {
			String line; int counter = 0;	String vec = "";
			while((line = br.readLine()) != null) {
				if((counter > 1) && (!line.trim().equals(""))) {
					int x = line.indexOf(" ");
					if(x != -1)	vec += line.substring(x+1) + "\n";
				}
				counter++;
			} br.close();
			writeToNewFile("dkrl_data/vectorfile.txt", vec);
		} catch(IOException e) {	e.printStackTrace();	}
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
		changeTripleOrder("triples/test.txt", "dkrl_data/test.txt");
		changeTripleOrder("triples/train.txt", "dkrl_data/train.txt");
		changeTripleOrder("triples/valid.txt", "dkrl_data/valid.txt");

		// Generate entity2id.txt:
		ArrayList<String> complete = new ArrayList<>();
		complete.addAll(getFirstColumn("kg_authors.txt"));
		complete.addAll(getFirstColumn("kg_papers.txt"));
		complete.addAll(getFirstColumn("kg_events.txt"));
		complete.addAll(getFirstColumn("kg_organizations.txt"));
		int counter = 0;
		for(String c : complete) {	appendToFile("dkrl_data/entity2id.txt", c + "\t" + counter + "\n");	counter++;	}	
		*/
		
		//correctFile("kg_authors_old.txt");
		
		// Generate entityWords.txt:
		//addEntities("kg_authors.txt");	addEntities("kg_papers.txt");
		//addEntities("kg_events.txt");	addEntities("kg_organizations.txt");
		
		// Generate file with all descriptions:
		//createKeywordsFile();
		transformWord2VecFiles("vocab.txt", "vec.txt");
		
	}
}
