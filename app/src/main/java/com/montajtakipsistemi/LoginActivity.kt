// LoginActivity.kt
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

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
        val tvKayitOl = findViewById<TextView>(R.id.tvKayitOlLink)

        tvKayitOl.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val sharedPref = getSharedPreferences("GirisBilgileri", Context.MODE_PRIVATE)
        val kayitliVeri = sharedPref.getString("mail", null)
        val kayitliSifre = sharedPref.getString("sifre", null)
        val hatirla = sharedPref.getBoolean("hatirla", false)

        if (hatirla && kayitliVeri != null) {
            etEmail.setText(kayitliVeri)
            etSifre.setText(kayitliSifre)
            cbBeniHatirla.isChecked = true
        }

        // --- GİRİŞ YAPMIŞ KULLANICIYI KONTROL ET ---
        if (auth.currentUser != null) {
            val email = auth.currentUser!!.email.toString()
            val uid = auth.currentUser!!.uid
            yonlendir(uid, email)
        }

        btnGiris.setOnClickListener {
            val hamVeri = etEmail.text.toString().trim()
            val sifre = etSifre.text.toString()
            var islenecekMail = hamVeri

            if (hamVeri.isNotEmpty() && sifre.isNotEmpty()) {
                if (!islenecekMail.contains("@")) {
                    val temizMail = mailIcinTemizle(hamVeri)
                    islenecekMail = temizMail.replace(" ", ".") + "@montajtakip.com"
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

                    val uid = auth.currentUser!!.uid
                    yonlendir(uid, islenecekMail)

                }.addOnFailureListener {
                    Toast.makeText(this, "Hata: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Lütfen alanları doldurunuz", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- YENİ YÖNLENDİRME FONKSİYONU (VERİTABANINA BAKAR) ---
    private fun yonlendir(uid: String, email: String) {
        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("Users").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Veritabanından rolü çek, bulamazsa varsayılan olarak "personel" yap
                val rol = snapshot.child("role").value?.toString() ?: "personel"

                val intent = if (rol == "admin" || email.contains("admin")) {
                    Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                } else {
                    Intent(this@LoginActivity, WorkerActivity::class.java).apply {
                        val duzenliIsim = email.substringBefore("@")
                            .split(".")
                            .joinToString(" ") { kelime ->
                                kelime.replaceFirstChar { it.uppercase() }
                            }
                        putExtra("ISCI_ISMI", duzenliIsim)
                    }
                }
                startActivity(intent)
                finish()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Rol okunamadı, personel girişi yapılıyor.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mailIcinTemizle(metin: String): String {
        var yeniMetin = metin.lowercase(Locale.ENGLISH)
        yeniMetin = yeniMetin.replace("ç", "c")
            .replace("ğ", "g")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ü", "u")
        return yeniMetin
    }
}