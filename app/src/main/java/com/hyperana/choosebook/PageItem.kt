package com.hyperana.choosebook

import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by alr on 2/8/18.
 */

interface PageItemListener {
    fun onPageChange(toName: String) : Boolean
    fun onTextClick(v: TextView)
    fun onImageClick(v: ImageView)
    fun onChoiceClick(texts: List<TextView>)
}

abstract class PageItem {
    abstract fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View
}

//todo: add TTS
class PageItemText(val text: String = "") : PageItem() {
    val TAG = "PageItemText"
    val viewId = R.layout.pageitem_textlayout
    val textId = R.id.pageitem_text

    override fun getView(parent: ViewGroup, convertView: View?, pageItemListener: PageItemListener?) : View {
        Log.d(TAG, "getView")
        return View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(textId) as? TextView)?.also {
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
        Log.d(TAG, "getView: " + uri)

        return convertView ?: View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(imageId) as? ImageView)?.also {
                (parent.context.applicationContext as? App)?.loadImageBitmap(it, uri)
                it.setOnClickListener {
                    pageItemListener?.onImageClick(it as ImageView)
                }
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
        Log.d(TAG, "getView: " + listOf(rightLink, centerLink, leftLink).joinToString())

        val texts: ArrayList<TextView> = arrayListOf()

        fun createLinkView(map: Map<String, String>, parent: ViewGroup) : View {
            return View.inflate(parent.context, linkId, parent).apply {
                (findViewById(linkTextId) as TextView).also {
                    it.text = map["text"]
                    texts.add(it)
                    setOnClickListener {
                        try {
                            pageItemListener?.onPageChange(map["toPage"]!!)
                        } catch (e: Exception) {
                            Log.e(TAG, "problem onClick link " + map.toString(), e)
                        }
                    }
                }
            }
        }

        // todo: make use of convertview if possible
        return View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(promptId) as? TextView)?.apply {
                if (prompt.isEmpty()) {
                    (this.parent as? ViewGroup)?.visibility = View.GONE
                } else {
                    text = prompt
                    texts.add(this)
                }
            }
            (findViewById(rightLinkId) as ViewGroup).apply {
                if (rightLink != null) {
                    createLinkView(rightLink, this)
                }
                else {
                    visibility = View.INVISIBLE
                }
            }

            (findViewById(leftLinkId) as ViewGroup).apply {
                if (leftLink != null) {
                    createLinkView(leftLink, this)
                }
                else {
                    visibility = View.INVISIBLE
                }
            }
            (findViewById(centerLinkId) as ViewGroup).apply {
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
        Log.d(TAG, "getView: " + toString())

        val texts: ArrayList<TextView> = arrayListOf()

        return View.inflate(parent.context,  R.layout.pageitem_choiceboxlayout, null).also {
            parent.addView(it)

            (it.findViewById(promptId) as? TextView)?.apply {
                if (prompt.isEmpty()) {
                    visibility = View.GONE
                } else {
                    text = prompt
                    texts.add(this)
                }
            }
            val box = (it.findViewById(choiceBoxId) as? ViewGroup)
            links.onEach {
                val to = it["toPage"]!!
                val text = it["text"]!!
                box?.addView(
                        (View.inflate(parent.context, linkViewId, null) as ViewGroup).also {
                            (it.findViewById(linkTextId) as? TextView)?.also {
                                it.text = text
                                texts.add(it)

                                it.setOnClickListener {
                                    try {
                                        pageItemListener?.onPageChange(to)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "problem onClick link " + text + "->" + to)
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
