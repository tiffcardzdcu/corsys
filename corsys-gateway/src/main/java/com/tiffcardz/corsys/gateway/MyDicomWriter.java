package com.tiffcardz.corsys.gateway;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.util.ByteUtils;
import org.dcm4che3.net.IncompatibleConnectionException;

public class MyDicomWriter {
	
	private static boolean IS_REALTIME = false;
	private static String CONQUEST_HOST = "35.153.27.6";
	private static int CONQUEST_PORT = 5678;
	private static final String GENERATED_DICOM_PATH = "/tmp/generated_dicoms";
	private static final String MIT_CSV_FILENAME = "MIT_NSR_16265.csv";
	
	private static final String PATIENT_NAME = "TIFFANY CARDOZO";
	private static final String PATIENT_ID = "tiffcardz"; //currTimeMsStr.substring(currTimeMsStr.length() - 8 - 1, currTimeMsStr.length() - 1); // 8 chars max
	private static final int AGE = 30;
	
	private static String STUDY_ID = genUUID();
	private static long STUDY_TIMESTAMP = getCurrentTimestamp();
	private static String STUDY_DATE = getCurrentDate();
	private static String STUDY_TIME = getCurrentTime();
	private static String ACQUISITION_DATE_TIME = getCurrentDateTime();
	private static String ACQUISITION_DATE = getCurrentDate();
	
	private static final String MEDIA_STORAGE_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.9.1.1"; //genUUID();
	private static final String MEDIA_STORAGE_SOP_INSTANCE_UID = "1.2.826.0.1.34471.2.44.6.20021122091000..1"; //genUUID();
	
	private static final int NUM_CHANNELS = 2;
	private static final int ECG_BLOCK_SIZE_SECS = 10;
	private static final int SAMPLE_RATE = 360;
	private static final int TOTAL_SAMPLES = SAMPLE_RATE * ECG_BLOCK_SIZE_SECS * NUM_CHANNELS;

	private static final int MAX_STUDY_TIME_MINS = 1;
	private static final int MAX_BLOCKS_TO_GENERATE = (MAX_STUDY_TIME_MINS * 60)/ECG_BLOCK_SIZE_SECS;
	
	private static final int WAVEFORM_BITS_ALLOCATION = 16;

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
	
	private static String readFromInputStream(InputStream inputStream)
			  throws IOException {
			    StringBuilder resultStringBuilder = new StringBuilder();
			    try (BufferedReader br
			      = new BufferedReader(new InputStreamReader(inputStream))) {
			        String line;
			        while ((line = br.readLine()) != null) {
			            resultStringBuilder.append(line).append("\n");
			        }
			    }
			  return resultStringBuilder.toString();
			}
	
