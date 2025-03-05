package app.serlanventas.mobile.ui.DataSyncManager

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import app.serlanventas.mobile.R
import com.bumptech.glide.Glide

class DialogManager(private val context: Context) {

    fun showSyncConfirmationDialog(onSync: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Sincronización de Datos")
            .setMessage("¿Deseas sincronizar los datos ahora? Si no sincronizas, no tendrás los datos actualizados.")
            .setPositiveButton("Sincronizar") { dialog, _ ->
                onSync(true)
                dialog.dismiss()
            }
            .setNegativeButton("Omitir") { dialog, _ ->
                onSync(true)
                dialog.dismiss()
            }
            .show()
    }

    fun showSuccessDialog(onDismiss: () -> Unit) {
        // Inflar el diseño personalizado
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_success, null)

        // Configurar el diálogo
        val dialog = AlertDialog.Builder(context)
            .setView(view) // Usar el diseño personalizado
            .create()

        // Personalizar elementos del diseño
        val gifSuccess = view.findViewById<ImageView>(R.id.gifSuccess)
        val titleSuccess = view.findViewById<TextView>(R.id.titleSuccess)
        val messageSuccess = view.findViewById<TextView>(R.id.messageSuccess)
        val btnAceptar = view.findViewById<Button>(R.id.btnAceptar)

        // Cargar el GIF usando Glide
        Glide.with(context)
            .asGif()
            .load(R.drawable.success_animation)
            .into(gifSuccess)

        // Acción del botón "Aceptar"
        btnAceptar.setOnClickListener {
            dialog.dismiss() // Cerrar el diálogo
            onDismiss.invoke() // Ejecutar la acción después de cerrar el diálogo
        }

        // Mostrar el diálogo
        dialog.show()
    }

    fun showErrorDialog(onRetry: ((Boolean) -> Unit)?) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Hubo un problema al sincronizar los datos. ¿Quieres reintentar o restaurar?")
            .setPositiveButton("Reintentar") { dialog, _ ->
                onRetry?.invoke(true)
                dialog.dismiss()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                onRetry?.invoke(false)
                dialog.dismiss()
            }
            .show()
    }
}