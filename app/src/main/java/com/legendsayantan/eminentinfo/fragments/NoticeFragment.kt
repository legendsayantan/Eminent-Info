package com.legendsayantan.eminentinfo.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AlertDialogLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.legendsayantan.eminentinfo.R
import com.rajat.pdfviewer.PdfRendererView
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import kotlin.properties.Delegates

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NoticeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NoticeFragment : DialogFragment() {
    lateinit var url : String;
    var date = System.currentTimeMillis()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val result =  AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_notice).show()
        result.window?.setBackgroundDrawableResource(android.R.color.transparent);
        result.findViewById<TextView>(R.id.title)?.text = "Notice - ${SimpleDateFormat("dd/MM/YYYY").format(date)}"
        val pdfView = result.findViewById<PdfRendererView>(R.id.pdf)!!
        val imageView = result.findViewById<ImageView>(R.id.image)!!
        val external = result.findViewById<ImageView>(R.id.external)
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
                .with(requireContext())
                .load(url)
                .fitCenter()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .into(imageView);
        }
        return result
    }
}