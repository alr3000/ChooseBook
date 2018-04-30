package com.hyperana.choosebook

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View

/************
 *  zoomscale is applied to the drawable's intrinsic width and height
 *  fit is for now assumed to be fit-center, don't know if it works for anything else
 */

class TouchZoomImageView : SizeAwareImageView,  View.OnTouchListener {
    constructor (context: Context) : super (context)
    constructor (context: Context, attrs: AttributeSet) : super (context, attrs)

    val TAG = "TouchZoomImageView"

    val CLICK_TIME_ALLOWANCE = 100 // no android standard? long-click is 500ms
    //val CLICK_MOTION_ALLOWANCE = 40

    var zoomScale = 4f // todo: -L- use xml attributes system

    // retain pre-zoom properties
    var oScaleType: ScaleType?  = null // retain scaleType for use on unzoom
    var oImageMatrix = Matrix()
    var oFitRectF = RectF()
    var mScaleX = 1f
    var mScaleY = 1f

    // retain data during gesture for relative calc
    var currentTouchId: Int? = null // working touch id
    var currentCoords = PointF(0f,0f) // view coordinates of last touch point
    var clipRect = RectF()
    val mMatrix = Matrix()


    init {
        setBackgroundColor(Color.parseColor("#bb000000"))
    }

    // ********************** LIFECYCLE ********************
    // set listeners
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

         setOnTouchListener(this)

    }

    // unset listeners
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        setOnTouchListener(null)
    }
