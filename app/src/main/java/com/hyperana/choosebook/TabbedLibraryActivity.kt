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

import android.widget.TextView
import java.io.File

class TabbedLibraryActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener{
    val TAG = "TabbedLibraryActivity"

    //tab 1
    val OFFLINE_TAG = "offline"
    val DEFAULT_SELECTED = OFFLINE_TAG

    //tab 2
    val ONLINE_TAG = "online"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "oncreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed_library)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // navigation to online/offline library lists -- set listener and first selected
        val tabs = (findViewById(R.id.tabs) as TabLayout).also {
            it.addTab(it.newTab().setCustomView(R.layout.library_tab_offline).also {
                it.tag = OFFLINE_TAG
            })
            it.addTab(it.newTab().setCustomView(R.layout.library_tab_online).also {
                it.tag = ONLINE_TAG
            })
            it.addOnTabSelectedListener(this)
        }
        (0 .. tabs.tabCount-1).map { tabs.getTabAt(it) }.find { it?.tag == OFFLINE_TAG }!!
                .also {
                    onTabSelected(it)
                }


    }


    // todo: help, settings, and share
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tabbed_library_actvity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onTabReselected(p0: TabLayout.Tab?) {
        Log.d(TAG, "onTabReselected: " + p0?.tag)
    }

    override fun onTabUnselected(p0: TabLayout.Tab?) {
        Log.d(TAG, "onTabUnselected: " + p0?.tag)
    }

    override fun onTabSelected(p0: TabLayout.Tab?) {
        Log.d(TAG, "onTabSelected: " + p0?.tag)
        val fm = when(p0?.tag) {
            OFFLINE_TAG -> {
                // Offline
                OfflineBookListFragment()
            }
            ONLINE_TAG -> {
                OnlineBookListFragment()
            }
            else -> PlaceholderFragment.newInstance(0)
        }
        supportFragmentManager.beginTransaction().replace(R.id.page_fragment_container, fm).commit()
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater!!.inflate(R.layout.fragment_tabbed_library_actvity, container, false)
            val textView = rootView.findViewById(R.id.section_label) as TextView
            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
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
