#!/bin/bash
set -x
i=41;
for i in {41..190}
do
   STUDY_TIME_MINS=1
   MQTT_BROKER_HOST="192.168.0.203"
   PATIENT_ID="TiffanyCardozo$i"
   REALTIME="true"

   java -jar target/corsys-sensor-0.0.1-SNAPSHOT-jar-with-dependencies.jar $REALTIME $STUDY_TIME_MINS $MQTT_BROKER_HOST $PATIENT_ID > sensor_$PATIENT_ID.log &
   echo $PATIENT_ID
   i=$((i + 1))
   sleep 0.1 #100 millis
done

set +x
