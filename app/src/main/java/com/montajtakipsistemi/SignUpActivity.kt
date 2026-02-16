package com.montajtakipsistemi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException // <-- BU EKLENDİ (ÖNEMLİ)
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val etAd = findViewById<EditText>(R.id.etAd)
        val etSoyad = findViewById<EditText>(R.id.etSoyad)
        val etSifre = findViewById<EditText>(R.id.etKayitSifre)
        val etSifreTekrar = findViewById<EditText>(R.id.etKayitSifreTekrar)
        val btnKayit = findViewById<Button>(R.id.btnKayitOl)
        val tvGirisDon = findViewById<TextView>(R.id.tvGirisDon)

        btnKayit.setOnClickListener {
            val ad = etAd.text.toString().trim()
            val soyad = etSoyad.text.toString().trim()
            val sifre = etSifre.text.toString().trim()
            val sifreTekrar = etSifreTekrar.text.toString().trim()

            if (ad.isEmpty() || soyad.isEmpty() || sifre.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sifre != sifreTekrar) {
                // Şifreler uyuşmazsa kutuyu kırmızı yapıp uyarı verelim
                etSifreTekrar.error = "Şifreler uyuşmuyor!"
                etSifreTekrar.requestFocus()
                return@setOnClickListener
            }

            // Arka planda mail oluşturma
            val temizAd = ingilizceKaraktereCevir(ad)
            val temizSoyad = ingilizceKaraktereCevir(soyad)
            val olusturulanEmail = "$temizAd.$temizSoyad@montajtakip.com"

            // KAYIT İŞLEMİ VE ÖZEL HATA MESAJI
            auth.createUserWithEmailAndPassword(olusturulanEmail, sifre)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // --- BAŞARILI KAYIT ---
                        val user = auth.currentUser
                        val uid = user!!.uid

                        // İsim Güncelleme
                        val profilGuncelleme = UserProfileChangeRequest.Builder()
                            .setDisplayName("$ad $soyad")
                            .build()
                        user.updateProfile(profilGuncelleme)

                        // Veritabanına Rol Ekleme (Varsayılan: personel)
                        val kullaniciBilgileri = hashMapOf(
                            "adSoyad" to "$ad $soyad",
                            "email" to olusturulanEmail,
                            "role" to "personel"
                        )

                        // URL'yi senin resimden aldım, aynen buraya yapıştırıyoruz:
                        FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app").getReference("Users")
                            .child(uid)
                            .setValue(kullaniciBilgileri)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Kayıt Başarılı! Hoşgeldin $ad  $soyad. ", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Hata olursa nedenini görelim
                                Toast.makeText(this, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                    } else {
                        // --- HATA YAKALAMA KISMI (BURASI DEĞİŞTİ) ---
                        val hata = task.exception

                        // Eğer hata "Bu kullanıcı zaten var" hatası ise:
                        if (hata is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Bu isim ve soyisimle zaten kayıtlı bir personel var!", Toast.LENGTH_LONG).show()
                        }
                        else {
                            // Başka bir hataysa (İnternet yok, şifre çok kısa vs.)
                            Toast.makeText(this, "Hata: ${hata?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        tvGirisDon.setOnClickListener { finish() }
    }

    private fun ingilizceKaraktereCevir(metin: String): String {
        var yeniMetin = metin.lowercase(Locale.ENGLISH)
        yeniMetin = yeniMetin.replace("ç", "c")
            .replace("ğ", "g")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ü", "u")
            .replace(" ", "")
        return yeniMetin
    }
}