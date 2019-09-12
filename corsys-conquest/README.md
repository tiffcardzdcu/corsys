# Conquest DICOM PACS Server (Modified for Corsys)
This is a modification of the Conquest DICOM PACS Server to run on NodeJS with CGI.

## Preparing Conquest for Portable Production
Conquest source was taken from https://github.com/marcelvanherk/Conquest-DICOM-Server
With instructions from linuxmanual.pdf, modifications were made to maklinux script.
Create a staging folder (for eg: /tmp/dgate-out) and this path should be set as CGI_BIN_PATH in maklinux. Run maklinux as follows:
```
$ chmod +x maklinux
$ ./maklinux
```
This will compile dgate and place all essential files into path set by $CGI_BIN_PATH.
All contents of this file were then placed into the root folder of this project.

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
Open the service in your favorite browser at http://<SERVER-HOST-OR-IP>:4000/

## Resetting the Conquest database
Modify the ```rm``` line in the file RESET_DATABASE.sh to include Patient IDs that have been used.
Then run the script ...
```
$ ./RESET_DATABASE.sh
```
