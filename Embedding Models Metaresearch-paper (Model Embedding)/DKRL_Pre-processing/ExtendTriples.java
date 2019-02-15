import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExtendTriples {
	public static void extendTriplesFile(String path, String outputpath) {
		ArrayList<String[]> triples = new ArrayList<>();
		// Retrieve existing triples:
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String subject = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String object = line.substring(0, line.indexOf("\t"));	String relation = line.substring(line.indexOf("\t")+1);
					triples.add(new String[] { subject, object, relation });
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Add recursive triples:
		ArrayList<String[]> newTriples = new ArrayList<>();
		for(String[] t : triples) {
			if(t[2].equals("/author/collaboration/coauthor"))	newTriples.add(new String[] { t[1], t[0], t[2] });	
		}
		triples.addAll(newTriples);
		
		// Shuffle list:
		Integer[] helplist = new Integer[triples.size()];	for(int i = 0; i < triples.size(); i++)	helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		for(int num : listtmp) {
			appendToFile(outputpath, triples.get(num)[0] + "\t" + triples.get(num)[1] + "\t" + triples.get(num)[2] + "\n");
		}	
	}

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}

	public static void main(String[] args) {
		extendTriplesFile("dkrl_data/valid.txt", "dkrl_data/triples_extended/valid.txt");
		extendTriplesFile("dkrl_data/train.txt", "dkrl_data/triples_extended/train.txt");
		extendTriplesFile("dkrl_data/test.txt", "dkrl_data/triples_extended/test.txt");	
	}
}
