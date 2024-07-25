package com.example.blueapp.ui.Jabas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.blueapp.R
import com.example.blueapp.ui.Interfaces.OnItemClickListener
import com.example.blueapp.ui.ViewModel.SharedViewModel


data class JabasItem(val id: Int, val numeroJabas: Int, val numeroPollos: Int, val pesoKg: Double, val conPollos: String,)

class JabasAdapter(private val itemList: MutableList<JabasItem>, private val listener: OnItemClickListener, private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<JabasAdapter.JabasViewHolder>() {

    // Variable para almacenar el conjunto de IDs eliminados
    private val deletedIds = mutableSetOf<Int>()

    class JabasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idJabas: TextView = itemView.findViewById(R.id.id_jabas)
        val numeroJabas: TextView = itemView.findViewById(R.id.numero_jabas)
        val numeroPollos: TextView = itemView.findViewById(R.id.numero_pollos)
        val pesoKg: TextView = itemView.findViewById(R.id.peso_kg)
        val conPollos: TextView = itemView.findViewById(R.id.con_pollos)
        val estadoIcon: ImageView = itemView.findViewById(R.id.estado_icon)


        // Método para enlazar los datos del item con las vistas
        fun bind(item: JabasItem) {
            idJabas.text = item.id.toString()
            numeroJabas.text = item.numeroJabas.toString()
            numeroPollos.text = item.numeroPollos.toString()
            pesoKg.text = item.pesoKg.toString()
            conPollos.text = item.conPollos

//            val context = itemView.context
//            when (item.conPollos) {
//                "JABAS SIN POLLOS" -> conPollos.setBackgroundColor(ContextCompat.getColor(context, R.color.orange))
//                "JABAS CON POLLOS" -> conPollos.setBackgroundColor(ContextCompat.getColor(context, R.color.your_greed))
//                else -> conPollos.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
//            }
            val iconRes = if (item.conPollos == "JABAS CON POLLOS") {
                R.drawable.cabezapollo
            } else {
                R.drawable.jabadepollo
            }
            estadoIcon.setImageResource(iconRes)
        }
    }

    // Método para inflar la vista del ítem
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JabasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_pesos, parent, false)
        return JabasViewHolder(view)
    }

    // Método para enlazar los datos con la vista del ViewHolder
    override fun onBindViewHolder(holder: JabasViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            handleItemClick(holder.adapterPosition)
        }
    }

    // Método para obtener el número total de ítems
    override fun getItemCount(): Int {
        return itemList.size
    }

    // Método para agregar un nuevo ítem a la lista
    fun addItem(item: JabasItem) {
        val newItem = createNewItem(item)

        // Obtener el contador actual de jabas
        var contadorAntiguo = sharedViewModel.getContadorJabasAntiguo() ?: 0
        var nuevoContador = sharedViewModel.getContadorJabas() ?: 0

        // Actualizar el contador según el tipo de ítem (con o sin pollos)
        if (item.conPollos == "JABAS SIN POLLOS") {
            nuevoContador += item.numeroJabas
            itemList.add(newItem)
        } else {
            if (item.numeroJabas > nuevoContador){
                if (contadorAntiguo != item.numeroJabas){
                    contadorAntiguo += item.numeroJabas
                }else {
                    contadorAntiguo = 0
                }
                sharedViewModel.setContadorJabasAntiguo(contadorAntiguo)
            }else{
                itemList.add(newItem)
                nuevoContador -= item.numeroJabas
                sharedViewModel.setContadorJabasAntiguo(item.numeroJabas)
            }

        }
        var ListJabas = sharedViewModel.getDataDetaPesoPollosJson()

        if (nuevoContador == 0){
            if (!ListJabas.isNullOrBlank()){
                sharedViewModel.setBtnFalse()
            }else{
                sharedViewModel.setBtnTrue()
            }
        }else{
            if (nuevoContador < 0){
                sharedViewModel.setBtnFalse()
            }else{
                if (ListJabas.isNullOrBlank()){
                    sharedViewModel.setBtnFalse()
                }
            }
        }


        updateIdsAfterAddition()
        notifyItemInserted(itemList.size - 1)

        // Establecer el nuevo valor del contador en SharedViewModel
        sharedViewModel.setContadorJabas(nuevoContador)

        // Notificar al listener que se añadió un ítem
        listener.onItemAdd()
    }

    // Maneja el clic en un ítem
    private fun handleItemClick(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            val currentId = itemList[position].id
            if (isDoubleClick(position)) {
                deleteItem(position, currentId)
            } else {
                recordLastClick(position)
            }
        }
    }

    // Verifica si el clic es un doble clic
    private fun isDoubleClick(position: Int): Boolean {
        return lastClickedPosition == position && System.currentTimeMillis() - lastClickTime < DOUBLE_CLICK_TIME_THRESHOLD
    }

    // Elimina un ítem de la lista y actualiza los IDs
    private fun deleteItem(position: Int, id: Int) {
        deletedIds.add(id)
        val jabasList = itemList[position]

        val numeroDel = jabasList.numeroJabas
        val NjabasActual = sharedViewModel.getContadorJabas() ?: 0

        var nuevoContador = NjabasActual

        // Verificar si el ítem tiene pollos o no
        if (jabasList.conPollos == "JABAS SIN POLLOS") {
            nuevoContador -= numeroDel
        } else {
            nuevoContador += numeroDel
        }
        var ListJabas = sharedViewModel.getDataDetaPesoPollosJson()

        if (nuevoContador == 0){
            if (ListJabas.isNullOrBlank()){
                sharedViewModel.setBtnFalse()
            }else{
                sharedViewModel.setBtnTrue()
            }
        }else{
            if (nuevoContador < 0){
                sharedViewModel.setBtnFalse()
            }else{
                if (ListJabas.isNullOrBlank()){
                    sharedViewModel.setBtnFalse()
                }
            }
        }

        sharedViewModel.setContadorJabas(nuevoContador)
        itemList.removeAt(position)
        // Actualiza el contador de jabas después de eliminar el ítem
        listener.onItemDeleted()

        notifyItemRemoved(position)
        updateIdsAfterDeletion()
        resetLastClicked()
    }

    // Crea un nuevo ítem reutilizando IDs eliminados si están disponibles
    private fun createNewItem(item: JabasItem): JabasItem {
        return if (deletedIds.isNotEmpty()) {
            val reusedId = deletedIds.minOrNull() ?: itemList.size + 1
            deletedIds.remove(reusedId)
            item.copy(id = reusedId)
        } else {
            item.copy(id = itemList.size + 1)
        }
    }

    // Actualiza los IDs después de eliminar un ítem
    private fun updateIdsAfterDeletion() {
        itemList.forEachIndexed { index, jabasItem ->
            itemList[index] = jabasItem.copy(id = index + 1)
        }
        notifyDataSetChanged()
    }

    // Actualiza los IDs después de agregar un ítem
    private fun updateIdsAfterAddition() {
        itemList.forEachIndexed { index, jabasItem ->
            itemList[index] = jabasItem.copy(id = index + 1)
        }
        notifyDataSetChanged()
    }

    // Registra la posición y el tiempo del último clic
    private fun recordLastClick(position: Int) {
        lastClickedPosition = position
        lastClickTime = System.currentTimeMillis()
    }

    // Reinicia las variables de control de doble clic
    private fun resetLastClicked() {
        lastClickedPosition = RecyclerView.NO_POSITION
        lastClickTime = 0
    }

    // Variables para controlar el último clic
    private var lastClickedPosition: Int = RecyclerView.NO_POSITION
    private var lastClickTime: Long = 0

    // Constante para definir el tiempo máximo entre clics para considerarlo doble clic (en milisegundos)
    private val DOUBLE_CLICK_TIME_THRESHOLD = 300 // 300 milisegundos
}
