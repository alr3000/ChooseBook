package com.hyperana.choosebook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.os.FileObserver
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OfflineBookListFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OfflineBookListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineBookListFragment : android.support.v4.app.Fragment(),
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener{

    val TAG = "OfflineBookListFragment"


   // Booklists
    var assetBookList: List<Book> = listOf()
        set(value) {
            field = value
            offlineListAdapter?.list = assetBookList.plus(downloadBookList)
        }
    var downloadBookList: List<Book> = listOf()
        set(value) {
            field = value
            offlineListAdapter?.list = assetBookList.plus(downloadBookList)
        }

    // ListAdapter for fragment's listview
    var offlineListAdapter: BookListAdapter? = null


    // Book directories:
    var filesPath: String? = null
    var assetPath: String? = null

    var fileObserver: FileObserver? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
//            Log.d(TAG, "onCreate")

            // set paths to watch
            filesPath = (activity!!.application as App).savedBookDir!!.path
            assetPath = ASSETS_BOOK_DIR


            fileObserver = object : FileObserver(filesPath) {
                    val TAG = "BookDirObserver"

                    override fun startWatching() {
//                        Log.d(TAG, "startWatching " + filesPath)
                        super.startWatching()
                        activity!!.runOnUiThread {
                            updateDownloadsList()
                        }
                    }

                    override fun stopWatching() {
//                        Log.d(TAG, "StopWatching " + filesPath)
                        super.stopWatching()
                    }

                    override fun onEvent(event: Int, path: String?) {
                        when (event and FileObserver.ALL_EVENTS) {
                            FileObserver.CREATE,
                            FileObserver.DELETE -> {
//                                Log.d(TAG, "event: " + event + " - " + path)
                                if (path != null) {
                                    activity!!.runOnUiThread {
                                        updateDownloadsList()
                                    }
                                }
                            }
                            else -> {
//                                Log.d(TAG, "non-event: " + event + " - " + path)
                            }
                        }
                    }

            }

            //initialize adapter
            offlineListAdapter = BookListAdapter(activity!!)
            updateAssetsList(activity!!.assets)
            fileObserver?.startWatching()
        }
        catch (e: Exception) {
//            Log.e(TAG, "problem creating fragment", e)
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        try {
            // last param is false because fragmentManager will attach this view later
            return inflater.inflate(R.layout.content_scrolling_library, container, false)
        }
        catch (e: Exception) {
//            Log.e(TAG, "problem creating view", e)
            return View(activity)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "onViewCreated")
        (view.findViewById<ListView>(R.id.booklist)).also {
            it.adapter = offlineListAdapter
            it.onItemClickListener = this  //start book activity
            it.onItemLongClickListener = this //remove book from device

        }
    }

    // given context, populate asset list and start observer for downloaded books
    override fun onAttach(context: Context?) {
        try {
//            Log.d(TAG, "onAttach")
            super.onAttach(context)


    }
        catch (e: Exception) {
//            Log.e(TAG, "problem on attaching offline list fragment", e)
        }
    }

    override fun onDetach() {
        super.onDetach()
        fileObserver?.stopWatching()
    }

    // long click prompts for remove (not applicable to assets)
    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        try {
//            Log.d(TAG, "onItemLongClick: " + position)
            val book = (parent?.adapter?.getItem(position) as Book)
            if (downloadBookList.contains(book)) {
                promptRemoveBook(book)
                return true
            }
        }
        catch (e: Exception) {
//            Log.e(TAG, "problem delete book", e)
        }
        return false
    }

    // click downloads if necessary, starts book activity
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        try {
//            Log.d(TAG, "onItemClick: " + position)
            (parent?.adapter?.getItem(position) as Book)
                    .also {
                        startBookActivity(it!!)
                    }
        }
        catch(e: Exception) {
//            Log.e(TAG, "problem starting book activity", e)
        }
    }


    fun updateAssetsList(assets: AssetManager) {
        assetBookList = assets.list(ASSETS_BOOK_DIR)
                // remove invisible automatic asset folders -- others may exist
                ?.filter{ !listOf("images", "sounds", "webkit").contains(it) }
                ?.map { BookFromAsset(assets, it) }
                ?.filterNotNull()
                ?: listOf()
    }

    //todo: -L- update the downloads
    fun updateDownloadsList() {
        //listFiles is null if empty
        downloadBookList =  File(filesPath).listFiles()
                ?.map {
                    Book (
                            path = it.name,
                            uri = Uri.fromFile(it),
                            jsonString = it.listFiles()?.find { it.extension == "json" }?.inputStream()?.let {
                                loadString(it)
                            } ?: ""
                    )
                }
                ?.filterNotNull()
                ?: listOf()
    }

    //prompts remove, open, cancel
    fun promptRemoveBook(book: Book) {
        AlertDialog.Builder(activity).apply {
            setMessage(resources.getString(R.string.alert_delete_book).plus(" " + book.title))
            setPositiveButton(R.string.alert_delete, {
                dialog: DialogInterface, which: Int ->
                deleteBook(book.path!!)
                dialog.dismiss()
            })
            setNegativeButton(R.string.alert_cancel,  {
                dialog: DialogInterface, which: Int ->
                dialog.dismiss()
            })
            create()
            show()
        }
    }

    //removeDownloadedBook(bookpath)
    fun deleteBook(bookPath: String) {
//        Log.d(TAG, "deleteBook: " + bookPath)
        val dir = File(filesPath).resolve(bookPath)
        if (!dir.exists() || !dir.deleteRecursively()) {
            throw Exception("book directory not deleted at " + dir.path)
        }
    }

    //startBookActivity(bookpath)
    fun startBookActivity(book: Book) {
//        Log.d(TAG, "start book activity: " + book.parentUri)
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

        fun onFragmentInteraction(uri: Uri)
    }

    companion object {

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
