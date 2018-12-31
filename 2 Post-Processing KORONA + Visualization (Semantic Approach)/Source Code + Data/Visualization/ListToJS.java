import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ListToJS {
	
	public static ArrayList<String[]> retrieveList(String path) {
		ArrayList<String[]>	authorlist = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String id = line.substring(0, line.indexOf("\t")).trim();	String name = line.substring(line.indexOf("\t")+1);
					String[] tmp = new String[2];	tmp[0] = id;	tmp[1] = name;	authorlist.add(tmp);
				}
			}
			br.close();
		} catch(IOException e)	{	e.printStackTrace();	}
		return authorlist;
	}
	
	public static String convertList(ArrayList<String[]> authorlist, String name) {
		String jsString = "";
		String filename = name + ".txt";
		
		// Convert ArrayList:
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for(int i = 0; i < authorlist.size(); i++) {
			JsonObject entry = Json.createObjectBuilder().add("id", authorlist.get(i)[0]).add("name", authorlist.get(i)[1]).build();
			builder.add(entry);
		}
		JsonArray authors = builder.build();
		
		// Create JSON-Object:
		JsonObject obj = Json.createObjectBuilder().add("file", filename).add("size", authorlist.size())
				.add("entities", authors).build();
		
		// Store Object in String:
		String objJS = obj.toString();
		jsString = "var " + name + " = " + objJS + ";";
		return jsString;
	}
	
	public static void writeToNewFile(String path, String content) {
		try {
			File file = new File(path);	file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(content);	out.close();	System.out.println("File \"" + path + "\" was created! ");
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		// Input-file:
		Scanner reader = new Scanner(System.in);	System.out.println("Name of File with authors-list: ");
		String authorlistpath = reader.nextLine();	reader.close();
		if(authorlistpath.indexOf(".txt") == -1)	authorlistpath += ".txt";
		
		// Retrieve List from Input-file:
		ArrayList<String[]> list = retrieveList(authorlistpath);
		
		// Output-file:
		String outputpath = "";	String outputfile = "";
		if(authorlistpath.indexOf("/") != -1)	
			outputpath = authorlistpath.substring(authorlistpath.lastIndexOf("/")+1, authorlistpath.indexOf(".txt")) + ".js";
		else	outputpath = authorlistpath.substring(0, authorlistpath.indexOf(".txt")) + ".js";
		
		// Convert List:
		outputfile = convertList(list, outputpath.substring(0, outputpath.indexOf(".js")));
		
		// Create Output-file:
		writeToNewFile(outputpath, outputfile);
	}
}
