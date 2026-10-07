<?php
require_once 'koneksi.php';

$tanggal = date("Y-m-d");

$query = "SELECT id_karyawan, nama, departemen, jam_masuk, jam_pulang, status FROM absensi WHERE tanggal = '$tanggal' ORDER BY id DESC";
$result = mysqli_query($koneksi, $query);

$data = [];
while ($row = mysqli_fetch_assoc($result)) {
    $data[] = [
        "id_karyawan" => $row['id_karyawan'],
        "nama" => $row['nama'],
        "departemen" => $row['departemen'],
        "jam_masuk" => $row['jam_masuk'],
        "jam_pulang" => $row['jam_pulang'],
        "status" => $row['status']
    ];
}

echo json_encode([
    "status" => "success",
    "total" => count($data),
    "data" => $data
]);
?>
