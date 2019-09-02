-- mvh 20151206: fix 1.4.17d compatibility
-- 20160709   mvh   fix for PatientID with spaces in it
-- 20160717   mvh   Added experimental renderseries
-- 20160830   mvh   Removed that again
-- 20170305   mvh   start with send etc functions (1.4.19a)
-- 20170430   mvh   Indicate viewer in menu
-- 20180203   mvh   Removed opacity control for s[1] which does not exist

webscriptaddress = webscriptaddress or webscriptadress or 'dgate.exe'
local ex = string.match(webscriptaddress, 'dgate(.*)')
local query_pid = '';
local query_pna = '';
local query_pst = '';
local query_sta = '';
 
function InitializeVar()
 if (CGI('patientidmatch') ~='') then
     query_pid='*'..CGI('patientidmatch')..'*'
 else
  query_pid='*' 
 end

 if (patientnamematch ~= '') then
     query_pna='*'..CGI('patientnamematch')..'*' ;
 else 
  query_pna='*' 
 end

 if (CGI('studydatematch') ~= '') then
     query_pst='*'..CGI('studydatematch')..'*' ;
 else
   query_pst='*' 
 end

 if (CGI('startdatematch') ~= '') then
     query_sta='*'..CGI('startdatematch')..'*' ;
 else 
  query_sta='*' 
 end
end;
 

function mc(fff)
  return fff or ''
end;

function queryserie()
  local patis, b, s, pid, i,j,k,l, siuid;
 
  InitializeVar()
  s = servercommand('get_param:MyACRNema')

  q=CGI('query')
  --print(q) 
  i, j = string.find(q, "DICOMStudies.patientid = '")

  k, l = string.find(q, "' and")
  pid=(string.sub(q, j+1,k-1))       --> patientid
  
  i, j = string.find(q, "DICOMSeries.studyinsta = '")
  siuid=(string.sub(q, j+1))       --> studyinsta
  siuid = string.gsub(siuid, "'", '')


  b=newdicomobject();
  b.QueryRetrieveLevel='SERIES'
  b.PatientName = ''
  b.PatientID        = pid
  b.StudyDate        = ''; 
  b.StudyInstanceUID = siuid;
  b.StudyDescription = '';
  b.SeriesDescription= '';
  b.SeriesInstanceUID= '';
  b.SeriesDate= '';
  b.Modality         = '';
  b.SeriesTime       = '';

  series=dicomquery(s, 'SERIES', b);

  -- convert returned DDO (userdata) to table; needed to allow table.sort
  seriest={}
  for k1=0,#series-1 do
    seriest[k1+1]={}
	--series[k1].PatientName=querypats(series[k1].PatientID)
    seriest[k1+1].PatientName      = series[k1].PatientName
	
    seriest[k1+1].StudyDate        = series[k1].StudyDate
    seriest[k1+1].PatientID        = series[k1].PatientID
    seriest[k1+1].StudyDate        = series[k1].StudyDate
    seriest[k1+1].SeriesTime       = series[k1].SeriesTime
    seriest[k1+1].SeriesDate       = series[k1].SeriesDate
    seriest[k1+1].StudyInstanceUID = series[k1].StudyInstanceUID
    seriest[k1+1].SeriesDescription= series[k1].SeriesDescription
    seriest[k1+1].StudyDescription = series[k1].StudyDescription
    seriest[k1+1].SeriesInstanceUID= series[k1].SeriesInstanceUID
    seriest[k1+1].Modality         = series[k1].Modality
  end
  return seriest
end


HTML("Content-type: text/html\nCache-Control: no-cache\n");

print(
[[
<html>
<head>
<title>Conquest DICOM server - version]]..version..[[</title>]]..
[[<style type="text/css">
body { font:10pt Verdana; }
a { color:blue; }
#content { background-color:#dddddd; width:200px; margin-top:2px; }
</style>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
]]
)

print([[
<!-- Javascript goes in the document HEAD -->
<script type="text/javascript">
function altRows(id){
	if(document.getElementsByTagName){  
		
		var table = document.getElementById(id);  
		var rows = table.getElementsByTagName("tr"); 
		 
		for(i = 0; i < rows.length; i++){          
			if(i % 2 == 0){
				rows[i].className = "evenrowcolor";
			}else{
				rows[i].className = "oddrowcolor";
			}      
		}
	}
}
window.onload=function(){
	altRows('alternatecolor');
}
</script>
]])

print([[
<script language=JavaScript>
function servicecommand(a) {
  xmlhttp = new XMLHttpRequest(); 
  xmlhttp.open('GET',']]..script_name..[[?mode=listpatients&parameter='+a, true);
  xmlhttp.timeout = 60000
  xmlhttp.send()
  xmlhttp.onreadystatechange = function() {
    if (this.readyState == 4 && this.status == 200) {
      var s = this.responseText.toString()
      document.getElementById("statusarea").innerHTML = s.replace("DICOM ERROR Protocol error 0 in PDU:Read", "");
    }
  }
}  
</script>
]])

