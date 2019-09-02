package com.tiffcardz.corsys.gateway;

import java.net.URI;

public class Constants {
	public static final int SAMPLE_RATE = 360;
	public static final int MAX_ECG_BLOCK_SIZE_SECONDS = 10;
	public static final int MAX_ECG_BLOCK_SIZE_SAMPLES = MAX_ECG_BLOCK_SIZE_SECONDS * SAMPLE_RATE;
	public static final String MIT_CSV_FILE_PATH = "src/main/resources/MIT_NSR_16795.csv";
	
	

}
