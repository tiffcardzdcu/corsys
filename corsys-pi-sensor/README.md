# Corsys ECG Sensor Simulator
Simulates an MIT ECG file to loop. This project assumes you have maven installed.


## Compiling and creating the fat JAR
```
$ mvn clean compile assembly:single
```
This will create executable fat JAR in the target directory of the project.


## Run the JAR
```
$ java -jar <jar-file-path> <realtime? true|false> <STUDY_TIME_IN_MINS> <MQTT_BROKER_HOST> <PATIENT_ID>
```
