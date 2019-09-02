package com.tiffcardz.corsys.sensor;

import java.util.List;


public class EcgSampleGenerator {
	List<String> ecgLines;
	boolean realTime = true;
	boolean stopGeneration = false;
	public EcgSampleGenerator(boolean realTime, List<String> ecgLines, EcgBlockCreatedListener ecgBlockCreatedListener) {
		
		this.ecgLines = ecgLines;
		this.realTime = realTime;
		
		EcgSampleAcculumator.initQueues();
		EcgSampleAcculumator.setEcgBlockCreatedListener(this, ecgBlockCreatedListener);
		
	}
	
	private void log(String str) {
		Log.log(EcgSampleGenerator.class, str);
	}
	
	public void generateEcgSamples() {
		
		long n = 0;
		long sec = 1;
		
		while(true) {
			for(String s: ecgLines) {
				
				// break inner while
				if(stopGeneration)
				{
					log("Stopped Generation");
					break;
				}
				String[] parts = s.split(",");
				short ch1 = Short.parseShort(parts[1]);
				short ch2 = Short.parseShort(parts[2]);
				//System.out.println("ECG sample#  " + n + ": " + parts[1] + ", " + parts[2] );
				
				
				EcgSampleAcculumator.addSamplesToQueues(ch1, ch2);
				
				if((n > 0) && (n % Constants.SAMPLE_RATE == 0)) {
					
					
					System.out.println("Time: " + sec + " seconds");
					sec++;
					//System.out.println("the value of n is: " + n);
					
					if(realTime) {
						try {
							//Thread.sleep((long)(1000/(float) Constants.SAMPLE_RATE));
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					n = 0;
				}
				
				n++; 
			}
			
			// break outer while
			if(stopGeneration)
			{
				log("Stopped Generation");
				break;
			}
		}
	}

	public void stopGenerator() {
		stopGeneration = true;
	}
}