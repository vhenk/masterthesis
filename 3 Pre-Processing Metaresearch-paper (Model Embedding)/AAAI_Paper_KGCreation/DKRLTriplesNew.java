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

public class DKRLTriplesNew {
	
	public static ArrayList<String[]> getValidData(ArrayList<String[]> training) {
		ArrayList<String[]> tmp = shuffleTriples(training);	ArrayList<String[]> valid = new ArrayList<>();
		for(int i = 0; i < 2000; i++)	valid.add(tmp.get(i));
		return valid;
	}
	
	public static ArrayList<String[]> convert2DKRL(ArrayList<String[]> triples) {
		ArrayList<String[]> converted = new ArrayList<>();
		for(String[] t : triples)	converted.add(new String[] { t[0], t[2], t[1] });
		return converted;
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
		String testpath = "test2.tsv";	String trainpath = "train2.tsv";
		String testoutputpath = "dkrlnew/test.txt";	String trainoutputpath = "dkrlnew/train.txt";	
		String validoutputpath = "dkrlnew/valid.txt";
		ArrayList<String[]> traindata = convert2DKRL(getFileContent(trainpath, 3));	storeNewTriples(traindata, trainoutputpath);
		ArrayList<String[]> testdata = convert2DKRL(getFileContent(testpath, 3));	storeNewTriples(testdata, testoutputpath);
		ArrayList<String[]> validdata = getValidData(traindata);	storeNewTriples(validdata, validoutputpath);
	}
	
}
