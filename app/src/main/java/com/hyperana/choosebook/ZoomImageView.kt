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
 */
class ZoomImageView : ImageView, View.OnClickListener {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    val TAG = "ZoomImageView"

    var zoomOverlay: ImageView? = null // full-screen shaded view accepts clicks to disappear

    init {
        setOnClickListener(this)
    }

    // when bitmap set (by Glide -- don't know if other uses bypass this call --
    // get the original image dimensions and view screen coords
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
//        Log.d(TAG, "setImageDrawable")

        zoomOverlay?.setImageDrawable(drawable)

    }

    // when attached (rootView == rootview of activity), PREPARE invisible overlay
    // outside the scroll parent so touches are not intercepted
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        Log.d(TAG, "onAttached")

        try {

            // attach overlay to root so zoomed image will be outside scroll
          zoomOverlay = TouchZoomImageView(context).apply {

              // set drawable, make invisible, and set toggle listener
                visibility = View.GONE
                setOnClickListener(this@ZoomImageView)
                setImageDrawable(drawable)
            }
            (rootView as ViewGroup).addView(zoomOverlay)
        }
        catch (e:Exception) {
//            Log.e(TAG, "problem attaching zoom overlay", e)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        Log.d(TAG, "onDetached")
        (zoomOverlay?.parent as? ViewGroup)?.removeView(zoomOverlay)
    }


    // toggle overlay
    override fun onClick(v: View?) {
//        Log.d(TAG, "onClick:" + if (v==this) "original image" else "zoomed image")

        zoomOverlay?.visibility = if (v == this) View.VISIBLE else View.GONE

    }

}