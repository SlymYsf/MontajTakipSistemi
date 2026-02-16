package com.montajtakipsistemi

import android.content.Intent // EKLENDİ
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // EKLENDİ
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth // EKLENDİ
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var workerAdapter: WorkerAdapter
    private val workerList = ArrayList<WorkerModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        recyclerView = findViewById(R.id.recyclerWorkers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        workerAdapter = WorkerAdapter(workerList)
        recyclerView.adapter = workerAdapter

        // HARİTAYA GİT BUTONU
        findViewById<Button>(R.id.btnHaritayaGit).setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // ÇIKIŞ YAP BUTONU

        findViewById<Button>(R.id.btnCikisYap).setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Firebase'den çık

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Bu sayfayı kapat, geri dönülemesin
        }

        verileriDinle()
    }

    private fun verileriDinle() {
        // DİKKAT: Buradaki linkin senin veritabanı linkinle aynı olduğundan emin ol
        val dbRef = FirebaseDatabase.getInstance("https://montajtakip-ebf80-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("WorkerStatus")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                workerList.clear()
                for (data in snapshot.children) {
                    val isim = data.child("isim").value?.toString() ?: ""
                    val durum = data.child("durum").value?.toString() ?: ""

                    if(isim.isNotEmpty()) {
                        workerList.add(WorkerModel(isim, durum))
                    }
                }
                workerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Hata durumu
            }
        })
    }
}

data class WorkerModel(val isim: String, val durum: String)

class WorkerAdapter(private val workerList: List<WorkerModel>) :
    RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    class WorkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvWorkerName)
        val tvStatus: TextView = view.findViewById(R.id.tvWorkerStatus)
        val cardStatus: CardView = view.findViewById(R.id.cardStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_worker_status, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val worker = workerList[position]
        holder.tvName.text = worker.isim
        holder.tvStatus.text = worker.durum

        if (worker.durum == "Çalışıyor") {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#4CAF50")) // Yeşil
        } else {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#F44336")) // Kırmızı
        }
    }

    override fun getItemCount() = workerList.size
}