package com.hyperana.choosebook

import android.util.Log
import android.content.Context
import android.content.res.AssetManager
import android.os.AsyncTask
import org.json.JSONArray
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import java.io.*


/**
 * Created by alr on 1/8/18.
 *
 * Moves asset files into internal memory, retaining organization
 *
 * fromPath: path string to navigate in assets to root of files to be copied (assets.list(fromPath))
 * toDir: Existing file object describing app data subdirectory to copy files into
 * (from getFilesDir(somePath)).
 *
 *
 */
class AsyncMoveAssetsParams(val assets: AssetManager, val fromPath: String = "", val toDir: File)

open class AsyncMoveAssetsTask: AsyncTask<AsyncMoveAssetsParams, Float, Int>() {

    val TAG = "AsyncMoveAssetsTask"

    override fun doInBackground(vararg params: AsyncMoveAssetsParams): Int {
        try {
            Log.d(TAG, "moveAssets doInBackground: " + params[0].toString())
            val assets = params[0].assets
            val fromPath = params[0].fromPath
            val toDir = params[0].toDir

            // output:
            var outFileCount = 0
            var inFileCount = 0

            fun moveRecursive(path: String) {
                if (isCancelled) {
                    throw Exception("cancelled")
                }

                // if directory, call this function on children
                if (File(path).extension == "") {
                    assets.list(path).toList().onEach {
                        moveRecursive(File(path, it).path)
                    }
                }

                // if file, copy to app data
                else {
                    inFileCount++
                    var fis: InputStream? = null
                    var fos: OutputStream? = null

                    try {
                        // create file to receive asset data
                        val outFile = File(toDir, path)
                        if (!outFile.parentFile.mkdirs() || !outFile.createNewFile()) {
                            throw Exception("Could not make directory path")
                        }

                        // write asset data to file
                        val size = assets.openFd(path).length
                        fis = assets.open(path)
                        fos = outFile.outputStream()
                        fos.write(fis.readBytes(size.toInt()))

                        outFileCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "problem copying file: " + path, e)
                    } finally {
                        fos?.close()
                        fis?.close()
                    }
                }
            }

            // move files:
            moveRecursive(fromPath)
            if (outFileCount != inFileCount) {
                throw Exception((outFileCount - inFileCount).toString() + "/" + inFileCount +
                        " files not copied!")
            }

            // return number of files successfully moved
            return outFileCount

        } catch (e: Exception) {
            Log.e(TAG, "moveAssets failed", e)
            //todo: delete half-created app data directories
            return 0
        }
    }

}