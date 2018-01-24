package com.hyperana.choosebook

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.net.ConnectivityManager
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
//import jdk.nashorn.internal.runtime.PropertyDescriptor.GET




//IF WIFI: download contents list of books not already on the device and give to library to store
//todo: upload usage statistics
//when finished, start library activity

// use commandline to make dns work on emulator:
// Library/Android/sdk/emulator/emulator -avd Nexus_5X_API_24 -dns-server 8.8.8.8


class StartActivity : AppCompatActivity() {
    val TAG = "StartActivity"
    val TITLES_URL = "http://www.jir.com"
    val TITLES_FILE = "available_titles"

    var loadingView: View? = null
    var resultText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        try {
            //check first if connected by wifi
            val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (conn.activeNetworkInfo.isConnected
                    ) {// && (conn.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI)) {

                loadingView = findViewById(R.id.loading_view).apply {
                    visibility = View.VISIBLE
                }

                //todo: make toast?
                resultText = (findViewById(R.id.result_text) as? TextView)

                // Instantiate the RequestQueue.
                val queue = Volley.newRequestQueue(this)

                // Request a string response from the provided URL.
                val stringRequest = StringRequest(
                        Request.Method.GET,
                        TITLES_URL,
                        { response ->
                                // Display the first 500 characters of the response string.
                                onLoadingComplete("Response is: " + response.substring(0, 50))
                        },
                        { error ->
                                Log.e(TAG, "error loading titles: " + error.message)
                                onLoadingComplete()
                        }
                )
                // Add the request to the RequestQueue.
                queue.add(stringRequest)
            } else {
                Log.d(TAG, "not connected to wifi ("+ conn.activeNetworkInfo.typeName + "), so do not fetch titles")
                startLibraryActivity()
            }
        }
        catch(e: Exception) {
            Log.e(TAG, "problem on create", e)
        }

    }

    fun onLoadingComplete(data: String? = null) {
        loadingView?.visibility = View.GONE
        resultText?.text = resources.getString(
                if (data != null) R.string.library_sync_success else R.string.library_sync_failed
        )


        startLibraryActivity()
    }

    fun startLibraryActivity() {
        Log.d(TAG, "startLibraryActivity")

        //start library activity after 1 second delay
        Handler().postDelayed({
            startActivity(Intent(this, ScrollingLibraryActivity::class.java))

            // prevent return to this page by back button
            finish()
        }, 1000)
     }
}
