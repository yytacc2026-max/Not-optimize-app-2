<?php
require_once 'koneksi.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'POST') {
    $aksi = isset($_POST['aksi']) ? $_POST['aksi'] : 'tambah';

    if ($aksi === 'tambah') {
        $nip        = isset($_POST['nip']) ? mysqli_real_escape_string($koneksi, trim($_POST['nip'])) : '';
        $nama       = isset($_POST['nama']) ? mysqli_real_escape_string($koneksi, trim($_POST['nama'])) : '';
        $departemen = isset($_POST['departemen']) ? mysqli_real_escape_string($koneksi, trim($_POST['departemen'])) : 'Umum';

        if (empty($nip) || empty($nama)) {
            echo json_encode(["status" => "error", "message" => "NIP dan Nama tidak boleh kosong"]);
            exit();
        }

        // Cek duplikasi NIP
        $cek = mysqli_query($koneksi, "SELECT id FROM karyawan WHERE nip = '$nip' LIMIT 1");
        if ($cek && mysqli_num_rows($cek) > 0) {
            echo json_encode(["status" => "error", "message" => "NIP '$nip' sudah terdaftar"]);
            exit();
        }

        // Buat ID Karyawan berikutnya
        $count_res = mysqli_query($koneksi, "SELECT MAX(id) as max_id FROM karyawan");
        $next_num = 1;
        if ($count_res && $row = mysqli_fetch_assoc($count_res)) {
            $next_num = ((int)$row['max_id']) + 1;
        }
        $id_karyawan = sprintf("K-%03d", $next_num);

        $insert = mysqli_query($koneksi, "INSERT INTO karyawan (id_karyawan, nip, nama, departemen) 
            VALUES ('$id_karyawan', '$nip', '$nama', '$departemen')");

        if ($insert) {
            echo json_encode([
                "status" => "success",
                "message" => "Karyawan baru ($nama) berhasil ditambahkan.",
                "data" => [
                    "id" => $id_karyawan,
                    "nip" => $nip,
                    "nama" => $nama,
                    "departemen" => $departemen
                ]
            ]);
        } else {
            echo json_encode(["status" => "error", "message" => "Gagal menambahkan karyawan: " . mysqli_error($koneksi)]);
        }
    } else if ($aksi === 'hapus') {
        $id_karyawan = isset($_POST['id_karyawan']) ? mysqli_real_escape_string($koneksi, trim($_POST['id_karyawan'])) : '';
        if (empty($id_karyawan)) {
            echo json_encode(["status" => "error", "message" => "ID Karyawan tidak diberikan"]);
            exit();
        }
        $delete = mysqli_query($koneksi, "DELETE FROM karyawan WHERE id_karyawan = '$id_karyawan'");
        if ($delete) {
            echo json_encode(["status" => "success", "message" => "Karyawan $id_karyawan berhasil dihapus."]);
        } else {
            echo json_encode(["status" => "error", "message" => "Gagal menghapus karyawan: " . mysqli_error($koneksi)]);
        }
    }
} else {
    // GET: Tampilkan seluruh master karyawan
    $res = mysqli_query($koneksi, "SELECT id_karyawan, nip, nama, departemen FROM karyawan ORDER BY id ASC");
    $list = [];
    if ($res) {
        while ($row = mysqli_fetch_assoc($res)) {
            $list[] = [
                "id" => $row['id_karyawan'],
                "nip" => $row['nip'],
                "nama" => $row['nama'],
                "departemen" => $row['departemen']
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
