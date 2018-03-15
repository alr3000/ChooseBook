package com.hyperana.choosebook

import android.app.AlertDialog
import android.support.v4.app.LoaderManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v4.content.Loader
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroupOverlay
import android.widget.AdapterView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OfflineBookListFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OfflineBookListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */


// S3 access constants
val S3_URL = URL("http://choose-book.s3.amazonaws.com/")
val TITLES_FILE = "books584694858739230987.json"
val TITLES_ARRAY_KEY = "books"

val SAVE_BOOK_REQUEST = 58763
val OPEN_BOOK_REQUEST = 45



class OnlineBookListFragment : android.support.v4.app.Fragment(),
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    val TAG = "OnlineBookListFragment"

    var queue: RequestQueue? = null

    // ListAdapter for fragment's listview
    var listAdapter: BookListAdapter? = null
    var bookListView: ListView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")
            queue = (activity!!.application as App).requestQueue!!

            //initialize adapter
            listAdapter = BookListAdapter(activity!!)
            sync()
        } catch (e: Exception) {
            Log.e(TAG, "problem creating fragment", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
            // last param is false because fragmentManager will attach this view later
            return inflater.inflate(R.layout.content_scrolling_library, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "problem creating view", e)
            return View(activity)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        bookListView = (view.findViewById<ListView>(R.id.booklist)).also {
            it.adapter = listAdapter
            it.onItemClickListener = this  //start book activity
            it.onItemLongClickListener = this //remove book from device

        }
    }

    // given context, populate asset list and start observer for downloaded books
    override fun onAttach(context: Context?) {
        try {
            Log.d(TAG, "onAttach")
            super.onAttach(context)


        } catch (e: Exception) {
            Log.e(TAG, "problem on attaching offline list fragment", e)
        }
    }

    override fun onDetach() {
        super.onDetach()
    }

    // ************************** ListView Item UI Callbacks *****************************
    // long click prompts for open online or download
    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        try {
            Log.d(TAG, "onItemLongClick: " + position)

            val book = (parent?.adapter?.getItem(position) as Book)
            AlertDialog.Builder(activity).apply {
                setMessage(resources.getString(R.string.alert_download_book).plus(" " + book.title))
                setNeutralButton(R.string.alert_open, {
                    dialog: DialogInterface, which: Int ->
                    try {
                        dialog.dismiss()
                        openBook(book)
                    } catch (e: Exception) {
                        Log.e(TAG, "problem starting book activity from dialog " + book.path, e)
                    }
                })
                setPositiveButton(R.string.alert_download, {
                    dialog: DialogInterface, which: Int ->
                    try {
                        dialog.dismiss()
                        saveBook(book)
                    } catch(e: Exception) {
                        Log.e(TAG, "problem saving book " + book.path, e)
                    }

                })
                setNegativeButton(R.string.alert_cancel, {
                    dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                })
                create()
                show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "problem download book", e)
        }
        return true
    }


    // click downloads if necessary, starts book activity
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        try {
            Log.d(TAG, "onItemClick: " + position)
            (parent?.adapter?.getItem(position) as Book)
                    .also {
                        openBook(it)
                    }
        } catch(e: Exception) {
            Log.e(TAG, "problem starting book activity", e)
        }
    }


    fun sync() {
        Log.d(TAG, "SYNC")

        //onlineListAdapter.notifyDataSetInvalidated() -- don't do this: invalidates adapter!
        // instead, just empty the list
        listAdapter?.list = listOf()


        // load titles page and initiate load of each book json into memory
        queue!!.add(createTitlesRequest())

    }

    fun isDownloaded(id: String) : Boolean {
        return ((activity!!.application as App).savedBookDir!!.resolve(id).exists()
                || activity!!.assets.list(ASSETS_BOOK_DIR).contains(id))
    }

    // get titles (bookpaths) array from special s3 index file
    // add requests for each book json
    fun createTitlesRequest() : JsonObjectRequest {
        return JsonObjectRequest(
                URL(S3_URL, TITLES_FILE).toString(),
                null,
                { jsonObject: JSONObject ->
                    try {
                        val jArr = jsonObject.getJSONArray(TITLES_ARRAY_KEY)
                        (0..jArr.length() - 1)
                                .map { jArr[it] as String }
                                .onEach {
                                    queue!!.add(createBookStubRequest(it))
                                }
                    }
                    catch (e: Exception) {
                        Log.e(TAG, "failed parse titles request", e)
                    }
                },
                { volleyError: VolleyError -> Log.e(TAG, "failed load titles", volleyError)}
        ).also {
            it.tag = TAG
        }
    }

    // get book json file from s3 sub-bucket
    // create Book and add to adapter's list
    fun createBookStubRequest(bookPath: String) : StringRequest {
        val uri = Uri.parse(URL(S3_URL, bookPath).toString())
        return StringRequest(
                uri.buildUpon().appendPath(BOOK_FILENAME).toString(),
                {
                    string: String ->
                    try {
                        listAdapter?.list = listAdapter?.list!!.plus(Book(
                                string,
                                bookPath,
                                uri
                        ))
                    }
                    catch(e: Exception) {
                        Log.e(TAG, "failed parse book stub", e)
                    }
                },
                { volleyError -> Log.e(TAG, "failed load book stub: " + bookPath, volleyError)}
        ).also {
            it.tag = TAG
        }
    }


    // save book temporarily into cacheDir/bookpath before opening for "online" viewing
    // todo: check if the book is already downloaded and use that one instead
    fun openBook(book: Book) {
        val tempDir = (activity!!.application as App).tempBookDir
        tempDir!!.deleteRecursively()

        val intent = Intent(activity, com.hyperana.choosebook.LoadAllActivity::class.java)
        intent.apply {
            putExtra(EXTRA_URI_STRING, File(tempDir, book.path).path)
            putExtra(EXTRA_URI_STRING_ARR, book.resources.map {
                book.getResourceUri(it).toString()
            }.toTypedArray())
        }
        startActivityForResult(intent, OPEN_BOOK_REQUEST)
    }

    //save book json and resources to bookpath in filesdir
    fun saveBook(book:Book) {

        Log.d(TAG, "saveBook: " + book.parentUri)

        val intent = Intent(activity, com.hyperana.choosebook.LoadAllActivity::class.java)

        intent.apply {
            putExtra(EXTRA_BOOKPATH, book.path)
            putExtra(EXTRA_URI_STRING, File((activity!!.application as App).savedBookDir, book.path).toURI().toString())
            putExtra(EXTRA_URI_STRING_ARR, book.resources.map {
                book.getResourceUri(it).toString()
            }.toTypedArray())
        }

        startActivityForResult(intent, SAVE_BOOK_REQUEST)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            when (requestCode) {
                SAVE_BOOK_REQUEST -> {
                    Log.d(TAG, "result from save book: " + resultCode)
                    Toast.makeText(
                            activity,
                            if (resultCode != 0) "saved book for offline use" else "Could not save book!",
                            Toast.LENGTH_SHORT
                    ).show()
                }
                OPEN_BOOK_REQUEST -> {
                    Log.d(TAG, "result from open book: " + resultCode)
                    val bookUriString = Uri.parse(data?.getStringExtra(EXTRA_URI_STRING))
                    if (resultCode != 0) {
                        startBookActivity(
                                bookUriString,
                                bookUriString!!.lastPathSegment
                        )
                    }
                    else {
                        Toast.makeText(
                                activity,
                                data?.getStringExtra(EXTRA_MESSAGE) ?: "Could not open book!",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "failed return from save or open activity", e)
        }
    }


    //startBookActivity(bookpath)
    fun startBookActivity(parentUri: Uri, bookPath: String) {
        Log.d(TAG, "start book activity: " + parentUri)

        val myIntent = Intent(activity, BookActivity::class.java)
        myIntent.putExtra(EXTRA_BOOKPATH, bookPath)
        myIntent.data = parentUri
        startActivity(myIntent)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val FILE_DIR = "bookDirectory"
        private val ASSET_DIR = "assetDirectory"
        // private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.

         * @param1 bookFilesDir Directory to observe to find books (after filesDir path)
         * *
         * @param2 assetDir "Directory" in assets to find books
         * *
         * @return A new instance of fragment OfflineBookListFragment.
         */
        //
        fun newInstance(bookFilesDir: String?, bookAssetDir: String?): OfflineBookListFragment {
            val fragment = OfflineBookListFragment()
            val args = Bundle()
            args.putString(FILE_DIR, bookFilesDir)
            args.putString(ASSET_DIR, bookAssetDir)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
