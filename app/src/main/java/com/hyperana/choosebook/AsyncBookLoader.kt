package com.hyperana.choosebook

import android.support.v4.content.AsyncTaskLoader
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import java.net.URL

/**
 * Created by alr on 1/22/18.
 */
class AsyncBookLoader(
        context: Context,
        val parentUri: Uri,
        val jsonFilename: String) : AsyncTaskLoader<Book>(context) {
    val TAG = "AsyncBookLoader"

    val mContext = context

    init {
        //otherwise it will not be started by manager:
        //https://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground#answer-1661495
        onContentChanged()
    }

    //todo: cache all bmps from resource list at this time
    override fun loadInBackground(): Book {
        Log.d(TAG, "loadInBackground")

        val bookUri = parentUri.buildUpon().appendPath(jsonFilename).build()

        // uri is parent directory of book
        return when (bookUri.scheme) {

            // case for assets
            ContentResolver.SCHEME_CONTENT -> {
                Log.d(TAG, "loadBookJson (asset): " + bookUri.encodedPath)
                try {
                    val string = loadString(
                          mContext.assets.open(bookUri.path.substring(1))

                            //AssetContentProvider can't find asset!
                          //mContext.applicationContext.contentResolver. openInputStream(bookUri)
                    )
                    Book(
                            jsonString = string,
                            uri = parentUri,
                            path = parentUri.lastPathSegment
                    )
                }
                catch(e: Exception) {
                    Log.e(TAG, "problem loading asset/file book at " + parentUri.toString(), e)
                    Book()
                }
            }
            // case for files:
            ContentResolver.SCHEME_FILE -> {
                Log.d(TAG, "loadBookJson (file): " + bookUri.encodedPath)
                try {
                    val string = loadString(
                            mContext.filesDir.resolve(bookUri.path).inputStream()
                    )
                    Book(
                            jsonString = string,
                            uri = parentUri,
                            path = parentUri.lastPathSegment
                    )
                }
                catch(e: Exception) {
                    Log.e(TAG, "problem loading file book at " + parentUri.toString(), e)
                    Book()
                }
            }
            // case for http request
            else -> {
                Log.d(TAG, "loadBookJson (http): " + parentUri)
                try {
                    downloadUrl(URL(parentUri.buildUpon().appendPath(BOOK_FILENAME).build().toString())).let {
                        Book(jsonString = it!!, path = parentUri.lastPathSegment, uri = parentUri)
                    }
                }
                catch (e: Exception) {
                    Log.e(TAG, "problem loading http book at " + parentUri.toString())
                    Book()
                }
            }
        }
    }

    // must implement these or loadInBackground is never called.
    // Also, call onContentChanged() in constructor or when initialized by loaderManager
    override fun onStartLoading() {
        if (takeContentChanged())
            forceLoad()
    }
    override fun onStopLoading() {
        cancelLoad()
    }

    override fun deliverResult(data: Book?) {
        Log.d(TAG, "deliverResult: " + data?.path)
        super.deliverResult(data)
    }
}

