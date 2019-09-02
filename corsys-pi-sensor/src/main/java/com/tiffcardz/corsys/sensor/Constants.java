package com.tiffcardz.corsys.sensor;

public class Constants {
	public static final int SAMPLE_RATE = 360;
	public static final int MAX_ECG_BLOCK_SIZE_SECONDS = 10;
	public static final int MAX_ECG_BLOCK_SIZE_SAMPLES = MAX_ECG_BLOCK_SIZE_SECONDS * SAMPLE_RATE;
}
