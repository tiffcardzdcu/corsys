package com.tiffcardz.corsys.sensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class EcgSensor {
	
	private static int MAX_STUDY_TIME_MINS = 1;
	private static final int NUM_CHANNELS = 2;
	private static final int ECG_BLOCK_SIZE_SECS = Constants.MAX_ECG_BLOCK_SIZE_SECONDS;
	private static final int SAMPLE_RATE = Constants.SAMPLE_RATE;
	private static final int TOTAL_SAMPLES = SAMPLE_RATE * ECG_BLOCK_SIZE_SECS * NUM_CHANNELS;	
	private static final int MAX_BLOCKS_TO_GENERATE = (MAX_STUDY_TIME_MINS * 60)/ECG_BLOCK_SIZE_SECS;
	private static final int AGE = 30;

	
	private static final String PATIENT_NAME = "TIFFANY CARDOZO";
	private static String PATIENT_ID = "TiffanyCardozo0"; //currTimeMsStr.substring(currTimeMsStr.length() - 8 - 1, currTimeMsStr.length() - 1); // 8 chars max
	
	private static boolean IS_REALTIME = false;
	private static String MQTT_BROKER_HOST = "192.168.0.203";
	
	private static final String MIT_CSV_FILENAME = "MIT_NSR_16265.csv";
	
	private static String STUDY_ID = genUUID();
	private static long STUDY_TIMESTAMP = getCurrentTimestamp();
	private static String STUDY_DATE = getCurrentDate();
	private static String STUDY_TIME = getCurrentTime();
	private static String ACQUISITION_TIME = getCurrentTime();
	private static String ACQUISITION_DATE_TIME = getCurrentDateTime();
	private static String ACQUISITION_DATE = getCurrentDate();
	
	private static final int DEV_MESG_TYPE_ECG_BLOCK = 1;
	private static final int DEV_MESG_TYPE_STUDY_FINISHED = 2;
		
	private static String genUUID() {
		return UUID.randomUUID().toString();
	}
	

	private static void log(String str) {
		System.out.println(str);
	}
	
	private static String getCurrentDate() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate localDate = LocalDate.now();
		return dtf.format(localDate);
	}
	
	private static String getCurrentDate(long timestamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
		return dtf.format(localDate);
	}
	
	private static String getCurrentTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss.SSS");
		LocalTime localTime = LocalTime.now();
		return dtf.format(localTime);
	}
	
	private static String getCurrentTime(long timestamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss.SSS");
		LocalTime localTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime();
		return dtf.format(localTime);
	}
	
	private static String getCurrentDateTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		LocalDateTime localDateTime = LocalDateTime.now();
		return dtf.format(localDateTime);
	}
	
	private static String getCurrentDateTime(long timestamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());  
		return dtf.format(localDateTime);
	}
	
	private static long getCurrentTimestamp() {
		return System.currentTimeMillis();
	}
	
	private static int ecgBlocksGenerated = 0;
	
	private static List<String> readFromInputStream(InputStream inputStream) throws IOException {

		List<String> lineList = new ArrayList<String>();
	    
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            lineList.add(line);
	        }
	    }
	    return lineList;
	}
	
	private static MqttClient client;
	
	// MAIN(...)
	// java -jar <realtime> <total-time> <mqtt-host> <patient-id>
	public static void main(String[] args) throws MqttException, InterruptedException {
		
		System.out.println(args[0]);
		System.out.println(args[1]);
		System.out.println(args[2]);
		System.out.println(args[3]);
		
		try {			
			IS_REALTIME = Boolean.parseBoolean(args[0]);
			MAX_STUDY_TIME_MINS = Integer.parseInt(args[1]);
			MQTT_BROKER_HOST = args[2];		
			PATIENT_ID = args[3];
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.out.println("Input args: <realtime-bool> <total-time-mins> <mqtt-host> <patient-id>");
			return;
		}
		
		
		
		client = new MqttClient("tcp://"+MQTT_BROKER_HOST+":1883", MqttClient.generateClientId(), new MqttDefaultFilePersistence("/tmp/paho"));
		client.setCallback( new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
				log("Connection Lost: " + cause.getMessage());
			}
		});
		
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		client.connect(options);
		log("MQTT isConnected: " + client.isConnected());

		MqttMessage regMesg = new MqttMessage(("REGISTER,"+PATIENT_ID).getBytes());
		client.publish(Topics.TIFFCARDZ_GATEWAY.name(), regMesg);
		
		STUDY_ID = genUUID();
		STUDY_DATE = getCurrentDate();
		STUDY_TIMESTAMP = System.currentTimeMillis();

		// Reading Physionet CSV source into a list of Strings
		//final File f = new File(MyDicomWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		InputStream inputStream = EcgSensor.class.getResourceAsStream("/" + MIT_CSV_FILENAME);
	    List<String> ecgLines;
		try {
			ecgLines = readFromInputStream(inputStream);
		} catch (IOException e1) {
			log("Couldn't read CSV Example file");
			e1.printStackTrace();
			return;
		}
		
		Thread.sleep(3000);
		
		// Defining a 10-second block generator
		EcgSampleGenerator ecgSampleGenerator = new EcgSampleGenerator(IS_REALTIME, ecgLines, new EcgBlockCreatedListener() {
			public void onEcgBlockCreated(EcgSampleGenerator ecgSampleGenerator, EcgBlock ecgBlock) {
				
				long currentTimestamp = -1;
				if(IS_REALTIME) {
					currentTimestamp = getCurrentTimestamp();
				}
				else {
					currentTimestamp = STUDY_TIMESTAMP + (ecgBlocksGenerated * (ECG_BLOCK_SIZE_SECS * 1000));
				}

				ACQUISITION_DATE = getCurrentDate(currentTimestamp);
				ACQUISITION_TIME = getCurrentTime(currentTimestamp);
				ACQUISITION_DATE_TIME = getCurrentDateTime(currentTimestamp);
				
				int seriesNumber = ecgBlocksGenerated + 1;
				
				log(" ");
				log("======================================================");
				log("Series Number: " + seriesNumber);
				
				/*
				==========================================================================================
				msg_type[4]=1, size[4], patientId[size], 
				block_sample_size[4], ch1[TOTAL_SAMPLES X 2], ch2[TOTAL_SAMPLES X 2], 
				STUDY_DATE[8], STUDY_TIME[10], STUDY_ID[36], 
				seriesNumber[4], seriesDate[8], seriesTime[10], 
				acqDateTime[14]
				===========================================================================================
				msg_type[2]=2
				===========================================================================================
				*/
				int totalBytes = 4 + (4 + PATIENT_ID.length()) + 4 + (ecgBlock.ch1ShortArr.length*2*2) 
									+ 8 + 10 + 36
									+ 4 + 8 + 10 + 14;
				
				ByteBuffer buff = ByteBuffer.allocate(totalBytes);
					buff.putInt(DEV_MESG_TYPE_ECG_BLOCK);
					
					buff.putInt(PATIENT_ID.length());
					buff.put(PATIENT_ID.getBytes());
					
					buff.putInt(ecgBlock.ch1ShortArr.length);
					for(short s: ecgBlock.ch1ShortArr)
						buff.putShort(s);
					for(short s: ecgBlock.ch2ShortArr)
						buff.putShort(s);
					
					buff.put(STUDY_DATE.getBytes());
					buff.put(STUDY_TIME.getBytes());
					buff.put(STUDY_ID.getBytes());
					
					buff.putInt(seriesNumber);
					buff.put(ACQUISITION_DATE.getBytes());
					log(ACQUISITION_DATE_TIME);
					log(ACQUISITION_DATE_TIME.length() + "");
					buff.put(ACQUISITION_DATE_TIME.getBytes());
				
				MqttMessage mesg = new MqttMessage(buff.array());				
				try {
					client.publish(Topics.TIFFCARDZ_DEV_ + PATIENT_ID, mesg);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
									
				ecgBlocksGenerated++;

				if(ecgBlocksGenerated >= MAX_BLOCKS_TO_GENERATE) {
					log("Study Finiahed");
					ecgSampleGenerator.stopGenerator();
					
					ByteBuffer buffType = ByteBuffer.allocate(Integer.BYTES);
					buffType.putInt(DEV_MESG_TYPE_STUDY_FINISHED);
					MqttMessage mesgEnded = new MqttMessage(buffType.array());				
					try {
						client.publish(Topics.TIFFCARDZ_DEV_ + PATIENT_ID, mesgEnded);
					} catch (MqttException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					System.exit(1);
				}
			}
		});
		
		// Generating the 10 second block
		ecgSampleGenerator.generateEcgSamples();
	}
	
	
}
