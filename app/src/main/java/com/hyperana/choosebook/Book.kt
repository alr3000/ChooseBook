package com.hyperana.choosebook

import android.content.res.AssetManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * Created by alr on 12/29/17.
 *
 * inflated from json string:
 * object {
 *      title: String
 *      pages: array [
 *          name: String
 *          contents: array [
 *              object {
 *                  type: String (text, image, rlchoice, choice)
 *                  <varied>: String
 *                  <varied>: Array [ {<varied>: String} ]
 *              }
 *          ]
 *      ]
 * }
 */

val SAMPLE_BOOK = """
    {
        "title" : "Sample Book",
        "author" : "Anonymous",
        "thumbnail" : "cover_image_small.png",
        "id" : "48375",
        "cover" : "image_for_page_one.jpg",
        "pages" : [
            {
                "title" : "Page One",
                "contents" : [
                    {
                        "text" : "Text for page one. Text for page one."
                    },
                    {
                        "image" : "image_for_page_one.jpg"
                    },
                    {
                        "prompt" : "right/left choice for page one?",
                        "rightLink" : {
                            "text" : "right",
                            "to" : "Page Two"
                        }
                    }
                ]
            },
            {
                "title" : "Page Two",
                "contents" : [
                    {
                        "text" : "Text for page two. Text for page two."
                    },
                    {
                        "prompt" : "multiple choice prompt for page two?",
                        "links" : [
                            {
                                "text" : "next page",
                                "to" : "Page Three"
                            },
{
                                "text" : "previous page",
                                "to" : "Page One"
                            }
                        ]
                    }
                ]
            },
            {
                "title" : "Page Three",
                "contents" : [
                    {
                        "text" : "Text for page three."
                    }
                ]
            }
        ]
    }
"""


fun loadString(stream: InputStream) : String {
    var str = ""
    try {
        val buffer = ByteArray(stream.available())
        stream.read(buffer)
        str = String(buffer)
    }
    catch (e: Exception) {
        Log.e("static", "problem load string", e)
    }
    finally {
        stream.close()
        return str
    }
}

fun parseJsonObject(jObject: JSONObject) : Map<String, Any?> {
    return jObject.keys()
            .asSequence()
            .map { Pair<String, Any?>(
                    it,
                    if (jObject.isNull(it)) null
                    else parseJsonValue(jObject.get(it))
            ) }
            .toMap()
}

fun parseJsonValue(value: Any) : Any {
    return (value as? JSONArray)?.let {
        parseJsonArray(it)
    } ?:
            (value as? JSONObject)?.let {
                parseJsonObject(it)
            } ?:
            value.toString()
}


fun parseJsonArray(jArray: JSONArray) : List<Any?> {
    return (0 .. jArray.length() - 1)
            .map {
                if (jArray.isNull(it)) null
                else parseJsonValue(jArray.get(it))
            }
}


interface PageListener {
    fun onPageChange(toName: String) : Boolean
}
/*//todo: use contentprovider to abstract from assets
// create a book from an assets "folder": on creation parses only for stub (title, cover, etc, not pages)
fun Book(assets: AssetManager, assetPath: String = "") : Book? {
 try {
     val jsonPath = File(
             assetPath,
             assets.list(assetPath).map { File(it) }.find { it.extension == "json" }!!.path
     ).path
     Log.d("Book", "from json in assets: " + jsonPath)

     return object : Book(jsonStream = assets.open(jsonPath), path = assetPath) {

     }
 }
 catch (e: Exception) {
     Log.e("BookFromAsset", "problem creating from assetPath: " + assetPath, e)
     return null
 }
}*/

fun BookFromAsset(assets: AssetManager, bookPath: String) : Book? {
    try {
        val json = assets.list(bookPath).map { File(it) }.find { it.extension == "json" }!!.name
        return Book(
                jsonString = loadString(assets.open(File(bookPath, json).path)),
                path = bookPath,
                uri = AssetContentProvider.CONTENT_URI.buildUpon().appendPath(bookPath).build()
        )
    }
    catch (e: Exception) {
        Log.e("BookFromAsset", "failed for " + bookPath, e)
        return null
    }
}

