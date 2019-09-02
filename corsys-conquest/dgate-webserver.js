var cgi = require('cgi');
var http = require('http');
var path = require('path');

var dgate_exe = path.resolve(__dirname, 'dgate');

http.createServer( cgi(dgate_exe) ).listen(4000);
