package scigraph_dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GenerateMETISGraph {
	static HashMap<String, Integer> authordict, confdict;
	static double[][] authmatrix, confmatrix;
	
	private static HashMap<String, Integer> retrieveDict(String path) {
		HashMap<String, Integer> dict = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int lcounter = 0;	int idx = 0;
			while((line = br.readLine()) != null) {
				if(lcounter > 0) {
					if(!line.trim().equals("")) {	dict.put(line.trim(), idx);	idx++;	}
				}
				lcounter++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return dict;
	}
	
	private static double[][] loadMatrix(String path) {
		System.out.println("Loading Matrix " + path + " ....");
		double[][] matrix = null;	int x = -1;
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int lcounter = 0;	int row = 0;
			while((line = br.readLine()) != null) {
				if(lcounter == 0)	{
					try { x = Integer.parseInt(line.trim()); } catch(Exception e) {}
					if(x != -1)	matrix = new double[x][x];
				}
				else {
					int column = 0;
					while(!line.trim().equals("")) {
						String tmp = "";
						if(line.indexOf(" ") != -1) {
							tmp = line.substring(0, line.indexOf(" "));	line = line.substring(line.indexOf(" ") + 1);
						}
						else {	tmp = line.trim();	line = "";	}
						try {	double value = Double.parseDouble(tmp); matrix[row][column] = value;
						} catch(Exception e) {}
						column++;
					}
					row++;
				}
				lcounter++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		return matrix;
	}
	
	private static ArrayList<String[]> loadInteractions(String path) {
		ArrayList<String[]> interactions = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line; int counter = 0;
			while((line = br.readLine()) != null) {
				if(counter != 0) {
					if(line.indexOf("\t") != -1) {
						String aID = line.substring(0, line.indexOf("\t"));	line = line.substring(line.indexOf("\t")+1);
						String cID = line.substring(0, line.indexOf("\t"));
						interactions.add(new String[] {aID, cID});
					}
				}
				counter++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return interactions;
	}
	
	private static double[][] getSimMatrix(double thresholdA, double thresholdC, ArrayList<String[]> interactions) {
		int size = interactions.size();
		double[][] simmatrix = new double[size][size];
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				String author1 = interactions.get(i)[0];	String conf1 = interactions.get(i)[1];
				String author2 = interactions.get(j)[0];	String conf2 = interactions.get(j)[1];
				double simAuth = authmatrix[authordict.get(author1)][authordict.get(author2)];
				double simConf = confmatrix[confdict.get(conf1)][confdict.get(conf2)];
				if((simAuth < thresholdA) || (simConf < thresholdC))	simmatrix[i][j] = 0.0;
				else	simmatrix[i][j] = simAuth * simConf;
			}
		}
		System.out.println("Interrelational Similarity-Matrix was generated!");
		return simmatrix;
	}
	
	private static void generateMETISGraph(double[][] simMatrix, String outputpath) {
		System.out.println("Generating METIS-graph ....");
		int size = simMatrix.length;	int counter = 0;	String tmpoutputpath = "tmp_" + outputpath;
		for(int i = 0; i < size; i++) {
			String output = "";	boolean emptyline = true;
			for(int j = 0; j < size; j++) {
				double tmp = simMatrix[i][j] * 10000;	int value = (int) Math.round(tmp);
				if((value > 0) && (i != j)) {
					counter++;
					if(emptyline) {
						int idx = j+1;
						output += idx + " " + value;
						emptyline = false;
					}
					else {
						int idx = j+1;
						output += " " + idx + " " + value;
					}
				}
			}
			if(output.equals(""))	output = " ";
			appendToFile(tmpoutputpath, output + "\n");
		}
		
		int num = counter / 2;
		String output = size + " " + num + " 001\n";
		appendToFile(outputpath, output);
		try(BufferedReader br = new BufferedReader(new FileReader(tmpoutputpath))) {
			String line;
			while((line = br.readLine()) != null) {
				if(!line.equals(""))	appendToFile(outputpath, line + "\n");
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		File tmp = new File(tmpoutputpath); tmp.delete();
		System.out.println("METIS-graph file was created!");
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		double author_percentile = 0.5;	double conf_percentile = 0.1413;
		String authorpath = "data_sg/output/Author.txt";	String authmatrixpath = "data_sg/output/Auth_matrix.txt";
		String confpath = "data_sg/output/Conf.txt";	String confmatrixpath = "data_sg/output/Conf_matrix.txt";
		String graphpath = "data_sg/output/Auth-Conf_graph.txt";	
		String outputpath = "simrelations98.txt";	String graphoutputpath = "metis98.txt";
		
		authordict = retrieveDict(authorpath);	confdict = retrieveDict(confpath);
		authmatrix = loadMatrix(authmatrixpath);	confmatrix = loadMatrix(confmatrixpath);
		ArrayList<String[]> interactions = loadInteractions(graphpath);
		double[][] simMatrix = getSimMatrix(author_percentile, conf_percentile, interactions);
		generateMETISGraph(simMatrix, graphoutputpath);
		
		// Print Interrelations-similarity-matrix:
		int x = simMatrix.length;	System.out.println(x + " column + rows!");
		System.out.println("Printing Matrix ....");
		for(int i = 0; i < x; i++) {
			if((i % 500) == 0)	System.out.print("..");
			String output = "";
			for(int j = 0; j < x; j++) {
				if(j != (x-1))	output += simMatrix[i][j] + ",";
				else	output += simMatrix[i][j] + "\n";
			}
			appendToFile(outputpath, output);
		}
	}
}
