# Conquest DICOM PACS Server (Corsys-mod)
This is a modification of the Conquest DICOM PACS Server to run on NodeJS with CGI.

## Running the Conquest DGATE TCP Server
To run dgate, run the shell script
```
$ ./RUN_DGATE_MAIN.sh
```

## Running the CGI Server
Make sure you have NodeJS installed on your machine. The same folder has a NodeJS project with package.json in the same folder. After cloning the project, **in another terminal** run 
```
$ npm install
```
The Conquest CGI Mode can now be run on Node JS by simple running ...
```
$ ./RUN_CGI_DGATE_NODEJS.sh
```
This runs dgate in CGI Mode for the Conquest Web Service. The default port is 4000.
Open the service in your favorite browser at http://localhost:4000/

## Resetting the Conquest database
Modify the ```rm``` line the file RESET_DATABASE.sh to include Patient IDs that have been used.
Then run the script ...
```
$ ./RESET_DATABASE.sh
```
