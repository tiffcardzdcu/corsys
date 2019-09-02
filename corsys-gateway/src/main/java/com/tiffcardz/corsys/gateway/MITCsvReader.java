package com.tiffcardz.corsys.gateway;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MITCsvReader {
	
	List<String> ecgLines;
	
	public MITCsvReader(Path csvFile) {
		// convert csv file contents to list of lines
		try {
			ecgLines = Files.readAllLines(csvFile);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<String> getEcgLines() {
		return ecgLines;
	}
}