/*
    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        try {
            canvas!!.drawRect(
                    fitRectF,
                    Paint().apply {
                        color = Color.parseColor("#99ffff00")
                        strokeWidth = 20f
                    })

        }
        catch (e: Exception) {
            Log.e(TAG, "problem drawing foreground")
        }
    }*/

    // ********************* LISTENERS *************************


    //    ACTION_DOWN—For the first pointer that touches the screen. This starts the gesture.
    //      The pointer data for this pointer is always at index 0 in the MotionEvent.
    //    ACTION_POINTER_DOWN—For extra pointers that enter the screen beyond the first.
    //      The pointer data for this pointer is at the index returned by getActionIndex().
    //    ACTION_MOVE—A change has happened during a press gesture.
    //    ACTION_POINTER_UP—Sent when a non-primary pointer goes up.
    //    ACTION_UP—Sent when the last pointer leaves the screen.

    // gets info of relevant touch:
    // down and up record most recent (the one creating the event)
    // move gets info for supplied id, ignoring others and throwing error if not found
    class MyTouchEventInfo(event: MotionEvent, var id: Int?) {
        val TAG = "MyTouchEventInfo"
        val coords = MotionEvent.PointerCoords()

        init {

            when (event.action) {
                MotionEvent.ACTION_DOWN, ACTION_UP -> {
                    id = event.getPointerId(0)
                    event.getPointerCoords(0, coords)
                }
                MotionEvent.ACTION_POINTER_DOWN, ACTION_POINTER_UP -> {
                    id = event.getPointerId(event.actionIndex)
                    event.getPointerCoords(event.actionIndex, coords)
                }
                ACTION_MOVE -> {
                    val i = event.findPointerIndex(id!!)
                    event.getPointerCoords(i, coords)
                }
            }
           // Log.d(TAG, toString())
        }

        override fun toString(): String {
            return "MyTouchInfo ( id: " + id + " coords: " + coords.x + "," + coords.y + " ) "
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        try {
            // get appropriate pointer info
            val info = MyTouchEventInfo(event!!, currentTouchId)
//            Log.d(TAG, "myTouch(" + event.action.toString() + ": " + info.toString())

            when (event.action) {

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
//                    Log.d(TAG, "fitRectF: " + fitRectF.toString())

                    if (fitRectF.contains(info.coords.x, info.coords.y)) {

                        // replace current movement with new one
                        currentTouchId = info.id
                        zoomToViewCoords(info.coords.x, info.coords.y)
                        return true // consume to receive future events
                    }
                }

                MotionEvent.ACTION_MOVE -> {

                    // pan to new coords
                    changePosition(info.coords.x, info.coords.y)

                    return true // consume to receive future events
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (info.id == currentTouchId) {

                        cancelZoom()

                        // check if click-like
                        if (
                                (event.eventTime - event.downTime < CLICK_TIME_ALLOWANCE)
                        //        && (integrateHistory(event, info.id) < CLICK_MOTION_ALLOWANCE)
                        ) {
//                            Log.d(TAG, "touch-click")
                            performClick()
                        }
                        return true
                    }
                }
                else  -> {
                    cancelZoom()
                }
            }
        }
        catch (e: Exception) {
//            Log.e(TAG, "failed touch event: " + event?.toString(), e)
        }
        return false
    }

    fun zoomToViewCoords(x: Float, y: Float) {
//        Log.d(TAG, "zoomToViewCoords: " + x + "," + y)

        // retain pre-zoom fitRect for MOVE bounding and to trigger calculation
        oFitRectF.set(fitRectF)
//        Log.d(TAG, "fitRectF: " + oFitRectF.toString())

        // get composite scale conversion from fitRect to scaled intrinsic
        mScaleX = drawable.intrinsicWidth/oFitRectF.width() * zoomScale
        mScaleY = drawable.intrinsicHeight/oFitRectF.height() * zoomScale

        // get zoomed-image coords from view coords
        val zoomedClipX = (x - oFitRectF.left) * mScaleX
        val zoomedClipY = (y - oFitRectF.top) * mScaleY
//        Log.d(TAG, "zoomedClip center coords: " + zoomedClipX + "," + zoomedClipY)

        // view dimensions
        val viewRect = Rect()
        getLocalVisibleRect(viewRect)
//        Log.d(TAG, "viewRect: " + viewRect.toString())


        // set rectangle of viewed portion of the image
        clipRect.apply {
            set(
                    zoomedClipX,
                    zoomedClipY,
                    viewRect.width().toFloat() + zoomedClipX,
                    viewRect.height().toFloat() + zoomedClipY
            )

            // centered on touch point
            offset( - viewRect.exactCenterX(),  - viewRect.exactCenterY() )

            // bounded by zoomed image dimensions
            boundRectInZoomedImage(this)

        }
//        Log.d(TAG, "clipRect: " + clipRect.toString())

        // retain screen touch coords for MOVE calculations
        currentCoords = PointF(x, y)

        // create matrix and apply to image
        oScaleType = scaleType
        oImageMatrix = imageMatrix

        scaleType = ScaleType.MATRIX
        imageMatrix = mMatrix.apply {

            // zoom from scratch
            setScale(zoomScale, zoomScale)

            // translate to clipRect
            postTranslate(-clipRect.left, -clipRect.top)
        }

//        Log.d(TAG, "fitRectF (zoomed): " + fitRectF.toString())
    }

    fun cancelZoom() {
//        Log.d(TAG, "cancelZoom")

        currentTouchId = null

        // return to fit image
        scaleType = oScaleType
        imageMatrix = oImageMatrix
    }

    fun changePosition(x: Float, y: Float) {

        // bound coords to original fitrect
        val bx = boundInRange(x, oFitRectF.left .. oFitRectF.right)
        val by = boundInRange(y, oFitRectF.top .. oFitRectF.bottom)

        // screen movement, scaled to zoomed image
        val dx = (bx - currentCoords.x) * mScaleX
        val dy = (by - currentCoords.y) * mScaleY
//        Log.d(TAG, "changePosition by: " +dx + "," + dy)

        // current clip position
        val cLeft = clipRect.left
        val cTop = clipRect.top

        // adjust viewed portion (clip) of zoomed image
        // and check bounds
        clipRect.offset(dx, dy)
        boundRectInZoomedImage(clipRect)
//        Log.d(TAG, "moved clipRect: " + clipRect.toString())

        // adjust image matrix: must reassign to make image redraw
        imageMatrix = mMatrix.apply {
            postTranslate(
                    cLeft - clipRect.left,
                    cTop - clipRect.top
            )
        }

        // save new coords for next move
        currentCoords = PointF(bx, by)
    }


    fun boundRectInZoomedImage(rect: RectF) {

        // zoomed image dimensions
        val zoomWidth = drawable.intrinsicWidth * zoomScale
        val zoomHeight = drawable.intrinsicHeight * zoomScale
//        Log.d(TAG, "zoomed image: " + zoomWidth + "," + zoomHeight)


        rect.apply {
            offsetTo(
                    maxOf(0f, left),
                    maxOf(0f, top)
            )
            offsetTo(
                    minOf(left, zoomWidth - width()),
                    minOf(top, zoomHeight - height())
            )
        }

    }

    // todo: -L- throw if range start>end
    fun boundInRange(x: Float, range: ClosedFloatingPointRange<Float>) : Float {
        return maxOf(x, range.start).let { minOf( it, range.endInclusive)}
    }

}