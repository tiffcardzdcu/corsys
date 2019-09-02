package com.tiffcardz.corsys.gateway;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class EcgBlock implements Serializable {

	public long startSampleNumber;
	public int numberOfSamples;
	public int sampleRate;
	public short[] ch1ShortArr;
	public short[] ch2ShortArr;
	
	protected EcgBlock(long startSampleNumber, int numberOfSamples, int sampleRate, short[] ch1ShortArr, short[] ch2ShortArr) {
		this.startSampleNumber = startSampleNumber;
		this.numberOfSamples = numberOfSamples;
		this.sampleRate = sampleRate;
		this.ch1ShortArr = ch1ShortArr;
		this.ch2ShortArr = ch2ShortArr;
	}
	
	
	public byte[] serialize() throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(this);
	    return out.toByteArray();
	}
	
	public static EcgBlock deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return (EcgBlock) is.readObject();
	}
	
	
}
