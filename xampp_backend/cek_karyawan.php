<?php
require_once 'koneksi.php';

$nip = isset($_GET['nip']) ? trim($_GET['nip']) : '';

if (empty($nip)) {
    echo json_encode([
        "status" => "error",
        "message" => "NIP tidak boleh kosong"
    ]);
    exit();
}

$query = "SELECT id_karyawan, nip, nama, departemen FROM karyawan WHERE nip = '$nip' LIMIT 1";
$result = mysqli_query($koneksi, $query);

if ($result && mysqli_num_rows($result) > 0) {
    $row = mysqli_fetch_assoc($result);
    echo json_encode([
        "status" => "success",
        "message" => "Data karyawan ditemukan",
        "data" => [
            "id" => $row['id_karyawan'],
            "nip" => $row['nip'],
            "nama" => $row['nama'],
            "departemen" => $row['departemen']
        ]
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "NIP '$nip' tidak ditemukan di database kantor"
    ]);
}
?>
