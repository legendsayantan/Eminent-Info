package com.legendsayantan.eminentinfo

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.legendsayantan.eminentinfo.data.Account
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc
import com.legendsayantan.eminentinfo.utils.Scrapers
import com.rajat.pdfviewer.PdfRendererView
import java.text.SimpleDateFormat
import java.util.Calendar

class NoticeView : AppCompatActivity() {
    lateinit var url: String;
    var date = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_notice)
        url = intent.getStringExtra("url")!!
        date = intent.getLongExtra("date", System.currentTimeMillis())
        findViewById<TextView>(R.id.title)?.text =
            Misc.extractNoticeName(url) ?: "Notice - ${SimpleDateFormat("dd/MM/YYYY").format(date)}"
        val pdfView = findViewById<PdfRendererView>(R.id.pdf)!!
        val imageView = findViewById<ImageView>(R.id.image)!!
        val external = findViewById<ImageView>(R.id.external)
        external.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        fetchIfExpired(date, url) {
            url = it
            if(url.isEmpty()) {
                Toast.makeText(this, "Notice not found", Toast.LENGTH_SHORT).show()
                finish()
            }
            else if (url.contains(".pdf")) {
                imageView.visibility = View.GONE
                try {
                    pdfView.initWithUrl(
                        url = url,
                        lifecycle = lifecycle,
                        lifecycleCoroutineScope = lifecycleScope,
                    )
                } catch (e: Exception) {
                    finish()
                }
            } else {
                pdfView.visibility = View.GONE
                Glide
                    .with(this)
                    .load(url)
                    .fitCenter()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            finish()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageView.layoutParams = LinearLayout.LayoutParams(
                                imageView.width,
                                ((resource.minimumHeight.toDouble() / resource.minimumWidth) * imageView.width).toInt()
                            )
                            return false
                        }
                    })
                    .into(imageView);
            }
        }

    }

    private fun fetchIfExpired(date: Long, url: String, callback: (String) -> Unit) {
        val expiry = url.split("?Expires=").let {
            if (it.isNotEmpty()) ((it[1].split("&")[0].toLong()*1000) < System.currentTimeMillis())
            else false
        }
        if (expiry) {
            val account = Gson().fromJson(intent.getStringExtra("account"), Account::class.java)
            Thread{
                Scrapers(applicationContext).getNewsOfDate(
                    account,
                    Calendar.getInstance().apply { timeInMillis = date }) { news ->
                    if (!news.isNullOrEmpty()) {
                        news.filter { it.value.split("?Expires")[0] == url.split("?Expires")[0] }.let {
                            if (it.isNotEmpty()) {
                                Handler(mainLooper).post {
                                    callback(it.values.first())
                                }
                            } else {
                                Handler(mainLooper).post {
                                    callback("")
                                }
                            }
                        }
                    }
                    news?.forEach {
                        AppStorage(this).updateNoticeUrl(account,it.key,it.value)
                    }
                }
            }.start()
        } else {
            callback(url)
        }
    }
}