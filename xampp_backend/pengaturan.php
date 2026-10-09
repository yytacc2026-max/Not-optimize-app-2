<?php
require_once 'koneksi.php';

// Endpoint untuk sinkronisasi jam kerja, pengumuman, dan total kuota karyawan antar semua device
$method = $_SERVER['REQUEST_METHOD'];

// Ambil jumlah master karyawan resmi di database
$count_query = mysqli_query($koneksi, "SELECT COUNT(*) as total_master FROM karyawan");
$total_master = 5;
if ($count_query && $row = mysqli_fetch_assoc($count_query)) {
    $total_master = (int)$row['total_master'];
}

if ($method === 'POST') {
    // Admin mengubah pengaturan dashboard untuk semua device
    $jam_masuk       = isset($_POST['jam_masuk']) ? mysqli_real_escape_string($koneksi, trim($_POST['jam_masuk'])) : '08:00';
    $batas_terlambat = isset($_POST['batas_terlambat']) ? mysqli_real_escape_string($koneksi, trim($_POST['batas_terlambat'])) : '08:00';
    $jam_pulang      = isset($_POST['jam_pulang']) ? mysqli_real_escape_string($koneksi, trim($_POST['jam_pulang'])) : '17:00';
    $pengumuman      = isset($_POST['pengumuman']) ? mysqli_real_escape_string($koneksi, trim($_POST['pengumuman'])) : 'Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.';
    $total_karyawan  = isset($_POST['total_karyawan']) ? (int)$_POST['total_karyawan'] : $total_master;

    // Pastikan tabel ada
    mysqli_query($koneksi, "CREATE TABLE IF NOT EXISTS pengaturan_kantor (
        id INT PRIMARY KEY DEFAULT 1,
        jam_masuk VARCHAR(10) NOT NULL DEFAULT '08:00',
        batas_terlambat VARCHAR(10) NOT NULL DEFAULT '08:00',
        jam_pulang VARCHAR(10) NOT NULL DEFAULT '17:00',
        pengumuman TEXT NOT NULL,
        total_karyawan INT NOT NULL DEFAULT 5,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");

    $check = mysqli_query($koneksi, "SELECT id FROM pengaturan_kantor WHERE id = 1");
    if ($check && mysqli_num_rows($check) > 0) {
        $update = mysqli_query($koneksi, "UPDATE pengaturan_kantor SET 
            jam_masuk = '$jam_masuk',
            batas_terlambat = '$batas_terlambat',
            jam_pulang = '$jam_pulang',
            pengumuman = '$pengumuman',
            total_karyawan = $total_karyawan
            WHERE id = 1");
    } else {
        $update = mysqli_query($koneksi, "INSERT INTO pengaturan_kantor (id, jam_masuk, batas_terlambat, jam_pulang, pengumuman, total_karyawan)
            VALUES (1, '$jam_masuk', '$batas_terlambat', '$jam_pulang', '$pengumuman', $total_karyawan)");
    }

    if ($update) {
        echo json_encode([
            "status" => "success",
            "message" => "Pengaturan berhasil diperbarui di server dan berlaku untuk seluruh perangkat.",
            "data" => [
                "jam_masuk" => $jam_masuk,
                "batas_terlambat" => $batas_terlambat,
                "jam_pulang" => $jam_pulang,
                "pengumuman" => $pengumuman,
                "total_karyawan" => $total_karyawan,
                "total_karyawan_master" => $total_master
            ]
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Gagal memperbarui pengaturan: " . mysqli_error($koneksi)
        ]);
    }
} else {
    // GET: Ambil pengaturan kantor terkini untuk sinkronisasi perangkat (karyawan & admin)
    $res = mysqli_query($koneksi, "SELECT * FROM pengaturan_kantor WHERE id = 1 LIMIT 1");
    if ($res && $row = mysqli_fetch_assoc($res)) {
        $total_karyawan = (int)$row['total_karyawan'];
        if ($total_karyawan <= 0) {
            $total_karyawan = $total_master;
        }
        echo json_encode([
            "status" => "success",
            "data" => [
                "jam_masuk" => $row['jam_masuk'],
                "batas_terlambat" => $row['batas_terlambat'],
                "jam_pulang" => $row['jam_pulang'],
                "pengumuman" => $row['pengumuman'],
                "total_karyawan" => $total_karyawan,
                "total_karyawan_master" => $total_master
            ]
        ]);
    } else {
        // Default jika tabel belum terisi
        echo json_encode([
            "status" => "success",
            "data" => [
                "jam_masuk" => "08:00",
                "batas_terlambat" => "08:00",
                "jam_pulang" => "17:00",
                "pengumuman" => "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.",
                "total_karyawan" => $total_master,
                "total_karyawan_master" => $total_master
            ]
        ]);
    }
}
?>
