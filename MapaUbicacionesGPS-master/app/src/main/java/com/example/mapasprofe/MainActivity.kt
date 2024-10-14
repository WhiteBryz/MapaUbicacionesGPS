package com.example.mapasprofe

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.widget.Toast
import org.osmdroid.views.overlay.Polygon

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var database: SQLiteDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var locationAdapter: LocationAdapter
    private var tempMarker: Marker? = null // Marcador temporal
    private var selectedLocationCircle: Polygon? = null

    // Clase para extraer los datos de la BD
    data class Location(
        val id: Int,
        val locationName: String,
        val aLatitude: Double,
        val aLongitude: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = openOrCreateDatabase("gps_locations", Context.MODE_PRIVATE, null)

        // Crea la tabla si esta no existe
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS locations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "locationName TEXT, " +
                    "aLatitude DOUBLE, " +
                    "aLongitude DOUBLE)"
        )

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        locationAdapter = LocationAdapter(mutableListOf(),
            { location ->
                centerMapOnLocation(location)
            },
            { location ->
                editLocation(location)
            },
            { location ->
                deleteLocation(location)
            }
        )
        recyclerView.adapter = locationAdapter

        // Configura la librería OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        // Configura el MapView
        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK) // Estilo del mapa
        mapView.controller.setZoom(17.0)
        mapView.setMultiTouchControls(true) // Habilita los controles (para hacer zoom con dos dedos)

        // Añade la facultad de telemática como ubicación inicial si no hay ninguna aún
        val cursor = database.rawQuery("SELECT * FROM locations", null)
        if (cursor.count == 0) {
            database.execSQL("INSERT INTO locations (locationName, aLatitude, aLongitude) VALUES ('Facultad de Telemática', 19.24914, -103.69740)")

        }
        //Coloca la primera ubicación como inicio
        cursor.moveToFirst()
        val latitude = cursor.getDouble(2)
        val longitude = cursor.getDouble(3)
        val startPoint = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(startPoint)

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { geoPoint ->
                    // Verifica si el toque se realiza sobre un marcador existente
                    val isMarkerTapped = mapView.overlays.filterIsInstance<Marker>().any { marker ->
                        marker.bounds.contains(geoPoint)
                    }
                    if (!isMarkerTapped) {
                        // Si no se tocó un marcador, añade uno temporal
                        showTemporaryMarker(geoPoint)
                    }
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }

        val overlayEvents = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(overlayEvents)
    }

    // Función: Crear círculo
    private fun showCircleAroundLocation(geoPoint: GeoPoint) {
        if (selectedLocationCircle != null) {
            mapView.overlays.remove(selectedLocationCircle)
        }

        selectedLocationCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(geoPoint, 50.0)
            fillColor = 0x12121212
            strokeColor = 0xFF0000FF.toInt()
            strokeWidth = 2.5f
        }

        mapView.overlays.add(selectedLocationCircle)
        mapView.invalidate()
    }
    // Función: Centrar dentro del mapa un punto
    fun centerMapOnLocation(location: Location, zoomLevel: Double = 18.0) {
        val geoPointEl = GeoPoint(location.aLatitude, location.aLongitude)
        mapView.controller.animateTo(geoPointEl)
        mapView.controller.setZoom(zoomLevel)
        showCircleAroundLocation(geoPointEl)
    }

    // Función: Muestra el marcador temporal en el mapa
    private fun showTemporaryMarker(location: GeoPoint) {
        if (tempMarker == null) {
            tempMarker = Marker(mapView)
            tempMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(tempMarker)
        }

        // Actualiza la posición del marcador temporal
        tempMarker?.position = location
        tempMarker?.title = "Temporary point at ${location.latitude}, ${location.longitude}"
        mapView.controller.animateTo(location) // Mueve la cámara al nuevo marcador temporal

        askToSavePoint(location)
    }

    // Función: Pregunta al usuario si desea guardar el punto
    private fun askToSavePoint(location: GeoPoint) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Guardar punto")
            .setMessage("¿Deseas guardar este punto? (${location.latitude}, ${location.longitude})")
            .setPositiveButton("Guardar") { dialog, _ ->
                // Inicia la actividad para guardar ubicación
                val intent = Intent(this, SaveLocationActivity::class.java)
                intent.putExtra("latitude", location.latitude)
                intent.putExtra("longitude", location.longitude)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume() // Reinicia el mapa
        loadLocations() // Carga las ubicaciones desde la BD
    }


    // Función: Carga las ubicaciones desde la BD
    private fun loadLocations() {
        val cursor = database.rawQuery("SELECT * FROM locations", null)
        val locations = mutableListOf<Location>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val latitude = cursor.getDouble(2)
            val longitude = cursor.getDouble(3)
            locations.add(Location(id, name, latitude, longitude))

            // Añade el marcador al mapa
            val marker = Marker(mapView)
            marker.position = GeoPoint(latitude, longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = name
            marker.setOnMarkerClickListener { m, mapView ->
                m.showInfoWindow()
                true
            }
            mapView.overlays.add(marker)
        }
        cursor.close()

        // Actualiza el adaptador
        locationAdapter.updateLocations(locations)
    }


    // Función: Edita una ubicación
    private fun editLocation(location: Location) {
        val intent = Intent(this, SaveLocationActivity::class.java)
        intent.putExtra("latitude", location.aLatitude)
        intent.putExtra("longitude", location.aLongitude)
        intent.putExtra("locationId", location.id)
        startActivity(intent)
    }

    // Función: Elimina una ubicación
    fun deleteLocation(location: Location) {
        // Elimina la ubicación de la BD
        database.execSQL("DELETE FROM locations WHERE id = ?", arrayOf(location.id))

        // Actualiza la lista de ubicaciones
        loadLocations()

        // Valor de tolerancia de comparación
        val tolerance = 0.00001

        // Elimina la ubicación del mapa
        val markerToDelete = mapView.overlays.find { overlay ->
            overlay is Marker &&
                    Math.abs(overlay.position.latitude - location.aLatitude) < tolerance &&
                    Math.abs(overlay.position.longitude - location.aLongitude) < tolerance
        }

        if (markerToDelete != null) {
            mapView.overlays.remove(markerToDelete)
            mapView.invalidate() // Redibuja el mapa para mostrar los cambios


            Toast.makeText(this, "Ubicación eliminada", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }
}
