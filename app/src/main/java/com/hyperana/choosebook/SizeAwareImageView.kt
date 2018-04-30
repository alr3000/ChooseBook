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

    // todo: -L- separate access from calculation to control re-use
    val fitRectF = RectF()
    get() {
        field = measureFitImage()
        return field
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
            //Log.d("SizeAwareImageView", "fitRect: " + toString())
        }
    }

}