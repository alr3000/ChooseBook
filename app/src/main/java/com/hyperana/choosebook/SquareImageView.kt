package com.hyperana.choosebook

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

/**
 * Created by alr on 1/25/18.
 */
class SquareImageView(context: Context, attrs: AttributeSet): ImageView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        setMeasuredDimension(width, width)
    }
}