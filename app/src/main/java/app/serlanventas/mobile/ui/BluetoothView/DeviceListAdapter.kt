package app.serlanventas.mobile.ui.BluetoothView

import android.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class DeviceListAdapter(context: Context, devices: List<BluetoothDevice?>) :
    ArrayAdapter<BluetoothDevice?>(context, 0, devices) {
    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.simple_list_item_1, parent, false)
        }

        val device = getItem(position) ?: return convertView!!

        val textView = convertView!!.findViewById<TextView>(R.id.text1)
        textView.text = """
            ${device.name ?: "Dispositivo desconocido"}
            ${device.address}
            """.trimIndent()

        return convertView
    }
}
