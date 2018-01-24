package com.hyperana.choosebook

import android.support.v4.app.LoaderManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import java.io.*
import java.net.URL

/**
 * Created by alr on 1/22/18.
 *
 * for now I have wrapped all the resource loading in one loader so I know when it's all done/ can
 * cancel the rest and erase already loaded if something goes wrong, etc,
 *
 * but, would probably perform better if each load/store was separate
 */
class AsyncMultiResLoader(
        context: Context,
        val uriList: List<String>,
        val toDirPath: String,
        val loaderCallbacks: LoaderManager.LoaderCallbacks<Int>
) : android.support.v4.content.AsyncTaskLoader<Int>(context) {

    val TAG = "AsyncMultiResLoader"


    init {
        //must call this in constructor and implement onStartLoading and onStopLoading
        //otherwise it will not be started/canceled  by manager:
        //https://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground#answer-1661495
        onContentChanged()
    }


    override fun loadInBackground(): Int {
        Log.d(TAG, "loadInBackground")

        var result = 0
        uriList.forEachIndexed {
            index, uriString ->
            if (!this.isAbandoned) {
                try {
                    val toFile = File(toDirPath, Uri.parse(uriString).lastPathSegment)
                    downloadUrl(
                            url = URL(uriString),
                            saveTo = toFile
                    )
                    result = index + 1 //todo: some fancy percentage thing?

                } catch(e: Exception) {
                    Log.e(TAG, "problem moving resource: " + uriString + " -> " + toDirPath, e)
                }
            }
        }
        return result
    }



    override fun onStartLoading() {

        val networkInfo = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            // If no connectivity, cancel task and update Callback with null data.
            cancelLoad()
        } else if (takeContentChanged())
            forceLoad()

    }


    override fun onStopLoading() {
        cancelLoad()
    }



    // Downloading function and callbacks:


}