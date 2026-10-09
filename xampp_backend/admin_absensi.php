<?php
require_once 'koneksi.php';

$tanggal = date("Y-m-d");

// 1. Ambil jumlah karyawan master resmi
$count_query = mysqli_query($koneksi, "SELECT COUNT(*) as total_master FROM karyawan");
$total_master = 5;
if ($count_query && $row = mysqli_fetch_assoc($count_query)) {
    $total_master = (int)$row['total_master'];
}

// 2. Ambil konfigurasi kantor (jam kerja, toleransi, pengumuman, total target)
$config = [
    "jam_masuk" => "08:00",
    "batas_terlambat" => "08:00",
    "jam_pulang" => "17:00",
    "pengumuman" => "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.",
    "total_karyawan" => $total_master
];

$res_cfg = mysqli_query($koneksi, "SELECT * FROM pengaturan_kantor WHERE id = 1 LIMIT 1");
if ($res_cfg && $row_cfg = mysqli_fetch_assoc($res_cfg)) {
    $target_karyawan = (int)$row_cfg['total_karyawan'];
    $config["jam_masuk"] = $row_cfg['jam_masuk'];
    $config["batas_terlambat"] = $row_cfg['batas_terlambat'];
    $config["jam_pulang"] = $row_cfg['jam_pulang'];
    $config["pengumuman"] = $row_cfg['pengumuman'];
    $config["total_karyawan"] = ($target_karyawan > 0) ? $target_karyawan : $total_master;
}

// 3. Ambil data absensi hari ini
$query = "SELECT id_karyawan, nama, departemen, jam_masuk, jam_pulang, status FROM absensi WHERE tanggal = '$tanggal' ORDER BY id DESC";
$result = mysqli_query($koneksi, $query);

$data = [];
$hadir_count = 0;
$terlambat_count = 0;

if ($result) {
    while ($row = mysqli_fetch_assoc($result)) {
        $st = $row['status'];
        if ($st === 'TEPAT_WAKTU' || $st === 'TERLAMBAT') {
            $hadir_count++;
        }
        if ($st === 'TERLAMBAT') {
            $terlambat_count++;
        }

        $data[] = [
            "id_karyawan" => $row['id_karyawan'],
            "nama" => $row['nama'],
            "departemen" => $row['departemen'],
            "jam_masuk" => $row['jam_masuk'],
            "jam_pulang" => $row['jam_pulang'],
            "status" => $st
        ];
    }
}

// Total karyawan akurat yang ditampilkan
$total_karyawan_tampil = $config["total_karyawan"];
if ($total_karyawan_tampil <= 0) {
    $total_karyawan_tampil = max($total_master, count($data));
}

$total_belum_absen = max(0, $total_karyawan_tampil - $hadir_count);

echo json_encode([
    "status" => "success",
    "total" => count($data),
    "total_karyawan" => $total_karyawan_tampil,
    "total_karyawan_master" => $total_master,
    "total_hadir" => $hadir_count,
    "total_terlambat" => $terlambat_count,
    "total_belum_absen" => $total_belum_absen,
    "config" => $config,
    "data" => $data
]);
?>
