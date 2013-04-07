<?php

header("Content-type: image/png");
header("Cache-Control: no-cache");
$imageKey = $_GET['ImageKey'];
$targetDateString = $_GET['TargetDate'];
$overrideNum = $_GET['Override'];
$isYearly = $_GET['Yearly'] == 'yes' ? true : false;
$targetDate = new DateTime($targetDateString);
$targetDate->modify("-1 minute");
$now = new DateTime(null, $targetDate->getTimezone());

$nowIsSmaller = $now < $targetDate;

if($isYearly && !$nowIsSmaller)
{
	$newNow = new DateTime($now->format('r'));
	$newNow->modify("-24 hours");
	if($newNow < $targetDate)
	{
		while($now < $targetDate)
		{
			$targetDate->modify("+1 year");
		}
		$nowIsSmaller = false;
	}
}

$smaller = $nowIsSmaller ? $now : $targetDate;
$larger = $nowIsSmaller ? $targetDate : $now;

$smaller = new DateTime($smaller->format('r'));

$diffCount = $nowIsSmaller? 0 : -1;
while($smaller < $larger)
{
	$smaller->modify("+24 hours");
	$diffCount++;
}

if($nowIsSmaller)
	$diffCount = -$diffCount;
	
if($overrideNum != null)
	$diffCount = intval($overrideNum);

$filename = $imageKey.'/'.$diffCount.".png";
$useDefaultImage = !file_exists($filename);
if($useDefaultImage)
	if($nowIsSmaller)
		$filename = $imageKey.'/'."defaultBefore.png";
	else
		$filename = $imageKey.'/'."defaultAfter.png";

$im = imagecreatefrompng($filename);
imagealphablending($im, true);
imagesavealpha($im, true);

if($useDefaultImage && $nowIsSmaller)
{
	$fontName = 'fonts/SegoeWP-Semibold.ttf';
	$fontSize = 30;
	$numDays = abs($diffCount);
	$dayText = ''.$numDays."&#8239;".($numDays==1?'day':'days');
	$bounds = imagettfbbox($fontSize, 0, $fontName, $dayText);
	$imageWidth = imagesx($im);
	$imageHeight = imagesy($im);
	$xPos = $imageWidth / 2 - abs($bounds[4] / 2);
	$yPos = $imageHeight / 2 + abs($fontSize / 2);
	@imagettftext($im, $fontSize, 0, $xPos, $yPos, imagecolorallocate($im, 0xFF, 0xFF, 0xFF), $fontName, $dayText);
	$val = 1;
}

$orange = imagecolorallocate($im, 220, 210, 60);
$px = (imagesx($im) - 7.5 * strlen($string)) / 2;
imagestring($im, 3, $px, 9, $string, $orange);
imagepng($im);
imagedestroy($im);

$first = file_exists("countdownLog.csv");
$logFile = fopen("countdownLog.csv", "a");
flock($logFile, LOCK_EX);
if(!$first)
	fwrite($logFile, "ImageKey,Timestamp,TargetDate,Request IP\n");
$realNow = new DateTime(null, $targetDate->getTimezone());
$toWrite = $imageKey.','.$realNow->format('c').','.$targetDateString.','.$_SERVER['REMOTE_ADDR']."\n";
fwrite($logFile, $toWrite);
flock($logFile, LOCK_UN);
fclose($logFile);

?>