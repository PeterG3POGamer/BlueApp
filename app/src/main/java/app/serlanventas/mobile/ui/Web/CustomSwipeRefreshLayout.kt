package app.serlanventas.mobile.ui.Web

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

class CustomSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var initialDownY: Float = 0f
    private var initialDownX: Float = 0f
    private var isBeingDragged: Boolean = false

    private val maxTopSwipePercentage = 0.10f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialDownY = ev.y
                initialDownX = ev.x
                isBeingDragged = false

                // Solo permitimos el swipe si el toque inicial está en la parte superior (10%)
                if (initialDownY > height * maxTopSwipePercentage) {
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isBeingDragged) {
                    val yDiff = ev.y - initialDownY
                    val xDiff = abs(ev.x - initialDownX)
                    if (yDiff > touchSlop && yDiff > xDiff && !canChildScrollUp()) {
                        isBeingDragged = true
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
