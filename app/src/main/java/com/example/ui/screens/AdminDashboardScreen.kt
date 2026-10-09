package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.network.XamppApiClient
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class TabAdmin(val label: String, val icon: ImageVector) {
  KEHADIRAN("Kehadiran", Icons.Default.Assessment),
  MASTER_KARYAWAN("Master Karyawan", Icons.Default.People),
  PENGAJUAN_IZIN("Pengajuan Izin", Icons.Default.AssignmentTurnedIn)
}

enum class FilterStatusAbsensi(val label: String) {
  SEMUA("Semua"),
  HADIR("Hadir"),
  TERLAMBAT("Terlambat")
}

// Dashboard Administrator untuk memantau data kehadiran dan mengelola data kantor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val sessionManager = remember { SessionManager(context) }
  val jaringanManager = remember { JaringanKantorManager(context) }
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(TabAdmin.KEHADIRAN) }
  var filterStatus by remember { mutableStateOf(FilterStatusAbsensi.SEMUA) }

  var configAdmin by remember { mutableStateOf(sessionManager.getKonfigurasiAdmin()) }
  var isEditModeOpen by remember { mutableStateOf(false) }
  var isTambahKaryawanOpen by remember { mutableStateOf(false) }
  var showXamppConfigDialog by remember { mutableStateOf(false) }

  var isSyncing by remember { mutableStateOf(false) }
  var isConnectedToServer by remember { mutableStateOf(false) }
  var pesanStatusAdmin by remember { mutableStateOf<String?>(null) }

  // Daftar absensi hari ini
  var daftarAbsensi by remember {
    mutableStateOf(sessionManager.getAdminDaftarAbsensi())
  }

  // Daftar master karyawan & pengajuan izin
  var masterKaryawanList by remember { mutableStateOf<List<DataKaryawan>>(DummyDataKantor.daftarKaryawanResmi) }
  var pengajuanIzinList by remember { mutableStateOf<List<ItemPengajuanIzinAdmin>>(emptyList()) }

  // Fungsi sinkronisasi data lengkap dari server XAMPP
  fun muatDataDariServer() {
    coroutineScope.launch {
      isSyncing = true
      // 1. Ambil data dashboard absensi & konfigurasi terpusat
      val dashResult = XamppApiClient.getDashboardAdminLengkap()
      if (dashResult.isSuccess) {
        val dataLengkap = dashResult.getOrNull()
        if (dataLengkap != null) {
          configAdmin = dataLengkap.config
          daftarAbsensi = dataLengkap.daftarAbsensi
          sessionManager.simpanKonfigurasiAdmin(dataLengkap.config)
          sessionManager.simpanAdminDaftarAbsensi(dataLengkap.daftarAbsensi)
          isConnectedToServer = true
        }
      } else {
        // Fallback jika belum konek server
        isConnectedToServer = false
      }

      // 2. Ambil master karyawan
      val masterResult = XamppApiClient.getDaftarMasterKaryawan()
      if (masterResult.isSuccess) {
        val listMaster = masterResult.getOrNull().orEmpty()
        if (listMaster.isNotEmpty()) {
          masterKaryawanList = listMaster
        }
      }

      // 3. Ambil pengajuan izin
      val izinResult = XamppApiClient.getDaftarPengajuanIzin()
      if (izinResult.isSuccess) {
        pengajuanIzinList = izinResult.getOrNull().orEmpty()
      }

      isSyncing = false
    }
  }

  // Muat data saat layar dibuka
  LaunchedEffect(Unit) {
    muatDataDariServer()
  }

  // Menghitung metrik kehadiran secara real-time & akurat
  val totalHadir = remember(daftarAbsensi) {
    daftarAbsensi.count { it.status == StatusAbsensi.TEPAT_WAKTU || it.status == StatusAbsensi.TERLAMBAT }
  }
  val totalTerlambat = remember(daftarAbsensi) {
    daftarAbsensi.count { it.status == StatusAbsensi.TERLAMBAT }
  }

  // Total karyawan dihitung akurat dari konfigurasi atau master karyawan di database
  val totalKaryawanTampil = remember(configAdmin.totalKaryawan, masterKaryawanList, daftarAbsensi) {
    if (configAdmin.totalKaryawan > 0) {
      configAdmin.totalKaryawan
    } else if (masterKaryawanList.isNotEmpty()) {
      masterKaryawanList.size
    } else {
      daftarAbsensi.size
    }
  }

  val totalBelumAbsen = remember(totalKaryawanTampil, totalHadir) {
    maxOf(0, totalKaryawanTampil - totalHadir)
  }

  // Filter daftar absensi
  val filteredAbsensi = remember(daftarAbsensi, filterStatus) {
    when (filterStatus) {
      FilterStatusAbsensi.SEMUA -> daftarAbsensi
      FilterStatusAbsensi.HADIR -> daftarAbsensi.filter { it.status == StatusAbsensi.TEPAT_WAKTU || it.status == StatusAbsensi.TERLAMBAT }
      FilterStatusAbsensi.TERLAMBAT -> daftarAbsensi.filter { it.status == StatusAbsensi.TERLAMBAT }
    }
  }

  Scaffold(
    containerColor = BgLight,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Dashboard Administrator",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Text(
              text = if (isConnectedToServer) "🟢 Terhubung ke Server (Sinkronisasi Semua Device)" else "🟠 Mode Offline / Belum Konek Server",
              fontSize = 11.sp,
              color = if (isConnectedToServer) StatusGreen else StatusAmber,
              fontWeight = FontWeight.SemiBold
            )
          }
        },
        actions = {
          // Tombol Pengaturan IP Server XAMPP
          IconButton(
            onClick = { showXamppConfigDialog = true },
            modifier = Modifier.testTag("admin_xampp_server_button")
          ) {
            Icon(
              imageVector = Icons.Default.Dns,
              contentDescription = "Pengaturan Server XAMPP",
              tint = TealPrimary
            )
          }
          // Tombol Sinkronisasi / Refresh Server
          IconButton(
            onClick = { muatDataDariServer() },
            modifier = Modifier.testTag("admin_sync_button")
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = TealPrimary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sinkronkan Data Server",
                tint = TealPrimary
              )
            }
          }
          // Tombol Logout
          IconButton(onClick = onLogout, modifier = Modifier.testTag("admin_logout_button")) {
            Icon(
              imageVector = Icons.Default.Logout,
              contentDescription = "Keluar",
              tint = StatusRed
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = CardWhite,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("admin_bottom_navigation")
      ) {
        TabAdmin.values().forEach { tab ->
          val isSelected = selectedTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label
              )
            },
            label = {
              Text(
                text = tab.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TealPrimary,
              selectedTextColor = TealPrimary,
              indicatorColor = TealContainer,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted
            )
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item(key = "top_spacer") {
          Spacer(modifier = Modifier.height(4.dp))
        }

        // Banner Status Notifikasi Admin
        if (pesanStatusAdmin != null) {
          item(key = "admin_status_banner") {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = StatusGreenBg),
              border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.35f)),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_status_banner")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = StatusGreen,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Pembaruan Tersinkronisasi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                  )
                  Text(
                    text = pesanStatusAdmin ?: "",
                    fontSize = 12.sp,
                    color = TextDark
                  )
                }
                IconButton(onClick = { pesanStatusAdmin = null }, modifier = Modifier.size(28.dp)) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        // KARTU STATUS KONEKSI SERVER XAMPP
        item(key = "admin_server_status_card") {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isConnectedToServer) TealContainer.copy(alpha = 0.45f) else StatusAmberBg
            ),
            border = BorderStroke(
              1.dp,
              if (isConnectedToServer) TealPrimary.copy(alpha = 0.3f) else StatusAmber.copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_server_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isConnectedToServer) TealPrimary else StatusAmber),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isConnectedToServer) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = if (isConnectedToServer) "Sinkronisasi Server Aktif" else "Server Belum Terkoneksi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnectedToServer) TealDark else StatusAmber
                  )
                  Text(
                    text = XamppApiClient.baseUrl,
                    fontSize = 11.sp,
                    color = TextMuted
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                  onClick = { showXamppConfigDialog = true },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isConnectedToServer) TealPrimary.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.2f),
                    contentColor = if (isConnectedToServer) TealDark else StatusAmber
                  )
                ) {
                  Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("IP Server", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                  onClick = { muatDataDariServer() },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TealPrimary,
                    contentColor = Color.White
                  )
                ) {
                  Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // KONTEN BERDASARKAN TAB AKTIF
        when (selectedTab) {
          TabAdmin.KEHADIRAN -> {
            // 1. KARTU RINGKASAN METRIK ADMIN (4 KARTU)
            item(key = "admin_metrics") {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  AdminMetricCard(
                    judul = "Total Karyawan",
                    nilai = "$totalKaryawanTampil",
                    icon = Icons.Default.Groups,
                    warna = TealPrimary,
                    warnaBg = TealContainer,
                    modifier = Modifier.weight(1f)
                  )
                  AdminMetricCard(
                    judul = "Hadir",
                    nilai = "$totalHadir",
                    icon = Icons.Default.CheckCircle,
                    warna = StatusGreen,
                    warnaBg = StatusGreenBg,
                    modifier = Modifier.weight(1f)
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  AdminMetricCard(
                    judul = "Terlambat",
                    nilai = "$totalTerlambat",
                    icon = Icons.Default.Warning,
                    warna = StatusAmber,
                    warnaBg = StatusAmberBg,
                    modifier = Modifier.weight(1f)
                  )
                  AdminMetricCard(
                    judul = "Belum Absen",
                    nilai = "$totalBelumAbsen",
                    icon = Icons.Default.AccessTime,
                    warna = StatusRed,
                    warnaBg = StatusRedBg,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }

            // 2. KARTU PENGATURAN JAM KERJA & PENGUMUMAN (SINKRON SEMUA PERANGKAT)
            item(key = "admin_config_card") {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderMuted),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_config_card")
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Pengaturan Jam Kerja Kantor",
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextDark
                    )
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Text(
                        text = "Semua Device",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text(text = "Jam Masuk", fontSize = 11.sp, color = TextMuted)
                      Text(text = configAdmin.jamMasukKerja, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Column {
                      Text(text = "Batas Terlambat", fontSize = 11.sp, color = TextMuted)
                      Text(text = configAdmin.batasTerlambat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                    }
                    Column {
                      Text(text = "Jam Pulang", fontSize = 11.sp, color = TextMuted)
                      Text(text = configAdmin.jamPulangKerja, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Column {
                      Text(text = "Target Karyawan", fontSize = 11.sp, color = TextMuted)
                      Text(text = "$totalKaryawanTampil Org", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealDark)
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))
                  HorizontalDivider(color = BorderMuted)
                  Spacer(modifier = Modifier.height(10.dp))

                  Text(text = "Pesan Pengumuman Kantor:", fontSize = 11.sp, color = TextMuted)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = configAdmin.pengumuman,
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 18.sp
                  )
                }
              }
            }

            // 3. TOMBOL UTAMA: "EDIT DASHBOARD (UBAH DATA KANTOR & JAM SEMUA DEVICE)"
            item(key = "admin_edit_button") {
              Button(
                onClick = { isEditModeOpen = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("button_edit_dashboard")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Dashboard",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Edit Dashboard (Ubah Pengaturan Semua Device)",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }

            // 4. HEADER TABEL ABSENSI & FILTER
            item(key = "admin_table_header") {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Daftar Absensi Karyawan Hari Ini",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                  )
                  Text(
                    text = "${daftarAbsensi.size} Data Masuk",
                    fontSize = 12.sp,
                    color = if (daftarAbsensi.isEmpty()) StatusAmber else TealPrimary,
                    fontWeight = FontWeight.SemiBold
                  )
                }

                // Filter Chip
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  FilterStatusAbsensi.values().forEach { filter ->
                    val isFilterSelected = filterStatus == filter
                    FilterChip(
                      selected = isFilterSelected,
                      onClick = { filterStatus = filter },
                      label = {
                        val count = when (filter) {
                          FilterStatusAbsensi.SEMUA -> daftarAbsensi.size
                          FilterStatusAbsensi.HADIR -> totalHadir
                          FilterStatusAbsensi.TERLAMBAT -> totalTerlambat
                        }
                        Text("${filter.label} ($count)", fontSize = 11.sp)
                      },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealContainer,
                        selectedLabelColor = TealDark
                      )
                    )
                  }
                }
              }
            }

            // 5. ISI TABEL ABSENSI
            if (filteredAbsensi.isEmpty()) {
              item(key = "admin_empty_table") {
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = CardWhite),
                  border = BorderStroke(1.dp, BorderMuted),
                  elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_empty_table_card")
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Box(
                      modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(TealContainer.copy(alpha = 0.5f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(26.dp)
                      )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = if (daftarAbsensi.isEmpty()) "Belum Ada Data Absensi Masuk (0 Data)" else "Tidak Ada Data untuk Filter Ini",
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Seluruh data absensi dimulai dari 0 agar pencatatan akurat. Data kehadiran akan otomatis masuk saat karyawan melakukan Absen Masuk di jaringan kantor.",
                      fontSize = 12.sp,
                      color = TextMuted,
                      textAlign = TextAlign.Center,
                      lineHeight = 16.sp
                    )
                  }
                }
              }
            } else {
              items(items = filteredAbsensi, key = { it.idKaryawan }) { row ->
                BarisTabelAbsensiCard(row = row)
              }
            }
          }

          TabAdmin.MASTER_KARYAWAN -> {
            // TAB MASTER KARYAWAN: LIHAT & TAMBAH KARYAWAN RESMI
            item(key = "master_karyawan_header") {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderMuted),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(
                        text = "Master Karyawan Kantor",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                      )
                      Text(
                        text = "${masterKaryawanList.size} Karyawan Terdaftar di Database",
                        fontSize = 12.sp,
                        color = TextMuted
                      )
                    }
                    Button(
                      onClick = { isTambahKaryawanOpen = true },
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                      Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "Daftar karyawan resmi ini tersimpan di server MySQL XAMPP dan dipakai oleh setiap smartphone karyawan untuk registrasi akun dan absensi.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                  )
                }
              }
            }

            items(items = masterKaryawanList, key = { it.id }) { karyawan ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderMuted),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TealContainer),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = karyawan.avatarInisial,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = karyawan.nama,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                      )
                      Text(
                        text = "NIP: ${karyawan.nip} • ${karyawan.departemen}",
                        fontSize = 11.sp,
                        color = TextMuted
                      )
                      Text(
                        text = "ID: ${karyawan.id}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                      )
                    }
                  }

                  IconButton(
                    onClick = {
                      coroutineScope.launch {
                        val res = XamppApiClient.hapusMasterKaryawan(karyawan.id)
                        if (res.isSuccess) {
                          pesanStatusAdmin = "Karyawan ${karyawan.nama} berhasil dihapus dari server."
                          muatDataDariServer()
                        }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Hapus Karyawan",
                      tint = StatusRed.copy(alpha = 0.8f)
                    )
                  }
                }
              }
            }
          }

          TabAdmin.PENGAJUAN_IZIN -> {
            // TAB PENGAJUAN IZIN: VERIFIKASI IZIN DARI KARYAWAN
            item(key = "izin_header") {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderMuted),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Pengajuan Izin Karyawan",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextDark
                    )
                    Text(
                      text = "${pengajuanIzinList.size} Pengajuan",
                      fontSize = 12.sp,
                      color = TealPrimary,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Daftar permohonan izin/sakit yang diajukan oleh karyawan melalui smartphone mereka.",
                    fontSize = 12.sp,
                    color = TextMuted
                  )
                }
              }
            }

            if (pengajuanIzinList.isEmpty()) {
              item(key = "izin_empty") {
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = CardWhite),
                  border = BorderStroke(1.dp, BorderMuted),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Icon(
                      imageVector = Icons.Default.AssignmentLate,
                      contentDescription = null,
                      tint = TextMuted,
                      modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = "Belum Ada Pengajuan Izin",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextDark
                    )
                    Text(
                      text = "Permohonan izin atau sakit dari karyawan akan tampil di sini.",
                      fontSize = 12.sp,
                      color = TextMuted
                    )
                  }
                }
              }
            } else {
              items(items = pengajuanIzinList, key = { it.id }) { itemIzin ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = CardWhite),
                  border = BorderStroke(1.dp, BorderMuted),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          text = itemIzin.nama,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextDark
                        )
                        Text(
                          text = "Jenis: ${itemIzin.jenisIzin} • Diajukan: ${itemIzin.tanggalDiajukan}",
                          fontSize = 11.sp,
                          color = TextMuted
                        )
                      }
                      // Badge Status Izin
                      val statusColor = when (itemIzin.statusPersetujuan) {
                        "Disetujui" -> StatusGreen
                        "Ditolak" -> StatusRed
                        else -> StatusAmber
                      }
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(8.dp))
                          .background(statusColor.copy(alpha = 0.15f))
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text(
                          text = itemIzin.statusPersetujuan,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = statusColor
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = "Alasan: \"${itemIzin.alasan}\"",
                      fontSize = 12.sp,
                      color = TextDark,
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    if (itemIzin.statusPersetujuan == "Menunggu Verifikasi") {
                      Spacer(modifier = Modifier.height(10.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                      ) {
                        OutlinedButton(
                          onClick = {
                            coroutineScope.launch {
                              val res = XamppApiClient.updateStatusIzin(itemIzin.id, "Ditolak")
                              if (res.isSuccess) {
                                pesanStatusAdmin = "Izin ${itemIzin.nama} ditolak."
                                muatDataDariServer()
                              }
                            }
                          },
                          shape = RoundedCornerShape(8.dp),
                          colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                          Text("Tolak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                          onClick = {
                            coroutineScope.launch {
                              val res = XamppApiClient.updateStatusIzin(itemIzin.id, "Disetujui")
                              if (res.isSuccess) {
                                pesanStatusAdmin = "Izin ${itemIzin.nama} berhasil disetujui."
                                muatDataDariServer()
                              }
                            }
                          },
                          shape = RoundedCornerShape(8.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                          Text("Setujui", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        item(key = "admin_bottom_spacer") {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }

  // DIALOG EDIT DASHBOARD (BERLAKU UNTUK SEMUA DEVICE)
  if (isEditModeOpen) {
    EditDashboardDialog(
      configSaatIni = configAdmin,
      onSimpan = { configBaru ->
        configAdmin = configBaru
        sessionManager.simpanKonfigurasiAdmin(configBaru)

        coroutineScope.launch {
          val res = XamppApiClient.simpanPengaturanKantor(configBaru)
          if (res.isSuccess) {
            pesanStatusAdmin = "Pengaturan jam kantor & pengumuman berhasil disimpan ke server XAMPP dan langsung berlaku untuk SEMUA perangkat!"
          } else {
            pesanStatusAdmin = "Pengaturan tersimpan lokal (Gagal konek server: ${res.exceptionOrNull()?.message})"
          }
          muatDataDariServer()
        }
        isEditModeOpen = false
      },
      onBatal = {
        isEditModeOpen = false
      }
    )
  }

  // DIALOG TAMBAH MASTER KARYAWAN
  if (isTambahKaryawanOpen) {
    DialogTambahMasterKaryawan(
      onSimpan = { nip, nama, dept ->
        coroutineScope.launch {
          val res = XamppApiClient.tambahMasterKaryawan(nip, nama, dept)
          if (res.isSuccess) {
            pesanStatusAdmin = "Karyawan baru ($nama) berhasil ditambahkan ke database server!"
            muatDataDariServer()
          } else {
            pesanStatusAdmin = "Gagal menambah karyawan: ${res.exceptionOrNull()?.message}"
          }
        }
        isTambahKaryawanOpen = false
      },
      onBatal = { isTambahKaryawanOpen = false }
    )
  }

  // DIALOG PENGATURAN SERVER XAMPP
  if (showXamppConfigDialog) {
    DialogPengaturanXampp(
      jaringanManager = jaringanManager,
      onDismiss = {
        showXamppConfigDialog = false
        muatDataDariServer()
      }
    )
  }
}

// Kartu metrik kehadiran
@Composable
fun AdminMetricCard(
  judul: String,
  nilai: String,
  icon: ImageVector,
  warna: Color,
  warnaBg: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(warnaBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = warna,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = nilai,
          fontSize = 19.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextDark
        )
        Text(
          text = judul,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = TextMuted
        )
      }
    }
  }
}

// Baris item absensi karyawan
@Composable
fun BarisTabelAbsensiCard(row: BarisAbsensiKaryawan) {
  val inisial = remember(row.nama) {
    row.nama.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(TealContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = inisial,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = row.nama,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
          Text(
            text = "${row.idKaryawan} • ${row.departemen}",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        StatusPillBadge(status = row.status)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Masuk: ${row.jamMasuk} | Pulang: ${row.jamPulang}",
          fontSize = 11.sp,
          color = TextMuted
        )
      }
    }
  }
}

// Dialog konfigurasi jam kerja dan pengumuman untuk SEMUA perangkat
@Composable
fun EditDashboardDialog(
  configSaatIni: KonfigurasiAdminDashboard,
  onSimpan: (KonfigurasiAdminDashboard) -> Unit,
  onBatal: () -> Unit
) {
  var jamMasuk by remember { mutableStateOf(configSaatIni.jamMasukKerja) }
  var batasTerlambat by remember { mutableStateOf(configSaatIni.batasTerlambat) }
  var jamPulang by remember { mutableStateOf(configSaatIni.jamPulangKerja) }
  var pengumuman by remember { mutableStateOf(configSaatIni.pengumuman) }
  var totalKaryawan by remember { mutableStateOf(configSaatIni.totalKaryawan.toString()) }
  val scrollState = rememberScrollState()

  AlertDialog(
    containerColor = CardWhite,
    onDismissRequest = onBatal,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Tune, contentDescription = null, tint = TealPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Edit Dashboard & Pengaturan Kantor",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = TealContainer.copy(alpha = 0.5f))
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = TealDark, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Perubahan jam masuk, toleransi terlambat, jam pulang, dan pengumuman akan disinkronkan ke server XAMPP dan langsung berlaku untuk SEMUA perangkat admin & karyawan.",
              fontSize = 11.sp,
              color = TealDark,
              lineHeight = 15.sp
            )
          }
        }

        OutlinedTextField(
          value = jamMasuk,
          onValueChange = { jamMasuk = it },
          label = { Text("Jam Masuk Kerja (Contoh: 08:00)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = batasTerlambat,
          onValueChange = { batasTerlambat = it },
          label = { Text("Batas Toleransi Terlambat (Contoh: 08:00)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = jamPulang,
          onValueChange = { jamPulang = it },
          label = { Text("Jam Pulang Kerja (Contoh: 17:00)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = totalKaryawan,
          onValueChange = { totalKaryawan = it },
          label = { Text("Target Total Karyawan (Isi 0 untuk auto-hitung)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = pengumuman,
          onValueChange = { pengumuman = it },
          label = { Text("Pesan Pengumuman Kantor Resmi") },
          minLines = 2,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val baru = configSaatIni.copy(
            jamMasukKerja = jamMasuk.trim(),
            batasTerlambat = batasTerlambat.trim(),
            jamPulangKerja = jamPulang.trim(),
            pengumuman = pengumuman.trim(),
            totalKaryawan = totalKaryawan.toIntOrNull() ?: 0
          )
          onSimpan(baru)
        },
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
      ) {
        Text("Simpan ke Semua Device", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onBatal) {
        Text("Batal", color = TextMuted)
      }
    }
  )
}

// Dialog tambah master karyawan ke database
@Composable
fun DialogTambahMasterKaryawan(
  onSimpan: (nip: String, nama: String, departemen: String) -> Unit,
  onBatal: () -> Unit
) {
  var nip by remember { mutableStateOf("") }
  var nama by remember { mutableStateOf("") }
  var departemen by remember { mutableStateOf("Teknologi") }

  AlertDialog(
    containerColor = CardWhite,
    onDismissRequest = onBatal,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Tambah Karyawan Baru",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Karyawan yang didaftarkan di sini akan langsung tercatat di database MySQL XAMPP dan dapat dipakai untuk login/registrasi di smartphone karyawan.",
          fontSize = 11.sp,
          color = TextMuted,
          lineHeight = 15.sp
        )

        OutlinedTextField(
          value = nip,
          onValueChange = { nip = it },
          label = { Text("NIP Karyawan") },
          placeholder = { Text("Contoh: 19980512006") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = nama,
          onValueChange = { nama = it },
          label = { Text("Nama Lengkap Karyawan") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = departemen,
          onValueChange = { departemen = it },
          label = { Text("Departemen / Divisi") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (nip.isNotBlank() && nama.isNotBlank()) {
            onSimpan(nip.trim(), nama.trim(), departemen.trim())
          }
        },
        enabled = nip.isNotBlank() && nama.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
      ) {
        Text("Tambah ke Server", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onBatal) {
        Text("Batal", color = TextMuted)
      }
    }
  )
}
