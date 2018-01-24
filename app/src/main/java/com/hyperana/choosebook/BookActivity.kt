package com.hyperana.choosebook

import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import java.io.File

val EXTRA_URI_STRING = "uriString"

class BookActivity :
        AppCompatActivity(),
        PageFragment.OnFragmentInteractionListener,
        PageListener,
        LoaderManager.LoaderCallbacks<Book>
{

    val TAG = "BookActivity"
    var loaderId = 0
    var book: Book? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")
            setContentView(R.layout.activity_book)


            //get book file uri from intent
            val loaderArgs = Bundle().apply {
                putString(EXTRA_URI_STRING, intent.data.toString())
            }

            // start book load
            loaderId = (Math.random()*1000).toInt()
            supportLoaderManager.initLoader(loaderId, loaderArgs, this)

        }
        catch (e: Exception) {
            Log.e(TAG, "problem onCreate", e)
        }
    }

  /*  // handle user click to change page
    override fun onPageChange(toName: String) : Boolean {
        Log.d(TAG, "onPageChange to " + toName)
        try {
            setPage(book!!.pages[toName]!!)
        }
        catch (e: Exception) {
            Log.e(TAG, "problem onPageChange to " + toName, e)
        }
        return false
    }*/

    //todo: -L- touch zooms/pans in a spiral or masks on larger screens and reads text until release?
    //todo: -L- items with "touch" property can have .wav or alt image or sprite

    //todo: -L- save page on activity.backpressed, offer choice to resume
    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()


    }

    //**************** AsyncBookLoader Callbacks ********************
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Book>? {
        try {
            Log.d(TAG, "onCreateLoader: " + id)

            return AsyncBookLoader(
                    this,
                    Uri.parse(args!!.getString(EXTRA_URI_STRING)),
                    BOOK_FILENAME)
        }
        catch (e: Exception) {
            Log.e(TAG, "problem creating book loader", e)
            return null
        }
    }

    override fun onLoadFinished(loader: Loader<Book>?, data: Book?) {
        Log.d(TAG, "onLoaderFinished: " + data?.parentUri)
        try {
            book = data!!

            //set activity title
            title = book!!.title

            book!!.createPages()

            //display first page of book
            Log.d(TAG, "set first page fragment...")
            book!!.pages.entries.first().toPair().also {
                (name, contents) ->
                //check that this activity is still on top
                supportFragmentManager.beginTransaction()
                        .replace(
                                R.id.page_fragment_container,
                                PageFragment.newInstance(contents, this),
                                name
                        )
                        .commitAllowingStateLoss() //Should check if activity is still on top?
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "problem with loaded book json", e)
            book = null
        }

    }

    // loader is destroyed -- data (book) is unavailable
    override fun onLoaderReset(loader: Loader<Book>?) {
        Log.d(TAG, "onLoaderReset: " + loader?.id)
        book = null
    }

    override fun onPageChange(toName: String): Boolean {
        Log.d(TAG, "onPageChange to " + toName)
        book?.pages?.get(toName)?.also {
             setPage(Pair(toName, it))
        }
        return true
    }

    override fun onFragmentInteraction(uri: Uri) {
        Log.d(TAG, "onFragmentInteraction: " + uri)
    }

    fun setPage(page: Pair<String,List<PageItem>>) {
        supportFragmentManager.beginTransaction()
                //.addToBackStack(page.first) -- don't want to reverse through pages with back button
                .replace(
                        R.id.page_fragment_container,
                        PageFragment.newInstance(page.second, this),
                        page.first
                )
                .commit()

    }

}
