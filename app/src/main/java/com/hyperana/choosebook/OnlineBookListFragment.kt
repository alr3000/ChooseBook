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



class OnlineBookListFragment : android.support.v4.app.Fragment(),
        LoaderManager.LoaderCallbacks<Book>,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener
{

    val TAG = "OnlineBookListFragment"

    // ListAdapter for fragment's listview
    var listAdapter: BookListAdapter? = null
    var bookListView: ListView? = null

    // id to distinguish loader for titles file from book loader
    val TITLES_LOADER = 768570
    val loadTitlesListener = object: LoaderManager.LoaderCallbacks<List<String>> {
        override fun onLoaderReset(loader: Loader<List<String>>?) {
            Log.d(TAG, "titlesLoaderReset")
        }

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<String>> {
            Log.d(TAG, "titlesCreateLoader")
            val url = URL(S3_URL, TITLES_FILE)

            return AsyncTitlesLoader(activity, url, this)
        }

        override fun onLoadFinished(loader: Loader<List<String>>?, data: List<String>?) {
            Log.d(TAG, "titlesLoadFinished")
                    data?.onEach {
                        //load book json and create stub in temp filesystem (online dir)
                        val uri = URL(S3_URL, it).toURI().toString()
                        val loaderId = (Math.random() * 1000).toInt()
                        activity.supportLoaderManager.initLoader(
                                loaderId,
                                Bundle().apply {
                                    putCharSequence(EXTRA_URI_STRING, uri)
                                },
                                this@OnlineBookListFragment
                        )
                    }
        }
    }

    // Loader callbacks for MultiResourceLoader to download and store a book's resources
    val MULTI_RES_LOADER_ID = 343276
    val EXTRA_URI_STRING_ARR = "uriStringArrayList"
    val resourceLoadListener = object: LoaderManager.LoaderCallbacks<Int> {

        var activeView: View? = null

        override fun onLoaderReset(p0: Loader<Int>?) {
            Log.d(TAG, "resourceLoaderReset")
            activeView?.isActivated = false
        }

        override fun onLoadFinished(p0: Loader<Int>?, p1: Int?) {
            Log.d(TAG, "resourceLoadFinished: " + p1)
            try {
                //todo: refresh list against offline items -- disable duplicates

                activeView?.isActivated = false
                Toast.makeText(activity, R.string.download_book_finished, Toast.LENGTH_LONG).show()
            }
            catch (e: Exception) {
                Log.e(TAG, "problem after loading book", e)
            }

        }

        override fun onCreateLoader(p0: Int, p1: Bundle?): android.support.v4.content.Loader<Int> {
            Log.d(TAG, "resourceCreateLoader")
            activeView = view?.findViewWithTag(MULTI_RES_LOADER_ID)?.apply { isActivated = true }

            return AsyncMultiResLoader(
                    context = activity,
                    uriList = p1?.getStringArrayList(EXTRA_URI_STRING_ARR)?.toList()!!.also {
                        Log.d(TAG, "loading " + it.count() + " items")
                    },
                    toDirPath = p1.getString(EXTRA_URI_STRING)!!,
                    loaderCallbacks = this
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")

            //initialize adapter
            listAdapter = BookListAdapter(activity)
            sync()
        }
        catch (e: Exception) {
            Log.e(TAG, "problem creating fragment", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
            // last param is false because fragmentManager will attach this view later
            return inflater!!.inflate(R.layout.content_scrolling_library, container, false)
        }
        catch (e: Exception) {
            Log.e(TAG, "problem creating view", e)
            return View(activity)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        bookListView = (view!!.findViewById(R.id.booklist) as ListView).also {
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


        }
        catch (e: Exception) {
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
                        startBookActivity(book)
                        dialog.dismiss()
                    }
                    catch (e: Exception) {
                        Log.e(TAG, "problem starting book activity from dialog " + book.path, e)
                    }
                })
                setPositiveButton(R.string.alert_download, {
                    dialog: DialogInterface, which: Int ->
                    try {
                        saveBook(book)
                        dialog.dismiss()
                    }
                    catch(e: Exception) {
                        Log.e(TAG, "problem saving book " + book.path)
                    }

                })
                setNegativeButton(R.string.alert_cancel,  {
                    dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                 })
                create()
                show()
            }
        }
        catch (e: Exception) {
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
                        startBookActivity(it)
                    }
        }
        catch(e: Exception) {
            Log.e(TAG, "problem starting book activity", e)
        }
    }

    //*********************** asyncBookLoader Callbacks *****************
    override fun onLoaderReset(p0: Loader<Book>?) {
        Log.d(TAG, "bookLoaderReset")
    }

    // todo: identify books already loaded? offer to replace on longclick?
    override fun onLoadFinished(p0: Loader<Book>?, p1: Book?) {
        Log.d(TAG, "bookLoadFinished")
        if (p1 != null)
        listAdapter!!.list = listAdapter!!.list.plus(p1)
    }

    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<Book> {
        Log.d(TAG, "bookCreateLoader")

        return AsyncBookLoader(activity, Uri.parse(p1?.getString(EXTRA_URI_STRING)), BOOK_FILENAME)

    }

    fun sync() {
        Log.d(TAG, "SYNC")
        //onlineListAdapter.notifyDataSetInvalidated() -- don't do this: invalidates adapter!
        listAdapter?.list = listOf()

        loaderManager.initLoader<List<String>>(
                TITLES_LOADER, null,
                 loadTitlesListener)
    }

    fun isDownloaded(id: String) : Boolean {
        return (File(activity.getFilesDir(), OFFLINE_BOOKS_DIR).resolve(id).exists()
                || activity.assets.list(ASSETS_BOOK_DIR).contains(id))
    }

    //save book json and resources to bookpath in filesdir
    fun saveBook(book:Book) {
        val dir = File(File(activity.filesDir, OFFLINE_BOOKS_DIR), book.path)
        Log.d(TAG, "saveBook: " + dir.path)
        if (!dir.exists() && !dir.mkdirs()) {
            throw Exception("could not open file: " + dir.path)
        }

     /*   // save book
        File(dir, BOOK_FILENAME)
                .let {
                    it.createNewFile()
                    it.outputStream()
                }
                .apply {
                    write(book.jsonString!!.toByteArray())
                    close()
                }
        */
        // save book resources, including json
        activity.supportLoaderManager.initLoader(
                MULTI_RES_LOADER_ID,
                Bundle().apply {
                    putStringArrayList(
                            EXTRA_URI_STRING_ARR,
                            ArrayList(book.resources.map { book.getResourceUri(it).toString() })
                                    .also { it.add(book.getResourceUri(BOOK_FILENAME).toString())}
                    )
                    putCharSequence(EXTRA_URI_STRING, dir.path)
                },
                resourceLoadListener
        )
    }

    //startBookActivity(bookpath)
    //todo: if book in offline library, use that one
    fun startBookActivity(book: Book) {
        Log.d(TAG, "start book activity: " + book.parentUri)
        val myIntent = Intent(activity, BookActivity::class.java)
        myIntent.putExtra(EXTRA_BOOKPATH, book.path)
        myIntent.data = book.parentUri
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
