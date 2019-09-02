package com.tiffcardz.corsys.gateway;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class DicomGateway {
	private static String CONQUEST_HOST = "35.153.27.6";
	private static int CONQUEST_PORT = 5678;
	private static final String MQTT_BROKER_URL = "localhost";
	
	private static final String MEDIA_STORAGE_SOP_CLASS_UID = "1.2.840.10008.5.1.4.1.1.9.1.1"; //genUUID();
	private static final String MEDIA_STORAGE_SOP_INSTANCE_UID = "1.2.826.0.1.34471.2.44.6.20021122091000..1"; //genUUID();
	
	private static final int NUM_CHANNELS = 2;
	private static final int ECG_BLOCK_SIZE_SECS = 10;
	private static final int SAMPLE_RATE = 360;
	private static final int TOTAL_SAMPLES = SAMPLE_RATE * ECG_BLOCK_SIZE_SECS * NUM_CHANNELS;

	private static final int MAX_STUDY_TIME_MINS = 1;
	private static final int MAX_BLOCKS_TO_GENERATE = (MAX_STUDY_TIME_MINS * 60)/ECG_BLOCK_SIZE_SECS;
	
	private static final int WAVEFORM_BITS_ALLOCATION = 16;
	
	private static final int DEV_MESG_TYPE_ECG_BLOCK = 1;
	private static final int DEV_MESG_TYPE_STUDY_FINISHED = 2;
	
	private static String genUUID() {
		return UUID.randomUUID().toString();
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
	
	protected static void log(String str) {
		System.out.println(DicomGateway.class.getSimpleName() + ": " + str);
	}
	
	private static MqttClient client;
	private static MqttConnectOptions options; 
	
	private static void connectMqtt(MqttConnectOptions options) throws MqttSecurityException, MqttException {
		client.connect(options);
	}
	public static void main(String args[]) throws MqttException {
		String mqttBrokerUrl = "tcp://"+MQTT_BROKER_URL+":1883";
		
		client = new MqttClient(mqttBrokerUrl, MqttClient.generateClientId(), new MqttDefaultFilePersistence("/tmp/paho"));
		client.setCallback( new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				
				if(topic.equals(Topics.TIFFCARDZ_GATEWAY.name())) {
					handleGatewayMessage(message);
				}
				if(topic.contains(Topics.TIFFCARDZ_DEV_.name())) {
					final String patientId = topic.split(Topics.TIFFCARDZ_DEV_.name())[1]; // TIFFCARDS_DEV_DEVICE1
					final byte[] payload = message.getPayload();
					log("Received Message from Device: " + patientId);
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							handleDeviceMessage(patientId, payload);
						}
					}).start();
					
				}
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				
			}
			
			@Override
			public void connectionLost(Throwable cause) {
				log("Connection Lost: " + cause.getMessage());
				try {
					connectMqtt(options);
				} catch (MqttSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		connectMqtt(options);
		client.subscribe(Topics.TIFFCARDZ_GATEWAY.name());
		log("MQTT isConnected: " + client.isConnected());
	}
	
	private static void handleGatewayMessage(MqttMessage message) throws MqttException, InterruptedException {
		String str = new String(message.getPayload());
		log("Message Recd (Topic: " + Topics.TIFFCARDZ_GATEWAY + ": " + str);
		String strs[] = str.split(",");
		log(strs[0]);
		if(strs[0].equals("REGISTER")) {
			log("Registration Message recd");
			String patientId = strs[1];
			//DeviceManager.setSensorStatus(patientId, SensorStatus.IDLE);
			final String sensorTopic = Topics.TIFFCARDZ_DEV_ + patientId;
			log("Subscribing to: " + sensorTopic);
			client.subscribe(sensorTopic);
			/*
			Thread.sleep(5000);
			MqttMessage beginStudyMessage = new MqttMessage("BEGIN_STUDY".getBytes());
			client.publish(sensorTopic, beginStudyMessage);
			*/
		}
	}
	
	private static void handleDeviceMessage(String patientId, final byte[] msgBytes) {
		// receives ecg blocks
		log("\t From " + patientId + " Recd: " + msgBytes.length + " bytes");

		/*
		==========================================================================================
		msg_type[4]=1, size[4], patientId[size], 
		block_sample_size[4], ch1[TOTAL_SAMPLES X 2], ch2[TOTAL_SAMPLES X 2], 
		STUDY_DATE[8], STUDY_TIME[10], STUDY_ID[36], 
		seriesNumber[2], seriesDate[8], seriesTime[10], 
		acqDateTime[14]
		===========================================================================================
		msg_type[2]=2
		===========================================================================================
		*/
		
		ByteBuffer buff = ByteBuffer.wrap(msgBytes);
		int mesgType = buff.getInt();
		
		if(mesgType == DEV_MESG_TYPE_ECG_BLOCK) {
		
			int patientIdLength = buff.getInt();
			byte[] dst = new byte[patientIdLength];
			buff.get(dst, 0, patientIdLength);
			String patientIdFromDev = new String(dst);
			log("patientIdFromDev: " + patientIdFromDev);
		
			int sampleLength = buff.getInt();
			log("Sample Length: " + sampleLength);
			short[] ch1 = new short[sampleLength];
			short[] ch2 = new short[sampleLength];
			for(int i = 0; i < sampleLength; i++)
				ch1[i] = buff.getShort();
			for(int i = 0; i < sampleLength; i++)
				ch2[i] = buff.getShort();
		
			log("ch2.length: " + ch2.length);
			
			dst = new byte[8];
			buff.get(dst, 0, 8);
			String studyDate = new String(dst);
			
			dst = new byte[10];
			buff.get(dst, 0, 10);
			String studyTime = new String(dst);
			
			log("Study Time: " + studyTime);
			
			dst = new byte[36];
			buff.get(dst, 0, 36);
			String studyId = new String(dst);
			log("StudyId: " + studyId);
			
			int seriesNumber = buff.getInt();
			log("SeriesNumber: " + seriesNumber);
			
			dst = new byte[8];
			buff.get(dst, 0, 8);
			String seriesDate = new String(dst);
			log("SeriesDate: " + seriesDate);
			
			dst = new byte[10];
			buff.get(dst, 0, 10);
			String seriesTime = new String(dst);
			log("SeriesTime: " + seriesTime);
			
			log("Rem Bytes: " + buff.remaining());
			
			dst = new byte[14];
			buff.get(dst, 0, 14);
			String acqDateTime = new String(dst);
			
			log("AcqDateTime: " + acqDateTime);
			
			try {
				String dicomFilePathStr = createDicomFile(patientId, ch1, ch2, studyDate, studyTime, studyId, seriesNumber, seriesDate, seriesTime, acqDateTime);
				cstore(dicomFilePathStr);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		} 
		else if (mesgType == DEV_MESG_TYPE_STUDY_FINISHED) {
			log("ECG STUDY FINISHED!");
			try {
				client.unsubscribe(Topics.TIFFCARDZ_DEV_ + patientId);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	
	private static void cstore(String dicomFilePath) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        try {
            CStoreSCU scu = new CStoreSCU("corsys");
            scu.setExecutor(executor);
            scu.setScheduledExecutor(scheduledExecutor);
            scu.store("", CONQUEST_HOST, CONQUEST_PORT, new ArrayList<File>() {{add(new File(dicomFilePath));}});
            System.out.println("Success");
            System.out.println("Finished!");
        } catch (IOException | InterruptedException | GeneralSecurityException | IncompatibleConnectionException e) {
			e.printStackTrace();
		} finally {
            executor.shutdown();
            scheduledExecutor.shutdown();
        }
	}
	
	
	private static String createDicomFile(final String patientId, final short[] ch1ShortArr, final short[] ch2ShortArr, final String studyDate, final String studyTime, final String studyId, final int seriesNumber, final String seriesDate, final String seriesTime, final String acquisitionDateTime) throws IOException {
		
		short[][] chShorts = new short[][] {
				ch1ShortArr, 
				ch2ShortArr,
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
		
		dcmObj.putString(Tag.PatientName, VR.PN, patientId);
		dcmObj.putString(Tag.PatientID, VR.LO, patientId);
		dcmObj.putString(Tag.PatientBirthDate, VR.DA, "19880720");
		dcmObj.putString(Tag.PatientSex, VR.CS, "F");
		dcmObj.putString(Tag.OtherPatientIDs, VR.LO, "");
		dcmObj.putString(Tag.PatientAge, VR.AS, 45 + "");
		dcmObj.putString(Tag.PatientSize, VR.DS, "");
		dcmObj.putString(Tag.PatientWeight, VR.DS, "");
		dcmObj.putString(Tag.CurrentPatientLocation, VR.LO, "");
		dcmObj.putString(Tag.PatientInstitutionResidence, VR.LO, "");
		dcmObj.putString(Tag.VisitComments, VR.LT, "");
		dcmObj.putString(Tag.DeviceSerialNumber, VR.LO, "2");
		dcmObj.putString(Tag.SoftwareVersions, VR.LO, "0.13");
		
		dcmObj.putString(Tag.StudyDate, VR.DA, studyDate);
		dcmObj.putString(Tag.StudyTime, VR.TM, studyTime);
		dcmObj.putString(Tag.ContentDate, VR.DA, seriesDate);
		dcmObj.putString(Tag.ContentTime, VR.TM, seriesTime);
		dcmObj.putString(Tag.StudyInstanceUID, VR.UI, studyId);
		
		dcmObj.putString(Tag.SeriesInstanceUID, VR.UI, studyId);
		dcmObj.putString(Tag.SeriesNumber, VR.IS, "1");
		dcmObj.putString(Tag.SeriesDate, VR.DA, studyDate);
		dcmObj.putString(Tag.SeriesTime, VR.TM, getCurrentDateTime());
		
		dcmObj.putString(Tag.InstanceNumber, VR.IS, seriesNumber + "");
		dcmObj.putString(Tag.AcquisitionDateTime, VR.DT, acquisitionDateTime);
		dcmObj.putString(Tag.InstanceCreationDate, VR.DA, seriesDate);
		dcmObj.putString(Tag.InstanceCreationTime, VR.TM, seriesTime);
		
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
		
		
		String dicomOutFilePath = "/tmp/"+patientId.replaceAll(" ", "_")+"_"+patientId+".dcm";
		log("Writing DICOM to " + dicomOutFilePath);
		
		File dicomFile = new File(dicomOutFilePath);
		dicomFile.deleteOnExit();
		DicomOutputStream dout = new DicomOutputStream(dicomFile);
		dout.setTransferSyntax(TransferSyntax.ExplicitVRLittleEndian);
		dout.writeDicomFile(dcmObj);
		dout.close();
		
		return dicomFile.getAbsolutePath();
	}
	
}
