<?php
require_once 'koneksi.php';

$id_karyawan = isset($_POST['id_karyawan']) ? $_POST['id_karyawan'] : '';
$nama        = isset($_POST['nama']) ? $_POST['nama'] : '';
$jenis       = isset($_POST['jenis']) ? $_POST['jenis'] : 'Sakit';
$alasan      = isset($_POST['alasan']) ? $_POST['alasan'] : '';
$tanggal     = date("Y-m-d H:i:s");

if (empty($id_karyawan) || empty($alasan)) {
    echo json_encode([
        "status" => "error",
        "message" => "Alasan izin tidak boleh kosong"
    ]);
    exit();
}

$query = "INSERT INTO pengajuan_izin (id_karyawan, nama, jenis_izin, alasan, tanggal_diajukan, status_persetujuan) 
          VALUES ('$id_karyawan', '$nama', '$jenis', '$alasan', '$tanggal', 'Menunggu Verifikasi')";

if (mysqli_query($koneksi, $query)) {
    echo json_encode([
        "status" => "success",
        "message" => "Pengajuan izin berhasil dikirim ke server kantor."
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Gagal mengirim izin: " . mysqli_error($koneksi)
    ]);
}
?>
