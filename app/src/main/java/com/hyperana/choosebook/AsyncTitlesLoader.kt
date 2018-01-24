package com.hyperana.choosebook

import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection

/**
 * Created by alr on 1/22/18.
 */
class AsyncTitlesLoader(
        context: Context,
        val url: URL,
        val loaderCallbacks: LoaderManager.LoaderCallbacks<List<String>>
) : AsyncTaskLoader<List<String>>(context) {

    val TAG = "AsyncTitlesLoader"
    val BOOK_PATH_ARRAY_KEY = "books"


    init {
        //otherwise it will not be started by manager:
        //https://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground#answer-1661495
        onContentChanged()
    }


    override fun loadInBackground(): List<String> {
        Log.d(TAG, "loadInBackground")

        var result = listOf<String>()
        if (!this.isAbandoned) {
            try {
                val jsonArray = JSONObject(downloadUrl(url)).getJSONArray(BOOK_PATH_ARRAY_KEY)
                result = (0..jsonArray.length() - 1).map {
                    jsonArray[it] as String
                }
            } catch(e: Exception) {
                Log.e(TAG, "problem parsing string", e)
            }
        }
        return result
        //return listOf("carnival")
    }

    // must implement these or loadInBackground is never called.
    // Also, call onContentChanged() in constructor or when initialized by loaderManager
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

    /***
    /**
     * Implementation of AsyncTask designed to fetch data from the network.
     */
    private class DownloadTask extends AsyncTask<String, Integer, DownloadTask.Result> {

        private DownloadCallback<String> mCallback;

        DownloadTask(DownloadCallback<String> callback) {
            setCallback(callback);
        }

        void setCallback(DownloadCallback<String> callback) {
            mCallback = callback;
        }

        /**
         * Wrapper class that serves as a union of a result value and an exception. When the download
         * task has completed, either the result value or exception can be a non-null value.
         * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
         */
        static class Result {
            public String mResultValue;
            public Exception mException;
            public Result(String resultValue) {
                mResultValue = resultValue;
            }
            public Result(Exception exception) {
                mException = exception;
            }
        }

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected DownloadTask.Result doInBackground(String... urls) {
            Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    String resultString = downloadUrl(url);
                    if (resultString != null) {
                        result = new Result(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                } catch(Exception e) {
                    result = new Result(e);
                }
            }
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(Result result) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.updateFromDownload(result.mException.getMessage());
                } else if (result.mResultValue != null) {
                    mCallback.updateFromDownload(result.mResultValue);
                }
                mCallback.finishDownloading();
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(Result result) {
        }
        ...
    }
    Using HttpsUrlConnection to Fetch Data
    In the snippet above, the doInBackground() method runs in a background thread and calls the helper method downloadUrl(). The downloadUrl() method should take the given URL and use it to perform an HTTP GET request. Once a connection has been established, you should use the method getInputStream() to retrieve the data as an InputStream. The following snippet uses the HttpsURLConnection API to accomplish this:

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    private String downloadUrl(URL url) throws IOException {
        InputStream stream = null;
        HttpsURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
            if (stream != null) {
                // Converts Stream to String with max length of 500.
                result = readStream(stream, 500);
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
        return result;
    }
    ***/




