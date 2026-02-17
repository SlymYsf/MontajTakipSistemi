package com.montajtakipsistemi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Harita Fragmentini Bağla
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Geri Dön (Home) Butonu
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // --- 1. KAMERA AYARI (Daha Yakın Başlasın) ---
        // 10f çok uzaktı, 15f yaptık (Mahalle görünümü)
        val fabrikaKonum = LatLng(37.8279885, 29.3325512)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fabrikaKonum, 15f))

        // Personelleri Getir ve Çiz
        personelleriHaritayaEkle()
    }

    private fun personelleriHaritayaEkle() {
        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("WorkerStatus")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear() // Haritayı temizle (Yeni veri gelince eskileri sil)

                for (data in snapshot.children) {
                    val uid = data.key ?: continue // Kullanıcı ID'si (Ayrıştırma için lazım)

                    val isim = data.child("isim").value.toString()
                    val lat = data.child("latitude").value.toString().toDoubleOrNull() ?: 0.0
                    val lng = data.child("longitude").value.toString().toDoubleOrNull() ?: 0.0
                    val durum = data.child("durum").value.toString()

                    if (lat != 0.0 && lng != 0.0) {

                        // --- KRİTİK NOKTA: ÇAKIŞMAYI ÖNLEME ---
                        // İşçinin gerçek konumunu alıp, ekranda göstermek için
                        // hafifçe sağa sola kaydırıyoruz.
                        val guncelKonum = cakismayiOnle(uid, lat, lng)

                        // İsmin Baş Harflerini Al (Ahmet Yılmaz -> AY)
                        val basHarfler = isimdenBasHarfAl(isim)

                        // Turuncu Yuvarlak İkonu Oluştur
                        val ozelIcon = createFixedSizeMarker(this@MapActivity, basHarfler)

                        // Haritaya Ekle
                        mMap.addMarker(MarkerOptions()
                            .position(guncelKonum) // <-- Dikkat: Kaydırılmış konumu kullanıyoruz
                            .title("$isim ($durum)")
                            .icon(BitmapDescriptorFactory.fromBitmap(ozelIcon)))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * --- ÇAKIŞMAYI ÖNLEME SİHİRBAZI ---
     * İki işçi aynı GPS noktasındaysa (aynı oda, aynı masa vb.)
     * onları haritada üst üste bindirmemek için ID'lerine göre
     * matematiksel olarak birbirinden uzaklaştırır.
     */
    private fun cakismayiOnle(uid: String, lat: Double, lng: Double): LatLng {
        // ID'den benzersiz bir sayı üret
        val hash = uid.hashCode()

        // --- AYAR: NE KADAR UZAĞA GİTSİN? ---
        // 0.0005 = Yaklaşık 50-60 metre (Haritada net ayrılır)
        val sapmaMiktari = 0.0005

        // -1 ile +1 arasında sabit bir yön belirle
        // (Math.sin ve cos kullanarak dairesel dağıtırız)
        val latYon = Math.sin(hash.toDouble())
        val lngYon = Math.cos(hash.toDouble())

        // Orijinal konuma bu sapmayı ekle
        val yeniLat = lat + (latYon * sapmaMiktari)
        val yeniLng = lng + (lngYon * sapmaMiktari)

        return LatLng(yeniLat, yeniLng)
    }

    // --- ÖZEL MARKER TASARIMI (TURUNCU YUVARLAK) ---
    private fun createFixedSizeMarker(context: Context, text: String): Bitmap {
        val resources = context.resources
        val scale = resources.displayMetrics.density

        val diameterDp = 40
        val diameterPx = (diameterDp * scale).toInt()
        val radius = diameterPx / 2f

        val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5722") // Turuncu Renk
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius, radius, radius, circlePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16 * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textYOffset = radius - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, radius, textYOffset, textPaint)

        return bitmap
    }

    // --- İSİMDEN BAŞ HARF ALMA ---
    private fun isimdenBasHarfAl(isim: String): String {
        if (isim.isEmpty()) return "?"
        val kelimeler = isim.trim().split("\\s+".toRegex())
        var sonuc = ""

        if (kelimeler.isNotEmpty()) {
            if (kelimeler[0].isNotEmpty()) {
                sonuc += kelimeler[0][0].uppercase()
            }
            if (kelimeler.size > 1 && kelimeler.last().isNotEmpty()) {
                sonuc += kelimeler.last()[0].uppercase()
            }
        }
        return sonuc
    }
}