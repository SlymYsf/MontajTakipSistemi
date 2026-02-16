package com.montajtakipsistemi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
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

        // --- 👇 DÜZELTİLEN KISIM BURASI 👇 ---
        // Senin XML dosyanı inceledim, ID'si: tvKayitOlLink
        // Biz de burada aynısını kullanıyoruz:
        val tvKayitOl = findViewById<TextView>(R.id.tvKayitOlLink)

        tvKayitOl.setOnClickListener {
            // Kayıt Ekranına Git
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        // -------------------------------------

        // --- BENİ HATIRLA ve GİRİŞ İŞLEMLERİ (AYNI KALDI) ---
        val sharedPref = getSharedPreferences("GirisBilgileri", Context.MODE_PRIVATE)
        val kayitliVeri = sharedPref.getString("mail", null)
        val kayitliSifre = sharedPref.getString("sifre", null)
        val hatirla = sharedPref.getBoolean("hatirla", false)

        if (hatirla && kayitliVeri != null) {
            etEmail.setText(kayitliVeri)
            etSifre.setText(kayitliSifre)
            cbBeniHatirla.isChecked = true
        }

        if (auth.currentUser != null) {
            val email = auth.currentUser!!.email.toString()
            yonlendir(email)
        }

        btnGiris.setOnClickListener {
            val hamVeri = etEmail.text.toString().trim()
            val sifre = etSifre.text.toString()
            var islenecekMail = hamVeri

            if (hamVeri.isNotEmpty() && sifre.isNotEmpty()) {
                if (!islenecekMail.contains("@")) {
                    islenecekMail = islenecekMail.replace(" ", ".") + "@montajtakip.com"
                }

                auth.signInWithEmailAndPassword(islenecekMail, sifre).addOnSuccessListener {
                    val editor = sharedPref.edit()
                    if (cbBeniHatirla.isChecked) {
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