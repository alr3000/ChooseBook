package com.hyperana.choosebook

import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

import android.support.v4.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import android.widget.TextView
import java.io.File

class TabbedLibraryActivity : AppCompatActivity() {
    val TAG = "TabbedLibraryActivity"

   //tab 1
    var offlineTab: TextView? = null
    val OFFLINE_TAG = R.string.offline_library_tab

    //tab 2
    var onlineTab: TextView? = null
    val ONLINE_TAG = R.string.online_library_tab

    val settings = SettingsFragment()


    //todo: -L- remember tab selected
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "oncreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed_library)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //todo: -L- check WIFI
        val canGetOnlineBooks = false

        // navigation to online/offline library lists -- set listener and first selected
        offlineTab = (findViewById<TextView?>(R.id.tab1))?.apply {
           // it.tag = OFFLINE_TAG
            setOnClickListener{
                v ->
                listOf(offlineTab, onlineTab).onEach { it?.isSelected = if (v == it) true else false }
                gotoFragment(OfflineBookListFragment())
            }

        }

        onlineTab = (findViewById<TextView?>(R.id.tab2))?.apply {
            if (!canGetOnlineBooks) {
                visibility = View.GONE
            } else {
                //tag = ONLINE_TAG
                setOnClickListener{
                    v ->
                    listOf(offlineTab, onlineTab).onEach { it?.isSelected = if (v == it) true else false }
                    gotoFragment(OnlineBookListFragment())
                }
            }
        }

        offlineTab?.performClick()
    }



    // todo: -L- help and share
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tabbed_library_actvity, menu)
//        menu.add(0, R.id.action_settings, 0, R.string.action_settings)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        try {
            Log.d(TAG, "onOptionsItemSelected")
            val id = item.itemId

            if (id == R.id.action_settings) {
                fragmentManager.beginTransaction()
                        .addToBackStack(TAG)
                        .add(R.id.main_content, settings, settings.TAG)
                        .commit()
                return true
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "failed on select settings", e)
        }
        return super.onOptionsItemSelected(item)
    }

    fun gotoFragment(newFrag: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.page_fragment_container, newFrag)
                .commit()
    }

    /*override fun onClick(v: View?) {
        Log.d(TAG, "onTabSelected: " + v?.tag)
        listOf(offlineTab, onlineTab).onEach { it?.isSelected = if (v == it) true else false }
        val fm = when(v?.tag) {
            OFFLINE_TAG -> {
                // Offline
                OfflineBookListFragment()
            }
            ONLINE_TAG -> {
                OnlineBookListFragment()
            }
            else -> PlaceholderFragment.newInstance(0)
        }
        //supportFragmentManager.beginTransaction().replace(R.id.page_fragment_container, fm).commit()
        supportFragmentManager.beginTransaction().add(R.id.page_fragment_container, fm).commit()
    }*/


    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_tabbed_library_actvity, container, false)
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

}