	// MAIN(...)
	public static void main(String[] args) {
		
		File tmpGenDicoms = new File(GENERATED_DICOM_PATH);
		tmpGenDicoms.mkdirs();
		tmpGenDicoms.deleteOnExit();
		
		if(args[0] != null) {
			CONQUEST_HOST = args[0];
		}
		
		try {
			if(args[1] != null) {
				IS_REALTIME = Boolean.parseBoolean(args[1]);
			}
		} catch(Exception e) {
			// ignore: remains false
		}
		
		log("Conquest Host: " + CONQUEST_HOST);

		// Reading Physionet CSV source into a list of Strings
		//final File f = new File(MyDicomWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		InputStream inputStream = MyDicomWriter.class.getResourceAsStream("/" + MIT_CSV_FILENAME);
	    String data = "";
	    Path path = null;
	
	    List<String> ecgLines;
	    
		try {
			data = readFromInputStream(inputStream);
			path = Files.createTempFile("myTempFile", ".txt");
	        path.toFile().deleteOnExit();
	        Files.write(path, data.getBytes());
	        ecgLines = new MITCsvReader(Paths.get(path.toUri())).getEcgLines();
		} catch (IOException e1) {
			log("Problem with reading example file.");
			e1.printStackTrace();
			return;
		}
		
		
		STUDY_ID = genUUID();
		STUDY_DATE = getCurrentDate();
		STUDY_TIMESTAMP = System.currentTimeMillis();
		
		// Defining a 10-second block generator
		EcgSampleGenerator ecgSampleGenerator = new EcgSampleGenerator(IS_REALTIME, ecgLines, new EcgBlockCreatedListener() {
			public void onEcgBlockCreated(EcgSampleGenerator ecgSampleGenerator, EcgBlock ecgBlock) {
				
				try {
					long currentTimestamp = -1;
					if(IS_REALTIME) {
						currentTimestamp = getCurrentTimestamp();
					}
					else {
						currentTimestamp = STUDY_TIMESTAMP + (ecgBlocksGenerated * (ECG_BLOCK_SIZE_SECS * 1000));
					}

					ACQUISITION_DATE_TIME = getCurrentDateTime(currentTimestamp);
					ACQUISITION_DATE = getCurrentDate(currentTimestamp);
					
					int seriesNumber = ecgBlocksGenerated + 1;
					
					log(" ");
					log("======================================================");
					log("Series Number: " + seriesNumber);
					
					final String dicomFilePath = createDicomFile(ecgBlock, seriesNumber, ACQUISITION_DATE_TIME, ACQUISITION_DATE);
					
					// Upload DICOM File to Conquest Server
					// not needed anymore. But also works
					//uploadDicomToConquestCurl("http://"+CONQUEST_HOST+"/dgate", dicomFilePath);					
					//viewDicomFile(dicomFilePath);
					
					cstore(dicomFilePath, CONQUEST_HOST, CONQUEST_PORT);
										
					ecgBlocksGenerated++;
					
				} catch (IOException e) {
					e.printStackTrace();
				}

				if(ecgBlocksGenerated >= MAX_BLOCKS_TO_GENERATE) {
					log("Study Finiahed");
					ecgSampleGenerator.stopGenerator();
					return;
				}
				
			}
		});
		
		// Generating the 10 second block
		ecgSampleGenerator.generateEcgSamples();
	}
	
