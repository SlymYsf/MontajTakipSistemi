package com.montajtakipsistemi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WorkerActivity : AppCompatActivity(), LocationListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvDurum: TextView
    private lateinit var locationManager: LocationManager
    private var isTracking = false // Takip ediliyor mu?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker)

        auth = FirebaseAuth.getInstance()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val tvHosgeldin = findViewById<TextView>(R.id.tvWorkerWelcome)
        tvDurum = findViewById<TextView>(R.id.tvWorkStatus)
        val btnIsBasi = findViewById<Button>(R.id.btnStartWork)
        val btnPaydos = findViewById<Button>(R.id.btnStopWork)
        val btnCikis = findViewById<Button>(R.id.btnGuvenliCikis)

        // Giriş yapan kişinin ismini al (Login'den gelen veri)
        val isciIsmi = intent.getStringExtra("ISCI_ISMI") ?: "Personel"
        tvHosgeldin.text = "Hoşgeldin, $isciIsmi"

        // İŞ BAŞI BUTONU
        btnIsBasi.setOnClickListener {
            if (konumIzniVarMi()) {
                durumGuncelle(isciIsmi, "Çalışıyor")
                konumTakibiniBaslat()
            } else {
                konumIzniIste()
            }
        }

        // PAYDOS BUTONU
        btnPaydos.setOnClickListener {

            durumGuncelle(isciIsmi, "Çalışmıyor")
            konumTakibiniDurdur()
        }

        // ÇIKIŞ BUTONU
        btnCikis.setOnClickListener {
            konumTakibiniDurdur() // Çıkarken takibi bırak
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // --- KONUM TAKİBİNİ BAŞLAT ---
    private fun konumTakibiniBaslat() {
        if (isTracking) return // Zaten takip ediliyorsa tekrar başlatma

        try {
            // GPS ve Ağ sağlayıcılarından konum iste (Min: 10 metrede bir veya 10 saniyede bir)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 10f, this)

            isTracking = true
            Toast.makeText(this, "Konum takibi başladı...", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- KONUM TAKİBİNİ DURDUR ---
    private fun konumTakibiniDurdur() {
        if (isTracking) {
            locationManager.removeUpdates(this)
            isTracking = false
            Toast.makeText(this, "Takip durduruldu.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- KONUM DEĞİŞTİKÇE BURASI ÇALIŞIR ---
    override fun onLocationChanged(location: Location) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Firebase'e Enlem ve Boylamı Gönder
        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("WorkerStatus").child(uid)

        val konumMap = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )

        dbRef.updateChildren(konumMap)
    }

    // Firebase Durum Güncelleme (Çalışıyor / Paydos)
    private fun durumGuncelle(isim: String, durum: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("WorkerStatus").child(uid)

        val veriMap = mapOf(
            "isim" to isim,
            "durum" to durum
        )

        dbRef.updateChildren(veriMap)
        tvDurum.text = "Durum: $durum"
    }

    // --- İZİN KONTROLLERİ ---
    private fun konumIzniVarMi(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun konumIzniIste() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
    }

    // Konum kapalıysa vs. bu fonksiyonlar boş kalabilir
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}