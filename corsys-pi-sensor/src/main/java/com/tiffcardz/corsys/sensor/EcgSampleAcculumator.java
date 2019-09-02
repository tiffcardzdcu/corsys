package com.tiffcardz.corsys.sensor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

public class EcgSampleAcculumator {
	public static ConcurrentLinkedQueue<Short> CH1_QUEUE;
	public static ConcurrentLinkedQueue<Short> CH2_QUEUE;
	
	private static EcgBlockCreatedListener ecgBlockCreatedListener;
	private static EcgSampleGenerator ecgSampleGenerator;
	
	public static void initQueues() {
		CH1_QUEUE = new ConcurrentLinkedQueue<Short>();
		CH2_QUEUE = new ConcurrentLinkedQueue<Short>();
	}
	
	public static void setEcgBlockCreatedListener(EcgSampleGenerator ecgSampleGenerator, EcgBlockCreatedListener ecgBlockCreatedListener) {
		EcgSampleAcculumator.ecgBlockCreatedListener = ecgBlockCreatedListener;
		EcgSampleAcculumator.ecgSampleGenerator = ecgSampleGenerator;
	}
	
	public static void addSamplesToQueues(short ch1, short ch2) {
		CH1_QUEUE.add(ch1);
		CH2_QUEUE.add(ch2);
		//System.out.println("CH1_QUEUE size(): " + CH1_QUEUE.size());
		
		if(CH1_QUEUE.size() >= Constants.MAX_ECG_BLOCK_SIZE_SAMPLES) {
			
			System.out.println(Constants.MAX_ECG_BLOCK_SIZE_SECONDS + " secs complete");
			
			// get the oldest max queue size samples from queue and save to local storage
			short[] ch1Shorts = new short[Constants.MAX_ECG_BLOCK_SIZE_SAMPLES];
			for(int i = 0; i < Constants.MAX_ECG_BLOCK_SIZE_SAMPLES; i++) {
				ch1Shorts[i] = CH1_QUEUE.poll();
			}
			
			short[] ch2Shorts = new short[Constants.MAX_ECG_BLOCK_SIZE_SAMPLES];
			for(int i = 0; i < Constants.MAX_ECG_BLOCK_SIZE_SAMPLES; i++) {
				ch2Shorts[i] = CH2_QUEUE.poll();
			}
			
			EcgBlock ecgBlock = new EcgBlock(0, Constants.MAX_ECG_BLOCK_SIZE_SAMPLES, Constants.SAMPLE_RATE, ch1Shorts, ch2Shorts);
			//App.ecgStorage.saveEcgBlock(ecgBlock);
			if(!ecgSampleGenerator.realTime) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ecgBlockCreatedListener.onEcgBlockCreated(ecgSampleGenerator, ecgBlock);
		}
	}

}
