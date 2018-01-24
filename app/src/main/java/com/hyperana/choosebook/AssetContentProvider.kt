package com.hyperana.choosebook

/**
 * Created by alr on 1/16/18
 * ---from https://stackoverflow.com/questions/7531989/android-setimageuri-not-working-with-asset-uri#answer-7664861
 *
 *
 *
 */


import java.io.FileNotFoundException
import java.io.IOException

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import java.io.File



class AssetContentProvider : ContentProvider() {
    val TAG = "AssetContentProvider"

    private var mAssetManager: AssetManager? = null

    override fun delete(arg0: Uri, arg1: String?, arg2: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun onCreate(): Boolean {
        mAssetManager = context!!.assets
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getStreamTypes(uri: Uri?, mimeTypeFilter: String?): Array<String> {
        return super.getStreamTypes(uri, mimeTypeFilter)
    }

    @Throws(FileNotFoundException::class)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val path = uri.path.substring(1)
        Log.d(TAG, "openAssetFile: " + path)
        try {
            val afd = mAssetManager!!.openFd(path)
            return afd
        } catch (e: IOException) {
            throw FileNotFoundException("No asset found: " + uri)
        }

    }

    //open streams to/from app's cacheDir to store files accessible by Uri
    //from https://stackoverflow.com/questions/13286918/how-to-caching-bitmaps-in-a-contentprovider#answer-15035138
    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val root = context!!.cacheDir
        val path = File(root, uri.encodedPath)
        path.mkdirs()
        val file = File(path, "file_" + uri.lastPathSegment)

        var imode = 0
        if (mode.contains("w")) {
            imode = imode or ParcelFileDescriptor.MODE_WRITE_ONLY
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        if (mode.contains("r"))
            imode = imode or ParcelFileDescriptor.MODE_READ_ONLY
        if (mode.contains("+"))
            imode = imode or ParcelFileDescriptor.MODE_APPEND

        return ParcelFileDescriptor.open(file, imode)
    }


    companion object {
        val CONTENT_URI = Uri.parse("content://com.hyperana.choosebook.provider")
    }

}