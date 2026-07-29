package com.estehkasir

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Product(var id:Long,var name:String,var category:String,var price:Int,var stock:Int,var active:Boolean=true)
data class Sale(val id:Long,val date:String,val items:String,val total:Int,val paid:Int,val change:Int)

class MainActivity:Activity(){
 private val prefs by lazy{getSharedPreferences("estehkasir21",MODE_PRIVATE)}
 private val products=mutableListOf<Product>(); private val cart=mutableMapOf<Long,Int>(); private val sales=mutableListOf<Sale>()
 private val rupiah=NumberFormat.getCurrencyInstance(Locale("id","ID"))
 private val df=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("id","ID"))
 private val stack=ArrayDeque<()->Unit>(); private var current:(()->Unit)?=null

 override fun onCreate(b:Bundle?){super.onCreate(b);loadProducts();loadSales();dashboard(false)}
 override fun onBackPressed(){if(stack.isNotEmpty()){current=stack.removeLast();current!!()}else super.onBackPressed()}
 private fun nav(f:()->Unit){current?.let{stack.addLast(it)};current=f;f()}
 private fun base()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20);setBackgroundColor(Color.rgb(248,250,248))}
 private fun page(t:String)=base().apply{val h=LinearLayout(this@MainActivity).apply{gravity=Gravity.CENTER_VERTICAL};h.addView(TextView(this@MainActivity).apply{text=t;textSize=24f;setTextColor(Color.rgb(35,85,45))},LinearLayout.LayoutParams(0,-2,1f));h.addView(Button(this@MainActivity).apply{text="← Kembali";setOnClickListener{onBackPressed()}});addView(h)}
 private fun scroll(v:View)=ScrollView(this).apply{addView(v)}
 private fun btn(s:String,f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()}}
 private fun store()=prefs.getString("store","Es Teh Kasir")?:"Es Teh Kasir"
 private fun loadProducts(){products.clear();val r=prefs.getString("products",null);if(r==null){products.addAll(listOf(Product(1,"Es Teh Jumbo","Es Teh",8000,100),Product(2,"Es Jeruk","Minuman",7000,100),Product(3,"Matcha","Minuman",12000,100),Product(4,"Thai Tea","Minuman",10000,100),Product(5,"Green Tea","Minuman",10000,100));saveProducts()}else r.split("||").filter{it.isNotBlank()}.forEach{val x=it.split("|");if(x.size>=6)products.add(Product(x[0].toLong(),x[1],x[2],x[3].toInt(),x[4].toInt(),x[5]=="1"))}}
 private fun saveProducts(){prefs.edit().putString("products",products.joinToString("||"){"${it.id}|${it.name}|${it.category}|${it.price}|${it.stock}|${if(it.active)1 else 0}"}).apply()}
 private fun loadSales(){sales.clear();(prefs.getString("sales",null)?:"").split("|||").filter{it.isNotBlank()}.forEach{val x=it.split("##");if(x.size>=6)sales.add(Sale(x[0].toLong(),x[1],x[2],x[3].toInt(),x[4].toInt(),x[5].toInt()))}}
 private fun saveSales(){prefs.edit().putString("sales",sales.joinToString("|||"){"${it.id}##${it.date}##${it.items}##${it.total}##${it.paid}##${it.change}"}).apply()}

 private fun dashboard(push:Boolean){if(push)nav{dashboard(false)};val r=base();r.addView(TextView(this).apply{text="🧋 ${store()}";textSize=26f;setTextColor(Color.rgb(35,85,45))});r.addView(TextView(this).apply{text="Es Teh Kasir v2.1 • ${df.format(Date())}";setPadding(0,0,0,15)})
  r.addView(btn("🛒 KASIR"){nav{cashier()}});r.addView(btn("📦 KELOLA PRODUK"){nav{productsPage()}});r.addView(btn("🧾 RIWAYAT TRANSAKSI"){nav{history()}});r.addView(btn("📊 LAPORAN HARIAN"){nav{report()}});r.addView(btn("⚙️ PENGATURAN"){nav{settings()}})
  setContentView(scroll(r));current={dashboard(false)}
 }

 private fun cashier(){val r=page("🛒 Kasir");val search=EditText(this).apply{hint="🔎 Cari minuman"};val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val cartBox=TextView(this).apply{textSize=16f};val total=TextView(this).apply{textSize=20f};val pay=EditText(this).apply{hint="Uang diterima (Rp)";inputType=2}
  fun render(q:String=""){list.removeAllViews();products.filter{it.active&&it.name.contains(q,true)}.forEach{p->list.addView(btn("${p.name}\n${rupiah.format(p.price)} • Stok ${p.stock}"){val n=cart[p.id]?:0;if(n<p.stock){cart[p.id]=n+1;refresh(cartBox,total)}else Toast.makeText(this,"Stok habis",0).show()})}}
  search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){render(s.toString())};override fun afterTextChanged(e:Editable?) {}})
  r.addView(search);r.addView(scroll(list),LinearLayout.LayoutParams(-1,0,1f));r.addView(cartBox);r.addView(total);r.addView(pay)
  r.addView(btn("SELESAIKAN TRANSAKSI"){val t=cart.entries.sumOf{(id,q)->products.find{it.id==id}?.price?.times(q)?:0};val p=pay.text.toString().toIntOrNull()?:0;if(t==0)Toast.makeText(this,"Keranjang kosong",0).show()else if(p<t)Toast.makeText(this,"Pembayaran kurang",0).show()else{cart.forEach{(id,q)->products.find{it.id==id}?.stock-=q};val items=cart.entries.mapNotNull{(id,q)->products.find{it.id==id}?.let{"${it.name} x$q"}}.joinToString(", ");val s=Sale(System.currentTimeMillis(),df.format(Date()),items,t,p,p-t);sales.add(s);saveProducts();saveSales();cart.clear();pay.text.clear();refresh(cartBox,total);receipt(s)}})
  r.addView(btn("Kosongkan Keranjang"){cart.clear();refresh(cartBox,total)});render();refresh(cartBox,total);setContentView(r);current={cashier()}}
 private fun refresh(c:TextView,t:TextView){c.text=if(cart.isEmpty())"Keranjang kosong" else "Pesanan:\n"+cart.entries.mapNotNull{(id,q)->products.find{it.id==id}?.let{"${it.name} x$q = ${rupiah.format(it.price*q)}"}}.joinToString("\n");t.text="TOTAL: ${rupiah.format(cart.entries.sumOf{(id,q)->products.find{it.id==id}?.price?.times(q)?:0})}"}

 private fun productsPage(){val r=page("📦 Kelola Produk");val wrap=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val search=EditText(this).apply{hint="🔎 Cari produk"};val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};wrap.addView(btn("➕ Tambah Minuman"){dialog(null){productsPage()}});wrap.addView(search);wrap.addView(list)
  fun render(q:String=""){list.removeAllViews();products.filter{it.name.contains(q,true)||it.category.contains(q,true)}.forEach{p->list.addView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,8,0,14);addView(TextView(this@MainActivity).apply{text="${p.name}\n${p.category} • ${rupiah.format(p.price)} • Stok ${p.stock} • ${if(p.active)"Aktif" else "Nonaktif"}";textSize=16f});addView(btn("✏️ Edit"){dialog(p){productsPage()}});addView(btn(if(p.active)"🔴 Nonaktifkan" else "🟢 Aktifkan"){p.active=!p.active;saveProducts();render(q)});addView(btn("🗑️ Hapus"){products.removeIf{it.id==p.id};saveProducts();render(q)})})}}
  search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){render(s.toString())};override fun afterTextChanged(e:Editable?) {}});render();r.addView(scroll(wrap),LinearLayout.LayoutParams(-1,0,1f));setContentView(r);current={productsPage()}}
 private fun dialog(ex:Product?,done:()->Unit){val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,10,30,10)};val n=EditText(this).apply{hint="Nama minuman";setText(ex?.name?:"")};val c=EditText(this).apply{hint="Kategori";setText(ex?.category?:"Minuman")};val p=EditText(this).apply{hint="Harga";inputType=2;setText(ex?.price?.toString()?:"")};val s=EditText(this).apply{hint="Stok";inputType=2;setText(ex?.stock?.toString()?:"100")};l.addView(n);l.addView(c);l.addView(p);l.addView(s);AlertDialog.Builder(this).setTitle(if(ex==null)"Tambah Minuman" else "Edit Minuman").setView(l).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->val pr=p.text.toString().toIntOrNull()?:0;if(n.text.isNotBlank()&&pr>0){if(ex==null)products.add(Product(System.currentTimeMillis(),n.text.toString(),c.text.toString().ifBlank{"Minuman"},pr,s.text.toString().toIntOrNull()?:0))else{ex.name=n.text.toString();ex.category=c.text.toString();ex.price=pr;ex.stock=s.text.toString().toIntOrNull()?:0};saveProducts();done()}}.show()}

 private fun receipt(s:Sale){val r=page("🧾 Struk");r.addView(TextView(this).apply{text="${store()}\n${s.date}\n\n${s.items}\n\nTOTAL ${rupiah.format(s.total)}\nBAYAR ${rupiah.format(s.paid)}\nKEMBALI ${rupiah.format(s.change)}\n\nTerima kasih 🙏";textSize=18f});r.addView(btn("📤 Bagikan Struk"){share(s)});setContentView(scroll(r));current={receipt(s)}}
 private fun share(s:Sale){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"${store()}\n${s.date}\n${s.items}\nTOTAL ${rupiah.format(s.total)}\nBAYAR ${rupiah.format(s.paid)}\nKEMBALI ${rupiah.format(s.change)}")}, "Bagikan struk"))}
 private fun history(){val r=page("🧾 Riwayat Transaksi");val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};sales.asReversed().forEach{s->l.addView(TextView(this).apply{text="${s.date}\n${s.items}\nTotal ${rupiah.format(s.total)}";textSize=16f;setPadding(0,8,0,14)})};r.addView(scroll(l),LinearLayout.LayoutParams(-1,0,1f));setContentView(r);current={history()}}
 private fun report(){val r=page("📊 Laporan Harian");val d=SimpleDateFormat("dd/MM/yyyy",Locale("id","ID")).format(Date());val x=sales.filter{it.date.startsWith(d)};r.addView(TextView(this).apply{text="Tanggal: $d\nTransaksi: ${x.size}\nOmzet: ${rupiah.format(x.sumOf{it.total})}";textSize=19f});setContentView(scroll(r));current={report()}}
 private fun settings(){val r=page("⚙️ Pengaturan");val n=EditText(this).apply{hint="Nama toko";setText(store())};r.addView(n);r.addView(btn("💾 Simpan"){prefs.edit().putString("store",n.text.toString().ifBlank{"Es Teh Kasir"}).apply();dashboard(false)});setContentView(scroll(r));current={settings()}}
}
