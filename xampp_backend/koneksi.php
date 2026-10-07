<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

$host = "localhost";
$user = "root";
$pass = "";
$db   = "db_absensi_kantor";

$koneksi = mysqli_connect($host, $user, $pass, $db);

if (!$koneksi) {
    echo json_encode([
        "status" => "error",
        "message" => "Gagal terhubung ke MySQL: " . mysqli_connect_error()
    ]);
    exit();
}
?>
