package com.raywenderlich.placebook.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.databinding.ActivityMapsBinding
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // This creates a new mapsViewModel only the first time the activity is created
    private val mapsViewModel by viewModels<MapsViewModel>()
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
        setupPlacesClient()
    }
    // 3
    private fun setupLocationClient() {
        // by this variable, I get the user location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // 6 Places client is the gateway to access all of the available APis provided by Places API.
    // Initialized when the activity is created
    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
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
        setupMapListeners()
        createBookmarkObserver()
        getCurrentLocationAndCentersOnIt()

    }

    private fun setupMapListeners() {
        // Indicate that the map should use the custom InfoWindow class
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        map.setOnPoiClickListener { pointOfInterest -> displayPoi(pointOfInterest) }
        map.setOnInfoWindowClickListener { marker -> handleInfoWindowClick(marker)}
    }

    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {

            is PlaceInfo -> {
                val placeInfo = marker.tag as PlaceInfo
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
                    }
                }
                marker.remove()
            }

            is MapsViewModel.BookmarkMarkerView -> {
                val bookmarkMarkerView = marker.tag as MapsViewModel.BookmarkMarkerView
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let { id -> startBookmarkDetails(id) }
            }
        }
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        startActivity(intent)
    }

    // Observes changes in the database
    private fun createBookmarkObserver() {
        // Everytime the data changes in the database, this lambda is called
        val bookmarksObserver = Observer<List<MapsViewModel.BookmarkMarkerView>> { bookmarks ->
                map.clear()
                bookmarks?.let { bookmarks ->
                    displayAllBookmarks(bookmarks)
                }
        }
        mapsViewModel.getBookmarkMarkerViews()?.observe(this, bookmarksObserver)
    }

    // Adds blue markers for bookmarks
    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        bookmarks.forEach { bookmark -> addPlaceMarker(bookmark) }
    }

    // Creates the blue marker
    private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = map.addMarker(
                 MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.8f)
        )
        marker.tag = bookmark
        return marker
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

    //1 Called when there is a tap on a point of interest on the map.
    // This call is made within onMapReady
    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
    }

    //2 Called within the method above just to get the place
    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {

        val placeId = pointOfInterest.placeId

        // These data is returned to me
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG)

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                displayPoiGetPhotoStep(place)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(TAG, "Place not found: " + exception.message + ", " + "statusCode: " + statusCode)
                }
            }
    }

    //3 Called within the method above just to get the photo
    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place.photoMetadatas?.get(0)

        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }

        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
            .build()

        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { response ->
                val photo = response.bitmap
                displayPoiDisplayStep(place,photo)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(TAG, "Place not found: " + exception.message + ", " + "statusCode: " + statusCode)
                }
            }
    }

    // Add a marker when a point of interest is tapped
    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {

        val marker = map.addMarker(
            MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )

        marker?.tag = PlaceInfo(place, photo)
        // If the marker is tapped, show the info window immediately
        marker?.showInfoWindow()
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }

    // This class is used to store data in the marker tag
    // I modified that because the place will not be null
    class PlaceInfo(val place: Place, val image: Bitmap? = null)
}