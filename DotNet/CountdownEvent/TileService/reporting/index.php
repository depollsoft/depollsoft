<?php
$logFile = fopen("../countdownLog.csv", "r");
fgetcsv($logFile);
?>
<html>
<body>
<table>
<tr>
<th>ImageKey</th>
<th>Timestamp</th>
<th>TargetDate</th>
<th>Request IP</th>
</tr>
<?php
while($csv = fgetcsv($logFile))
{
	$timestampString = date_create($csv[1])->format("r");
	$targetDateString = date_create($csv[2])->format("r");
	echo("<tr><td>$csv[0]</td><td>$timestampString</td><td>$targetDateString</td><td>$csv[3]</td></tr>");
}
fclose($logFile);
?>
</table>
</body>
</html>