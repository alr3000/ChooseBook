package com.hyperana.choosebook

import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import java.io.BufferedOutputStream
import java.io.File
import java.net.URL

/**
 * Created by alr on 2/26/18.
 */
class ByteArrayRequest(
        val url: URL,
         val listener: Response.Listener<ByteArray>?,
        errorListener: Response.ErrorListener) :
        Request<ByteArray>(
                Request.Method.GET,
                url.toExternalForm(), // toString() returns only path, no scheme or host
                errorListener
        ) {

    val TAG = "ByteArrayRequest"

    override fun deliverResponse(p0: ByteArray?) {
//        Log.d(TAG, "deliverResponse: " + p0?.size)
        listener?.onResponse(p0)
    }

    override fun parseNetworkResponse(p0: NetworkResponse?): Response<ByteArray> {
//       Log.d(TAG, "parseResponse: " + p0?.data?.size)
        return p0?.data?.let { Response.success(it, HttpHeaderParser.parseCacheHeaders(p0)) } ?:
                Response.error(VolleyError("No Data!"))
    }
}