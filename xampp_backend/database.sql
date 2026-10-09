-- Skrip Database MySQL untuk XAMPP
-- Nama Database: db_absensi_kantor

CREATE DATABASE IF NOT EXISTS db_absensi_kantor;
USE db_absensi_kantor;

-- 1. Tabel Karyawan Master
CREATE TABLE IF NOT EXISTS karyawan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan VARCHAR(20) NOT NULL UNIQUE,
    nip VARCHAR(30) NOT NULL UNIQUE,
    nama VARCHAR(100) NOT NULL,
    departemen VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Data Karyawan Awal untuk Testing
INSERT IGNORE INTO karyawan (id_karyawan, nip, nama, departemen) VALUES
('K-001', '19940115001', 'Ahmad Fauzi', 'Teknologi'),
('K-002', '19920320002', 'Budi Santoso', 'Operasional'),
('K-003', '19950711003', 'Citra Dewi', 'Keuangan'),
('K-004', '19901105004', 'Dedi Prasetyo', 'Sumber Daya Manusia'),
('K-005', '19960825005', 'Eka Rahmawati', 'Pemasaran');

-- 2. Tabel Catatan Absensi
CREATE TABLE IF NOT EXISTS absensi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan VARCHAR(20) NOT NULL,
    nip VARCHAR(30) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    departemen VARCHAR(100) NOT NULL,
    tanggal DATE NOT NULL,
    jam_masuk VARCHAR(10) DEFAULT '-',
    jam_pulang VARCHAR(10) DEFAULT '-',
    status VARCHAR(30) DEFAULT 'TEPAT_WAKTU',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabel Pengajuan Izin
CREATE TABLE IF NOT EXISTS pengajuan_izin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan VARCHAR(20) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    jenis_izin VARCHAR(50) NOT NULL,
    alasan TEXT NOT NULL,
    tanggal_diajukan DATETIME NOT NULL,
    status_persetujuan VARCHAR(50) DEFAULT 'Menunggu Verifikasi'
);

-- 4. Tabel Pengaturan Dashboard & Jam Kerja Kantor (Sinkronisasi Semua Perangkat)
CREATE TABLE IF NOT EXISTS pengaturan_kantor (
    id INT PRIMARY KEY DEFAULT 1,
    jam_masuk VARCHAR(10) NOT NULL DEFAULT '08:00',
    batas_terlambat VARCHAR(10) NOT NULL DEFAULT '08:00',
    jam_pulang VARCHAR(10) NOT NULL DEFAULT '17:00',
    pengumuman TEXT NOT NULL,
    total_karyawan INT NOT NULL DEFAULT 5,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Data Default Pengaturan Kantor
INSERT IGNORE INTO pengaturan_kantor (id, jam_masuk, batas_terlambat, jam_pulang, pengumuman, total_karyawan) VALUES
(1, '08:00', '08:00', '17:00', 'Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.', 5);

