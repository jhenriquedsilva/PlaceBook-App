package com.raywenderlich.placebook

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.raywenderlich.placebook.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // private var locationRequest: LocationRequest? = null

    //1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
    }
    // 3
    private fun setupLocationClient() {
        // by this variaable, I get the user location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //2 This function is called by supportMapFragment when the map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        // The GoogleMap object is used to control and query the map
        map = googleMap
        getCurrentLocationAndCentersOnIt()
    }
    //3 When the app launches, get the current location and centers the map on it
    private fun getCurrentLocationAndCentersOnIt() {
        // If the permission is not granted, request it
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {

            requestLocationPermissions()

        } else {
            /*
            if (locationRequest == null) {
                locationRequest = LocationRequest.create()
                locationRequest?.let { locationRequest ->
                    // These are just guide lines, not rules
                    locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    locationRequest.interval = 5000 // Milliseconds
                    locationRequest.fastestInterval = 1000
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            getCurrentLocationAndCentersOnIt()
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }
            */

            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                val location = task.result
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    // map.clear()
                    // map.addMarker(MarkerOptions().position(latLng).title("You're current location!"))
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(cameraUpdate)
                } else {
                    Log.e(TAG, "No location found")
                }
            }

        }
    }

     //4 To get the user location, I need this permission
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
    }

    //5 Called with the request permissions result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getCurrentLocationAndCentersOnIt()

            } else {

                Log.e(TAG, "Location permission denied")
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }
}