open class Book() {
    val TAG = "Book"

    var id: Long = 0
    var title: String = "Book Not Found"
    var author: String = ""
    var cover: Uri = Uri.EMPTY
    var thumb: Uri = Uri.EMPTY
    var path: String? = null
    var parentUri: Uri = Uri.EMPTY

    var jsonString: String? = null
    var pageScheme: List<Any?> = listOf()
    var pages: Map<String, List<PageItem>> = mapOf()
    var resources: List<String> = listOf() // list of filenames


    // todo: Book takes json strinf (and parent uri) so it can make new uri's from relative paths in json
    constructor(jsonString: String,  path: String, uri: Uri) : this() {
        Log.d(TAG, "init: " + uri)
        this.path = path
        this.parentUri = uri
        this.jsonString = jsonString
        parseJsonStream()
    }

    // fills out Book properties except for pages (assigns pageScheme = list of objects(pages))
   fun parseJsonStream() {
       try {
           //todo: make async json parse
           val jBook: Map<String, Any?> = parseJsonObject(JSONObject(jsonString))
           Log.d(TAG, "parseJsonStream  -> " + jBook.size)

           // assign book properties
           title = (jBook["title"] as? String) ?: title
           author = (jBook["author"] as? String) ?: author
           id = (jBook["id"] as? Long) ?: (Math.random()*100000).toLong() // for listadapter??

           //todo: load thumbnail into contentprovider, store content Uri
           thumb = (jBook["thumbnail"] as? String)
                   ?.let {
                       getResourceUri(it)
                   }
                   ?: thumb
           cover = (jBook["cover"] as? String)
                   ?.let {
                       getResourceUri(it)
                   }
                   ?: cover

           resources = (jBook["resources"] as? List<String>) ?: listOf()

           // create page data map:
           // expects to find "pages" -> a list of page maps
           // in each page is expected a "title" -> string, and "contents" -> a list of maps
           pageScheme = ((jBook["page"] as? List<Any?>) ?: Collections.EMPTY_LIST)

       }
       catch(e: Exception) {
           Log.e(TAG, "problem parsing json stream", e)
       }
    }

    //get contents as pageitems
    fun createPages() {
        pages = pageScheme.mapIndexed {
            index, item ->

            // (pageId -> content list of pageitems) or null
            try {
                item as Map<String, Any?>

                Pair(

                        item["title"]?.toString() ?: index.toString(),

                        (item["contents"] as? List<Any?>)
                                ?.map {
                                    createPageItem(it)
                                }
                                ?.filterNotNull()
                                ?: listOf()
                )
            } catch (e: Exception) {
                Log.e(TAG, "problem creating page contents", e)
                null
            }

        }
                .filterNotNull()
                .toMap()
    }


    // inspects object, returns initialized item (text, image, choice, etc) or null if no suitable item
    fun createPageItem(anyMap: Any?) : PageItem? {
        val map = anyMap as? Map<String, Any?>
        try {
            map as Map<String, Any?>
            if (map.contains("text")) return PageItemText(map["text"].toString())
            if (map.contains("image")) return PageItemImage(getResourceUri(map["image"] as String))
            if (map.contains("rightLink") || map.contains("leftLink"))
                return PageItemRLChoice(
                        prompt = map["prompt"]?.toString() ?: "",
                        rightLink = map["rightLink"] as? Map<String, String>,
                        leftLink = map["leftLink"] as? Map<String, String>
                )
            if (map.contains("link")) {
                val links = map["link"] as List<Map<String, String>>
                return if (links.count() > 0) PageItemChoiceBox(
                        prompt = map["prompt"].toString(),
                        links = links
                )
                else PageItemRLChoice(
                        prompt = map["prompt"].toString(),
                        leftLink = if (links.count() > 1) links.getOrNull(0) else null,
                        rightLink = if (links.count() > 1) links.getOrNull(1) else null,
                        centerLink = if (links.count() > 1) links.getOrNull(2) else links.get(0)
                )
            }
            else throw Exception("item doesn't fit any page item pageScheme: " + map.toString())
        }
        catch (e: Exception) {
            Log.e(TAG, "problem createpageitem: " + map?.toString(), e)
            return null
        }
    }


