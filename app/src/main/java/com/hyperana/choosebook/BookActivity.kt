package com.hyperana.choosebook

import android.animation.*
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
import android.os.Handler
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.LinearInterpolator


val EXTRA_URI_STRING = "uriString"

class BookActivity :
        AppCompatActivity(),
        PageFragment.OnFragmentInteractionListener,
        PageItemListener,
        LoaderManager.LoaderCallbacks<Book>
{

    val TAG = "BookActivity"
    var loaderId = 0
    var book: Book? = null


    //**************************** TTS Utterance Implementation ***********************
    var TTS: TextToSpeech? = null
        get() {
            field = field ?: TextToSpeech(this, {
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

    val utteranceMap: MutableMap<String, TextView> = mutableMapOf()

    val utteranceListener = object: UtteranceProgressListener() {
        val TAG = "UtteranceProgress"

        fun highlight(v: TextView, isActive: Boolean = true) {
            runOnUiThread {
                v.setTextColor(if (isActive) resources.getColor(R.color.colorAccent)
                else resources.getColor(R.color.colorNeutralDark))
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
        }
    }

    fun interruptSpeak() {
        TTS?.apply {
            speak("", QUEUE_FLUSH, null, null)
        }
    }

    //todo: -L- add highlight view
    fun speakTextView(v: TextView, interrupt: Boolean = true) {
        TTS?.apply {
            val id = randomString(8)
            utteranceMap.put(id, v)
            speak(v.text, if (interrupt) QUEUE_FLUSH else QUEUE_ADD, Bundle(), id)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate")
            setContentView(R.layout.activity_book)


            //get book file uri from intent
            val loaderArgs = Bundle().apply {
                putString(EXTRA_URI_STRING, intent.data.toString())
            }

            // start book load
            loaderId = (Math.random()*1000).toInt()
            supportLoaderManager.initLoader(loaderId, loaderArgs, this)

        }
        catch (e: Exception) {
            Log.e(TAG, "problem onCreate", e)
        }
    }

  /*  // handle user click to change page
    override fun onPageChange(toName: String) : Boolean {
        Log.d(TAG, "onPageChange to " + toName)
        try {
            setPage(book!!.pages[toName]!!)
        }
        catch (e: Exception) {
            Log.e(TAG, "problem onPageChange to " + toName, e)
        }
        return false
    }*/

    //todo: -L- touch zooms/pans in a spiral or masks on larger screens and reads text until release?
    //todo: -L- items with "touch" property can have .wav or alt image or sprite

    //todo: -L- save page on activity.backpressed, offer choice to resume
    // release TTS resources
    override fun onPause() {
        super.onPause()
        TTS?.stop()
        TTS = null
    }

    // attempt to resume TTS
    override fun onResume() {
        super.onResume()
        interruptSpeak()
    }

    //**************** AsyncBookLoader Callbacks ********************
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Book>? {
        try {
            Log.d(TAG, "onCreateLoader: " + id)

            return AsyncBookLoader(
                    this,
                    Uri.parse(args!!.getString(EXTRA_URI_STRING)),
                    BOOK_FILENAME)
        }
        catch (e: Exception) {
            Log.e(TAG, "problem creating book loader", e)
            return null
        }
    }

    override fun onLoadFinished(loader: Loader<Book>?, data: Book?) {
        Log.d(TAG, "onLoaderFinished: " + data?.parentUri)
        try {
            book = data!!

            //set activity title
            title = book!!.title

            book!!.createPages()

            //display first page of book
            Log.d(TAG, "set first page fragment...")
            book!!.pages.entries.first().toPair().also {
                (name, contents) ->
                //check that this activity is still on top
                supportFragmentManager.beginTransaction()
                        .replace(
                                R.id.page_fragment_container,
                                PageFragment.newInstance(contents, this),
                                name
                        )
                        .commitAllowingStateLoss() //Should check if activity is still on top?
            }
        }
        catch (e: Exception) {
            //todo: display error message
            Log.e(TAG, "problem with loaded book json", e)
            book = null
        }

    }

    // loader is destroyed -- data (book) is unavailable
    override fun onLoaderReset(loader: Loader<Book>?) {
        Log.d(TAG, "onLoaderReset: " + loader?.id)
        book = null
    }



    //************************ PageItemListener Implementation **********************
    override fun onPageChange(toName: String): Boolean {
        Log.d(TAG, "onPageChange to " + toName)
        book?.pages?.get(toName)?.also {
             setPage(Pair(toName, it))
        }
        return true
    }



    override fun onTextClick(v: TextView) {
        Log.d(TAG, "onTextClick: " + v.text)
       speakTextView(v)
    }

    // speak prompt and each link in turn with highlights
    override fun onChoiceClick(texts: List<TextView>) {
        Log.d(TAG, "onChoiceClick")

        interruptSpeak()

        // read texts in turn
        texts.onEach {
            speakTextView(it, false)
        }
    }


    override fun onImageClick(v: ImageView) {
        Log.d(TAG, "onImageClick")
        try {
            val highlight = ImageView(this).apply {
                setImageDrawable(v.drawable)
                setOnClickListener { v -> (parent as ViewGroup).removeView(this) }

                // todo: -L- on touch drags zoomed image
            }

            val orig = highlight.drawable.bounds
            val viewRect = Rect()


            (findViewById(R.id.content_frame) as ViewGroup).apply {
                addView(
                        highlight,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
                getLocalVisibleRect(viewRect)
            }

            highlight.scaleType = ImageView.ScaleType.MATRIX

            val SCALE = 3f
            // zoomed to center
            val scaledRect = Rect(orig).apply {inset((orig.width()/SCALE).toInt(), (orig.height()/SCALE).toInt())}

            val top = 0
            val left = 0
            val centerHoriz = orig.width() - scaledRect.width()*2
            val centerVert = orig.height() - scaledRect.height()*2
            val bottom = orig.height() - scaledRect.height()
            val right = orig.width() - scaledRect.width()

            // figure eight
            val offsetScript = listOf(
                    Point(left,top),
                    Point(right, top),
                    Point(right, centerVert),
                    Point(centerHoriz, centerVert),
                    Point(left, centerVert),
                    Point(left, bottom),
                    Point(right, bottom),
                    Point(right, centerVert),
                    Point(centerHoriz, centerVert),
                    Point(left, centerVert),
                    Point(left, top)
            )

            val applyOffsetListener = object: ValueAnimator.AnimatorUpdateListener {
                override fun onAnimationUpdate(animation: ValueAnimator?) {
                    try {
                        highlight.imageMatrix = Matrix().apply {
                            val pt = animation?.animatedValue as? Point ?: Point(0,0)
                            setRectToRect(
                                    RectF(scaledRect.apply {offsetTo(pt.x, pt.y)}),
                                    RectF(viewRect),
                                    Matrix.ScaleToFit.START
                            )
                        }
                    }
                    catch (e: Exception) {
                        // do nothing
                    }
                }
            }


            val pointEvaluator = object: TypeEvaluator<Point> {
                override fun evaluate(fraction: Float, startValue: Point?, endValue: Point?): Point {
                    try {
                        return Point(
                                (startValue!!.x + (endValue!!.x - startValue!!.x)*fraction).toInt(),
                                (startValue.y + (endValue!!.y - startValue!!.y)*fraction).toInt()
                        )
                    }
                    catch (e:Exception) {
                        return Point(0, 0)
                    }
                }
            }

            Log.d(TAG, "onImageClick: " + orig.toString() + " in " + viewRect.toString())
            val panScript = AnimatorSet().apply {
                duration = 2000
                interpolator = LinearInterpolator()
            }
            panScript.playSequentially(

                    (1 .. offsetScript.count() -1).map {
                        Log.d(TAG, "set animator from " + offsetScript[it - 1].toString() +
                                " to " + offsetScript[it].toString() )
                        ValueAnimator.ofObject(
                                pointEvaluator,
                                offsetScript[it - 1],
                                offsetScript[it]
                        ).apply {
                            addUpdateListener(applyOffsetListener)
                        } as Animator

                    }.toMutableList()
            )
            panScript.start()



        }
        catch (e: Exception) {
            Log.e(TAG, "failed image click", e)
        }
    }

    override fun onFragmentInteraction(uri: Uri) {
        Log.d(TAG, "onFragmentInteraction: " + uri)
    }

    fun setPage(page: Pair<String,List<PageItem>>) {
        supportFragmentManager.beginTransaction()
                //.addToBackStack(page.first) -- don't want to reverse through pages with back button
                .replace(
                        R.id.page_fragment_container,
                        PageFragment.newInstance(page.second, this),
                        page.first
                )
                .commit()
    }

}
