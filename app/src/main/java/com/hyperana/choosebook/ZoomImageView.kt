package com.hyperana.choosebook

import android.animation.TypeEvaluator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup
import android.view.ViewGroupOverlay


/*******
 *  ImageView which behaves as normal until click in target area (to avoid interfering with scroll
 *  or activating an image that has scrolled mostly offscreen),
 *  then attaches new full-screen view to root (outside scroll) with zoomview directly overlaying
 *  the original image,
 *  then on touch, zooms to clicked point,
 *  then on touch move, pans to keep point in original view centered
 *  then unzooms overlay on touch up,
 *  removes full-screen overlay on click
 */
class ZoomImageView : ImageView, View.OnTouchListener, View.OnClickListener {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    val TAG = "ZoomImageView"


    //onlayout, get view layout dimensions and calculate center and a padded target for the initial hit
    val viewRect = Rect() // screen-coordinate rectangle of original image
    var targetRect = Rect() // view-coordinate rectangle within which the clicks are accepted

    var zoomOverlay: SuperimposedView? = null
    var zoomView: ImageView? = null
    val zoomMatrix = Matrix()
    val zoomRect = Rect() // zoomview-coordinate rectangle from which touches are translated into zoomed-image coordinates

    val zoomFactor = 4f
    val targetInsetX = 48
    val targetInsetY = 48

    var currentCoords: Pair<Float, Float> = Pair(0f, 0f)

    init {
        setOnClickListener(this)
    }

    // when bitmap set (by Glide -- don't know if other uses bypass this call --
    // get the original image dimensions and view screen coords
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        Log.d(TAG, "setImageDrawable")
        zoomView?.setImageDrawable(drawable)
    }

    // when attached (rootView == rootview of activity), PREPARE invisible overlay
    // outside the scroll parent so touches are not intercepted
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttached")

        /*zoomOverlay = (context as? Activity)
                ?.let {
                    it.layoutInflater.inflate(
                            R.layout.overlay_zoomimageview,
                            it.findViewById<ViewGroup?>(R.id.content_frame),
                            true
                    )
                    it.findViewById<ViewGroup>(R.id.zoom_image_overlay)
                }*/

        zoomView = ImageView(this.context).apply {
            setImageDrawable(drawable)
            setOnTouchListener(this@ZoomImageView)
        }

        zoomOverlay = SuperimposedView(this)
                .apply {
                    //visibility = View.GONE
                    setOnClickListener(this@ZoomImageView)
                    addView(zoomView)
                }


