package com.hyperana.choosebook

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


/*******
 *  ImageView which when attached to window, attaches new full-screen view (zoomOverlay) to root (outside scroll)
 *  with new imageview (zoomView) centered fit-xy, drawable set to original drawable
 *
 *  Behaves as normal until click in target area (to avoid interfering with scroll
 *  or activating an image that has scrolled mostly offscreen),
 *  then overlay is made visible
 *
 *  zoomOverlay: on click, makes visibility gone
 *
 *  zoomView: on touch down, zooms and pans to make original-coordinate touch point in center
 *  on touch move, pans zoomed image to make new original-coordinate point in center
 *  on touch up, if "click-like", performs click on overlay (removes), else unzooms image
 *
 */
class ZoomImageView : ImageView, View.OnClickListener {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    val TAG = "ZoomImageView"

    var currentCoords: Pair<Float, Float> = Pair(0f, 0f) // overlay-view touch coordinates
    val targetRect = Rect() // view-coordinate rectangle within which the clicks are accepted to show overlay
    val zoomRect = Rect() // zoomed image rectangle
    var zoomMatrix = Matrix()
    var fitMatrix = Matrix()


    var zoomOverlay: ImageView? = null // full-screen shaded view accepts clicks to disappear
    var zoomView: ImageView? = null // duplicates this view's drawable in overlay
    var oScaleType: ScaleType = ScaleType.FIT_CENTER // in case the overlay xml layout differs


    // todo: -L- make companion object for these and drawable
    //todo: -L- incorporate attributes from xml
    val zoomFactor = 4f
    val targetInsetX = 48
    val targetInsetY = 48


    init {
        setOnClickListener(this)
    }

    // when bitmap set (by Glide -- don't know if other uses bypass this call --
    // get the original image dimensions and view screen coords
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        Log.d(TAG, "setImageDrawable")

        zoomOverlay?.setImageDrawable(drawable)

    }

    // when attached (rootView == rootview of activity), PREPARE invisible overlay
    // outside the scroll parent so touches are not intercepted
    // calculate it's fullscreen and imageview rectangles
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttached")

        try {
         /*   // inflate zoom overlay layout
            zoomOverlay = zoomOverlay ?: (context as? Activity)
                    ?.let {
                        it.layoutInflater.inflate(
                                R.layout.overlay_zoomimageview,
                                it.findViewById<ViewGroup?>(R.id.content_frame),
                                true
                        )
                        it.findViewById<ViewGroup>(R.id.zoom_image_overlay)
                    }
*/
            zoomOverlay = TouchZoomImageView(context).apply {
                // make invisible and set toggle listener
                visibility = View.GONE
                setOnClickListener(this@ZoomImageView)
                setImageDrawable(drawable)
            }
            (rootView as ViewGroup).addView(zoomOverlay)
        }
        catch (e:Exception) {
            Log.e(TAG, "problem attaching zoom overlay", e)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetached")
        (zoomOverlay?.parent as? ViewGroup)?.removeView(zoomOverlay)
    }




    // on layout, set target region in this view
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onLayout")
        super.onLayout(changed, left, top, right, bottom)
        try {
            // click has no coordinates (doesn't pass event)
            /*    getLocalVisibleRect(targetRect) // click view in these coords

                // exclude edges of view
                targetRect.inset(targetInsetX, targetInsetY)
                Log.d(TAG, "targetRect: " + targetRect.toString())*/
            }

        catch (e:Exception) {
            Log.e(TAG, "failed on layout determine view rect", e)
        }
    }


    // toggle overlay
    override fun onClick(v: View?) {
        Log.d(TAG, "onClick:" + if (v==this) "original image" else "zoomed image")

        zoomOverlay?.visibility = if (v == this) View.VISIBLE else View.GONE

    }


    // remove offset from overlay
    fun overlayToImageCoords(oCoords: Pair<Float, Float>) : Pair<Float, Float> {
        Log.d(TAG, "overlayToImageCoords: " + oCoords.toString())

        val originalRect = Rect()  //getGlobalVisibleRect(originalRect)

            val parentRect = Rect()
            zoomOverlay!!.getGlobalVisibleRect(parentRect)

            return Pair (
                    oCoords.first - (originalRect.left - parentRect.left),
                    oCoords.second - (originalRect.top - parentRect.top)
            )

    }

    // zoom and center on given coords
   fun zoomToImageCoords(coords: Pair<Float, Float>) {
        Log.d(TAG, "zoomToImageCoords: " + coords.toString())

        // point in zoomed image on which to center view
//        val viewCenter = FloatArray(2)

        // calculate zoomed positions
//        zoomMatrix.apply {
//
//            // scale image
//            setScale(zoomFactor, zoomFactor)
//
//            // get chosen point in zoomed image coordinates
//            mapPoints(viewCenter, coords.toList().toFloatArray())
//        }
//        //Log.d(TAG, "zoomed center: " + viewCenter.contentToString())

        // view bounds
        val viewRect = Rect()
        zoomOverlay!!.getLocalVisibleRect(viewRect)
        Log.d(TAG, "viewRect, clipRect init: " + viewRect.toString())

        // image bounds
        val imageRect = RectF(zoomView!!.drawable.bounds)
        Log.d(TAG, "imageRect: " + imageRect.toString())

        // center viewRect (in pre-zoomed image coords) around viewCenter, then offset it to keep within imageRect
        val clipRect = RectF()
        clipRect.apply {
            set(viewRect)
            // scale rect and point to 1/zoom
            clipRect.set(left, top, left + width()/zoomFactor, top + height()/zoomFactor)

            // offset to center the given point
            offsetTo(coords.first/zoomFactor-centerX(), coords.second/zoomFactor-centerY())
            Log.d(TAG, "clipRect, scaled and centered on point: " + clipRect.toString())

            // shift to keep in bounds of imageRect
            offsetTo(
                    maxOf(imageRect.left, left),
                    maxOf(imageRect.top, top)
            )
            offsetTo(
                    minOf(left, imageRect.right - width()),
                    minOf(top, imageRect.bottom - height())
            )
        }
        Log.d(TAG, "clipRect, final: " + clipRect.toString())

        // offset matrix to viewRect
/*
        zoomMatrix.postTranslate(
                viewRect.left.toFloat(),
                viewRect.top.toFloat()
        )
*/

        // the closer it is to 0,0 the better the result -- the problem is with something being over-zoomed
       // set image matrix
        zoomView!!.scaleType = ScaleType.MATRIX

        // matrix scales up after translating mini-view to clip coords
        zoomView!!.imageMatrix = Matrix().apply {
            setTranslate(clipRect.left, clipRect.top)

            postScale(zoomFactor, zoomFactor)
        }




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


    // return zoomView to original scale/layout
    fun unzoom() {
        Log.d(TAG, "unzoom")
        zoomView?.scaleType = oScaleType
    }

    fun isInTarget(coords: Pair<Float, Float>): Boolean {
        return targetRect.contains(coords.first.toInt(), coords.second.toInt())
    }


}