    open fun getResourceUri(filename: String): Uri {
        return parentUri.buildUpon().appendPath(filename).build()
                .also {
                    Log.d(TAG, "getResourceUri: " + it.toString())
                }
    }
}

abstract class PageItem {
    abstract fun getView(parent: ViewGroup, convertView: View?, pageListener: PageListener?) : View
}

class PageItemText(val text: String = "") : PageItem() {
    val TAG = "PageItemText"
    val viewId = R.layout.pageitem_textlayout
    val textId = R.id.pageitem_text

    override fun getView(parent: ViewGroup, convertView: View?, pageListener: PageListener?) : View {
        Log.d(TAG, "getView")
        return convertView ?: View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(textId) as? TextView)?.text = text
        }
    }
}

class PageItemImage(val uri: Uri?) : PageItem() {
    val TAG = "PageItemImage"
    val viewId = R.layout.pageitem_imagelayout
    val imageId = R.id.pageitem_image

    override fun getView(parent: ViewGroup, convertView: View?, pageListener: PageListener?) : View {
        Log.d(TAG, "getView")

        return convertView ?: View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(imageId) as? ImageView)
                    ?.setImageURI(uri) //(resources.getDrawable(R.drawable.carnival_cover_image))
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



    override fun getView(parent: ViewGroup, convertView: View?, pageListener: PageListener?) : View {
        Log.d(TAG, "getView: " + listOf(rightLink, centerLink, leftLink).joinToString())

        fun createLinkView(map: Map<String, String>, parent: ViewGroup) : View {
            return View.inflate(parent.context, linkId, parent).apply {
                (findViewById(linkTextId) as TextView).text = map["text"]
                setOnClickListener {
                    try {
                        pageListener?.onPageChange(map["toPage"]!!)
                    } catch (e: Exception) {
                        Log.e(TAG, "problem onClick link " + map.toString(), e)
                    }
                }
            }
        }
            // todo: make use of convertview if possible
        return  View.inflate(parent.context, viewId, null).apply {
            parent.addView(this)
            (findViewById(promptId) as? TextView)?.apply {
                if (prompt.isEmpty()) {
                    (this.parent as? ViewGroup)?.visibility = View.GONE
                } else {
                    text = prompt
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

    override fun getView(parent: ViewGroup, convertView: View?, pageListener: PageListener?) : View {
        Log.d(TAG, "getView: " + toString())

        return convertView ?: View.inflate(parent.context,  R.layout.pageitem_choiceboxlayout, null).also {
            parent.addView(it)
            (it.findViewById(promptId) as? TextView)?.apply {
                if (prompt.isEmpty()) {
                    visibility = View.GONE
                } else {
                    text = prompt
                }
            }
            val box = (it.findViewById(choiceBoxId) as? ViewGroup)
            links.onEach {
                val to = it["toPage"]!!
                val text = it["text"]!!
                box?.addView(
                        (View.inflate(parent.context, linkViewId, null) as ViewGroup).also {
                            (it.findViewById(linkTextId) as? TextView)?.text = text

                            it.setOnClickListener {
                                try {
                                    pageListener?.onPageChange(to)
                                } catch (e: Exception) {
                                    Log.e(TAG, "problem onClick link " + text + "->" + to)
                                }
                            }
                        }
                )
            }
        }
    }
}

