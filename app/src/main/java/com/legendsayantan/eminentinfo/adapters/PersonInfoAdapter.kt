package com.legendsayantan.eminentinfo.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.legendsayantan.eminentinfo.R
import com.legendsayantan.eminentinfo.data.PersonInfo
import com.legendsayantan.eminentinfo.utils.Images

/**
 * @author legendsayantan
 */
class PersonInfoAdapter(private val context: Context, private val people: List<PersonInfo>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return people.size
    }

    override fun getItem(position: Int): PersonInfo {
        return people[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder
        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_birthday, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val person = getItem(position)
        viewHolder.nameTextView.text = person.name
        viewHolder.batchTextView.text = person.data
        val prepDefaultImg = {
            viewHolder.imageView.imageTintList =
                ColorStateList.valueOf(context.getColor(R.color.mid))
            viewHolder.imageView.translationY = 8f
            viewHolder.imageView.scaleX = 1.15f
            viewHolder.imageView.scaleY = 1.15f
        }
        if (person.imageUrl != "images/HR/default_employee.png") {
            if (person.imageUrl.startsWith("local://")) {
                Images(context).getProfilePic(person.imageUrl.substring(8)).let {
                    if (it != null) {
                        Glide.with(context)
                            .load(it)
                            .fitCenter()
                            .transition(withCrossFade())
                            .into(viewHolder.imageView)
                        viewHolder.imageView.imageTintList = null
                        viewHolder.imageView.translationY = 0f
                        viewHolder.imageView.scaleX = 1f
                        viewHolder.imageView.scaleY = 1f
                    } else prepDefaultImg()
                }
            } else Glide.with(context)
                .load(person.imageUrl)
                .fitCenter()
                .transition(withCrossFade())
                .into(viewHolder.imageView)
        } else prepDefaultImg()
        return view
    }

    private class ViewHolder(view: View) {
        val batchTextView: TextView = view.findViewById(R.id.batch)
        val nameTextView: TextView = view.findViewById(R.id.name)
        val imageView: ImageView = view.findViewById(R.id.image)
    }
}
