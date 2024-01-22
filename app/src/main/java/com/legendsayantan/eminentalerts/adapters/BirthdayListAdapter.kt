package com.legendsayantan.eminentalerts.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.legendsayantan.eminentalerts.R
import com.legendsayantan.eminentalerts.data.Birthday

/**
 * @author legendsayantan
 */
class BirthdayListAdapter(private val context: Context, private val birthdays: List<Birthday>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return birthdays.size
    }

    override fun getItem(position: Int): Birthday {
        return birthdays[position]
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
        val birthday = getItem(position)
        viewHolder.nameTextView.text = birthday.name
        viewHolder.batchTextView.text = birthday.batch
        Glide.with(context)
            .load(birthday.imageUrl)
            .fitCenter()
            .transition(withCrossFade())
            .into(viewHolder.imageView)
        return view
    }

    private class ViewHolder(view: View) {
        val batchTextView: TextView = view.findViewById(R.id.batch)
        val nameTextView: TextView = view.findViewById(R.id.name)
        val imageView: ImageView = view.findViewById(R.id.image)
    }
}
