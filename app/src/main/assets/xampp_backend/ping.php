<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

echo json_encode([
    "status" => "success",
    "message" => "Server XAMPP (Apache & PHP) berhasil terhubung!",
    "timestamp" => date("Y-m-d H:i:s")
]);
?>
