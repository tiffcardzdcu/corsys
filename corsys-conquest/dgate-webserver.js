var cgi = require('cgi');
var http = require('http');
var path = require('path');

const PORT = 4000;

//var dgate_exe_path = path.resolve(__dirname, 'dgate');
var dgate_exe_path = path.resolve(__dirname, 'dgate');

http.createServer( cgi(dgate_exe_path) ).listen(PORT);

/*
Runs DGATE in CGI mode using NodeJS with CGI library
Most ideas obtained from the Apache cgi-bin configuration variant described in linuxmanual.pdf
This makes it portable.
*/