import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RetrieveValidationData {

	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		ArrayList<String[]> triples = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader("train.txt"))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String subject = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
					String relation = line.substring(0, line.indexOf("\t"));	String object = line.substring(line.indexOf("\t")+1).trim();
					triples.add(new String[] {subject, relation, object});
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		Integer[] helplist = new Integer[triples.size()];	for(int i = 0; i < triples.size(); i++)	helplist[i] = i;
		List<Integer> listtmp = Arrays.asList(helplist);	Collections.shuffle(listtmp);
		int counter = 1;
		for(int num : listtmp) {
			appendToFile("valid.txt", triples.get(num)[0] + "\t" + triples.get(num)[1] + "\t" + triples.get(num)[2] + "\n");
			if(counter == 6000)	break;
			counter++;
		}
	}
}
