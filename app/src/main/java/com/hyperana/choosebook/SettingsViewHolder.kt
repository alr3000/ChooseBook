package com.hyperana.choosebook

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch

/**
 * Created by alr on 4/3/18.
 */
class SettingsViewHolder()  {
    val TAG = "SettingsView"

    var view: ViewGroup? = null
    var button: ImageButton? = null
    var content: ViewGroup? = null
    var soundSwitch: Switch? = null
    var feedbackSwitch: Switch? = null

    var timer: Handler? = null

    var sharedPref: SharedPreferences? = null


    fun addTo(parent: ViewGroup, activity: Activity) {
        Log.d(TAG, "addTo")
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        view = activity.layoutInflater.inflate(
                R.layout.settings_view,
                parent,
                true) as? ViewGroup
        content = view!!.findViewById<ViewGroup>(R.id.settings_content)
        soundSwitch = view?.findViewById<Switch?>(R.id.mute_switch)?.apply {
            setOnCheckedChangeListener { buttonView, isChecked ->
                sharedPref?.edit()?.putBoolean("sound", isChecked)?.apply()
            }
        }
        feedbackSwitch = view?.findViewById<Switch?>(R.id.feedback_switch)?.apply {
            setOnCheckedChangeListener { buttonView, isChecked ->
                sharedPref?.edit()?.putBoolean("feedback", isChecked)?.apply()
            }
        }
        button = view!!.findViewById<ImageButton>(R.id.settings_button)!!.apply {
            setOnClickListener({
                v: View? ->
                Log.d(TAG, "onClick")
                show()
            })
        }

    }

    fun show() {
        Log.d(TAG, "show")
        content?.visibility = View.VISIBLE
        button?.visibility = View.GONE
        view?.setOnClickListener {v: View? -> hide()}

        timer = Handler().apply {
            postDelayed( {
                hide()
            }, 2000)
        }
    }

    fun hide() {
        Log.d(TAG, "hide")
        content?.visibility = View.GONE
        button?.visibility = View.VISIBLE
        view?.setOnClickListener(null)

    }

}