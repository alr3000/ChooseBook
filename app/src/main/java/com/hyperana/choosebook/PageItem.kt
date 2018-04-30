package com.hyperana.choosebook

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

/**
 * Created by alr on 2/8/18.
 */

interface PageItemListener {
    fun onLinkClick(v: View, toName: String)
    fun onTextClick(v: TextView)
    fun onImageClick(v: ImageView)
    fun onChoiceClick(texts: List<View>)
}

abstract class PageItem {
    abstract fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View
    fun getTextViews() : List<TextView> {
        return listOf()
    }
}

class PageItemText(val text: String = "") : PageItem() {
    val TAG = "PageItemText"
    val viewId = R.layout.pageitem_textlayout
    val textId = R.id.pageitem_text

    override fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View {
//        Log.d(TAG, "getView")
        return View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById<TextView?>(textId))?.also {
                it.text = text
                it.setOnClickListener {
                    v ->
                    pageItemListener?.onTextClick(v as TextView)
                }
            }
        }
    }
}

class PageItemImage(val uri: Uri) : PageItem() {
    val TAG = "PageItemImage"
    val viewId = R.layout.pageitem_imagelayout
    val imageId = R.id.pageitem_image

    override fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View {
//        Log.d(TAG, "getView: " + uri)

        return convertView ?: View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById<ImageView?>(imageId))?.also {
                Glide
                        .with(parent.context)
                        .load(uri)
                        .into(it)

                /*it.setOnClickListener {
                    pageItemListener?.onImageClick(it as ImageView)
                }*/
            }
        }
    }

}


class PageItemRLChoice(val prompt: String = "",
                       val rightLink: Map<String, String>? = null,
                       val leftLink: Map<String, String>? = null,
                       val centerLink: Map<String, String>? = null) : PageItem() {

    val TAG = "PageItemRLChoice"
    val viewId = R.layout.pageitem_choicebox_horizontal
    val linkId = R.layout.pageitem_link_down
    val promptId = R.id.pageitem_text
    val linkTextId = R.id.pageitem_text
    val rightLinkId = R.id.pageitem_rightlink
    val leftLinkId = R.id.pageitem_leftlink
    val centerLinkId = R.id.pageitem_centerlink



    override fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View {
//        Log.d(TAG, "getView: " + listOf(rightLink, centerLink, leftLink).joinToString())

        val texts: ArrayList<View> = arrayListOf()

        fun createLinkView(map: Map<String, String>, parent: ViewGroup) : View {
            return View.inflate(parent.context, linkId, parent).apply {
                (findViewById<TextView>(linkTextId)).also {
                    it.text = map["text"]
                    setOnClickListener {
                        try {
                            pageItemListener?.onLinkClick(it, map["toPage"]!!)
                        } catch (e: Exception) {
//                            Log.e(TAG, "problem onClick link " + map.toString(), e)
                        }
                    }
                }
                texts.add(this)
            }
        }

        return View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById<TextView?>(promptId))?.apply {
                if (prompt.isEmpty()) {
                    (this.parent as? ViewGroup)?.visibility = View.GONE
                } else {
                    text = prompt
                    texts.add(this)
                }
            }
            (findViewById<ViewGroup>(rightLinkId)).apply {
                if (rightLink != null) {
                    createLinkView(rightLink, this)
                }
                else {
                    visibility = View.INVISIBLE
                }
            }

            (findViewById<ViewGroup>(leftLinkId)).apply {
                if (leftLink != null) {
                    createLinkView(leftLink, this)
                }
                else {
                    visibility = View.INVISIBLE
                }
            }
            (findViewById<ViewGroup>(centerLinkId)).apply {
                if (centerLink != null) {
                    createLinkView(centerLink, this)
                }
                else {
                    visibility = View.INVISIBLE
                }
            }
            setOnClickListener { pageItemListener?.onChoiceClick(texts.toList()) }
        }
    }
}

class PageItemChoiceBox( var prompt: String = "",
                         var links: List<Map<String, String?>> = listOf()) : PageItem() {
    val TAG = "PageItemChoiceBox"
    val promptId = R.id.pageitem_text
    val choiceBoxId = R.id.pageitem_linkcontainer
    val linkViewId = R.layout.pageitem_link_right
    val linkTextId = R.id.pageitem_text

    override fun toString(): String {
        return TAG + "{ " + prompt + ": " + links.joinToString() + " }"
    }

    override fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View {
//        Log.d(TAG, "getView: " + toString())

        val texts: ArrayList<TextView> = arrayListOf()

        return View.inflate(parent.context,  R.layout.pageitem_choiceboxlayout, null).also {
            parent.addView(it)

            (it.findViewById<TextView?>(promptId))?.apply {
                if (prompt.isEmpty()) {
                    visibility = View.GONE
                } else {
                    text = prompt
                    texts.add(this)
                }
            }
            val box = (it.findViewById<ViewGroup?>(choiceBoxId))
            links.onEach {
                val to = it["toPage"]!!
                val text = it["text"]!!
                box?.addView(
                        (View.inflate(parent.context, linkViewId, null) as ViewGroup).also {
                            (it.findViewById<TextView?>(linkTextId))?.also {
                                it.text = text
                                texts.add(it)

                                it.setOnClickListener {
                                    try {
                                        pageItemListener?.onLinkClick(it, to)
                                    } catch (e: Exception) {
//                                        Log.e(TAG, "problem onClick link " + text + "->" + to)
                                    }
                                }
                            }
                        }
                )
            }
            it.setOnClickListener {
                pageItemListener?.onChoiceClick(texts)
            }
        }
    }
}
