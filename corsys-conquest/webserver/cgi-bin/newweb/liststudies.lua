-- mvh 20151206: fix 1.4.17d compatibility
-- 20160709   mvh   fix for PatientID with spaces in it
-- 20170305   mvh   start with send etc functions (1.4.19a)
-- 20170430   mvh   Indicate viewer in menu
-- 20180203   mvh   Removed opacity control for s[1] which does not exist

webscriptaddress = webscriptaddress or webscriptadress or 'dgate.exe'
local ex = string.match(webscriptaddress, 'dgate(.*)')
local query_pid = '';
local query_pna = '';
local query_pst = '';
local query_sta = '';

function mycomp(a,b)
    if a.PatientName == nil and b.PatientName == nil then
      return false
    end
    if a.PatientName == nil then
      return true
    end
    if b.PatientName == nil then
      return false
    end
    return a.PatientName < b.PatientName
  end
  
function InitializeVar()
 if (CGI('patientidmatch') ~='') then
     query_pid='*'..CGI('patientidmatch')..'*'
 else
    q=CGI('query')
	i, j = string.find(q, "patientid = '")
    if j~=nil then
	 s=string.sub(q, j+1)
	 k, l = string.find(s, "'")
     pid=(string.sub(s, 1,l-1))       --> patientid
     if pid == nil or pid == '' then
	 query_pid=''
     else query_pid=pid
     end
   else	 query_pid=''
   end
 end
  
 if (patientnamematch ~= '') then
     query_pna='*'..CGI('patientnamematch')..'*' ;
 else 
  query_pna='' 
 end

 if (CGI('studydatematch') ~= '') then
   if string.len(CGI('studydatematch'))<8 then
     query_pst='*'..CGI('studydatematch')..'*'
   else
     query_pst=CGI('studydatematch')
   end
 else
   query_pst='' 
 end
end;
 
function mc(fff)
 return fff or ''
end;


function querystudy()
  local patis, b, s;
  
  InitializeVar()
  
  s = servercommand('get_param:MyACRNema')
  
  b=newdicomobject();
  b.QueryRetrieveLevel='STUDY'
  b['0008,0061']=''; --Modality
  b.StudyInstanceUID='' 
  b.PatientID=query_pid
  b.PatientName=query_pna
  b.StudyDate = query_pst
  b.StudyDescription=''

  patis=dicomquery(s, 'STUDY', b);
  
  patist={}
  for k1=0,#patis-1 do
	patist[k1+1]={}
	patist[k1+1].StudyInstanceUID = patis[k1].StudyInstanceUID
	patist[k1+1].StudyDate = patis[k1].StudyDate
	patist[k1+1].StudyTime = patis[k1].StudyTime
	patist[k1+1].StudyID = patis[k1].StudyID
	patist[k1+1].StudyDescription = patis[k1].StudyDescription
	patist[k1+1].AccessionNumber = patis[k1].AccessionNumber
	patist[k1+1].ReferPhysician = patis[k1].ReferPhysician
	patist[k1+1].PatientsAge = patis[k1].PatientsAge
	patist[k1+1].PatientsWeight = patis[k1].PatientsWeight
	patist[k1+1].StudyModality = patis[k1]['0008,0061'] 
	patist[k1+1].PatientName = patis[k1].PatientName
	patist[k1+1].PatientBirthDate = patis[k1].PatientBirthDate
	patist[k1+1].PatientSex = patis[k1].PatientSex
	patist[k1+1].PatientID = patis[k1].PatientID
  end
  return patist
end;


HTML("Content-type: text/html\nCache-Control: no-cache\n");

print(
[[
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<title>Conquest DICOM server - version]]..version..[[</title>]]..
[[<style type="text/css">
body { font:10pt Verdana; }
a { color:blue; }
#content { background-color:#dddddd; width:200px; margin-top:2px; }
</style>
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

local studyviewer=gpps('webdefaults', 'studyviewer', '');

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
<option value=viewerstudy>View with ]]..studyviewer..[[</option>')
<option value=nop>Cancel</option>')
</select>
</p>
]], i, i, i, i, item, i)
end

HTML("<H1>Welcome to the Conquest DICOM server - version %s</H1>", version)
local pats=querystudy() 
if query_pna ~= '' then
  table.sort(pats, mycomp)
end


print("<table class='altrowstable' id='alternatecolor' RULES=ALL BORDER=1>");

if (query ~= '') then   
	HTML("<Caption>List of selected studies (%s) on local server</caption>", #pats)
else   
	HTML("<Caption>List of all studies (%s) on local server</caption>", #pats)
end

print("<TR><TD>Patient ID<TD>Name<TD>Study Date<TD>Study description<TD>Study modality<TD>Menu</TR>"); 

for i=1,#pats do
  
  t = string.format("<A HREF=dgate%s?%s&mode=listseries&key=%s&query=DICOMStudies.patientid+=+'%s'+and+DICOMSeries.studyinsta+=+'%s' title='Click to see series'>%s</A>", ex, extra, tostring(key or ''),string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].StudyInstanceUID),mc(pats[i].PatientID));
  
  --if (studyviewer ~= '') then
  -- u = string.format("<A HREF=dgate%s?%s&mode=%s&study=%s:%s&size=%s>View study</A>", ex, tostring(extra or ''), tostring(studyviewer or ''),string.gsub(pats[i].PatientID, ' ', '+'),mc(pats[i].StudyInstanceUID),size); 
  --else
  --  u = 'No studyviewer'
  --end
  s = string.format("<TR><TD>%s<TD>%s<TD>%s<TD>%s<TD>%s%s</TR>",t,mc(pats[i].PatientName),mc(pats[i].StudyDate),
    mc(pats[i].StudyDescription),mc(pats[i].StudyModality),
    dropdown(i, string.gsub(pats[i].PatientID, ' ', '+')..':'..pats[i].StudyInstanceUID));
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
