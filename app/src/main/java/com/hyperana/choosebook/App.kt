package com.hyperana.choosebook

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Created by alr on 1/18/18.
 */
val BOOK_FILENAME = "book.json"
val ASSETS_BOOK_DIR = "" // relative to assets
val EXTRA_BOOKPATH = "bookPath"
val EXTRA_MESSAGE = "message"

class App : Application() {
    val TAG = "App"

    val OFFLINE_BOOKS_DIR = "downloaded" // from getDir, NOT in filesdir
    val TEMP_BOOKS_DIR = "temp" // from getDir

    var imageCache: LruCache<String, Bitmap>? = null

    // todo: -L- this and poss above should be in singleton for modularity
    val requestQueue: RequestQueue? = null
        get() {
            return field ?: Volley.newRequestQueue(this)
        }
    val bitmapConfig = Bitmap.Config.ARGB_8888
    val MAX_IMAGE_WIDTH = 1200
    val MAX_IMAGE_HEIGHT = 2000


    //todo: use disk cache -- add images to both, check memory cache, then disk cache
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        val availableMemory = Runtime.getRuntime().maxMemory() // bytes
        val availableDisk = cacheDir.freeSpace // bytes
        Log.d(TAG, "available space in memory ("+availableMemory+") disk ("+availableDisk+")")

      /*  // create cache (defined in kB)
        imageCache = object: LruCache<String, Bitmap>((availableMemory/(4 * 1024)).toInt()) {
            override fun sizeOf(key: String, value: Bitmap) : Int {
                return value.getByteCount();
            }
        }*/
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "onLowMemory")

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "onTRimMemory")
    }

    interface BitmapListener {
        fun onBitmap(bitmap: Bitmap?)
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
        Log.d(TAG, "loadString: " + uri.toString())
        try {

           when (uri.scheme) {

            // case for assets
                ContentResolver.SCHEME_CONTENT -> {
                    Log.d(TAG, "loadBookJson (asset): " + uri.encodedPath)
                    listener?.onString(
                            com.hyperana.choosebook.loadString(
                                    assets.open(uri.path.substring(1))
                            )
                    )

                }
            // case for files:
                ContentResolver.SCHEME_FILE -> {
                    Log.d(TAG, "loadBookJson (file): " +uri.encodedPath)
                    listener?.onString(
                            com.hyperana.choosebook.loadString(
                                    filesDir.resolve(uri.path).inputStream()
                            )
                    )
                }
            // case for http request
                else -> {
                    Log.d(TAG, "loadString (http): " + uri.encodedPath)
                    requestQueue!!.add(
                            StringRequest(
                                    uri.toString(),
                                    { string ->
                                        listener?.onString(string)
                                    },
                                    { volleyError ->
                                        Log.e(
                                                TAG,
                                                "requestError",
                                                Exception(volleyError?.message, volleyError?.cause)
                                        )
                                        listener?.onString(null)
                                    }
                            )
                    )
                }
            }
        }
        catch(e: Exception) {
            Log.e(TAG, "problem loading string: " + uri.toString(), e)
           listener?.onString(null)
        }
    }

    // listener may be called on non-ui thread
    fun loadImageBitmap(uri: Uri, listener: BitmapListener? = null) {
        Log.d(TAG, "loadImageBitmap: " + uri.toString())

        fun haveBitmap(bitmap: Bitmap, uriString: String) {
           // imageCache?.put(uriString, bitmap)
           // Log.d(TAG, "add bitmap to cache -> " + imageCache?.size().toString())
            listener?.onBitmap(bitmap)
        }

        try {
            if (uri == Uri.EMPTY) {
                throw Exception("uri is empty");
            }

            // if it's in the cache, set it and done
            imageCache?.get(uri.toString())?.also {
                listener?.onBitmap(it)
                return
            }

            // otherwise, load, put in cache, and set:
            // start a new thread for bitmap decoding
           Thread({

                when (uri.scheme) {

                // case for assets using AssetContentProvider
                    ContentResolver.SCHEME_CONTENT -> {
                        Log.d(TAG, "loadImageBitmap (asset): " + uri)
                        val path = uri.pathSegments.joinToString(File.separator) //remove starting forward slash
                            BitmapFactory.decodeStream(assets.open(path))?.also {
                               haveBitmap(it, uri.toString())
                            }


                    }

                // case for files
                    ContentResolver.SCHEME_FILE -> {
                        Log.d(TAG, "loadImageBitmap (file): " + uri)

                            BitmapFactory.decodeFile(filesDir.resolve(uri.path).path)?.also {
                                haveBitmap(it, uri.toString())
                        }
                    }
                // otherwise try the internet...
                    else -> {
                        Log.d(TAG, "loadImageBitmap (http): " + uri)
                        Log.d(TAG, "loading bmp...")

                        requestQueue!!.add(
                                ImageRequest(
                                        uri.toString(),
                                        {
                                            bitmap: Bitmap ->
                                            Log.d(TAG, "bitmap loaded: " + uri)
                                            haveBitmap(bitmap, uri.toString())
                                        },
                                        MAX_IMAGE_WIDTH,
                                        MAX_IMAGE_HEIGHT,
                                        bitmapConfig,
                                        {
                                            volleyError: VolleyError? ->
                                            Log.e(
                                                    TAG,
                                                    "requestError",
                                                    Exception(volleyError?.message, volleyError?.cause)
                                            )
                                            listener?.onBitmap(null)
                                        }
                                )
                        )
                    }
                }
            }).start()
        }
        catch(e: Exception) {
            Log.e(TAG, "problem loading bmp " + uri.toString(), e)
            listener?.onBitmap(null)
        }
    }


}
