import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class FilterRankedList {
	public static boolean hasItem(ArrayList<String[]> list, String[] item) {
		boolean exists = false;
		for(String[] x : list) {
			if((x[0].equals(item[0])) && (x[1]).equals(item[1]))	{ exists = true; break;}
			else if((x[1].equals(item[0])) && (x[0]).equals(item[1])) { exists = true; break; }
		}
		return exists;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		System.out.println("Filename: ");	Scanner reader = new Scanner(System.in);
		String filename = reader.nextLine();	reader.close();
		if(filename.indexOf(".tsv") == -1)	filename += ".tsv";
		ArrayList<String[]> added = new ArrayList<>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf(" ") != -1) {
					String id1 = "";	String id2 = "";	String tmp = line;
					id1 = tmp.substring(0, tmp.indexOf(" "));	tmp = tmp.substring(tmp.indexOf(" ")+1);
					tmp = tmp.substring(tmp.indexOf(" ")+1);	id2 = tmp.substring(0, tmp.indexOf(" "));
					
					if((!id1.equals(id2)) && (!hasItem(added, new String[] { id1, id2 }))) {
						appendToFile("_" + filename, line + "\n");	added.add(new String[] { id1, id2 });
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
}
