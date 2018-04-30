package com.hyperana.choosebook

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.File

/**
 * Created by alr on 1/18/18.
 */
val BOOK_FILENAME = "book.json"
val ASSETS_BOOK_DIR = "" // relative to assets
val EXTRA_BOOKPATH = "bookPath"
val EXTRA_MESSAGE = "message"
val SETTING_SOUND_STRING = "sound"
val SETTING_EFFECTS_STRING = "effects"

class App : Application() {
    val TAG = "App"

    val OFFLINE_BOOKS_DIR = "downloaded" // from getDir, NOT in filesdir
    val TEMP_BOOKS_DIR = "temp" // from getDir

    // todo: -L- this and poss above should be in singleton for modularity
    val requestQueue: RequestQueue? = null
        get() {
            return field ?: Volley.newRequestQueue(this)
        }

    override fun onCreate() {
        super.onCreate()
  //      Log.d(TAG, "onCreate")

        val availableMemory = Runtime.getRuntime().maxMemory() // bytes
        val availableDisk = cacheDir.freeSpace // bytes
//        Log.d(TAG, "available space in memory ("+availableMemory+") disk ("+availableDisk+")")

    }

    override fun onLowMemory() {
        super.onLowMemory()
//        Log.d(TAG, "onLowMemory")

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
//        Log.d(TAG, "onTRimMemory")
    }

    interface StringListener {
        fun onString(string: String?)
    }

    val savedBookDir: File?
    get() {
        return getDir(OFFLINE_BOOKS_DIR, 0)
    }

    val tempBookDir: File?
    get() {
        return getDir(TEMP_BOOKS_DIR, 0)
    }


    fun loadString(uri: Uri, listener: StringListener? = null) {
//        Log.d(TAG, "loadString: " + uri.toString())
        try {

           when (uri.scheme) {

           // case for assets
                ContentResolver.SCHEME_CONTENT -> {
//                    Log.d(TAG, "loadBookJson (asset): " + uri.encodedPath)
                    listener?.onString(
                            com.hyperana.choosebook.loadString(
                                    assets.open(uri.path.substring(1))
                            )
                    )

                }
            // case for files and assets:
           // assets may have a "file:///android_asset" uri for Glide's sake
               ContentResolver.SCHEME_FILE -> {
//                   Log.d(TAG, "loadBookJson (file): " + uri.encodedPath)
                   val assetPathSegment = uri.pathSegments.indexOf("android_asset")
                   if (assetPathSegment > -1) {
                       val correctedPath = uri.pathSegments
                               .drop(assetPathSegment + 1)
                               .joinToString(File.separator)
                       listener?.onString(
                               com.hyperana.choosebook.loadString(
                                       assets.open(correctedPath)
                               )
                       )
                   }
                   else {
                       listener?.onString(
                               com.hyperana.choosebook.loadString(
                                       filesDir.resolve(uri.path).inputStream()
                               )
                       )
                   }
               }
            // case for http request
                else -> {
//                    Log.d(TAG, "loadString (http): " + uri.encodedPath)
                    requestQueue!!.add(
                            StringRequest(
                                    uri.toString(),
                                    { string ->
                                        listener?.onString(string)
                                    },
                                    { volleyError ->
//                                        Log.e(
//                                                TAG,
//                                                "requestError",
//                                                Exception(volleyError?.message, volleyError?.cause)
//                                        )
                                        listener?.onString(null)
                                    }
                            )
                    )
                }
            }
        }
        catch(e: Exception) {
//            Log.e(TAG, "problem loading string: " + uri.toString(), e)
           listener?.onString(null)
        }
    }



}
