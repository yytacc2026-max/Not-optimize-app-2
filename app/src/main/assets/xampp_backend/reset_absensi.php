<?php
require_once 'koneksi.php';

$tanggal = date("Y-m-d");
$delete = mysqli_query($koneksi, "DELETE FROM absensi WHERE tanggal = '$tanggal'");

if ($delete) {
    echo json_encode([
        "status" => "success",
        "message" => "Data absensi hari ini berhasil direset ke 0."
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Gagal mereset absensi: " . mysqli_error($koneksi)
    ]);
}
?>
