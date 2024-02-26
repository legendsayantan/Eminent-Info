package com.legendsayantan.eminentinfo

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.legendsayantan.eminentinfo.utils.Misc
import com.rajat.pdfviewer.PdfRendererView
import java.text.SimpleDateFormat

class NoticeView : AppCompatActivity() {
    lateinit var url : String;
    var date = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_notice)
        url = intent.getStringExtra("url")!!
        date = intent.getLongExtra("date",System.currentTimeMillis())
        findViewById<TextView>(R.id.title)?.text = Misc.extractNoticeName(url)?:"Notice - ${SimpleDateFormat("dd/MM/YYYY").format(date)}"
        val pdfView = findViewById<PdfRendererView>(R.id.pdf)!!
        val imageView = findViewById<ImageView>(R.id.image)!!
        val external = findViewById<ImageView>(R.id.external)
        external.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
        if(url.contains(".pdf")){
            imageView.visibility = View.GONE
            pdfView.initWithUrl(
                url = url,
                lifecycle = lifecycle,
                lifecycleCoroutineScope = lifecycleScope,
            )
        }else{
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
                            imageView.width,((resource.minimumHeight.toDouble()/resource.minimumWidth) * imageView.width).toInt()
                        )
                        return false
                    }
                })
                .into(imageView);
        }
    }
}