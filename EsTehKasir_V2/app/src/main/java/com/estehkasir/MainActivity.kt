package com.estehkasir

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.content.*
import android.view.*
import android.widget.*
import java.text.NumberFormat
import java.util.Locale

data class Product(var id: Long, var name: String, var category: String, var price: Int, var stock: Int, var active: Boolean = true)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("estehkasir", MODE_PRIVATE) }
    private val products = mutableListOf<Product>()
    private val cart = mutableMapOf<Long, Int>()
    private val rupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private var screen: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadProducts()
        showDashboard()
    }

    private fun loadProducts() {
        products.clear()
        val raw = prefs.getString("products", null)
        if (raw.isNullOrBlank()) {
            products.addAll(listOf(
                Product(1,"Es Teh Jumbo","Es Teh",8000,100),
                Product(2,"Es Jeruk","Minuman",7000,100),
                Product(3,"Matcha","Minuman",12000,100),
                Product(4,"Thai Tea","Minuman",10000,100),
                Product(5,"Green Tea","Minuman",10000,100)
            ))
            saveProducts()
        } else {
            raw.split("||").filter { it.isNotBlank() }.forEach {
                val x = it.split("|")
                if (x.size >= 6) products.add(Product(x[0].toLong(),x[1],x[2],x[3].toInt(),x[4].toInt(),x[5]=="1"))
            }
        }
    }

    private fun saveProducts() {
        val raw = products.joinToString("||") { "${it.id}|${it.name}|${it.category}|${it.price}|${it.stock}|${if(it.active) 1 else 0}" }
        prefs.edit().putString("products", raw).apply()
    }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24,24,24,24)
        setBackgroundColor(Color.rgb(248,250,248))
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text; textSize = 26f; setTextColor(Color.rgb(35,85,45)); setPadding(0,0,0,18)
    }

    private fun btn(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; setOnClickListener { action() }
    }

    private fun showDashboard() {
        val root = base()
        root.addView(title("🧋 Es Teh Kasir v2.0"))
        root.addView(TextView(this).apply { text="Kasir offline • 1 toko"; textSize=15f })
        root.addView(btn("🛒 KASIR") { showCashier() })
        root.addView(btn("📦 KELOLA PRODUK") { showProducts() })
        root.addView(btn("📊 LAPORAN") { showReport() })
        root.addView(btn("ℹ️ Tentang") { Toast.makeText(this,"Es Teh Kasir v2.0",Toast.LENGTH_SHORT).show() })
        setContentView(root)
    }

    private fun showCashier() {
        val root = base()
        root.addView(title("🛒 Kasir"))
        val cartBox = TextView(this).apply { textSize=16f; setPadding(0,15,0,10) }
        val totalBox = TextView(this).apply { textSize=20f; setTextColor(Color.rgb(35,85,45)) }
        val payment = EditText(this).apply { hint="Uang diterima (Rp)"; inputType=2 }

        fun refresh() {
            val lines = cart.entries.mapNotNull { (id,q) ->
                val p=products.find{it.id==id} ?: return@mapNotNull null
                "• ${p.name} x$q = ${rupiah.format(p.price*q)}"
            }
            val total=cart.entries.sumOf { (id,q)->products.find{it.id==id}?.price?.times(q) ?: 0 }
            cartBox.text=if(lines.isEmpty()) "Keranjang kosong" else "Pesanan:\n"+lines.joinToString("\n")
            totalBox.text="TOTAL: ${rupiah.format(total)}"
        }

        products.filter{it.active}.forEach { p ->
            val b=btn("${p.name}\n${rupiah.format(p.price)} • Stok ${p.stock}") {
                val qty=cart[p.id] ?: 0
                if(qty < p.stock) cart[p.id]=qty+1 else Toast.makeText(this,"Stok habis",Toast.LENGTH_SHORT).show()
                refresh()
            }
            root.addView(b)
        }
        root.addView(cartBox); root.addView(totalBox); root.addView(payment)
        root.addView(btn("SELESAIKAN TRANSAKSI") {
            val total=cart.entries.sumOf{(id,q)->products.find{it.id==id}?.price?.times(q)?:0}
            val paid=payment.text.toString().toIntOrNull()?:0
            if(total==0) Toast.makeText(this,"Keranjang kosong",Toast.LENGTH_SHORT).show()
            else if(paid<total) Toast.makeText(this,"Pembayaran kurang",Toast.LENGTH_SHORT).show()
            else {
                cart.forEach{(id,q)->products.find{it.id==id}?.let{p->p.stock-=q}}
                saveProducts()
                val change=paid-total
                val sales=prefs.getInt("sales",0)+total
                prefs.edit().putInt("sales",sales).putInt("transactions",prefs.getInt("transactions",0)+1).apply()
                cart.clear(); payment.text.clear(); refresh()
                Toast.makeText(this,"Transaksi berhasil\nKembalian ${rupiah.format(change)}",Toast.LENGTH_LONG).show()
            }
        })
        root.addView(btn("Kosongkan Keranjang"){cart.clear();refresh()})
        root.addView(btn("← Kembali"){showDashboard()})
        refresh(); setContentView(root)
    }

    private fun showProducts() {
        val root=base()
        root.addView(title("📦 Kelola Produk"))
        root.addView(btn("➕ Tambah Minuman"){productDialog(null){showProducts()}})
        products.forEach { p ->
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,10,0,10)}
            box.addView(TextView(this).apply{text="${p.name}\n${p.category} • ${rupiah.format(p.price)} • Stok ${p.stock} • ${if(p.active)"Aktif" else "Nonaktif"}";textSize=16f})
            box.addView(btn("✏️ Edit"){productDialog(p){showProducts()}})
            box.addView(btn(if(p.active)"🔴 Nonaktifkan" else "🟢 Aktifkan"){
                p.active=!p.active;saveProducts();showProducts()
            })
            box.addView(btn("🗑️ Hapus"){
                products.removeIf{it.id==p.id};cart.remove(p.id);saveProducts();showProducts()
            })
            root.addView(box)
        }
        root.addView(btn("← Kembali"){showDashboard()})
        setContentView(root)
    }

    private fun productDialog(existing: Product?, done: () -> Unit) {
        val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,10,40,10)}
        val name=EditText(this).apply{hint="Nama minuman";setText(existing?.name?:"")}
        val cat=EditText(this).apply{hint="Kategori";setText(existing?.category?:"Minuman")}
        val price=EditText(this).apply{hint="Harga (Rp)";inputType=2;setText(existing?.price?.toString()?:"")}
        val stock=EditText(this).apply{hint="Stok";inputType=2;setText(existing?.stock?.toString()?:"100")}
        layout.addView(name);layout.addView(cat);layout.addView(price);layout.addView(stock)
        AlertDialog.Builder(this).setTitle(if(existing==null)"Tambah Minuman" else "Edit Minuman")
            .setView(layout).setNegativeButton("Batal",null)
            .setPositiveButton("Simpan"){_,_->
                val n=name.text.toString().trim(); val pr=price.text.toString().toIntOrNull()?:0; val st=stock.text.toString().toIntOrNull()?:0
                if(n.isNotEmpty()&&pr>0) {
                    if(existing==null) products.add(Product(System.currentTimeMillis(),n,cat.text.toString().ifBlank{"Minuman"},pr,st))
                    else {existing.name=n;existing.category=cat.text.toString().ifBlank{"Minuman"};existing.price=pr;existing.stock=st}
                    saveProducts();done()
                }
            }.show()
    }

    private fun showReport() {
    val root = base()

    root.addView(title("📊 Laporan"))

    val summary = TextView(this)
    summary.text =
        "Total omzet tersimpan: ${rupiah.format(prefs.getInt("sales", 0))}\n" +
        "Transaksi: ${prefs.getInt("transactions", 0)}\n\n" +
        "Stok produk:"
    summary.textSize = 18f
    root.addView(summary)

    products.forEach { p ->
        val item = TextView(this)
        item.text = "• ${p.name}: ${p.stock}"
        item.textSize = 16f
        item.setPadding(0, 10, 0, 10)
        root.addView(item)
    }

    root.addView(btn("← Kembali") {
        showDashboard()
    })

    setContentView(root)
  }
}
