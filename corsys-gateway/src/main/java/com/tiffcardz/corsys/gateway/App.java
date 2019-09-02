package com.tiffcardz.corsys.gateway;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{	
	public static EcgStorageInterface ecgStorage;
	
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        try {
			ecgStorage = TapeEcgStorage.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
        
        List<String> ecgLines = new MITCsvReader(Paths.get(Constants.MIT_CSV_FILE_PATH)).getEcgLines();
        EcgSampleGenerator ecgSampleGenerator = new EcgSampleGenerator(true, ecgLines, new EcgBlockCreatedListener() {
			public void onEcgBlockCreated(EcgSampleGenerator ecgSampleGenerator, EcgBlock ecgBlock) {
				ecgStorage.saveEcgBlock(ecgBlock);
			}
		});
        ecgSampleGenerator.generateEcgSamples();
    }
}
