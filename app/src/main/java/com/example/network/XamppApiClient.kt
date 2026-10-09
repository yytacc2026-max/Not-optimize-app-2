package com.example.network

import android.util.Log
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Client koneksi HTTP ke server lokal XAMPP (Apache + PHP + MySQL)
object XamppApiClient {
  private const val TAG = "XamppApiClient"

  // URL default server XAMPP (Port 80 standar Apache, atau port kustom misal 5000)
  var baseUrl: String = "http://192.168.1.10/absensi_api/"

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(4, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  // Format URL agar selalu memiliki trailing slash
  fun setServerUrl(ipOrUrl: String) {
    var cleaned = ipOrUrl.trim()
    if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
      cleaned = "http://$cleaned"
    }
    if (!cleaned.endsWith("/")) {
      cleaned = "$cleaned/"
    }
    baseUrl = cleaned
    Log.d(TAG, "Base URL XAMPP diperbarui: $baseUrl")
  }

  // Uji koneksi ke Apache XAMPP (Ping)
  suspend fun pingServer(): Result<PingResult> = withContext(Dispatchers.IO) {
    try {
      val startTime = System.currentTimeMillis()
      val url = "${baseUrl}ping.php"
      val request = Request.Builder()
        .url(url)
        .get()
        .build()

      val response = client.newCall(request).execute()
      val durationMs = System.currentTimeMillis() - startTime
      val code = response.code
      val bodyString = response.body?.string().orEmpty()

      if (response.isSuccessful) {
        Result.success(
          PingResult(
            isSuccess = true,
            statusCode = code,
            responseTimeMs = durationMs,
            message = if (bodyString.contains("status")) {
              val json = runCatching { JSONObject(bodyString) }.getOrNull()
              json?.optString("message", "Server XAMPP Merespons OK") ?: "Server XAMPP Merespons OK"
            } else {
              "Server Apache Merespons (HTTP $code)"
            }
          )
        )
      } else {
        Result.failure(Exception("HTTP $code: Server merespons tetapi mengembalikan error."))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Gagal koneksi XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // Verifikasi NIP Karyawan ke database XAMPP MySQL
  suspend fun cekKaryawan(nip: String): Result<DataKaryawan> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}cek_karyawan.php?nip=${nip.trim()}"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Server mengembalikan kode HTTP ${response.code}"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val status = json.optString("status", "")

      if (status.equals("success", ignoreCase = true)) {
        val dataObj = json.getJSONObject("data")
        val id = dataObj.optString("id", "K-001")
        val nipRes = dataObj.optString("nip", nip)
        val nama = dataObj.optString("nama", "")
        val dept = dataObj.optString("departemen", "Umum")
        val avatar = nama.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()

        Result.success(DataKaryawan(id, nipRes, nama, dept, avatar))
      } else {
        val pesan = json.optString("message", "NIP tidak terdaftar di database kantor")
        Result.failure(Exception(pesan))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error cek NIP XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // Kirim data absensi masuk atau pulang ke database XAMPP
  suspend fun kirimAbsen(
    idKaryawan: String,
    nip: String,
    nama: String,
    departemen: String,
    tipe: String, // "MASUK" atau "PULANG"
    jam: String,
    statusAbsen: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("id_karyawan", idKaryawan)
        .add("nip", nip)
        .add("nama", nama)
        .add("departemen", departemen)
        .add("tipe", tipe)
        .add("jam", jam)
        .add("status", statusAbsen)
        .build()

      val url = "${baseUrl}absen.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal simpan absen ke XAMPP"))
      }

      val body = response.body?.string().orEmpty()
      val json = runCatching { JSONObject(body) }.getOrNull()
      val msg = json?.optString("message") ?: "Absen $tipe berhasil disimpan ke server XAMPP"
      Result.success(msg)
    } catch (e: Exception) {
      Log.e(TAG, "Error simpan absen XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // Kirim pengajuan izin ke server XAMPP
  suspend fun kirimIzin(
    idKaryawan: String,
    nama: String,
    jenisIzin: String,
    alasan: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("id_karyawan", idKaryawan)
        .add("nama", nama)
        .add("jenis", jenisIzin)
        .add("alasan", alasan)
        .build()

      val url = "${baseUrl}izin.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal kirim izin ke XAMPP"))
      }

