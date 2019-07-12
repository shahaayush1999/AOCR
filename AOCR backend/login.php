<?php
$response = array();
include 'db_connect.php';
include 'functions.php';

//Get the input request parameters
$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, TRUE); //convert JSON into array


/////////////////////////////////////////////////////////////////////////////////////////////////
//status 0 = "Login Successful" INVITED REGISTERED
//status 1 = "Invalid Password" INVITED REGISTERED
//status 2 = "Number invalid or not registered" UNINVITED UNREGISTERED NUMBER
//status 3 = "Missing mandatory parameters"
//status 4 = "Please register first" INVITED UNREGISTERED NUMBER
/////////////////////////////////////////////////////////////////////////////////////////////////

//Check for Mandatory parameters
if(isset($input['phone']) && isset($input['password'])){
	$phone = $input['phone'];
	$password = $input['password'];
	if(userExists($phone)) {
		if(isUserRegistered($phone)) {
			$query    = "SELECT fullname, passwordhash, salt, userid FROM data WHERE phone = ?";
			if($stmt = $con->prepare($query)){
				$stmt->bind_param("s",$phone);
				$stmt->execute();
				$stmt->bind_result($fullName,$passwordHashDB,$salt,$userid);
				if($stmt->fetch()){
					//Validate the password
					if(password_verify(concatPasswordWithSalt($password,$salt),$passwordHashDB)){
						$response["status"] = 0;
						$response["message"] = "Login successful";
						$response["fullname"] = $fullName;
						$response['userid'] = $userid;
					} else{
						$response["status"] = 1;
						$response["message"] = "Invalid password";
					}
				} else {
					$response["status"] = 2;
					$response["message"] = "Number invalid or not registered";
				}

				$stmt->close();
			}
		} else {
			$response["status"] = 4;
			$response["message"] = "Please register first";
		}
	} else {
		$response["status"] = 2;
		$response["message"] = "Number invalid or not registered";
	}
} else {
	$response["status"] = 3;
	$response["message"] = "Missing mandatory parameters";
}
//Display the JSON response
echo json_encode($response);
?>