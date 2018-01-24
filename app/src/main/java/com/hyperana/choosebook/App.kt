package com.hyperana.choosebook

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.io.InputStream

/**
 * Created by alr on 1/18/18.
 */
val BOOK_FILENAME = "book.json"
val OFFLINE_BOOKS_DIR = "downloaded"
val ASSETS_BOOK_DIR = ""
val EXTRA_BOOKPATH = "bookPath"

class App : Application() {
    val TAG = "App"

    val imageCache: LruCache<String, Bitmap> = LruCache(10)
    val requestQueue: RequestQueue? = null
    get() {
        return field ?: Volley.newRequestQueue(this)
    }
    val bitmapConfig = Bitmap.Config.ARGB_8888
    val MAX_IMAGE_WIDTH = 1000
    val MAX_IMAGE_HEIGHT = 2000


    fun loadInputString(
            uri: Uri,
            responseListener: Response.Listener<String>,
            errorListener: Response.ErrorListener?
    ) {

        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE -> {
                Log.d(TAG, "loadBookJson (asset): " + uri)
                try {
                    val stream = contentResolver.openInputStream(uri)
                    responseListener.onResponse(stream.readBytes().toString())
                    stream.close()
                }
                catch(e: Exception) {
                    errorListener?.onErrorResponse(VolleyError(e))
                }
            }
            else -> {
                requestQueue!!.add(
                        StringRequest(
                                uri.toString(),
                                responseListener,
                                errorListener
                        )
                )
            }
        }

    }

    fun loadImageBitmap(imageView: ImageView?, uri: Uri) {
        when (uri.scheme) {

            //todo: use cache for asset bitmaps
        // case for assets using AssetContentProvider
            ContentResolver.SCHEME_CONTENT -> {
                Log.d(TAG, "loadImageBitmap (asset): " + uri)
                imageView?.setImageURI(uri)
            }
            // case for files
            ContentResolver.SCHEME_FILE -> {
                Log.d(TAG, "loadImageBitmap (file): " + uri)
                try {
                    val bmp: Bitmap = imageCache.get(uri.toString()) ?:
                            BitmapFactory.decodeFile(filesDir.resolve(uri.path).path).also {
                                imageCache.put(uri.toString(), it)
                            }
                    imageView?.setImageBitmap(bmp)
                }
                catch (e: Exception) {
                    Log.e(TAG, "problem loading bitmap from file: " + uri, e)
                }
            }
        // otherwise try the internet...
            else -> {
                Log.d(TAG, "loadImageBitmap (http): " + uri)
                try {
                    val bmp = imageCache.get(uri.toString())?.also {
                        Log.d(TAG, "bitmap found in cache")
                        imageView?.setImageBitmap(it)
                    }
                    if (bmp == null) {
                        Log.d(TAG, "loading bmp...")

                        requestQueue!!.add(
                                ImageRequest(
                                        uri.toString(),
                                        {
                                            bitmap: Bitmap ->
                                            Log.d(TAG, "bitmap loaded: " + uri)
                                            imageCache.put(uri.toString(), bitmap)
                                            imageView?.setImageBitmap(bitmap)
                                        },
                                        MAX_IMAGE_WIDTH,
                                        MAX_IMAGE_HEIGHT,
                                        bitmapConfig,
                                        {
                                            volleyError: VolleyError? ->
                                            throw Exception(volleyError?.message, volleyError?.cause)
                                        }
                                )
                        )
                    }
                }
                catch (e: Exception)  {
                    Log.e(TAG, "problem loading bitmap from http: " + uri, e)
                }
            }
        }
    }

}