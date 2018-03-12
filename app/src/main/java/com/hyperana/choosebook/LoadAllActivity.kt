package com.hyperana.choosebook

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.*
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import java.io.File
import java.io.OutputStream
import java.net.URL

/**
 * Created by alr on 2/21/18.
 */
val EXTRA_URI_STRING_ARR = "uriArray"

class LoadAllActivity : AppCompatActivity() {

    val TAG = "LoadAllActivity"
    var queue: RequestQueue? = null


    val EXTRA_REQUESTED_URLS = "requestedUrls"
    val EXTRA_ID = "groupId"
    var toDir: File? = null
    var requests: MutableList<CharSequence> = mutableListOf()
    var groupId: String = randomString(8)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")

            setContentView(R.layout.fragment_loading)


            queue = (applicationContext as App).requestQueue!!

            // if interrupted, get previously requested items and use same id
            requests = savedInstanceState?.getCharSequenceArray(EXTRA_REQUESTED_URLS)
                    ?.toMutableList() ?: requests

            groupId = savedInstanceState?.getString(EXTRA_ID) ?: groupId

            //get toDir name, and urlString list from bundle/arguments
            toDir = intent.getStringExtra(EXTRA_URI_STRING)!!
                    .let { File(Uri.parse(it).path) }
                    .apply {
                        if (!exists() && !mkdirs()) {
                            throw Exception("could not make book dir within directory: " + path)
                        }
                        Log.d(TAG, "created receiving directory " + path)
                    }

            // start requests
            intent.getCharSequenceArrayExtra(EXTRA_URI_STRING_ARR)!!
                    .toList()
                    .onEach {
                        try {
                            val uri = Uri.parse(it as String)
                            val file = File(toDir!!, uri.lastPathSegment)
                            Log.d(TAG, "requesting: " + it + " as " + file.path)
                            if (requests.contains(file.path)) {
                                Log.d(TAG, "already loaded: " + it + " as " + file.path)
                            } else {
                                queue!!.add(createFileRequest(URL(uri.toString()), file).apply { tag = groupId })
                                requests.add(file.path)
                            }
                        } catch(e: Exception) {
                            Log.e(TAG, "failed to add request: " + it, e)
                            cancelOnError("load failed: invalid request!")
                        }
                    }

            checkIfComplete()


        } catch (e: Exception) {
            Log.e(TAG, "problem on create loader activity", e)
            cancelOnError("load failed: invalid book!")
        }
    }

    // save current requests, id for configuration change
    override fun onSaveInstanceState(outState: Bundle?) {
        Log.d(TAG, "onSaveInstanceState: " + requests.count())

        outState?.apply {
            putCharSequenceArray(EXTRA_REQUESTED_URLS, requests.toTypedArray())
            putCharSequence(EXTRA_ID, groupId)
        }


        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: " + requests.count() + " to be cancelled")
        queue?.cancelAll(groupId)

        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()

    }


    //******************************** *************************************
    fun createFileRequest(url: URL, file: File): ByteArrayRequest {
        return ByteArrayRequest(
                url,
                object : Response.Listener<ByteArray> {
                    override fun onResponse(p0: ByteArray?) {
                        Log.d(TAG, "onResponse: " + p0?.size)
                        requests.remove(file.path)

                        try {
                            saveBytesToFile(p0!!, file)
                        }
                        catch (e: Exception) {
                            Log.e(TAG, "failed save file " + file.path, e)
                            cancelOnError("save failed!")
                        }

                        checkIfComplete()

                    }
                },
                object : Response.ErrorListener {
                    override fun onErrorResponse(p0: VolleyError?) {
                        Log.e(TAG, "onVolleyError: ", p0)

                        // stop requests, cancel activity:
                        cancelOnError("load failed: invalid data!")

                    }
                }
        )
    }

    fun saveBytesToFile(bytes: ByteArray, file: File) {
        var fos: OutputStream? = null
        try {
            fos = file.outputStream()
            fos.write(bytes)
        }
        finally {
            fos?.close()
        }
    }

    fun checkIfComplete() {
        Log.d(TAG, requests.count().toString() + " requests pending")
        if (requests.count() == 0) {
            Log.d(TAG, "loading complete for " + toDir?.toString())

            setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_URI_STRING, Uri.fromFile(toDir!!).toString())
            })
            finish()
        }
    }


    fun cancelOnError(message: String) {
        queue?.cancelAll(groupId)
        setResult(Activity.RESULT_CANCELED, Intent().apply {
            putExtra(EXTRA_URI_STRING, toDir?.path)
            putExtra(EXTRA_MESSAGE, message)
        })
        finish()
    }

}
