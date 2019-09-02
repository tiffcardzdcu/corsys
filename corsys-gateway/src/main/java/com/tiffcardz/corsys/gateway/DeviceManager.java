package com.tiffcardz.corsys.gateway;

import java.util.HashMap;
import java.util.Map.Entry;

public class DeviceManager {
	public static HashMap<String, SensorStatus> deviceStatus = new HashMap<String, SensorStatus>();
	public static HashMap<String, String> devicePatient = new HashMap<String, String>();

	public static void setSensorStatus(String deviceId, SensorStatus sensorStatus) {
		deviceStatus.put(deviceId, sensorStatus);
	}
	
	public static SensorStatus getSensorStatus(String deviceId) {
		DicomGateway.log("Devices: " + deviceStatus);
		return deviceStatus.get(deviceId);
	}
	
	public static void setSensorPatient(String deviceId, String patientId) {
		devicePatient.put(deviceId, patientId);
	}
	
	public static String getPatientByDeviceId(String deviceId) {
		return devicePatient.get(deviceId);
	}
	
	public static String getDeviceIdByPatientId(String patientId) {
		for (Entry<String, String> entry : devicePatient.entrySet()) {
            if (entry.getValue().equals(patientId)) {
            	return entry.getKey();
            }
        }
		return null;
	}
}