/*
            zoomOverlay.findViewById<ImageView>(R.id.zoom_image_view)
                ?.apply {
                    setImageDrawable(drawable)
                    setOnTouchListener(this@ZoomImageView)
                    scaleType = this@ZoomImageView.scaleType
                }*/
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetached")

        (rootView as ViewGroup).removeView(zoomOverlay)
    }

    // on layout, set target region,
    // initialize viewRect and zoomRect (the same, but left,top are 0)
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onLayout")
        super.onLayout(changed, left, top, right, bottom)
        try {

            // viewRect tells how to position the overlaid image within a full-screen overlay
            getGlobalVisibleRect(viewRect)

//            viewRect.offset(scrollX, scrollY)

                // zoomRect (view coordinates of overlaid image) are the same as this view's
                getLocalVisibleRect(zoomRect)

                // target rect excludes edges of original view in view-coordinates
                targetRect.apply {
                    set(zoomRect)
                    inset(targetInsetX, targetInsetY)
                }

                Log.d(TAG, "onLayout: \nviewRect - " + viewRect.toString() +
                        "\nzoomRect - " + zoomRect.toString())
            }

        catch (e:Exception) {
            Log.e(TAG, "failed on layout determine view rect", e)
        }
    }

    // toggle overlay attachment
    override fun onClick(v: View?) {
        Log.d(TAG, "onClick: " + if (v==this) "original image" else "zoomed image")
        try {
            when (v) {
                this -> { // show zoomView, calculate viewRect
                    (rootView as ViewGroup).addView(zoomOverlay)
                }
                zoomView, zoomOverlay -> { // hide zoomView
                    (rootView as ViewGroup).removeView(zoomOverlay)
                }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "failed onClick: " + v?.tag, e)
        }
    }

   // on touch zoomOverlay ( the superimposed
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        try {
            Log.d(TAG, "onTouch: " + event?.toString())

            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    //start zoom, center on coords, return true to receive future events
                    currentCoords = Pair(event.x, event.y)
                    zoomToViewCoords(currentCoords)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    // recenter zoom on coords, return true
                    panByViewCoords(Pair(event.x - currentCoords.first, event.y - currentCoords.second))
                    currentCoords = Pair(event.x, event.y)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (event!!.eventTime - event.downTime < 100) { //no click time standard, long-click is 500ms
                        // perform click to toggle overlay
                        zoomOverlay!!.performClick()
                    }
                    else {
                        unzoom()
                    }
                    return true
                }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "problem with touchevent", e)
        }
        return false
    }


    // zoom and center on given coords
   fun zoomToCoords(viewCoords: Pair<Float, Float>) {
        Log.d(TAG, "zoom")
        zoomView?.visibility = View.VISIBLE

        zoomView?.scaleType = ScaleType.MATRIX
        zoomView?.imageMatrix = zoomMatrix.apply {
            setScale(
                    1/zoomFactor,
                    1/zoomFactor,
                    viewCoords.first - viewRect.exactCenterX(),
                    viewCoords.second - viewRect.exactCenterY()
            )
        }

    }

    // resets matrix to scaled at 0,0; then translates to viewCoords, scaled and centered in view-- should be screenREct
    fun zoomToViewCoords(viewCoords: Pair<Float, Float>) {
        Log.d(TAG, "zoom to view coords: " + viewCoords.toString())
     /*   val clipRect = Rect(viewRect).apply {
            offsetTo(viewCoords.first.toInt(), viewCoords.second.toInt()) // offset to given point
            offset(-viewRect.width()/2, -viewRect.height()/2) // shift so given point is centered
            offsetTo(maxOf(0, left), maxOf(0, top)) // corral left and top edges
            offsetTo(
                    if (right >  ) //corral right and bottom edges
        }
        val zoomCentered =
            boundIn(
                centerIn(
                    scale(viewCoords, zoomFactor),
                    viewRect
                )*/
        // point in zoomed image on which to center view
        val zoomCenter = FloatArray(2)

        // zoomed image bounds
        val imageRect = RectF()

        // view bounds in zoomed image
        val zoomRect = Rect(viewRect)

        // calculate zoomed positions
        zoomMatrix.apply {
            // scale image
            setScale(zoomFactor, zoomFactor, 0f, 0f)
            // get new zoomed image rect (for bounds)
            mapRect(imageRect, RectF(drawable.bounds))
            // get chosen point in zoomed image
            mapPoints(zoomCenter, viewCoords.toList().toFloatArray())
        }

        Log.d(TAG, "zoomed image: " + imageRect.toString())
        Log.d(TAG, "zoomed center: " + zoomCenter.contentToString())

        // calculate zoomRect position
        zoomRect.apply {
            offsetTo(zoomCenter[0].toInt(), zoomCenter[1].toInt()) // offset to given point
            offset(-viewRect.width() / 2, -viewRect.height() / 2) // shift so given point is centered
            // shift zoomrect if necessary so zoomed image doesn't scroll offscreen
            offsetTo(maxOf(0, left), maxOf(0, top))
            offsetTo(
                    minOf(left, (imageRect.right - width()).toInt()),
                    minOf(top, (imageRect.bottom - height()).toInt())
            )
        }

        Log.d(TAG, "viewRect in zoomed image: " + zoomRect.toString())

        zoomMatrix.postTranslate(-zoomRect.left.toFloat(), -zoomRect.top.toFloat())
        zoomView!!.scaleType = ScaleType.MATRIX
        zoomView!!.imageMatrix = zoomMatrix
}

    // pans matrix in view coordinates before applying zoom
    fun panByViewCoords(change: Pair<Float, Float>) {
        Log.d(TAG, "panBy: " + change.toString())
        zoomView!!.imageMatrix = zoomMatrix.apply {
            preTranslate(
                    -change.first,
                    -change.second
            )
        }
    }

    /*// pans matrix in view coordinates before applying zoom
    fun panToViewCoords(viewCoords: Pair<Float, Float>) {
        Log.d(TAG, "panToCoords: " + viewCoords.toString())
        zoomView?.imageMatrix = zoomMatrix.apply {
           preTranslate(viewCoords.first, viewCoords.second)
        }
    }
*/
    fun centerIn(coords: Pair<Float, Float>, rect: Rect) : Pair<Float, Float> {
        return Pair(coords.first - rect.exactCenterX(), coords.second - rect.exactCenterY())
    }

    fun scale(coords: Pair<Float, Float>, factor: Float) : Pair<Float, Float> {
        return Pair(coords.first * factor, coords.second * factor)
    }

    fun unzoom() {
        Log.d(TAG, "unzoom")
        zoomView?.scaleType = this.scaleType
    }

    fun isInTarget(coords: Pair<Float, Float>): Boolean {
        return targetRect.contains(coords.first.toInt(), coords.second.toInt())
    }


}