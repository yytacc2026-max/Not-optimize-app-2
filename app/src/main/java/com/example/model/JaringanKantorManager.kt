package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.network.PingResult
import com.example.network.XamppApiClient

// Pengelola validasi jaringan kantor lokal dan status koneksi ke XAMPP
class JaringanKantorManager(private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("xampp_server_config", Context.MODE_PRIVATE)

  // Batas toleransi jarak radius kantor
  val radiusMaksimalKantorMeter: Int = 100

  // Nama SSID Wi-Fi kantor
  var namaWifiKantor: String by mutableStateOf("")

  // URL server lokal XAMPP (Apache + PHP)
  var serverXamppUrl by mutableStateOf(
    prefs.getString("key_xampp_url", "http://192.168.1.10/absensi_api/") ?: "http://192.168.1.10/absensi_api/"
  )
    private set

  // Status berada di dalam jangkauan kantor
  var isDalamJaringanKantor by mutableStateOf(true)
    private set

  // Perkiraan jarak ke kantor (meter)
  var jarakDariKantorMeter by mutableIntStateOf(15)
    private set

  // Status pengujian koneksi XAMPP
  var isTestingKoneksi by mutableStateOf(false)
    private set
  var lastPingResult by mutableStateOf<PingResult?>(null)
    private set

  init {
    XamppApiClient.setServerUrl(serverXamppUrl)
  }

  // Update alamat server lokal XAMPP
  fun updateServerUrl(urlBaru: String) {
    serverXamppUrl = urlBaru.trim()
    prefs.edit().putString("key_xampp_url", serverXamppUrl).apply()
    XamppApiClient.setServerUrl(serverXamppUrl)
    lastPingResult = null
  }

  // Uji koneksi langsung ke server XAMPP (HTTP Ping)
  suspend fun tesKoneksiXampp(): PingResult {
    isTestingKoneksi = true
    return try {
      val result = XamppApiClient.pingServer()
      val ping = result.getOrElse { err ->
        PingResult(
          isSuccess = false,
          statusCode = 0,
          responseTimeMs = 0,
          message = err.localizedMessage ?: "Tidak dapat terhubung ke server XAMPP"
        )
      }
      lastPingResult = ping
      ping
    } finally {
      isTestingKoneksi = false
    }
  }

  // Cek apakah perangkat terhubung ke jaringan internet/LAN
  fun isTerhubungKoneksi(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
  }

  // Simulasi berada di kantor
  fun setMasukKantor() {
    isDalamJaringanKantor = true
    jarakDariKantorMeter = 15
  }

  // Simulasi berada di luar jangkauan kantor
  fun setJauhDariKantor(jarakMeter: Int = 1200) {
    isDalamJaringanKantor = false
    jarakDariKantorMeter = jarakMeter
  }

  // Toggle simulasi lokasi untuk keperluan pengujian
  fun toggleModeSimulasi() {
    if (isDalamJaringanKantor) {
      setJauhDariKantor(1200)
    } else {
      setMasukKantor()
    }
  }
}
