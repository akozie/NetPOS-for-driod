package com.woleapp.netpos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.woleapp.netpos.R


class ServicesSpinnerAdapter(
    val context: Context,
    val items: List<String>,
    private val assets: List<String>? = null
) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var v: View? = convertView
        if (v == null)
            v = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.item_services_dropdown, parent, false)
        val textView = v?.findViewById<TextView>(R.id.item_text)!!
        val assetView = v.findViewById<ImageView>(R.id.asset)
        if (assets.isNullOrEmpty())
            assetView.visibility = View.GONE
        assets?.let {
            Glide.with(context)
                .load(it[position])
                .into(assetView)
        }
        textView.text = items[position]
        return v
    }

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = 0

    override fun getCount(): Int = items.size
}