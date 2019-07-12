<?php
define('DB_SERVER', 'localhost');
define('DB_USERNAME', 'root');
define('DB_PASSWORD', '');
define('DB_NAME', 'AOCR');

$con = new mysqli(DB_SERVER, DB_USERNAME, DB_PASSWORD, DB_NAME) or die("Connect failed: %s\n". $conn -> error);

?>