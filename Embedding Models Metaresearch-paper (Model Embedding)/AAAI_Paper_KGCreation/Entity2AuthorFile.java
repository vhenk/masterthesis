import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Entity2AuthorFile {

	public static void filterTestData(ArrayList<String[]> testdata) {
		for(String[] td : testdata) {
			String outputpath = "";
			if(td[1].equals("/author/publication/paper/publication_location/venue"))	outputpath = "edit/testVenue.txt";
			else if (td[1].equals("/author/affiliation/department"))	outputpath = "edit/testOrg.txt";
			else if (td[1].equals("/author/publication/paper"))	outputpath = "edit/testPaper.txt";
			else if (td[1].equals("/author/collaboration/coauthor"))	outputpath = "edit/testAuthor.txt";	
			String line = td[0] + "\t" + td[2] + "\t" + td[1];
			if(!outputpath.equals(""))	appendToFile(outputpath, line + "\n");
		}
	}
	
	public static void filterEntityData(ArrayList<String[]> edata) {
		for(String[] e : edata) {
			String outputpath = "";
			if(e[0].substring(0, 1).equals("A"))	outputpath = "edit/author2id.txt";
			if(e[0].substring(0, 1).equals("P"))	outputpath = "edit/paper2id.txt";
			if(e[0].substring(0, 1).equals("I"))	outputpath = "edit/event2id.txt";
			if(e[0].substring(0, 1).equals("O"))	outputpath = "edit/org2id.txt";
			String line = e[0] + "\t" + e[1];
			if(!outputpath.equals(""))	appendToFile(outputpath, line + "\n");
		}
	}
	
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

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String testpath = "test2.tsv";	String entitypath = "entity2id.txt";
		ArrayList<String[]> testdata = getFileContent(testpath, 3);	ArrayList<String[]> entitydata = getFileContent(entitypath, 2);
		filterTestData(testdata);	filterEntityData(entitydata);
		
		/* Entity2AuthorFile
		try(BufferedReader br = new BufferedReader(new FileReader("entity2id.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					if(line.substring(0, 1).equals("A"))	appendToFile("author2id.txt", line + "\n");
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	*/
	}
}
