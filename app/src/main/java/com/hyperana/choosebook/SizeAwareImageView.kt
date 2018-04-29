package com.hyperana.choosebook

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView


// access fitRect after drawable set, attached and measured in layout to get position and dimensions
// of actual image in the view
open class SizeAwareImageView : ImageView {
    constructor (context: Context) : super (context)
    constructor (context: Context, attrs: AttributeSet) : super (context, attrs)

    val mValues = FloatArray(9)

    val fitRectF = RectF()
    get() {
        field = measureFitImage()
        return field
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

     }

    /******
     *     https://stackoverflow.com/questions/3855218/
     *     trying-to-get-the-display-size-of-an-image-in-an-imageview#answer-15538856
     */
    // todo: first measure of fitrect sometimes gives incorrect values
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

       /* val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        // get matrix settings for this drawable
        imageMatrix.getValues(mValues)
        val tx = mValues[Matrix.MTRANS_X]
        val ty = mValues[Matrix.MTRANS_Y]
        val sx = mValues[Matrix.MSCALE_X]
        val sy = mValues[Matrix.MSCALE_Y]

        // get scaled bitmap height and width for this drawable
        val imageWidth = drawable.intrinsicWidth * sx //- Math.abs(tx)
        val imageHeight = drawable.intrinsicHeight * sy// - Math.abs(ty)

        // get the position of the image, ASSUMING CENTERED IN THIS VIEW
        val left = (viewWidth - imageWidth)/ 2// + tx
        val top = (viewHeight - imageHeight)/ 2// + ty


        // set fitrect using "naturally"-fit image position and scaled dimensions
        fitRectF.set( left, top, left + imageWidth, top + imageHeight)
        Log.d("SizeAwareImageView", "fitRect: " + fitRectF.toString())
*/
    }

    fun measureFitImage() : RectF {
        val viewWidth = width
        val viewHeight = height

        // get matrix settings for this drawable
        imageMatrix.getValues(mValues)
        val tx = mValues[Matrix.MTRANS_X]
        val ty = mValues[Matrix.MTRANS_Y]
        val sx = mValues[Matrix.MSCALE_X]
        val sy = mValues[Matrix.MSCALE_Y]

        // get scaled bitmap height and width for this drawable
        val imageWidth = drawable.intrinsicWidth * sx //- Math.abs(tx)
        val imageHeight = drawable.intrinsicHeight * sy// - Math.abs(ty)

        // get the position of the image, ASSUMING CENTERED IN THIS VIEW
        val left = (viewWidth - imageWidth)/ 2// + tx
        val top = (viewHeight - imageHeight)/ 2// + ty


        // set fitrect using "naturally"-fit image position and scaled dimensions
        return RectF().apply {
            set( left, top, left + imageWidth, top + imageHeight)
            Log.d("SizeAwareImageView", "fitRect: " + toString())
        }
    }

}