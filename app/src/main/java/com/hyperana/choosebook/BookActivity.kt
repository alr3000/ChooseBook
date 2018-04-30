package com.hyperana.choosebook

import android.animation.*
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.ImageView
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.PersistableBundle
import android.preference.DialogPreference
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton


val EXTRA_URI_STRING = "uriString"

class BookActivity :
        AppCompatActivity(),
        PageFragment.OnFragmentInteractionListener,
        PageItemListener // TextToSpeech.OnInitListener
{

    val TAG = "BookActivity"
    var loaderId = 0
    var book: Book? = null

    // saved state
    val CURRENT_PAGE_KEY = "currentPage"
    var currentPage: String? = null

    val settings = SettingsFragment()
    var isSoundOn: Boolean = false
    get() {
        return getPreferences(Context.MODE_PRIVATE).getBoolean(SETTING_SOUND_STRING, false)
    }
    var isEffectsOn: Boolean = false
    get() {
        return getPreferences(Context.MODE_PRIVATE).getBoolean(SETTING_EFFECTS_STRING, false)
    }


    //**************************** TTS Utterance Implementation ***********************
    var TTS: TextToSpeech? = null
        get() {
            // use application context because tts will bind a service that may outlast the activity
            field = field ?: TextToSpeech(applicationContext, {
                status ->
                Log.d(TAG, "TTS status: " + status)
                if (status == ERROR) {
                    field = null
                } else {
                    field?.setOnUtteranceProgressListener(utteranceListener)
                }
            })
            return field
        }

    val utteranceMap: MutableMap<String, View> = mutableMapOf()

    val utteranceListener = object: UtteranceProgressListener() {
        val TAG = "UtteranceProgress"

        fun highlight(v: View, isActive: Boolean = true) {
            runOnUiThread {
                if (isEffectsOn) {
                    v.isActivated = isActive
                }
            }
        }

        override fun onStart(utteranceId: String?) {
            Log.d(TAG, "utterance onStart: " + utteranceId)
            utteranceMap[utteranceId]?.also {
                highlight(it)
            } ?: Log.w(TAG, "no matching TextView")
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            Log.d(TAG, "utterance onStop: " + utteranceId)
            utteranceMap[utteranceId]?.also {
                highlight(it, false)
            } ?: Log.w(TAG, "no matching TextView")
        }

        override fun onDone(utteranceId: String?) {
            Log.d(TAG, "onDone: " + utteranceId)
            utteranceMap[utteranceId]?.also {
                highlight(it, false)
            } ?: Log.w(TAG, "no matching TextView")
        }

        override fun onError(utteranceId: String?) {
            Log.d(TAG, "onError: " + utteranceId)
            utteranceMap[utteranceId]?.also {
                highlight(it, false)
            }
        }
    }

    @TargetApi(21)
    fun speak(
            text: String? = "",
            mode: Int = QUEUE_FLUSH,
            options: Bundle? = null) : String? {
        if (!isSoundOn) {
            Log.d(TAG, "speak: MUTED")
            return null
        }

        TTS?.apply {
            if (Build.VERSION.SDK_INT <= 15) {
                speak(text, mode, hashMapOf<String, String>())
            }
            else {
                val id = randomString(8)
                speak(text, mode, options, id)
                return id
            }
        }
        return null
    }

    fun interruptSpeak() {
        speak("", QUEUE_FLUSH)
    }

    //todo: -L- add highlight view a la zoom image view
    //todo: -?- speakTextView should return a listener object for that utterance/view
    fun speakTextView(v: View, interrupt: Boolean = true) {
            val textView = (v as? TextView) ?: v.findViewById<TextView?>(R.id.pageitem_text)
            textView?.also {
                val utteranceId = speak(it.text?.toString(), if (interrupt) QUEUE_FLUSH else QUEUE_ADD)
                if (utteranceId != null) {
                    utteranceMap.put(utteranceId, v)
                }
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")
            setContentView(R.layout.activity_book)

            currentPage = savedInstanceState?.getString(CURRENT_PAGE_KEY)
            Log.d(TAG, "currentPage: " + currentPage)

            intent.data!!.also {
                (application as App).loadString(it.buildUpon().appendPath(BOOK_FILENAME).build(),
                      object: App.StringListener {
                          override fun onString(string: String?) {
                              Log.d(TAG, "loadString listener: " + string?.length)
                              book = Book( string ?: "", it.lastPathSegment, it)
                              setBook()
                          }
                      }
                )
            }

            findViewById<ImageButton>(R.id.settings_button)!!.apply {
                setOnClickListener {
                    v: View ->
                    try {
                        Log.d(TAG, "settings - onClick")
                        openSettings()
                    }
                    catch (e: Exception) {
                        Log.e(TAG, "failed click settings")
                    }
                }
            }


            //start up TTS:
            interruptSpeak()

        }
        catch (e: Exception) {
            Log.e(TAG, "problem onCreate", e)
        }
    }


    //todo: -L- items with "touch" property can have .wav or alt image or sprite
    //todo: -?- new pages appear below old in a long scroll "history"


    // release TTS resources
    override fun onPause() {
        super.onPause()
        TTS?.shutdown()
        TTS = null
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "saving state...")
        outState?.putString(CURRENT_PAGE_KEY, currentPage)
    }

    // confirm quit because page is not saved
    override fun onBackPressed() {

        // display dialog
        AlertDialog.Builder(this).apply {
            setMessage(R.string.alert_quit_message)

            // quit
            setPositiveButton(R.string.alert_quit_yes, {
                dialog, buttonIndex ->
                dialog.dismiss()
                finish()
            })

            // dismiss dialog
            setNegativeButton(R.string.alert_quit_no, {
                dialog, buttonIndex ->
                dialog.dismiss()
            })

            create().show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        TTS?.shutdown()
        TTS = null
    }

    // attempt to resume TTS
    override fun onResume() {
        super.onResume()
        interruptSpeak()
    }

    fun setBook()  {

        //set activity title
        title = book!!.title

        book!!.createPages()

        // get page entry (Map<name, contents>
        val page = (book!!.pages[currentPage])

                // get saved current page
                ?.let {
                    Pair(currentPage!!, it)
                }
                ?:
                // or first page
                book!!.pages.entries.first().toPair()

        setPage(page)
    }

    // add settings as root overlay
    fun openSettings() {
        Log.d(TAG, "openSettings")
        if (!settings.isAdded) {
            fragmentManager.beginTransaction()
                    .addToBackStack(TAG)
                    .add(R.id.content_frame, settings, settings.TAG)
                    .commit()
        }

    }


    //************************ PageItemListener Implementation **********************
    override fun onLinkClick(v: View, toName: String) {
        Log.d(TAG, "onLinkClick to " + toName)
        v.isActivated = true
        v.findViewById<TextView>(R.id.pageitem_text)?.also{ speakTextView(it)}

        Handler().postDelayed( {
        book?.pages?.get(toName)?.also {
             setPage(Pair(toName, it))
        }
        }, 1000)
    }



    override fun onTextClick(v: TextView) {
        Log.d(TAG, "onTextClick: " + v.text)
       speakTextView(v)
    }

    // speak prompt and each link in turn with highlights
    override fun onChoiceClick(texts: List<View>) {
        Log.d(TAG, "onChoiceClick: " + texts.count() + " texts")

        interruptSpeak()

        // read texts in turn
        texts.onEach {
            speakTextView(it, false)
        }
    }


    override fun onImageClick(v: ImageView) {
        Log.d(TAG, "onImageClick")

    }

    override fun onFragmentInteraction(uri: Uri) {
        Log.d(TAG, "onFragmentInteraction: " + uri)
    }

    fun setPage(page: Pair<String,List<PageItem>>) {
        val name = page.first
        val contents = page.second

        //todo: -?- check that this activity is still on top
        // replace current/default page fragment
        supportFragmentManager.beginTransaction()
                .replace(
                        R.id.page_fragment_container,
                        PageFragment.newInstance(contents, this@BookActivity),
                        name
                )

                .commitAllowingStateLoss()

        // retain page name for saved instance state
        currentPage = name

        Log.d(TAG, "set page fragment: " + name)

    }

}
