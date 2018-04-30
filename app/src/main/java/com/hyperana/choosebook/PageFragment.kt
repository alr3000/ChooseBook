package com.hyperana.choosebook

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PageFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [PageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PageFragment : Fragment() {
    private val TAG = "PageFragment"

    // TODO: -L- formalize interaction with activity through fragmentinteractionlistener (Uri)
    private var mPage: List<PageItem>? = null

    private var mPageItemListener: PageItemListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
//            Log.d(TAG, "onCreateView")

            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_page, container, false)

        } catch (e: Exception) {
//            Log.e(TAG, "problem creating view", e)
            return View(activity)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {

//            Log.d(TAG, "onViewCreated")
            setPageView((view.findViewById<ViewGroup>(R.id.page_container)))

        }catch (e: Exception) {
//            Log.e(TAG, "problem setting page view", e)
        }

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
//        Log.d(TAG, "onAttach")
    }

    override fun onDetach() {
        super.onDetach()
  //      mPageItemListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }


    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.

         * @param param1 Parameter 1.
         * *
         * @param param2 Parameter 2.
         * *
         * @return A new instance of fragment PageFragment.
         */
        fun newInstance(page: List<PageItem>, pageItemListener: PageItemListener): Fragment {
//            Log.d("PageFragment", "newInstance with " + page.count() + " items")
            val fragment = PageFragment()
            fragment.mPage = page
            fragment.mPageItemListener = pageItemListener

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    fun setPageView(container: ViewGroup) {
//        Log.d(TAG, "setPageView with " + mPage?.count() + " items")

        if (mPage != null) {

            // remove default view if any
            container.removeAllViews()

            // add pageitems
            mPage!!.onEach {
                try {
                    it.getView(container, null, mPageItemListener)
                }
                catch(e:Exception) {
//                    Log.e(TAG, "failed pageitem get view", e)
                }
            }

        }

    }



    override fun onDestroy() {
        super.onDestroy()
    }
}// Required empty public constructor
