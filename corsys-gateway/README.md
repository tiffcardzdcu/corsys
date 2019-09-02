# Corsys Gateway
This gateway assumes the MQTT Broker is on localhost.
The Conquest host is hardcoded into the class DicomGateway.java

## Compiling and creating the fat JAR
```
$ mvn clean compile assembly:single
```
This will create executable fat JAR in the target directory of the project.

## Run the JAR
```
$ java -jar <jar-file-path> 
