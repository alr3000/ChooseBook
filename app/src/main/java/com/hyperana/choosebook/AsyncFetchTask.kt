package com.hyperana.choosebook

import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import java.io.File
import java.net.URL

/**
 * Created by alr on 1/9/18.
 *
 * this background task fetches data from given url and stores in add data file
 */

class AsyncFetchParams(
        val url: URL,
        val outputDataFile: File,
        val progressBar: ProgressBar?,
        val errorTextView: TextView?
)

class AsyncFetchTask(val onComplete: Runnable?, val onFailed: Runnable?) : AsyncTask<AsyncFetchParams, Int, Int>() {
    val TAG = "AsyncFetchTask"


    var progressBar: ProgressBar? = null
    var errorView: TextView? = null

    override fun doInBackground(vararg params: AsyncFetchParams): Int {
        Log.d(TAG, "doInBackground: " + params[0].url)
        progressBar = params[0].progressBar
        errorView = params[0].errorTextView

        var result = 0
        var progress = 0
        val TOTAL_PROGRESS = 4
        try {
            //
            publishProgress(++progress, TOTAL_PROGRESS)

            publishProgress(++progress, TOTAL_PROGRESS)

            publishProgress(++progress, TOTAL_PROGRESS)

            publishProgress(++progress, TOTAL_PROGRESS)
            result = 1
        }
        catch (e: Exception) {
            Log.e(TAG, "problem background task", e)
        }

        return result
    }

    override fun onPreExecute() {
        super.onPreExecute()
        progressBar?.apply {
            progress = 0
            visibility = View.VISIBLE
        }
    }

    override fun onCancelled() {
        super.onCancelled()
        progressBar = null
        errorView = null
    }

    override fun onPostExecute(result: Int) {
        super.onPostExecute(result)
        if (result == 0) {
            errorView?.apply {
                text = "Could not sync with online library"
                visibility = View.VISIBLE
            }
            (onFailed ?: onComplete)?.run()
        }
        else {
            progressBar?.visibility = View.INVISIBLE
            onComplete?.run()
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        var ratio = 33
        try {
            ratio = (values[0]!!.toDouble()*100/values[1]!!.toDouble()).toInt()
        }
        catch (e: Exception) {
            Log.e(TAG, "problem with progress update", e)
        }
        progressBar?.progress = ratio
    }
}