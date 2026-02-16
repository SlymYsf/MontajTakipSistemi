package com.montajtakipsistemi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etSifre: EditText
    private lateinit var cbBeniHatirla: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etKullaniciAdi)
        etSifre = findViewById(R.id.etPassword)
        cbBeniHatirla = findViewById(R.id.cbBeniHatirla)
        val btnGiris = findViewById<Button>(R.id.btnLogin)

        // --- BENİ HATIRLA ---
        val sharedPref = getSharedPreferences("GirisBilgileri", Context.MODE_PRIVATE)
        val kayitliVeri = sharedPref.getString("mail", null) // Artık buraya "admin 1" gelecek
        val kayitliSifre = sharedPref.getString("sifre", null)
        val hatirla = sharedPref.getBoolean("hatirla", false)

        if (hatirla && kayitliVeri != null) {
            etEmail.setText(kayitliVeri) // Kutucuğa kısa ismi yaz
            etSifre.setText(kayitliSifre)
            cbBeniHatirla.isChecked = true
        }

        // Eğer kullanıcı zaten oturum açmışsa, tekrar giriş yapmasın
        if (auth.currentUser != null) {
            val email = auth.currentUser!!.email.toString()
            yonlendir(email)
        }

        btnGiris.setOnClickListener {
            // 1. Senin yazdığın orjinal veriyi al (Örn: "admin 1")
            val hamVeri = etEmail.text.toString().trim()
            val sifre = etSifre.text.toString()

            // 2. İşlem yapacağımız geçici değişkeni oluştur
            var islenecekMail = hamVeri

            if (hamVeri.isNotEmpty() && sifre.isNotEmpty()) {

                // Eğer @ yoksa, mail formatına çevir
                if (!islenecekMail.contains("@")) {
                    islenecekMail = islenecekMail.replace(" ", ".") + "@montajtakip.com"
                }

                // Giriş işlemini ÇEVRİLMİŞ mail ile yap (Firebase bunu ister)
                auth.signInWithEmailAndPassword(islenecekMail, sifre).addOnSuccessListener {

                    val editor = sharedPref.edit()
                    if (cbBeniHatirla.isChecked) {
                        // DİKKAT: Hafızaya ÇEVRİLMİŞ olani değil, HAM veriyi kaydet!
                        // Böylece bir dahaki sefere "admin 1" olarak hatırlanır.
                        editor.putString("mail", hamVeri)
                        editor.putString("sifre", sifre)
                        editor.putBoolean("hatirla", true)
                    } else {
                        editor.clear()
                    }
                    editor.apply()

                    yonlendir(islenecekMail)

                }.addOnFailureListener {
                    Toast.makeText(this, "Hata: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Lütfen alanları doldurunuz", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun yonlendir(email: String) {
        val intent = if (email.contains("admin")) {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, WorkerActivity::class.java).apply {
                putExtra("ISCI_ISMI", email.substringBefore("@"))
            }
        }
        startActivity(intent)
        finish()
    }
}