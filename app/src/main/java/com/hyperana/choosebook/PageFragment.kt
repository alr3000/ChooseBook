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

    // TODO: Rename and change types of parameters
    private var mPage: List<PageItem>? = null
    private var mParam1: String? = null
    private var mParam2: String? = null

    private var mPageItemListener: PageItemListener? = null
    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments.getString(ARG_PARAM1)
            mParam2 = arguments.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
            Log.d(TAG, "onCreateView")
            // Inflate the layout for this fragment
            return inflater!!.inflate(R.layout.fragment_page, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "problem creating view", e)
            return View(activity)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            Log.d(TAG, "onViewCreated")
            setPageView((view!!.findViewById(R.id.page_container) as ViewGroup))
        }catch (e: Exception) {
            Log.e(TAG, "problem setting page view", e)
        }

    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")
        if (context is OnFragmentInteractionListener) {
            mListener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
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
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }


    companion object {
        // TODO: Rename parameter arguments, choose names that match
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
        // TODO: Rename and change types and number of parameters
        fun newInstance(page: List<PageItem>, pageItemListener: PageItemListener): Fragment {
            Log.d("PageFragment", "newInstance with " + page.count() + " items")
            val fragment = PageFragment()
            fragment.mPage = page
            fragment.mPageItemListener = pageItemListener

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    fun setPageView(container: ViewGroup) {
        Log.d(TAG, "setPageView with " + mPage?.count() + " items")
        if (mPage != null) {
            container.removeAllViews()
            mPage!!.onEach {
                it.getView(container, null, mPageItemListener)
            }
        }
    }
}// Required empty public constructor
