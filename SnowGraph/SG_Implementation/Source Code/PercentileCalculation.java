package scigraph_dataset;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class PercentileCalculation {	
	private static double[][] readMatrixFile(String path) {
		// Get number of columns and rows:
		int total = 0;
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line = br.readLine(); br.close();
			try {	total = Integer.parseInt(line.trim());	} catch(Exception e) {	return null;	}
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Read matrix-file:
		double[][] matrix = new double[total][total]; 
		int lineidx = 0; int lcounter = 0;	int rcounter = 0;
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				rcounter = 0;
				if(lineidx != 0) {
					lcounter = lineidx - 1;
					while((lcounter < total) && (rcounter < total)) {
						String tmp = "";
						if(line.indexOf(" ") != -1) {
							tmp = line.substring(0, line.indexOf(" "));	line = line.substring(line.indexOf(" ")+1);
						}
						else {	tmp = line.trim();	line = "";	}
						// store value in matrix:
						try {	
							matrix[lcounter][rcounter] = Double.parseDouble(tmp);	
							matrix[rcounter][lcounter] = Double.parseDouble(tmp);
						} 
						catch(Exception e) {	e.printStackTrace(); return null;	}	
						rcounter++;
					}
				}
				lineidx++;
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return matrix;
	}
	
	private static ArrayList<Double> getSimValues(double[][] matrix) {
		ArrayList<Double> simvalues = new ArrayList<>();	int total = matrix.length;
		for(int i = 0; i < total; i++) {
			for(int j = 0; j < total; j++) {
				if(j > i)	simvalues.add(matrix[i][j]);
			}
		}
		return simvalues;
	}
	
	private static void printPercentiles(ArrayList<Double> simValues, String title, String outputpath) {
		double sum = 0;	for(double d : simValues)	sum += d;	
		double avgvalue = sum / (double) simValues.size();
		// Round value to 4 decimal places:
		long factor = (long) Math.pow(10, 4);	avgvalue = avgvalue * factor;
		long tmp = Math.round(avgvalue);	avgvalue = (double) tmp / factor;
		
		String output = "\n" + title + "\n******************\n";
		output += "Min: " + getPercentileValue(simValues, 0) + "\n";
		output += "Max: " + getPercentileValue(simValues, 100) + "\n";
		output += "Average: " + avgvalue + "\n";
		output += "Median: " + getPercentileValue(simValues, 50) + "\n";
		output += "\nPercentile\tSimilarity\n";
		for(int i = 10; i <=95; i=i+5) 	output += i + "\t" + getPercentileValue(simValues, i) + "\n";
		output += "98\t" + getPercentileValue(simValues, 98) + "\n";
		
		System.out.print(output);
		appendToFile(outputpath, output);
	}
	
	private static double getPercentileValue(ArrayList<Double> simValues, double percentile) {
		Collections.sort(simValues);	double value = -1;
		if(percentile == 0)	value = simValues.get(0);
		else if(percentile == 100)	value = simValues.get(simValues.size()-1);
		else {
			int idx = (int) Math.ceil(((double) percentile / 100.0) * (double) simValues.size());
			if((simValues.size() % 2) == 0)	value = ((simValues.get(idx - 1) + simValues.get(idx)) / 2);
			else value = simValues.get(idx - 1);
		}
		
		// Round value to 4 decimal places:
		long factor = (long) Math.pow(10, 4);	value = value * factor;
		long tmp = Math.round(value);	value = (double) tmp / factor;
		return value;
	}
	
	public static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		double[][] confs_matrix = readMatrixFile("data/output/Conf_matrix.txt");
		ArrayList<Double> confs_sim = getSimValues(confs_matrix);
		if(confs_sim.size() > 0)	printPercentiles(confs_sim, "Conf_matrix", "data/output/percentiles.txt");
		
		double[][] auths_matrix = readMatrixFile("data/output/Auth_matrix.txt");
		ArrayList<Double> auths_sim = getSimValues(auths_matrix);
		if(auths_sim.size() > 0)	printPercentiles(auths_sim, "Auth_matrix", "data/output/percentiles.txt");
	}
}
