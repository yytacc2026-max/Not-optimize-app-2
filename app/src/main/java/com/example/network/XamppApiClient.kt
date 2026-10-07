package com.example.network

import android.util.Log
import com.example.model.BarisAbsensiKaryawan
import com.example.model.DataKaryawan
import com.example.model.StatusAbsensi
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
}

// Data hasil ping ke server lokal
data class PingResult(
  val isSuccess: Boolean,
  val statusCode: Int,
  val responseTimeMs: Long,
  val message: String
)
