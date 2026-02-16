package com.montajtakipsistemi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton // ImageButton eklendi
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

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // --- DÜZELTME BURADA YAPILDI ---
        // Eski "btnPersonelListesi" yerine yeni "btnHome" butonunu tanımladık.
        // Türünü de "ImageButton" olarak belirttik.
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish() // Harita sayfasını kapat
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 1. KAMERA AYARI (Fabrikaya odaklansın)
        val fabrikaKonum = LatLng(37.8279885, 29.3325512)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fabrikaKonum, 10f))

        // 2. PERSONELLERİ GETİR
        personelleriHaritayaEkle()
    }

    private fun personelleriHaritayaEkle() {
        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("WorkerStatus")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear() // Haritayı temizle

                for (data in snapshot.children) {
                    val isim = data.child("isim").value.toString()
                    val lat = data.child("latitude").value.toString().toDoubleOrNull() ?: 0.0
                    val lng = data.child("longitude").value.toString().toDoubleOrNull() ?: 0.0
                    val durum = data.child("durum").value.toString()

                    if (lat != 0.0 && lng != 0.0) {
                        val konum = LatLng(lat, lng)

                        // İSMİN BAŞ HARFLERİNİ AL (Örn: Ahmet Yılmaz -> AY)
                        val basHarfler = isimdenBasHarfAl(isim)

                        // SABİT BOYUTLU MARKER OLUŞTUR
                        val ozelIcon = createFixedSizeMarker(this@MapActivity, basHarfler)

                        mMap.addMarker(MarkerOptions()
                            .position(konum)
                            .title("$isim ($durum)")
                            .icon(BitmapDescriptorFactory.fromBitmap(ozelIcon)))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- SABİT BOYUTLU MARKER (AYNI KALDI) ---
    private fun createFixedSizeMarker(context: Context, text: String): Bitmap {
        val resources = context.resources
        val scale = resources.displayMetrics.density

        val diameterDp = 40
        val diameterPx = (diameterDp * scale).toInt()
        val radius = diameterPx / 2f

        val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5722") // Turuncu
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

    private fun isimdenBasHarfAl(isim: String): String {
        if (isim.isEmpty()) return "?"
        val kelimeler = isim.trim().split("\\s+".toRegex())
        var sonuc = ""

        if (kelimeler.isNotEmpty()) {
            sonuc += kelimeler[0][0].uppercase()
            if (kelimeler.size > 1) {
                sonuc += kelimeler.last()[0].uppercase()
            }
        }
        return sonuc
    }
}