      val body = response.body?.string().orEmpty()
      val json = runCatching { JSONObject(body) }.getOrNull()
      val msg = json?.optString("message") ?: "Pengajuan izin berhasil dikirim ke server XAMPP"
      Result.success(msg)
    } catch (e: Exception) {
      Log.e(TAG, "Error kirim izin XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // Ambil daftar absensi admin hari ini dari database XAMPP
  suspend fun getDaftarAbsensiAdmin(): Result<List<BarisAbsensiKaryawan>> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}admin_absensi.php"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val arr = json.optJSONArray("data") ?: JSONArray()
      val resultList = mutableListOf<BarisAbsensiKaryawan>()

      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val stName = obj.optString("status", "TEPAT_WAKTU")
        val status = runCatching { StatusAbsensi.valueOf(stName) }.getOrDefault(StatusAbsensi.TEPAT_WAKTU)

        resultList.add(
          BarisAbsensiKaryawan(
            idKaryawan = obj.optString("id_karyawan", "K-$i"),
            nama = obj.optString("nama", ""),
            departemen = obj.optString("departemen", "Umum"),
            jamMasuk = obj.optString("jam_masuk", "-"),
            jamPulang = obj.optString("jam_pulang", "-"),
            status = status
          )
        )
      }
      Result.success(resultList)
    } catch (e: Exception) {
      Log.e(TAG, "Error get absensi admin XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // Reset absensi harian di database XAMPP
  suspend fun resetAbsensiAdmin(): Result<String> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}reset_absensi.php"
      val request = Request.Builder().url(url).post(FormBody.Builder().build()).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}"))
      }
      Result.success("Data absensi di XAMPP berhasil di-reset.")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // 1. Ambil Pengaturan Jam Kerja & Kantor dari Server XAMPP (Sinkronisasi Semua Device)
  suspend fun getPengaturanKantor(): Result<KonfigurasiAdminDashboard> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}pengaturan.php"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal mengambil pengaturan dari server"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val dataObj = json.optJSONObject("data") ?: JSONObject()

      val config = KonfigurasiAdminDashboard(
        jamMasukKerja = dataObj.optString("jam_masuk", "08:00"),
        batasTerlambat = dataObj.optString("batas_terlambat", "08:00"),
        jamPulangKerja = dataObj.optString("jam_pulang", "17:00"),
        pengumuman = dataObj.optString("pengumuman", "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB."),
        totalKaryawan = dataObj.optInt("total_karyawan", 0)
      )
      Result.success(config)
    } catch (e: Exception) {
      Log.e(TAG, "Error get pengaturan XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // 2. Simpan Pengaturan Jam Kerja & Kantor ke Server XAMPP (Berlaku untuk SEMUA Device)
  suspend fun simpanPengaturanKantor(config: KonfigurasiAdminDashboard): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("jam_masuk", config.jamMasukKerja)
        .add("batas_terlambat", config.batasTerlambat)
        .add("jam_pulang", config.jamPulangKerja)
        .add("pengumuman", config.pengumuman)
        .add("total_karyawan", config.totalKaryawan.toString())
        .build()

      val url = "${baseUrl}pengaturan.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal menyimpan pengaturan ke server"))
      }

      val body = response.body?.string().orEmpty()
      val json = runCatching { JSONObject(body) }.getOrNull()
      val msg = json?.optString("message") ?: "Pengaturan berhasil diperbarui di server XAMPP"
      Result.success(msg)
    } catch (e: Exception) {
      Log.e(TAG, "Error simpan pengaturan XAMPP: ${e.message}")
      Result.failure(e)
    }
  }

  // 3. Ambil Data Dashboard Admin Lengkap & Akurat dari Server XAMPP
  suspend fun getDashboardAdminLengkap(): Result<DashboardAdminData> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}admin_absensi.php"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal memuat data dashboard dari server"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)

      val totalKaryawanMaster = json.optInt("total_karyawan_master", 0)
      val totalKaryawan = json.optInt("total_karyawan", totalKaryawanMaster)
      val totalHadir = json.optInt("total_hadir", 0)
      val totalTerlambat = json.optInt("total_terlambat", 0)
      val totalBelumAbsen = json.optInt("total_belum_absen", maxOf(0, totalKaryawan - totalHadir))

      val cfgObj = json.optJSONObject("config")
      val config = KonfigurasiAdminDashboard(
        jamMasukKerja = cfgObj?.optString("jam_masuk", "08:00") ?: "08:00",
        batasTerlambat = cfgObj?.optString("batas_terlambat", "08:00") ?: "08:00",
        jamPulangKerja = cfgObj?.optString("jam_pulang", "17:00") ?: "17:00",
        pengumuman = cfgObj?.optString("pengumuman", "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.") ?: "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.",
        totalKaryawan = totalKaryawan,
        totalHadir = totalHadir,
        totalTerlambat = totalTerlambat,
        totalBelumAbsen = totalBelumAbsen
      )

      val arr = json.optJSONArray("data") ?: JSONArray()
      val absensiList = mutableListOf<BarisAbsensiKaryawan>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val stName = obj.optString("status", "TEPAT_WAKTU")
        val status = runCatching { StatusAbsensi.valueOf(stName) }.getOrDefault(StatusAbsensi.TEPAT_WAKTU)
        absensiList.add(
          BarisAbsensiKaryawan(
            idKaryawan = obj.optString("id_karyawan", "K-$i"),
            nama = obj.optString("nama", ""),
            departemen = obj.optString("departemen", "Umum"),
            jamMasuk = obj.optString("jam_masuk", "-"),
            jamPulang = obj.optString("jam_pulang", "-"),
            status = status
          )
        )
      }

      Result.success(
        DashboardAdminData(
          config = config,
          totalKaryawanMaster = totalKaryawanMaster,
          daftarAbsensi = absensiList,
          totalHadir = totalHadir,
          totalTerlambat = totalTerlambat,
          totalBelumAbsen = totalBelumAbsen
        )
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error getDashboardAdminLengkap: ${e.message}")
      Result.failure(e)
    }
  }

  // 4. Ambil Daftar Karyawan Master dari Server XAMPP
  suspend fun getDaftarMasterKaryawan(): Result<List<DataKaryawan>> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}karyawan_crud.php"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal mengambil data master karyawan"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val arr = json.optJSONArray("data") ?: JSONArray()
      val list = mutableListOf<DataKaryawan>()

      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val nama = obj.optString("nama", "")
        val avatar = nama.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        list.add(
          DataKaryawan(
            id = obj.optString("id", "K-$i"),
            nip = obj.optString("nip", ""),
            nama = nama,
            departemen = obj.optString("departemen", "Umum"),
            avatarInisial = if (avatar.isNotEmpty()) avatar else "K"
          )
        )
      }
      Result.success(list)
    } catch (e: Exception) {
      Log.e(TAG, "Error getDaftarMasterKaryawan: ${e.message}")
      Result.failure(e)
    }
  }

  // 5. Tambah Karyawan Baru ke Database XAMPP oleh Admin
  suspend fun tambahMasterKaryawan(nip: String, nama: String, departemen: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("aksi", "tambah")
        .add("nip", nip.trim())
        .add("nama", nama.trim())
        .add("departemen", departemen.trim())
        .build()

      val url = "${baseUrl}karyawan_crud.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal menambahkan karyawan"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val status = json.optString("status")
      val msg = json.optString("message", "Karyawan berhasil ditambahkan")

      if (status.equals("success", ignoreCase = true)) {
        Result.success(msg)
      } else {
        Result.failure(Exception(msg))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error tambahMasterKaryawan: ${e.message}")
      Result.failure(e)
    }
  }

  // 6. Hapus Karyawan dari Database XAMPP oleh Admin
  suspend fun hapusMasterKaryawan(idKaryawan: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("aksi", "hapus")
        .add("id_karyawan", idKaryawan.trim())
        .build()

      val url = "${baseUrl}karyawan_crud.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal menghapus karyawan"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val msg = json.optString("message", "Karyawan berhasil dihapus")
      Result.success(msg)
    } catch (e: Exception) {
      Log.e(TAG, "Error hapusMasterKaryawan: ${e.message}")
      Result.failure(e)
    }
  }

  // 7. Ambil Daftar Pengajuan Izin Karyawan untuk Admin
  suspend fun getDaftarPengajuanIzin(): Result<List<ItemPengajuanIzinAdmin>> = withContext(Dispatchers.IO) {
    try {
      val url = "${baseUrl}admin_izin.php"
      val request = Request.Builder().url(url).get().build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal mengambil data permohonan izin"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val arr = json.optJSONArray("data") ?: JSONArray()
      val list = mutableListOf<ItemPengajuanIzinAdmin>()

      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          ItemPengajuanIzinAdmin(
            id = obj.optInt("id", 0),
            idKaryawan = obj.optString("id_karyawan", ""),
            nama = obj.optString("nama", ""),
            jenisIzin = obj.optString("jenis_izin", "Izin"),
            alasan = obj.optString("alasan", "-"),
            tanggalDiajukan = obj.optString("tanggal_diajukan", "-"),
            statusPersetujuan = obj.optString("status_persetujuan", "Menunggu Verifikasi")
          )
        )
      }
      Result.success(list)
    } catch (e: Exception) {
      Log.e(TAG, "Error getDaftarPengajuanIzin: ${e.message}")
      Result.failure(e)
    }
  }

  // 8. Update Status Persetujuan Izin Karyawan oleh Admin
  suspend fun updateStatusIzin(idIzin: Int, status: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val formBody = FormBody.Builder()
        .add("id", idIzin.toString())
        .add("status", status.trim())
        .build()

      val url = "${baseUrl}admin_izin.php"
      val request = Request.Builder().url(url).post(formBody).build()
      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("HTTP ${response.code}: Gagal memperbarui status izin"))
      }

      val body = response.body?.string().orEmpty()
      val json = JSONObject(body)
      val msg = json.optString("message", "Status izin berhasil diubah menjadi $status")
      Result.success(msg)
    } catch (e: Exception) {
      Log.e(TAG, "Error updateStatusIzin: ${e.message}")
      Result.failure(e)
    }
  }
}

// Data hasil ping ke server lokal
data class PingResult(
  val isSuccess: Boolean,
  val statusCode: Int,
  val responseTimeMs: Long,
  val message: String
)
