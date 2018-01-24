package com.hyperana.choosebook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import android.R.attr.bitmap
import java.io.*


/**
 * Created by alr on 12/30/17.
 */

/**
 * Given a URL, sets up a connection and gets the HTTP response body from the server.
 * If the network request is successful, it returns the response body in String form. Otherwise,
 * it will throw an IOException.
 *
 * returns string conversion of stream or null if saved to provided file. throws exception if failed
 */
fun downloadUrl(url: URL, saveTo: File? = null): String? {
    var stream: InputStream? = null
    var connection: HttpURLConnection? = null
    var result: String? = null
    try {
        connection = url.openConnection() as HttpURLConnection
        // Timeout for reading InputStream arbitrarily set to 3000ms.
        connection.setReadTimeout(3000)
        // Timeout for connection.connect() arbitrarily set to 3000ms.
        connection.setConnectTimeout(3000)
        // For this use case, set HTTP method to GET.
        connection.setRequestMethod("GET")
        // Already true by default but setting just in case; needs to be true since this request
        // is carrying an input (response) body.
        connection.setDoInput(true)
        // Open communications link (network traffic occurs here).
        connection.connect()
        Log.d("downloadUrl", "connected")
//            publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);
        val responseCode = connection.responseCode
        if (responseCode != HttpsURLConnection.HTTP_OK) {
            throw IOException("HTTP error code: " + responseCode);
        }

        // Retrieve the response body as an InputStream.
        stream = connection.getInputStream()
        if (stream == null) {
            throw Exception("no inputStream on this connection")
        }
        Log.d("downloadUrl", "got inputstream: " + stream.available())


        val mimetype = connection.contentType.split("/").first()
        Log.d("downloadUrl", "found " + mimetype)

        if ((saveTo == null) && listOf("text", "application").contains(mimetype)) {
            result = readStream(stream, 10000)
        }
        else {
            saveBytes(stream, saveTo!!)
        }
    } finally {
        // Close Stream and disconnect HTTPS connection.
        if (stream != null) {
            stream.close();
        }
        if (connection != null) {
            connection.disconnect();
        }
    }
    return result
}

// does NOT close stream
fun saveBytes(stream: InputStream, file: File, maxReadSize: Int = 1024) {
    val maxReadSize = maxReadSize
    var bytesRead = 0

    // create output file writer
    val fos = file.apply {
        if (!exists() && !parentFile.exists() && !parentFile.mkdirs()) {
            throw Exception("could not create parent directory " + path)
        }
        if (!exists() && !createNewFile())  {
            throw Exception("could not create file " + path)
        }
        if (!canWrite()) {
            throw Exception("cannot write to file " + path)
        }
    }.outputStream()

    try {
        // create rawbuffer
        val data = ByteArray(maxReadSize)

        // read and write through raw buffer
        while (stream.read(data, 0, maxReadSize).also { bytesRead = it } >= 0) {
            fos.write(data, 0, bytesRead)
        }
    }
    finally {
        // close file stream
        fos.close()
       Log.d("saveBytes", bytesRead.toString() + "saved to " + file.path)
    }
}
/**
 * Converts the contents of an InputStream to a String.
 */
@Throws(IOException::class, UnsupportedEncodingException::class)
fun readStream(stream: InputStream, maxReadSize: Int): String {
    var maxReadSize = maxReadSize
    val reader = InputStreamReader(stream, "UTF-8")
    val rawBuffer = CharArray(maxReadSize)
    val buffer = StringBuffer()
    var readSize = reader.read(rawBuffer)
    while ((readSize  != -1) && maxReadSize > 0) {
        if (readSize > maxReadSize) {
            readSize = maxReadSize
        }
        buffer.append(rawBuffer, 0, readSize)
        maxReadSize -= readSize
        readSize = reader.read(rawBuffer)
    }
    return buffer.toString()
}

// loads from app memory (bmps already resized for app)
fun asyncSetImageBitmap(img: ImageView, path: String, bmpCache: LruCache<String, Bitmap>?) {
    val bmp = bmpCache?.get(path)
    if (bmp != null) {
        img.setImageBitmap(bmp)
        return
    }
    Log.d("IconData", "loading bitmap: " + path)
    val uiHandler = Handler()
    Thread({
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            uiHandler.post {
                Log.d("IconData", "bitmap loaded: " + path + "(" + bitmap.byteCount + ")")
                img.setImageBitmap(bitmap)
                bmpCache?.put(path, bitmap)
            }
        } catch (e: Exception) {
            Log.w("IconData", "set bitmap failed: " + path, e)
        }
    }).start()
}