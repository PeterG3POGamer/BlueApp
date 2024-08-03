package app.serlanventas.mobile.ui.Services

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import app.serlanventas.mobile.R

class PreLoading(private val context: Context) {
    private var dialogPreCarga: Dialog? = null
    private lateinit var rocketAnimation: AnimatedVectorDrawable

    fun showPreCarga() {
        dialogPreCarga = Dialog(context)
        dialogPreCarga?.apply {
            setContentView(R.layout.layout_barra_desplazable)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)

            val iconoCarga = findViewById<ImageView>(R.id.iconoCarga)
            val textoCarga = findViewById<TextView>(R.id.textoCarga)

            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_loading_anim)
            if (drawable is AnimatedVectorDrawableCompat) {
                rocketAnimation = drawable as AnimatedVectorDrawable
            } else if (drawable is AnimatedVectorDrawable) {
                rocketAnimation = drawable
            }
            iconoCarga.setImageDrawable(rocketAnimation)

            rocketAnimation.start()

            textoCarga.text = "Cargando..."

            show()
        }
    }

    fun hidePreCarga() {
        dialogPreCarga?.dismiss()
        dialogPreCarga = null
    }
}
