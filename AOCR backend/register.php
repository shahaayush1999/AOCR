<?php
$response = array();
include 'db_connect.php';
include 'functions.php';


/////////////////////////////////////////////////////////////////////////////////////////////////
//status 0 = "Uninvited user profile created" = UNINVITED UNREGISTERED, details are updated
//status 1 = "User Exists, please log in" =  INVITED REGISTERED thus no action is taken thus needs to login
//status 2 = "Missing mandatory parameters"
//status 3 = "Invited user profile created" = INVITED UNREGISTERED, details are updated
/////////////////////////////////////////////////////////////////////////////////////////////////


//Get the input request parameters
$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, TRUE); //convert JSON into array

//Check for Mandatory parameters
if(isset($input['fullname']) && isset($input['phone']) && isset($input['company']) && isset($input['position']) && isset($input['city']) && isset($input['password'])) {

	$fullname = $input['fullname'];
	$phone = $input['phone'];
	$email = $input['email'];
	$company = $input['company'];
	$position = $input['position'];
	$city = $input['city'];
	$pincode = $input['pincode'];
	$website = $input['website'];
	$password = $input['password'];
	$registered = 1;

	if(userExists($phone)) {
		if(isUserRegistered($phone)){
			$status = 1;
		} else {
			$status = 3;
		}
	} else {
		$status = 0;
	}


	switch ($status) {
		case 0: //User does not exist, therefore unregistered
			$salt = getSalt();
			//Generate a unique password Hash
			$passwordhash = password_hash(concatPasswordWithSalt($password,$salt),PASSWORD_DEFAULT);
			//Query to register new user
			$insertQuery  = "INSERT INTO data (fullname, phone, email, company, position, city, pincode, website, passwordhash, salt, registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			global $con;
			if($stmt = $con->prepare($insertQuery)){
				$stmt->bind_param("sssssssssss", $fullname, $phone, $email, $company, $position, $city, $pincode, $website, $passwordhash, $salt, $registered);
				$stmt->execute();

				//response key value array
				$response["status"] = 0;
				$response["message"] = "User created";
				$response["fullname"] = $fullname;
				$response["userid"] = getUserID($phone);
				$stmt->close();
			} else {
				$response["status"] = 2;
				$response["message"] = "Missing mandatory parameters";
				$response["userid"] = 0;
			}
			break;
		case 1: //User exists on db and is registered
			$response["status"] = 1;
			$response["message"] = "User exists";
			$response["userid"] = getUserID($phone);
			break;

		case 3: //User exists on db and is unregistered on app

			$salt = getSalt();
			//Generate a unique password Hash
			$passwordhash = password_hash(concatPasswordWithSalt($password,$salt),PASSWORD_DEFAULT);
			//Query to register new user
			$insertQuery  = "UPDATE data SET fullname = ?, email = ?, company = ?, position = ?, city = ?, pincode = ?, website = ?, passwordhash = ?, salt = ?, registered = ? WHERE phone = ?";
			global $con;
			if($stmt = $con->prepare($insertQuery)){
				$stmt->bind_param("sssssssssss", $fullname,  $email, $company, $position, $city, $pincode, $website, $passwordhash, $salt, $registered, $phone);
				$stmt->execute();

				//response key value array
				$response["status"] = 0;
				$response["message"] = "User created";
				$response["fullname"] = $fullname;
				$response["userid"] = getUserID($phone);
				$stmt->close();
			} else {
				$response["status"] = 2;
				$response["message"] = "Missing mandatory parameters";
				$response["userid"] = 0;
			}
			break;
			break;
		default:
			$response["status"] = 2;
			$response["message"] = "Missing mandatory parameters";
			$response["userid"] = 0;
			break;
	}

} else {
	$response["status"] = 2;
	$response["message"] = "Missing mandatory parameters";
	$response["userid"] = 0;
}

//send key value array
echo json_encode($response);

?>