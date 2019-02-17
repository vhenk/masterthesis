import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SplitTrainingData {

	public static boolean isIdentic(String[] item1, String[] item2) {
		int size = item1.length;	if(item2.length != size)	return false;
		boolean same = true;
		for(int i = 0; i < size; i++) {	if(!item1[i].equals(item2[i]))	same = false;	}
		return same;
	}
	
	public static boolean isInList(ArrayList<String[]> list, String[] item) {
		boolean exists = false;
		for(String[] l : list) {	if(isIdentic(item, l))	{	exists = true; break;	}	}
		return exists;
	}
	
	public static ArrayList<String[]> removeDuplicates(ArrayList<String[]> list) {
		ArrayList<String[]> unique = new ArrayList<>(); int counter = 1;
		for(int i = 0; i < list.size(); i++) {	
			if((counter % 1000) == 0)	System.out.print("...");
			for(int j = 0; j < list.size(); j++) {
				if(i != j) {
					if(isIdentic(list.get(i), list.get(j)))	if(!isInList(unique, list.get(i)))	unique.add(list.get(i));
				}
			}
			counter++;
		}
		for(String[] l : list)	if(!isInList(unique, l))	unique.add(l);
		return unique;
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
	
	public static void splitTriples(ArrayList<String[]> triples, String trainoutput, String testoutput) {
		ArrayList<String[]> shuffled = shuffleTriples(triples);
		ArrayList<String[]> test = new ArrayList<>();	ArrayList<String[]> train = new ArrayList<>();
		for(int i = 0; i < 6000; i++)	test.add(shuffled.get(i));	System.out.println("Test-Set: " + test.size());
		for(int i = 6000; i < shuffled.size(); i++)	train.add(shuffled.get(i));	System.out.println("Train-Set: " + train.size());
		storeNewTriples(train, trainoutput);	storeNewTriples(test, testoutput);	
	}
	
	public static ArrayList<String[]> shuffleTriples(ArrayList<String[]> triples) {
		ArrayList<String[]> shuffled = new ArrayList<>();
		Integer[] helplist = new Integer[triples.size()];	for(int i = 0; i < triples.size(); i++) helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		for(int num : listtmp) {
			shuffled.add(new String[] { triples.get(num)[0], triples.get(num)[1], triples.get(num)[2] });
		}
		return shuffled;
	}
	
	public static void storeNewTriples(ArrayList<String[]> triples, String filename) {
		String output = "";
		for(String[] t : triples) {
			output += t[0] + "\t" + t[1] + "\t" + t[2] + "\n";
		}
		writeToNewFile(filename, output);
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String trainpath = "train.tsv";	String trainoutput = "train2.tsv";	String testoutput = "test2.tsv";
		ArrayList<String[]> traindata = getFileContent(trainpath, 3);
		System.out.println("Triples in Training: " + traindata.size());
		ArrayList<String[]> unique = removeDuplicates(traindata);
		System.out.println("Training with removed duplicates: " + unique.size());
		splitTriples(unique, trainoutput, testoutput);
	}
}
