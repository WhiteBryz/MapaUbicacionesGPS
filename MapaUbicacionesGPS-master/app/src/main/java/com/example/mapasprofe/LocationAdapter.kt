package com.example.mapasprofe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador para la lista de ubicaciones
class LocationAdapter(
    private var locations: MutableList<MainActivity.Location>,
    private val onLocationClick: (MainActivity.Location) -> Unit,
    private val onLocationEdit: (MainActivity.Location) -> Unit,
    private val onLocationDelete: (MainActivity.Location) -> Unit
) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    // ViewHolder para cada elemento de la lista
    class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_view_name)
        val latTextView: TextView = view.findViewById(R.id.text_view_latitude)
        val longTextView: TextView = view.findViewById(R.id.text_view_longitude)
        val editButton: Button = view.findViewById(R.id.button_edit)
        val deleteButton: Button = view.findViewById(R.id.button_delete)
    }

    //Función: Crea un nuevo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return LocationViewHolder(view)
    }

    //Función: Vincula los datos de una ubicación con un ViewHolder
    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.nameTextView.text = location.locationName
        holder.latTextView.text = "Lat: ${location.aLatitude}"
        holder.longTextView.text = "Lon: ${location.aLongitude}"

        holder.itemView.setOnClickListener {
            onLocationClick(location)
        }
        holder.editButton.setOnClickListener {
            onLocationEdit(location)
        }
        holder.deleteButton.setOnClickListener {
            onLocationDelete(location)
        }
    }

    // Función: Devuelve el número de elementos en la lista
    override fun getItemCount(): Int = locations.size

    // Función: Actualiza la lista de ubicaciones
    fun updateLocations(newLocations: List<MainActivity.Location>) {
        locations.clear()
        locations.addAll(newLocations)
        notifyDataSetChanged()
    }
}
