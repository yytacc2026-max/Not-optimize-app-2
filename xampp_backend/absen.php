<?php
require_once 'koneksi.php';

$id_karyawan = isset($_POST['id_karyawan']) ? $_POST['id_karyawan'] : '';
$nip         = isset($_POST['nip']) ? $_POST['nip'] : '';
$nama        = isset($_POST['nama']) ? $_POST['nama'] : '';
$departemen  = isset($_POST['departemen']) ? $_POST['departemen'] : '';
$tipe        = isset($_POST['tipe']) ? strtoupper($_POST['tipe']) : 'MASUK';
$jam         = isset($_POST['jam']) ? $_POST['jam'] : date("H:i");
$status      = isset($_POST['status']) ? $_POST['status'] : 'TEPAT_WAKTU';
$tanggal     = date("Y-m-d");

if (empty($id_karyawan) && empty($nip)) {
    echo json_encode([
        "status" => "error",
        "message" => "Data karyawan tidak valid"
    ]);
    exit();
}

// Cek apakah sudah ada catatan untuk hari ini berdasarkan id_karyawan
$cek = mysqli_query($koneksi, "SELECT id FROM absensi WHERE id_karyawan = '$id_karyawan' AND tanggal = '$tanggal' LIMIT 1");

if ($cek && mysqli_num_rows($cek) > 0) {
    if ($tipe == 'PULANG') {
        $update = mysqli_query($koneksi, "UPDATE absensi SET jam_pulang = '$jam' WHERE id_karyawan = '$id_karyawan' AND tanggal = '$tanggal'");
        echo json_encode([
            "status" => "success",
            "message" => "Absen Pulang berhasil dicatat pada $jam WIB"
        ]);
    } else {
        echo json_encode([
            "status" => "success",
            "message" => "Absen Masuk sudah tercatat hari ini"
        ]);
    }
} else {
    // Insert baru
    $jam_masuk = ($tipe == 'MASUK') ? $jam : '-';
    $jam_pulang = ($tipe == 'PULANG') ? $jam : '-';

    $insert = mysqli_query($koneksi, "INSERT INTO absensi (id_karyawan, nip, nama, departemen, tanggal, jam_masuk, jam_pulang, status) 
        VALUES ('$id_karyawan', '$nip', '$nama', '$departemen', '$tanggal', '$jam_masuk', '$jam_pulang', '$status')");

    if ($insert) {
        echo json_encode([
            "status" => "success",
            "message" => "Absen Masuk berhasil dicatat pada $jam WIB ($status)"
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Gagal menyimpan absensi: " . mysqli_error($koneksi)
        ]);
    }
}
?>
