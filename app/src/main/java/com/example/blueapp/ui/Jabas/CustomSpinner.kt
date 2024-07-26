package com.example.blueapp.ui.Jabas

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import com.example.blueapp.R

class SearchableSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSpinner(context, attrs, defStyleAttr) {

    private var items: List<String> = listOf()
    private var onItemSelectedListener: OnItemSelectedListener? = null

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.onItemSelectedListener = listener
    }

    override fun performClick(): Boolean {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Seleccionar peso")

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.searchable_spinner_dialog, null)
        builder.setView(dialogView)

        val searchEditText: EditText = dialogView.findViewById(R.id.search_edit_text)
        val listView: ListView = dialogView.findViewById(R.id.list_view)

        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).text = items[position]
                return view
            }

            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val filterResults = FilterResults()
                        if (constraint.isNullOrEmpty()) {
                            filterResults.count = items.size
                            filterResults.values = items
                        } else {
                            val filteredItems = items.filter {
                                it.contains(constraint, ignoreCase = true)
                            }
                            filterResults.count = filteredItems.size
                            filterResults.values = filteredItems
                        }
                        return filterResults
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        @Suppress("UNCHECKED_CAST")
                        val filteredList = results?.values as? List<String> ?: listOf()
                        clear()
                        addAll(filteredList)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        listView.adapter = adapter

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
        })

        val dialog = builder.create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            val selectedPosition = items.indexOf(selectedItem)
            setSelection(selectedPosition)
            onItemSelectedListener?.onItemSelected(this, null, selectedPosition, 0)
            dialog.dismiss()
        }

        dialog.show()
        return true
    }

    fun setItems(items: List<String>) {
        this.items = items
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        setAdapter(adapter)
    }
}