<?php
require_once 'koneksi.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'POST') {
    // Admin mengubah status pengajuan izin (Disetujui / Ditolak)
    $id_izin = isset($_POST['id']) ? (int)$_POST['id'] : 0;
    $status  = isset($_POST['status']) ? mysqli_real_escape_string($koneksi, trim($_POST['status'])) : 'Disetujui';

    if ($id_izin <= 0) {
        echo json_encode(["status" => "error", "message" => "ID Izin tidak valid"]);
        exit();
    }

    $update = mysqli_query($koneksi, "UPDATE pengajuan_izin SET status_persetujuan = '$status' WHERE id = $id_izin");
    if ($update) {
        echo json_encode([
            "status" => "success",
            "message" => "Pengajuan izin berhasil diubah menjadi: $status"
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "Gagal update status izin: " . mysqli_error($koneksi)]);
    }
} else {
    // GET: Daftar pengajuan izin seluruh karyawan
    $res = mysqli_query($koneksi, "SELECT id, id_karyawan, nama, jenis_izin, alasan, tanggal_diajukan, status_persetujuan 
        FROM pengajuan_izin ORDER BY id DESC");
    $list = [];
    if ($res) {
        while ($row = mysqli_fetch_assoc($res)) {
            $list[] = [
                "id" => (int)$row['id'],
                "id_karyawan" => $row['id_karyawan'],
                "nama" => $row['nama'],
                "jenis_izin" => $row['jenis_izin'],
                "alasan" => $row['alasan'],
                "tanggal_diajukan" => $row['tanggal_diajukan'],
                "status_persetujuan" => $row['status_persetujuan']
            ];
        }
    }
    echo json_encode([
        "status" => "success",
        "total" => count($list),
        "data" => $list
    ]);
}
?>
