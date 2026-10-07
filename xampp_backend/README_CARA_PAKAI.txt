PANDUAN MENGHUBUNGKAN APLIKASI ANDROID KE LOCAL SERVER XAMPP:
=============================================================

1. Buka XAMPP Control Panel di Komputer/Laptop Anda:
   - Klik "Start" pada modul Apache
   - Klik "Start" pada modul MySQL

2. Letakkan File PHP:
   - Buat folder baru bernama "absensi_api" di dalam folder htdocs XAMPP Anda.
     Contoh lokasi: C:\xampp\htdocs\absensi_api\
   - Salin seluruh file PHP dari folder ini (koneksi.php, ping.php, cek_karyawan.php, absen.php, izin.php, admin_absensi.php, reset_absensi.php) ke dalam folder C:\xampp\htdocs\absensi_api\

3. Buat Database MySQL:
   - Buka browser dan buka http://localhost/phpmyadmin/
   - Buat database baru bernama "db_absensi_kantor"
   - Klik tab "Import", lalu pilih file "database.sql" yang ada di folder ini, lalu klik "Go".

4. Menghubungkan HP Android ke XAMPP:
   - Pastikan HP Android dan Komputer/Laptop terhubung ke jaringan Wi-Fi yang sama (1 router / hotspot).
   - Cek IP Komputer Anda melalui Command Prompt (ketik: ipconfig). Contoh IP: 192.168.1.10
   - Di aplikasi Android, klik ikon Server / Jaringan di bagian atas, masukkan URL:
     http://[IP_KOMPUTER_ANDA]/absensi_api/   (Contoh: http://192.168.1.10/absensi_api/)
   - Klik tombol "Tes Koneksi ke XAMPP". Jika status berwarna HIJAU, aplikasi sudah 100% terhubung!
