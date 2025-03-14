package app.serlanventas.mobile.ui.Jabas

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.serlanventas.mobile.R
import app.serlanventas.mobile.ui.Interfaces.OnItemClickListener
import app.serlanventas.mobile.ui.ViewModel.SharedViewModel

data class JabasItem(val id: Int, val numeroJabas: Int, val numeroPollos: Int, val pesoKg: Double, val conPollos: String, val idPesoPollo: String, val fechaPeso: String)

class JabasAdapter(private val itemList: MutableList<JabasItem>, private val listener: OnItemClickListener, private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<JabasAdapter.JabasViewHolder>() {

    private val deletedIds = mutableSetOf<Int>()

    class JabasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idJabas: TextView = itemView.findViewById(R.id.id_jabas)
        val numeroJabas: TextView = itemView.findViewById(R.id.numero_jabas)
        val numeroPollos: TextView = itemView.findViewById(R.id.numero_pollos)
        val pesoKg: TextView = itemView.findViewById(R.id.peso_kg)
        val conPollos: TextView = itemView.findViewById(R.id.con_pollos)
        val estadoIcon: ImageView = itemView.findViewById(R.id.estado_icon)
        val fechaPeso: TextView = itemView.findViewById(R.id.fecha_peso)
        val eliminarIcon: ImageView = itemView.findViewById(R.id.eliminar_icon)

        fun bind(item: JabasItem, listener: OnItemClickListener) {
            idJabas.text = item.id.toString()
            numeroJabas.text = item.numeroJabas.toString()
            numeroPollos.text = item.numeroPollos.toString()
            pesoKg.text = item.pesoKg.toString()
            conPollos.text = item.conPollos
            fechaPeso.text = item.fechaPeso

            val iconRes = if (item.conPollos == "JABAS CON POLLOS") {
                R.drawable.cabezapollo
            } else {
                R.drawable.jabadepollo
            }
            estadoIcon.setImageResource(iconRes)

            // Set click listener for the delete button
            eliminarIcon.setOnClickListener {
                // Highlight the entire row
                itemView.isSelected = true
                showDeleteConfirmationDialog(item, listener)
            }
        }

        private fun showDeleteConfirmationDialog(item: JabasItem, listener: OnItemClickListener) {
            val context = itemView.context
            AlertDialog.Builder(context)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este ítem?")
                .setPositiveButton("Eliminar") { _, _ ->
                    listener.onItemDeleted(item.id)
                    // Remove highlight after deletion
                    itemView.isSelected = false
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    // Remove highlight if canceled
                    itemView.isSelected = false
                }
                .show()
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JabasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_pesos, parent, false)
        return JabasViewHolder(view)
    }

    override fun onBindViewHolder(holder: JabasViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item, listener)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun addItem(item: JabasItem) {
        val newItem = createNewItem(item)
        var jabaSinPollosCount = sharedViewModel.getContadorJabas() ?: 0
        var jabasConPollosCount = sharedViewModel.getContadorJabasAntiguo() ?: 0

        when (item.conPollos) {
            "JABAS SIN POLLOS" -> {
                // Adding empty crates - increase the counter
                jabaSinPollosCount += item.numeroJabas
                itemList.add(newItem)
            }
            else -> {
                // Adding crates with chickens
                if (item.numeroJabas <= jabaSinPollosCount) {
                    // We have enough empty crates to fill with chickens
                    itemList.add(newItem)
                    jabaSinPollosCount -= item.numeroJabas
                    jabasConPollosCount += item.numeroJabas
                } else {
                    // Not enough empty crates available
                    jabasConPollosCount += item.numeroJabas
                }
            }
        }

        // Update counters in the shared view model
        sharedViewModel.setContadorJabas(jabaSinPollosCount)
        sharedViewModel.setContadorJabasAntiguo(jabasConPollosCount)

        // Update button state based on available crates
        updateButtonState(jabaSinPollosCount)

        updateIdsAfterAddition()
        notifyItemInserted(itemList.size - 1)
        listener.onItemAdd()
    }

    fun deleteItem(position: Int) {
        val itemToDelete = itemList[position]

        // Remove the item from the list
        itemList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemList.size)

        // Get current counters
        var jabaSinPollosCount = sharedViewModel.getContadorJabas() ?: 0
        var jabasConPollosCount = sharedViewModel.getContadorJabasAntiguo() ?: 0

        // Update counters based on the type of deleted item
        when (itemToDelete.conPollos) {
            "JABAS SIN POLLOS" -> {
                // Removing empty crates - decrease the counter
                jabaSinPollosCount -= itemToDelete.numeroJabas
            }
            else -> {
                // Removing crates with chickens - return them to empty crates count
                jabaSinPollosCount += itemToDelete.numeroJabas
                jabasConPollosCount -= itemToDelete.numeroJabas
                if (jabasConPollosCount < 0) jabasConPollosCount = 0
            }
        }

        // Update counters in the shared view model
        sharedViewModel.setContadorJabas(jabaSinPollosCount)
        sharedViewModel.setContadorJabasAntiguo(jabasConPollosCount)

        // Update button state based on available crates
        updateButtonState(jabaSinPollosCount)

        // Notify the listener of the deletion
        listener.onItemDeleted(itemToDelete.id)
    }

    // Helper function to update button state
    private fun updateButtonState(jabaSinPollosCount: Int) {
        val listJabas = sharedViewModel.getDataDetaPesoPollosJson()

        when {
            jabaSinPollosCount == 0 -> {
                // No empty crates left
                if (!listJabas.isNullOrBlank()) {
                    sharedViewModel.setBtnFalse()
                } else {
                    sharedViewModel.setBtnTrue()
                }
            }
            jabaSinPollosCount < 0 -> {
                // Invalid state - negative count
                sharedViewModel.setBtnFalse()
            }
            else -> {
                // We have empty crates
                if (listJabas.isNullOrBlank()) {
                    sharedViewModel.setBtnFalse()
                } else {
                    sharedViewModel.setBtnTrue()
                }
            }
        }
    }


    private fun createNewItem(item: JabasItem): JabasItem {
        return if (deletedIds.isNotEmpty()) {
            val reusedId = deletedIds.minOrNull() ?: itemList.size + 1
            deletedIds.remove(reusedId)
            item.copy(id = reusedId)
        } else {
            item.copy(id = itemList.size + 1)
        }
    }

    private fun updateIdsAfterDeletion() {
        itemList.forEachIndexed { index, jabasItem ->
            itemList[index] = jabasItem.copy(id = index + 1)
        }
        notifyDataSetChanged()
    }

    private fun updateIdsAfterAddition() {
        itemList.forEachIndexed { index, jabasItem ->
            itemList[index] = jabasItem.copy(id = index + 1)
        }
        notifyDataSetChanged()
    }
}
