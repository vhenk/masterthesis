import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UpdateAuthorKeywords {
	static class Author {
		String id, name; ArrayList<String> papers;
		public Author(String id, String name, ArrayList<String> papers) {
			this.id = id;	this.name = name;	this.papers = papers;
		}
	}
	
	public static HashMap<String, String> retrievePapers() {
		HashMap<String, String> papers = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_papers.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					papers.put(line.substring(0, line.indexOf("\t")), line.substring(line.indexOf("\t")+1));
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return papers;
	}
	
	public static ArrayList<Author> retrieveAuthors() {
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_authors_p.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String aID = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String name = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					ArrayList<String> papers = new ArrayList<>();
					while(!line.trim().equals("")) {
						if(line.indexOf("\t") != -1) {
							papers.add("P" + line.substring(0, line.indexOf("\t")));	line = line.substring(line.indexOf("\t")+1);
						}
						else {	papers.add("P" + line.trim());	line = "";	}
					}
					authors.add(new Author(aID, name, papers));
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}	
		return authors;
	}

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		HashMap<String, String> paperlist = retrievePapers();
		ArrayList<Author> authors = retrieveAuthors();
		for(Author a : authors) {
			String description = "";
			for(String pnr : a.papers)	description += " " + paperlist.get(pnr);	description = description.replaceAll("\\d", "");
			// Remove duplicate words:
			String[] words = description.split(" ");	ArrayList<String> wordsfiltered = new ArrayList<>();
			Set<String> cleanup = new HashSet<>();	for(String w : words)	cleanup.add(w);	wordsfiltered.addAll(cleanup);
			description = "";	for(String w : wordsfiltered)	description += " " + w;
			String ntmp = a.name.replaceAll("[^a-zA-ZÄäÖöÜüßéèáà ]", "");
			String keywords = ntmp + description;
			appendToFile("kg_authors_new.txt", a.id + "\t" + keywords + "\n");
		}
	}
}
