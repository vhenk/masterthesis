import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class KGStatistics {	// 4)
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
	
	public static void printNumberEntities(ArrayList<String[]> entityWords) {
		double authors = 0; double papers = 0; double events = 0; double departments = 0;
		double pkeysum = 0; double akeysum = 0; double ekeysum = 0; double dkeysum = 0;
		for(String[] entity : entityWords) {
			int keys = 0;	try { keys = Integer.parseInt(entity[1]); } catch(Exception e) {}
			if(entity[0].substring(0, 1).equals("A"))	{	authors++; akeysum += keys;	}
			else if(entity[0].substring(0, 1).equals("P"))	{	papers++; pkeysum += keys;	}
			else if(entity[0].substring(0, 1).equals("O"))	{	departments++; dkeysum += keys;	}
			else	{	events++; ekeysum += keys;	}
		}
		double avgkeyA = akeysum / authors; double avgkeyP = pkeysum / papers; 
		double avgkeyDep = dkeysum / departments; double avgkeyE = ekeysum / events;
		double sum = authors + papers + departments + events;
		
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
	}
	
	public static void printNumberTriples(ArrayList<String[]> validdata, ArrayList<String[]> traindata, ArrayList<String[]> testdata) {
		// Number of Triples
		double[][] triplenumbers = new double[3][4];
		double a = 0; double p = 0; double o = 0; double e = 0; double sumvalid = 0; double sumtrain = 0; double sumtest = 0;
		a = countRelTriples(validdata, "/author/collaboration/coauthor");
		p = countRelTriples(validdata, "/author/publication/paper");
		e = countRelTriples(validdata, "/author/publication/paper/publication_location/venue");
		o = countRelTriples(validdata, "/author/affiliation/department");
		triplenumbers[0][0] = a;	triplenumbers[0][1] = p;	triplenumbers[0][2] = e;	triplenumbers[0][3] = o;
		sumvalid = a + p + o + e;	a = 0; p = 0; e = 0; o = 0;
	
		a = countRelTriples(traindata, "/author/collaboration/coauthor");
		p = countRelTriples(traindata, "/author/publication/paper");
		e = countRelTriples(traindata, "/author/publication/paper/publication_location/venue");
		o = countRelTriples(traindata, "/author/affiliation/department");
		triplenumbers[1][0] = a;	triplenumbers[1][1] = p;	triplenumbers[1][2] = e;	triplenumbers[1][3] = o;
		sumtrain = a + p + o + e;	a = 0; p = 0; e = 0; o = 0;
		
		a = countRelTriples(testdata, "/author/collaboration/coauthor");
		p = countRelTriples(testdata, "/author/publication/paper");
		e = countRelTriples(testdata, "/author/publication/paper/publication_location/venue");
		o = countRelTriples(testdata, "/author/affiliation/department");
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
	}
	
	public static void printAverageValues(ArrayList<String[]> traindata, ArrayList<String[]> testdata) {
		// Number of Authors
		ArrayList<String> authorsorg = new ArrayList<>();	
		ArrayList<String> authorstest = new ArrayList<>();	ArrayList<String> authorstrain = new ArrayList<>();
		for(String[] t : traindata)	{
			authorstrain.add(t[0]);
			if(t[1].substring(0, 1).equals("A"))	authorstrain.add(t[1]);
			if(t[2].equals("/author/affiliation/department"))	authorsorg.add(t[0]);
		}
		for(String[] t : testdata) {	authorstest.add(t[0]);	authorstest.add(t[1]);	}
		ArrayList<String> authorsorg_unique = removeDuplicates(authorsorg);	double num_auth_org = authorsorg_unique.size();	
		ArrayList<String> authorstrain_unique = removeDuplicates(authorstrain);	double num_auth_train = authorstrain_unique.size();
		ArrayList<String> authorstest_unique = removeDuplicates(authorstest);	double num_auth_test = authorstest_unique.size();
		
		System.out.println("*******************");
		System.out.println("Number of Authors in train.txt: \t" + num_auth_train);
		System.out.println("Number of Authors in test.txt: \t" + num_auth_test);
		System.out.println("Authors with Affiliation(s): \t" + num_auth_org);
		System.out.println("*******************");
		
		// Average values
		double num_publication = countRelTriples(traindata, "/author/publication/paper");
		double avg_papers = num_publication / num_auth_train;
		double num_events = countRelTriples(traindata, "/author/publication/paper/publication_location/venue");
		double avg_events = num_events / num_auth_train;
		double num_org = countRelTriples(traindata, "/author/affiliation/department");
		double avg_org = num_org / num_auth_org;	double avg_org_all = num_org / num_auth_train;
		double num_coll = countRelTriples(traindata, "/author/collaboration/coauthor");
		double avg_coll = num_coll / num_auth_train;
		double num_coll_test = countRelTriples(testdata, "/author/collaboration/coauthor");
		double avg_coll_test = num_coll_test / num_auth_test;
		
		System.out.println("*******************");
		System.out.println("AVG Papers per Author: \t" + avg_papers);
		System.out.println("AVG Events per Author: \t" + avg_events);
		System.out.println("AVG Departments per Author (all): \t" + avg_org_all);
		System.out.println("AVG Departments per Author (filtered): \t" + avg_org);
		System.out.println("AVG Collaborations per Author (training): \t" + avg_coll);
		System.out.println("AVG Collaborations per Author (testing): \t" + avg_coll_test);
		System.out.println("*******************");
	}
	
	public static double countRelTriples(ArrayList<String[]> data, String relationship) {
		int result = 0;
		for(String[] d : data)	if(d[2].equals(relationship))	result++;
		return result;
	}
	
	// ------------------------------------------------------------------------------------------
	
	public static ArrayList<String> removeDuplicates(ArrayList<String> list) {
		Set<String> cleanup = new HashSet<>();	cleanup.addAll(list);	list.clear();	list.addAll(cleanup);
		return list;
	}
	
	public static void main(String[] args) {
		String ewpath = "data/entityWords.txt";	String testpath = "dkrlnew/test.txt";
		String trainpath = "dkrlnew/train.txt";	String validpath = "dkrlnew/valid.txt";
		ArrayList<String[]> entityWords = readFile(ewpath, 3);	ArrayList<String[]> testdata = readFile(testpath, 3);
		ArrayList<String[]> traindata = readFile(trainpath, 3);	ArrayList<String[]> validdata = readFile(validpath, 3);
	
		//printNumberEntities(entityWords);
		printNumberTriples(validdata, traindata, testdata);
		//printAverageValues(traindata, testdata);
	}
}