	private static void cstore(String dicomFilePath, String hostname, int port) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        try {
            CStoreSCU scu = new CStoreSCU("corsys");
            scu.setExecutor(executor);
            scu.setScheduledExecutor(scheduledExecutor);
            scu.store("", hostname, port, new ArrayList<File>() {{add(new File(dicomFilePath));}});
            System.out.println("Success");
            System.out.println("Finished!");
        } catch (IOException | InterruptedException | GeneralSecurityException | IncompatibleConnectionException e) {
			e.printStackTrace();
		} finally {
            executor.shutdown();
            scheduledExecutor.shutdown();
        }
        
	}
	
	
	private static String createDicomFile(final EcgBlock ecgBlock, final int seriesNumber, final String acquisitionDateTime, final String acquisitionDate) throws IOException {
		
		short[][] chShorts = new short[][] {
				ecgBlock.ch1ShortArr, 
				ecgBlock.ch2ShortArr,
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS],
				new short[SAMPLE_RATE * ECG_BLOCK_SIZE_SECS]
			};
		
		short[] waveformShorts = new short[TOTAL_SAMPLES];
		for(int i = 0, s = 0; i < TOTAL_SAMPLES; s += NUM_CHANNELS) {
			
			int cs = (int) (i/NUM_CHANNELS);
			
			for(int ch = 0; ch < NUM_CHANNELS; ch++) {
				waveformShorts[s + ch] = chShorts[ch][cs];
				i++;
			}
		}
		byte[] waveformBytes = ByteUtils.shorts2bytesLE(waveformShorts);
		
		DicomObject dcmObj = new BasicDicomObject();
		dcmObj.putInt(Tag.FileMetaInformationGroupLength, VR.UL, 170);
		dcmObj.putBytes(Tag.FileMetaInformationVersion, VR.OB, new byte[] {0x00, 0x01});
		dcmObj.putString(Tag.MediaStorageSOPClassUID, VR.UI, MEDIA_STORAGE_SOP_CLASS_UID);
		dcmObj.putString(Tag.MediaStorageSOPInstanceUID, VR.UI, MEDIA_STORAGE_SOP_INSTANCE_UID);
		
		dcmObj.putString(Tag.TransferSyntaxUID, VR.UI, TransferSyntax.ExplicitVRLittleEndian.uid());
		dcmObj.putString(Tag.ImplementationClassUID, VR.UI, "2.24.985.4");
		dcmObj.putString(Tag.ImplementationVersionName, VR.SH, "Corsys");
		dcmObj.putString(Tag.AccessionNumber, VR.SH, "");
		dcmObj.putString(Tag.Modality, VR.CS, "ECG");
		dcmObj.putString(Tag.Manufacturer, VR.LO, "Corsys Inc.");
		dcmObj.putString(Tag.AccessionNumber, VR.SH, "");
		dcmObj.putString(Tag.ReferringPhysicianName, VR.PN, "Dr. Ref Physician");
		dcmObj.putString(Tag.OperatorsName, VR.PN, "");
		dcmObj.putString(Tag.ManufacturerModelName, VR.LO, "Corsys Sensaware");
		
		dcmObj.putString(Tag.PatientName, VR.PN, PATIENT_NAME);
		dcmObj.putString(Tag.PatientID, VR.LO, PATIENT_ID);
		dcmObj.putString(Tag.PatientBirthDate, VR.DA, "19880720");
		dcmObj.putString(Tag.PatientSex, VR.CS, "F");
		dcmObj.putString(Tag.OtherPatientIDs, VR.LO, "");
		dcmObj.putString(Tag.PatientAge, VR.AS, AGE + "");
		dcmObj.putString(Tag.PatientSize, VR.DS, "");
		dcmObj.putString(Tag.PatientWeight, VR.DS, "");
		dcmObj.putString(Tag.CurrentPatientLocation, VR.LO, "");
		dcmObj.putString(Tag.PatientInstitutionResidence, VR.LO, "");
		dcmObj.putString(Tag.VisitComments, VR.LT, "");
		dcmObj.putString(Tag.DeviceSerialNumber, VR.LO, "2");
		dcmObj.putString(Tag.SoftwareVersions, VR.LO, "0.13");
		
		dcmObj.putString(Tag.StudyDate, VR.DA, STUDY_DATE +" "+STUDY_TIME);
		dcmObj.putString(Tag.StudyTime, VR.TM, STUDY_TIME);
		dcmObj.putString(Tag.ContentDate, VR.DA, acquisitionDate);
		dcmObj.putString(Tag.ContentTime, VR.TM, getCurrentTime());
		dcmObj.putString(Tag.StudyInstanceUID, VR.UI, STUDY_ID);
		
		dcmObj.putString(Tag.SeriesInstanceUID, VR.UI, STUDY_ID);
		dcmObj.putString(Tag.SeriesNumber, VR.IS, "1");
		dcmObj.putString(Tag.SeriesDate, VR.DA, STUDY_DATE);
		dcmObj.putString(Tag.SeriesTime, VR.TM, getCurrentDateTime());
		
		dcmObj.putString(Tag.InstanceNumber, VR.IS, seriesNumber + "");
		dcmObj.putString(Tag.AcquisitionDateTime, VR.DT, acquisitionDateTime);
		dcmObj.putString(Tag.InstanceCreationDate, VR.DA, getCurrentDate());
		dcmObj.putString(Tag.InstanceCreationTime, VR.TM, getCurrentTime());
		
		dcmObj.putString(Tag.SOPClassUID, VR.UI, genUUID());
		dcmObj.putString(Tag.SOPInstanceUID, VR.UI, genUUID());
		
		DicomElement acqContextSeq = dcmObj.putSequence(Tag.AcquisitionContextSequence);
			DicomObject oacs = new BasicDicomObject();
			oacs.putString(Tag.ValueType, VR.CS, "CODE");
			DicomElement cncs = oacs.putSequence(Tag.ConceptNameCodeSequence);
				DicomObject ocncs = new BasicDicomObject();
				ocncs.putString(Tag.CodeValue, VR.SH, "5.4.5-33-1");
				ocncs.putString(Tag.CodingSchemeDesignator, VR.SH, "SCPECG");
				ocncs.putString(Tag.CodingSchemeVersion, VR.SH, "1.3");
				ocncs.putString(Tag.CodeMeaning, VR.LO, "Electrode Placement");
				cncs.addDicomObject(ocncs);
			DicomElement ccs = oacs.putSequence(Tag.ConceptCodeSequence);
				DicomObject occs = new BasicDicomObject();
				occs.putString(Tag.CodeValue, VR.SH, "5.4.5-33-1-0");
				occs.putString(Tag.CodingSchemeDesignator, VR.SH, "SCPECG");
				occs.putString(Tag.CodingSchemeVersion, VR.SH, "1.3");
				occs.putString(Tag.CodeMeaning, VR.LO, "Unspecified");
				ccs.addDicomObject(occs);
			acqContextSeq.addDicomObject(oacs);		
		dcmObj.putString(Tag.ReasonForTheRequestedProcedure, VR.LO, "");			
		DicomElement wfSeq = dcmObj.putSequence(Tag.WaveformSequence);	
		
			DicomObject owfs = new BasicDicomObject();
			owfs.putString(Tag.MultiplexGroupTimeOffset, VR.DS, "0");
			owfs.putString(Tag.TriggerTimeOffset, VR.DS, "0");
			owfs.putString(Tag.WaveformOriginality, VR.CS, "ORIGINAL");
			owfs.putInt(Tag.NumberOfWaveformChannels, VR.US, NUM_CHANNELS);
			owfs.putInt(Tag.NumberOfWaveformSamples, VR.UL, TOTAL_SAMPLES/NUM_CHANNELS);
			owfs.putString(Tag.SamplingFrequency, VR.DS, "" + (float) SAMPLE_RATE);
			owfs.putString(Tag.MultiplexGroupLabel, VR.SH, "RHYTHM");
			DicomElement cds = owfs.putSequence(Tag.ChannelDefinitionSequence);
			
			for(int ch = 0; ch < NUM_CHANNELS; ch++) 
			{
				DicomObject ocds = new BasicDicomObject();
					DicomElement css = ocds.putSequence(Tag.ChannelSourceSequence);
						DicomObject ocss = new BasicDicomObject();
							ocss.putString(Tag.CodeValue, VR.SH, "5.6.3-9-" + ch);
							ocss.putString(Tag.CodingSchemeDesignator, VR.SH, "SCPECG");
							ocss.putString(Tag.CodingSchemeVersion, VR.SH, "1.3");
							ocss.putString(Tag.CodeMeaning, VR.LO, "Ch " + (ch+1));
						css.addDicomObject(ocss);					
					ocds.putString(Tag.ChannelSensitivity, VR.DS, "2.5");					
					DicomElement csus = ocds.putSequence(Tag.ChannelSensitivityUnitsSequence);
						DicomObject ocsus = new BasicDicomObject();
							ocsus.putString(Tag.CodeValue, VR.SH, "uV");
							ocsus.putString(Tag.CodingSchemeDesignator, VR.SH, "UCUM");
							ocsus.putString(Tag.CodingSchemeVersion, VR.SH, "1.4");
							ocsus.putString(Tag.CodeMeaning, VR.LO, "microvolt");
						csus.addDicomObject(ocsus);
					ocds.putString(Tag.ChannelSensitivityCorrectionFactor, VR.DS, "1");
					ocds.putString(Tag.ChannelBaseline, VR.DS, "0");
					ocds.putString(Tag.ChannelSampleSkew, VR.DS, "0");
					ocds.putInt(Tag.WaveformBitsStored, VR.US, WAVEFORM_BITS_ALLOCATION);					
				cds.addDicomObject(ocds);
			}
				
		owfs.putInt(Tag.WaveformBitsAllocated, VR.US, WAVEFORM_BITS_ALLOCATION);
		owfs.putString(Tag.WaveformSampleInterpretation, VR.CS, "SS");
		owfs.putBytes(Tag.WaveformPaddingValue, VR.OW, new byte[] {(byte) 0x80, 0x00});
		owfs.putBytes(Tag.WaveformData, VR.OW, waveformBytes);
		wfSeq.addDicomObject(owfs);		
		
		
		String dicomOutFilePath = GENERATED_DICOM_PATH + "/"+PATIENT_NAME.replaceAll(" ", "_")+"_"+PATIENT_ID+".dcm";
		log("Writing DICOM to " + dicomOutFilePath);
		
		File dicomFile = new File(dicomOutFilePath);
		dicomFile.deleteOnExit();
		DicomOutputStream dout = new DicomOutputStream(dicomFile);
		dout.setTransferSyntax(TransferSyntax.ExplicitVRLittleEndian);
		dout.writeDicomFile(dcmObj);
		dout.close();
		
		return dicomFile.getAbsolutePath();
	}
	
	private static void uploadDicomToConquestCurl(String url, String dcmFilePath) {
		
		File dcmFile = new File(dcmFilePath);
		
		try 
		{ 		
			// dgate runs with a set of modes. Uploading a DICOM file runs with "addlocalfile" mode.
			// Below: Running a curl command to upload DICOM File with dgate's "addlocalfile" mode. 
			String cmd = "curl -v -F mode=addlocalfile -F filetoupload=@"+dcmFile.getAbsolutePath()+ " " + url;
			Process	p = Runtime.getRuntime().exec(cmd); 			
			int exitValue = p.waitFor(); 
			log("Finished ...");
			log("Exited with: " + exitValue);
			
			Scanner reader = new Scanner(p.getInputStream());
			StringBuilder out = new StringBuilder(); 
			String line = "";
			while (reader.hasNextLine()) 
			{
				line = reader.nextLine();
				if(line == null) 
				{
					break;
				}
				out.append(line + "\n"); 
			}
			
			
			if(out.toString().contains("printer_files")) {
				log("Successfully Uploaded!");
			}
			else {
				System.out.println("Response: " + out.toString());
			}
			
			reader.close();
			
		} 
		catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		
	}

	// TEMPORARY VIEWER TO HELP VALIDATE DICOM CREATION
	private static void viewDicomFile(String dcmFilePath) {
		
		File dcmFile = new File(dcmFilePath);
		
		try { 		
			final String converterPath = "../conquest/dicomecg_convert/";
			String cmd = converterPath + "convertAndDisplay.sh " + dcmFile.getAbsolutePath() + " /tmp/" + dcmFile.getName() + ".png";
			System.out.println("Running: " + cmd);
			Process	p = Runtime.getRuntime().exec(cmd); 			
			
			
			new Thread(()->{
				InputStreamReader isr = new InputStreamReader(p.getInputStream());
				BufferedReader reader = new BufferedReader(isr);
				StringBuilder out = new StringBuilder(); 
				String line = "";
				
				try {
					while (true) 
					{
						line = reader.readLine();
						if(line == null) 
						{
							break;
						}
						out.append(line + "\n"); 
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println(out.toString());
				
			}).start();
			
			new Thread(()->{
				InputStreamReader isr = new InputStreamReader(p.getErrorStream());
				BufferedReader reader = new BufferedReader(isr);
				StringBuilder out = new StringBuilder(); 
				String line = "";
				
				try {
					while (true) 
					{
						line = reader.readLine();
						if(line == null) 
						{
							break;
						}
						out.append(line + "\n"); 
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println(out.toString());
				
			}).start();
			
			p.waitFor(); 
			int exitValue = p.waitFor(); 
			log("Finished ...");
			log("Exited with: " + exitValue);
			
		} 
		catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			System.exit(127);
		}
	}
	
	
}
