package com.hyperana.choosebook

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Created by alr on 9/17/17.
 *
 * View duplicates original view, then animates to final size and position:
 *      zoomed by given highlightZoom percentage,
 *      offset to show above or below original view, within the bounds of the screen.
 *
 * Translation is set relative to root view, so that is where highlightView should be attached.
 *
 *
 */
// todo: -L- map of options instead of app reference
class SuperimposedView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)
    constructor(origView: View) : super(origView.context) {
        originalView = origView
    }

    val TAG = "SuperimposedView"

    var originalView: View? = null
    val origRect = Rect()
    val parentRect = Rect()

    // options
    var borderColor = Color.parseColor("#000000aa")
    var borderWidth = 0f
    var zoomPercent = 100
    var placement ="in place" // "center", "above"
    var zoomDuration = 100L


    // returns a value within the range
    fun fit(value: Float, min: Float, max: Float) : Float {
        return when {
            (value < min) -> min
            (value > max) -> max
            else -> value
        }
    }

    // add this amount to a value to fit it into a range
    fun clip(value: Float, min: Float, max: Float) : Float {
        return if (value > max) max - value
        else if (value < min) min - value
        else 0f
    }

    init {

        setBackgroundColor(borderColor)

        // delay rest of initialization until parent is known
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                Log.d(TAG, "onViewDetached")
            }

            override fun onViewAttachedToWindow(v: View?) {
                try {
                    Log.d(TAG, "onViewAttached")

                    // initialize zoom and position once we have the parent
                    resizeAndPosition()
                }
                catch (e: Exception) {
                    Log.e(TAG, "problem on attaching superimposedView", e)
                }
            }
        })

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            setMeasuredDimension(origRect.width(), origRect.height())
            super.onMeasure(
                    makeMeasureSpec(origRect.width(), MeasureSpec.EXACTLY),
                    makeMeasureSpec(origRect.height(), MeasureSpec.EXACTLY)
            )
        }
        catch (e: Exception) {
            //Log.e(TAG, "problem measuring highlightView", e)
        }
    }

    // starts right on top of original view, expanded by "border" to let padding show
    fun resizeAndPosition() {

        originalView!!.getGlobalVisibleRect(origRect)
        (parent as View).getGlobalVisibleRect(parentRect)

        Log.d(TAG, "highlight positioning.. ")
        Log.d(TAG, "highlight parent in window: " + parentRect.toString())
        Log.d(TAG, "original in window: " + origRect.toString())

        // find original view in highlight parent coords
        origRect.offset(0 - parentRect.left, 0 - parentRect.top)

        // outset to allow for border (padding)
        origRect.inset(-borderWidth.toInt(), -borderWidth.toInt())

        translationX = origRect.left.toFloat()
        translationY = origRect.top.toFloat()

        doAnimation()

        //Log.d(TAG, "origRect (start): " + origRect.toString())

    }

    fun doAnimation() {

        // mock up final zoomed rect...
        val fWidth = Math.min(originalView!!.width*(zoomPercent/100) + borderWidth*2, parentRect.width().toFloat())
        val fHeight = Math.min(originalView!!.height*(zoomPercent/100) + borderWidth*2, parentRect.height().toFloat())
        val dW = fWidth - origRect.width()
        val dH = fHeight - origRect.height()

        // ...centered in place
        var dX = 0
        var dY = 0

        // adjust for different placement settings
        //Log.d(TAG, "placement = " + placement)
        when (placement) {
            "in place" -> {}
            "in center" -> {
                dX = parentRect.centerX() - origRect.centerX()
                dY = parentRect.centerY() - origRect.centerY()
            }
            "above" -> {
                dY = -((origRect.height() + fHeight)/2).toInt()
            }
        }

        // adjust to keep it in parent
        dX  += clip(origRect.left + dX - dW/2, 0.2f, parentRect.width() - fWidth).toInt()
        dY  += clip(origRect.top + dY - dH/2, 0.2f, parentRect.height() - fHeight).toInt()

        // animate to new size and position
        //Log.d(TAG, "animate highlight by: " + dW + "x" + dH + "  (" + dX + "," + dY+ ")")
        val animator = this.animate()

        // scale automatically keeps center in place!
        animator.scaleX(fWidth/origRect.width())
        animator.scaleY(fHeight/origRect.height())
        animator.xBy(dX.toFloat())
        animator.yBy(dY.toFloat())
        animator.duration = zoomDuration/3
    }

}