print([[
<script language=JavaScript>
function opencommand(a) {
  window.open (']]..script_name..[[?mode=listpatients&parameter='+a);
}  
</script>
]])

print([[
<!-- CSS goes in the document HEAD or added to your external stylesheet -->
<style type="text/css">
table.altrowstable {
	font-family: verdana,arial,sans-serif;
	font-size:16px;
	color:#333333;
	border-width: 1px;
	border-color: #a9c6c9;
	border-collapse: collapse;
}
table.altrowstable th {
	border-width: 1px;
	padding: 8px;
	border-style: solid;
	border-color: #a9c6c9;
}
table.altrowstable td {
	border-width: 1px;
	padding: 8px;
	border-style: solid;
	border-color: #a9c6c9;
}
.oddrowcolor{
	background-color:#d4e3e5;
}
.evenrowcolor{
	background-color:#c3dde0;
}
table.altrowstable tr:first-child {
	font-weight:bold;
}
table.altrowstable Caption {
    font-weight:bold;
    color: yellow;
    background: green;
}
</style>
</head> 
<body BGCOLOR='CFDFCF'>
]]
)

viewer=gpps('webdefaults', 'viewer', '');

function dropdown(i, item)
  return string.format([[
<td>
<p id='aap%d' onmouseover="var s=document.getElementById('aap%d').children; s[0].style.opacity=1;" onmouseout="var s=document.getElementById('aap%d').children; s[0].style.opacity=0.1;">
<select name=selectaction style="opacity:0.1; width:40" onchange="servicecommand(document.getElementById('aap%d').children[0].value+ '&item='+'%s');document.getElementById('aap%d').children[0].selectedIndex=0" >
<option value=nop>-</option>')
<option value=sender>Send</option>')
<option value=changerid>Change Patient ID</option>')
<option value=anonymizer>Anonymize</option>')
<option value=deleter>Delete</option>')
<option value=zipper>Zip</option>')
<option value=zipperanonymized>Zip anonymized</option>')
<option value=viewerseries>View with ]]..viewer..[[</option>')
<option value=nop>Cancel</option>')
</select>
</p>
]], i, i, i, i, item, i)
end

HTML("<H1>Welcome to the Conquest DICOM server - version %s</H1>", version)

local pats=queryserie() 

print("<table class='altrowstable' id='alternatecolor' RULES=ALL BORDER=1>");

HTML("<Caption>List of series (%s) on local server</caption>", #pats);
HTML("<TR><TD>Patient ID<TD>Name<TD>Series date<TD>Series time<TD>Series description<TD>Modality<TD>Thumbs<TD>Menu</TR>");
	
for i=1,#pats do
  t = string.format("<A HREF=dgate%s?%s&mode=listimages&key=%s&query=DICOMStudies.patientid+=+'%s'+and+DICOMSeries.seriesinst+=+'%s' title='Click to see images'>%s</A>", ex, extra, tostring(key or ''),string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].SeriesInstanceUID),mc(pats[i].PatientID));
  v = string.format("<A HREF=dgate%s?%s&mode=listimageswiththumbs&query=DICOMStudies.patientid+=+'%s'+and+DICOMSeries.seriesinst+=+'%s'&size=%s>Thumbs</A>", 
       ex, tostring(extra or ''), string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].SeriesInstanceUID),size); 
  --if (viewer ~= '') then
  -- u = string.format("<A HREF=dgate%s?%s&mode=%s&series=%s:%s&size=%s>View series</A>", ex, tostring(extra or ''), tostring(viewer or ''),string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].SeriesInstanceUID),size); 
  --else
  -- u='no viewer'
  --end

--  r = string.format("<A HREF=dgate%s?%s&mode=renderseries&series=%s:%s&size=%s>Render series</A>", ex, tostring(extra or ''), string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].SeriesInstanceUID),size); 

  s = string.format("<TR><TD>%s<TD>%s<TD>%s<TD>%s<TD>%s<TD>%s<TD>%s%s</TR>",t,mc(pats[i].PatientName),
    mc(pats[i].SeriesDate),mc(pats[i].SeriesTime), mc(pats[i].SeriesDescription),mc(pats[i].Modality),v,
    dropdown(i, string.gsub(pats[i].PatientID, ' ', '+')..'::'..pats[i].SeriesInstanceUID));
  print(s)
end 

print
[[
</table>
<p id=statusarea>
<i>Extra information to be shown here</i>
</p>
</body>
</html>
]]
