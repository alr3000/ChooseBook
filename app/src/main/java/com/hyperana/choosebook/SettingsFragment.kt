package com.hyperana.choosebook


import android.os.Bundle
import android.app.Fragment
import android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioManager.FLAG_SHOW_UI
import android.media.AudioManager.STREAM_MUSIC
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast


/**
 * A simple [Fragment] subclass.
 *
 * //todo: -?- settings should be a dialog or implement a listener to have activity orchestrate
 */
class SettingsFragment : Fragment() {
    val TAG = "SettingsFragment"

    var view: ViewGroup? = null
    var button: ImageButton? = null
    var content: ViewGroup? = null
    var soundSwitch: Switch? = null
    var feedbackSwitch: Switch? = null

    var sharedPref: SharedPreferences? = null
    var audioManager: AudioManager? = null

    var timer = Handler()

    val closer = object: Runnable {
        override fun run() {
            try {
//                Log.d(TAG, "closeSettings")

                // todo: -L- should be done through listener so that back button can close also
                fragmentManager.popBackStack()
            }
            catch (e: Exception) {
//                Log.e(TAG, "close")
            }
        }
    }

    fun resetTimer() {
//        Log.d(TAG, "resetTimer")
        timer.apply {
            removeCallbacks(closer)
            postDelayed(closer, 3000)
        }
    }

    fun bindSwitchToPreference(switch: Switch, key: String, default: Boolean = false) {

        // set touch listener to restart timer on user activity
        switch.setOnTouchListener {
            v, event ->
            resetTimer()
            false
        }

        // set default state
        if (!sharedPref!!.contains(key)) {
            sharedPref!!.edit().putBoolean(key, default).apply()
        }

        //get current state
        switch.isChecked = sharedPref!!.getBoolean(key, default)

        // apply changes to preferences
        switch.setOnCheckedChangeListener {
            buttonView, isChecked ->
            try {
                sharedPref?.edit()?.putBoolean(key, isChecked)?.apply()
            }
            catch(e: Exception) {
//                Log.e(TAG, "failed set preferences", e)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
//        Log.d(TAG, "onCreateView")
        var view: View? = null

        try {
            // Inflate the layout for this fragment
            view = inflater.inflate(R.layout.fragment_settings, container, false)
        }
        catch (e: Exception) {
//            Log.e(TAG, "failed create settings view")
        }
        finally {
            return view
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
//        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        try {
            // show volume control for music stream (disappears on its own)
            audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager?.adjustStreamVolume(STREAM_MUSIC, AudioManager.ADJUST_SAME, FLAG_SHOW_UI)

            // connect settings switches to preferences
            sharedPref = activity.getPreferences(Context.MODE_PRIVATE)!!

            soundSwitch = view?.findViewById<Switch?>(R.id.mute_switch)?.also {
                bindSwitchToPreference(it, SETTING_SOUND_STRING)
            }
            /*feedbackSwitch = view?.findViewById<Switch?>(R.id.feedback_switch)?.also {
                bindSwitchToPreference(it, SETTING_EFFECTS_STRING)
            }*/

            // set click listener to close settings when user touches outside the content
            view?.setOnClickListener{ closer.run() }

            // start the timer to close settings
            resetTimer()

        }
        catch (e: Exception) {
//            Log.e(TAG, "failed onViewCreated", e)
            closer.run()
        }

    }

    override fun onPause() {
        super.onPause()
//        Log.d(TAG, "pause")
        timer.removeCallbacks(closer)

    }

    override fun onDetach() {
        super.onDetach()
//        Log.d(TAG, "detach")
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        Log.d(TAG, "destroyView")
    }

    override fun onStop() {
        super.onStop()
//        Log.d(TAG, "stop")
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
}
