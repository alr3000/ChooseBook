package com.hyperana.choosebook

import android.app.Activity
import android.content.Context
import android.database.DataSetObserver
import android.net.Uri
import android.os.FileObserver
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import java.io.File
import java.io.FilenameFilter

/**
 * Created by alr on 1/12/18.
 * Creates from directory listing if path provided for observer, otherwise, set list of books
 * listPath must be absolute -- doesn't use getFilesDir, etc
 * start and stop observer on pause and resume
 * todo: make helpers to keep list from being reset (use mutable)
 */
fun randomString( length: Int = 8) : String {
    val char = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    return (0 .. length - 1).map {
        char[Math.round(Math.random() * (char.count() - 1)).toInt()]
    }.joinToString("")
}

class BookListAdapter(val activity: Activity) : BaseAdapter() {
     var TAG = "BookListAdapter" + randomString(4)

    var list: List<Book> = listOf()
    set(value) {
        Log.d(TAG, "set list: " + value.count() + " items")
        field = value
        notifyDataSetChanged()
    }

    override fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        Log.d(TAG, "getView: " + position)
        val book = getItem(position) as Book
        return (convertView ?:
                activity.layoutInflater.inflate(
                        R.layout.library_list_item,
                        parent,
                        false
                ))
                .apply {
                    try {
                        (findViewById(R.id.title) as? TextView)?.text = book.title
                        (findViewById(R.id.author) as? TextView)?.text = book.author

                        //set cover image
                        (findViewById(R.id.cover) as? ImageView)?.apply {
                            setImageResource(R.drawable.no_image)
                            (activity.application as App).loadImageBitmap(this, book.thumb)
                        }
                    }
                    catch(e: Exception) {
                        Log.w(TAG, "problem filling view", e)
                    }

                }
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItem(position: Int): Any {
        return list.getOrElse(position,  { Book() })
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return (getItem(position) as Book).id
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getCount(): Int {
        return list.count()
    }

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }
}