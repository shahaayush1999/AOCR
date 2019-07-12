<?php 

include 'db_connect.php';
include 'functions.php';
	
$fullname = $phone = $email = $company = $position = $city = $pincode = $website = $addedbyuserid = "";

//$sql = "INSERT INTO `business_card_data` (`fullname`, `phone`, `email`, `company`, `position`, `city`, `pincode`, `website`, `addedbyuserid`) VALUES ('Sayush Ahah','+919082358941','shahaayush160499@gmail.com','MindSpark','Technical Exhibition Head','Pune','411005','www.mind-spark.org', 1)";


$fullname = trim($_POST["fullname"]);
$phone = trim($_POST["phone"]);
$email = trim($_POST["email"]);
$company = trim($_POST["company"]);
$position = trim($_POST["position"]);
$city = trim($_POST["city"]);
$pincode = trim($_POST["pincode"]);
$website = trim($_POST["website"]);
$addedbyuserid = (int) trim($_POST["addedbyuserid"]);

if(!userExists($phone)) {
	$sql = "INSERT INTO data (fullname, phone, email, company, position, city, pincode, website)
	VALUES ('{$fullname}', '{$phone}', '{$email}', '{$company}', '{$position}', '{$city}', '{$pincode}', '{$website}')";
	if ($conn->query($sql) === TRUE) {
		echo "New record created successfully";
	} else {
		echo "Error: " . $sql . "\n" . $con->error;
	}



}

$invitedbyuserid = $addedbyuserid;
$userid = getUserID($phone);

$sql = "INSERT INTO addedby (userid, invitedbyuserid)
VALUES ('{$userid}', '{$invitedbyuserid}')";
if ($conn->query($sql) === TRUE) {
	echo "New record created successfully";
} else {
	echo "Error: " . $sql . "\n" . $con->error;
}
	
 ?>