package com.example.mapasprofe

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class SaveLocationActivity : AppCompatActivity() {
    private lateinit var locationNameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private var locationId: Int? = null

    // Clase para extraer los datos de la BD
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_location)

        locationNameEditText = findViewById(R.id.edit_text_location_name)
        saveButton = findViewById(R.id.button_save)
        cancelButton = findViewById(R.id.button_cancel) //

        // Obtiene los datos de la ubicación de la actividad anterior
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        locationId = intent.getIntExtra("locationId", -1) // -1 si no se pasa un ID

        if (locationId != -1) {
            loadLocationData(locationId!!)
        }
        saveButton.setOnClickListener {
            saveLocation(latitude, longitude)
        }
        cancelButton.setOnClickListener {
            finish() // Cerrar la actividad y volver a la anterior
        }
    }

    // Función: Carga los datos de la ubicación
    private fun loadLocationData(locationId: Int) {
        val database = openOrCreateDatabase("gps_locations", Context.MODE_PRIVATE, null)
        val cursor = database.rawQuery("SELECT locationName FROM locations WHERE id = ?", arrayOf(locationId.toString()))
        if (cursor.moveToFirst()) {
            val locationName = cursor.getString(0)
            locationNameEditText.setText(locationName)
        }
        cursor.close()
        database.close()
    }

    // Función: Guarda la ubicación
    private fun saveLocation(latitude: Double, longitude: Double) {
        val locationName = locationNameEditText.text.toString()

        if (locationName.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un nombre para la ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val database = openOrCreateDatabase("gps_locations", Context.MODE_PRIVATE, null)

        // Si no se pasa un ID, se está guardando una nueva ubicación
        if (locationId == -1) {
            database.execSQL("INSERT INTO locations (locationName, aLatitude, aLongitude) VALUES (?, ?, ?)",
                arrayOf(locationName, latitude, longitude))
            Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()

        // Si se pasa un ID, se está editando una ubicación existente
        } else {
            database.execSQL("UPDATE locations SET locationName = ? WHERE id = ?",
                arrayOf(locationName, locationId))
            Toast.makeText(this, "Ubicación actualizada", Toast.LENGTH_SHORT).show()
        }
        database.close()
        finish()
    }
}
