import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
public class RetrieveStatistics {
	
	public static ArrayList<String[]> readFile(String path, int columns) {
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
	
	public static ArrayList<String> readAuth_OrgFile() {
		ArrayList<String> authorIDs = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("kg_authors.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	authorIDs.add(line.substring(0, line.indexOf("\t")));
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		ArrayList<String> authorsOrg = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("authors_org.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	{
					String id = line.substring(0, line.indexOf("\t"));	if(authorIDs.contains(id))	authorsOrg.add(id);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authorsOrg;
	}
	
	public static void main(String[] args) {
		ArrayList<String[]> entityWords = readFile("dkrl_data/entityWords.txt", 3);
		ArrayList<String[]> testdata = readFile("dkrl_data/test.txt", 3);
		ArrayList<String[]> traindata = readFile("dkrl_data/train.txt", 3);
		ArrayList<String[]> validdata = readFile("dkrl_data/valid.txt", 3);
	
		int authors = 0; int papers = 0; int events = 0; int departments = 0;
		int pkeysum = 0; int akeysum = 0; int ekeysum = 0; int dkeysum = 0;
		for(String[] entity : entityWords) {
			int keys = 0;	try { keys = Integer.parseInt(entity[1]); } catch(Exception e) {}
			if(entity[0].substring(0, 1).equals("A"))	{	authors++; akeysum += keys;	}
			else if(entity[0].substring(0, 1).equals("P"))	{	papers++; pkeysum += keys;	}
			else if(entity[0].substring(0, 1).equals("O"))	{	departments++; dkeysum += keys;	}
			else	{	events++; ekeysum += keys;	}
		}
		double avgkeyA = akeysum / authors; double avgkeyP = pkeysum / papers; 
		double avgkeyDep = dkeysum / departments; double avgkeyE = ekeysum / events;
		int sum = authors + papers + departments + events;
		
		System.out.println("*******************");
		System.out.println("Number of Entities in KG: \n");
		System.out.println("Authors:\t" + authors);
		System.out.println("Papers:\t" + papers);
		System.out.println("Departments:\t" + departments);
		System.out.println("Events:\t" + events);
		System.out.println("SUM:\t" + sum);
		System.out.println("*******************");
		
		System.out.println("*******************");
		System.out.println("Number of Keywords / Entity: \n");
		System.out.println("Authors:\t" + avgkeyA);
		System.out.println("Papers:\t" + avgkeyP);
		System.out.println("Departments:\t" + avgkeyDep);
		System.out.println("Events:\t" + avgkeyE);
		System.out.println("*******************");
		
		// Number of Triples
		int[][] triplenumbers = new int[3][4];
		int a = 0; int p = 0; int o = 0; int e = 0; int sumvalid = 0; int sumtrain = 0; int sumtest = 0;
		for(String[] d : validdata) {
			if(d[2].equals("/author/publication/paper"))	p++;
			else if(d[2].equals("/author/publication/paper/publication_location/venue"))	e++;
			else if(d[2].equals("/author/collaboration/coauthor"))	a++;
			else if(d[2].equals("/author/affiliation/department"))	o++;
		}
		triplenumbers[0][0] = a;	triplenumbers[0][1] = p;	triplenumbers[0][2] = e;	triplenumbers[0][3] = o;
		sumvalid = a + p + o + e;	a = 0; p = 0; e = 0; o = 0;
		for(String[] d : traindata) {
			if(d[2].equals("/author/publication/paper"))	p++;
			else if(d[2].equals("/author/publication/paper/publication_location/venue"))	e++;
			else if(d[2].equals("/author/collaboration/coauthor"))	a++;
			else if(d[2].equals("/author/affiliation/department"))	o++;
		}
		triplenumbers[1][0] = a;	triplenumbers[1][1] = p;	triplenumbers[1][2] = e;	triplenumbers[1][3] = o;
		sumtrain = a + p + o + e;	a = 0; p = 0; e = 0; o = 0;
		for(String[] d : testdata) {
			if(d[2].equals("/author/publication/paper"))	p++;
			else if(d[2].equals("/author/publication/paper/publication_location/venue"))	e++;
			else if(d[2].equals("/author/collaboration/coauthor"))	a++;
			else if(d[2].equals("/author/affiliation/department"))	o++;
		}
		sumtest = a + p + o + e;	
		triplenumbers[2][0] = a;	triplenumbers[2][1] = p;	triplenumbers[2][2] = e;	triplenumbers[2][3] = o;

		System.out.println("*******************");
		System.out.println("Number of Triples: \tvalid.txt\ttrain.txt\ttest.txt\n");
		System.out.println("Author -> Co-Author:\t" + triplenumbers[0][0] + "\t\t" + triplenumbers[1][0] + "\t\t" + triplenumbers[2][0]);
		System.out.println("Author -> Paper:\t" + triplenumbers[0][1] + "\t\t" + triplenumbers[1][1] + "\t\t" + triplenumbers[2][1]);
		System.out.println("Author -> Event:\t" + triplenumbers[0][2] + "\t\t" + triplenumbers[1][2] + "\t\t" + triplenumbers[2][2]);
		System.out.println("Author -> Depart.:\t" + triplenumbers[0][3] + "\t\t" + triplenumbers[1][3] + "\t\t" + triplenumbers[2][3]);
		System.out.println("SUM:\t\t\t" + sumvalid + "\t\t" + sumtrain + "\t\t" + sumtest);
		System.out.println("*******************");
		
		ArrayList<String> authorsOrg = readAuth_OrgFile(); System.out.println("Authors with Affiliation(s): " + authorsOrg.size());
		
	}
}
