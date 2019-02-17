package scigraph_dataset;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthorSimilarityMatrix {
	static class Author {
		String id;	HashMap<String, Integer> conferences;
		public Author(String id) {
			this.id = id;	conferences = new HashMap<>();
		}
	}
	
	private static ArrayList<Author> getAuthors(String mappath, String auth_conf_path) {
		// Get Author-IDs from Author-Key-Map:
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(mappath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	authors.add(new Author(line.substring(0, line.indexOf("\t"))));
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Add Conferences to Author-Objects:
		try(BufferedReader br = new BufferedReader(new FileReader(auth_conf_path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t"));	String conf = line.substring(line.indexOf("\t")+1);
					for(Author a : authors) {
						if(a.id.equals(id)) {
							if(a.conferences.containsKey(conf)) {
								int counter = a.conferences.get(conf);	counter++;	a.conferences.replace(conf, counter);
							}
							else	a.conferences.put(conf, 1);
							break;	
						}
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authors;
	}
	
	// Here the similarity of two authors is calculated given their IDs
	private static double getSimilarity(ArrayList<Author> authors, String aID1, String aID2) {
		double similarity = -1;
		if(aID1.equals(aID2))	return 1.0;
		Author author1 = null;	Author author2 = null;
		for(Author a : authors)	if(a.id.equals(aID1)) {	author1 = a; break;	}
		for(Author a : authors)	if(a.id.equals(aID2)) {	author2 = a; break;	}
		
		// Get num of papers overall for both authors:
		int num_a1 = 0;	int num_a2 = 0;
		for(String x : author1.conferences.keySet())	num_a1 = num_a1 + author1.conferences.get(x);
		for(String x : author2.conferences.keySet())	num_a2 = num_a2 + author2.conferences.get(x);
				
		// Get num of papers for each conference:
		double bothnum = 0;	double maxnum = 0;
		for(String conf1 : author1.conferences.keySet()) {	
			for(String conf2 : author2.conferences.keySet()) {
				if(conf1.equals(conf2)) {
					int counter1 = author1.conferences.get(conf1);	int counter2 = author2.conferences.get(conf2);
					if(counter1 > counter2)	bothnum += counter2;	else bothnum += counter1;
				}
			}
			
		}	

 		maxnum = num_a1 + num_a2;
		
 		if(bothnum == 0)	return 0;
 		else similarity = bothnum / maxnum;
 		
		return similarity;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		ArrayList<Author> authors = getAuthors("data/output/author-key-map.txt", "data/output/author_confs.txt");

		System.out.println("Calculating Similarities ...");
		double authorsmatrix[][] = new double[authors.size()][authors.size()];
		for(int i = 0; i < authors.size(); i++) {
			for(int j = 0; j < authors.size(); j++) {
				if(j >= i) {
					double similarity = getSimilarity(authors, authors.get(i).id, authors.get(j).id);
					authorsmatrix[i][j] = similarity;	authorsmatrix[j][i] = similarity;
				}
			}
		}

		System.out.println("Writing Matrix-file ....");
		String outputpath = "data/output/Auth_matrix.txt";
		appendToFile(outputpath, authors.size() + "\n");
		for(int i = 0; i < authors.size(); i++) {
			String output = "";
			for(int j = 0; j < authors.size(); j++) {
				output+= authorsmatrix[i][j] + "";
				if(j != authors.size()-1)	output += " ";
			}
			appendToFile(outputpath, output += "\n");
		}
		
	}
}
