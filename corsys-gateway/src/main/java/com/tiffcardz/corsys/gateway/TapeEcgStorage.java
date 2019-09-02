package com.tiffcardz.corsys.gateway;

import java.io.File;
import java.io.IOException;

import com.squareup.tape2.QueueFile;

public class TapeEcgStorage implements EcgStorageInterface {

	private QueueFile ecgTape;
	private QueueFile ecgTransmittedTape;
	
	private static TapeEcgStorage INSTANCE;
	
	private TapeEcgStorage() throws IOException {
		ecgTape = new QueueFile.Builder(new File("ecgTape.dat")).build();
		ecgTransmittedTape = new QueueFile.Builder(new File("ecgTransmittedTape.dat")).build();
	}
	
	public static TapeEcgStorage getInstance() throws IOException {
		if(INSTANCE == null) {
			INSTANCE = new TapeEcgStorage();
		}
		
		return INSTANCE;
	}

	public void saveEcgBlock(EcgBlock ecgBlock) {
		try {
			ecgTape.add(ecgBlock.serialize());
			System.out.println(ecgTape.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public EcgBlock getOldestUntransmittedEcgBlock() {
		byte[] oldestEcgBlockBytes = ecgTape.iterator().next();
		EcgBlock ecgBlock;
		try {
			ecgBlock = EcgBlock.deserialize(oldestEcgBlockBytes);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}
