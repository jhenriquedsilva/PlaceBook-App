package com.raywenderlich.placebook.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.adapter.BookmarkListAdapter
import com.raywenderlich.placebook.databinding.ActivityMapsBinding
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // This creates a new mapsViewModel only the first time the activity is created
    private val mapsViewModel by viewModels<MapsViewModel>()
    private lateinit var databinding: ActivityMapsBinding
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    // private var locationRequest: LocationRequest? = null
    private var markers = HashMap<Long, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupPlacesClient()
        setupToolBar()
        setupNavigationDrawer()
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

    private fun setupToolBar() {
        setSupportActionBar(databinding.mainMapView.toolbar)
        // This fully manages the the display and functionality of the toggle icon
        val toggle = ActionBarDrawerToggle(
            this,
            databinding.drawerLayout,
            databinding.mainMapView.toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        // Ensures that the toggle is displayed initially
        toggle.syncState()
    }

    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        databinding.drawerViewMaps.bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        databinding.drawerViewMaps.bookmarkRecyclerView.adapter = bookmarkListAdapter
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
        map.setOnInfoWindowClickListener { marker -> handleInfoWindowClick(marker) }
        databinding.mainMapView.fab.setOnClickListener {
            searchAtCurrentLocation()
        }
        /**
         * Feature: creates a bookmark from a place that does not show up on the map
         * Creates a bookmark from a map location
         * A listener for when a view is clicked and held
         */
        map.setOnMapLongClickListener { latLng ->
            newBookmark(latLng)
        }
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

            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = marker.tag as MapsViewModel.BookmarkView
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let { id -> startBookmarkDetails(id) }
            }
        }
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    // Observes changes in the database
    private fun createBookmarkObserver() {
        // Everytime the data changes in the database, this lambda is called
        val bookmarksObserver = Observer<List<MapsViewModel.BookmarkView>> { bookmarks ->
            map.clear()
            markers.clear()

            bookmarks?.let { bookmarks ->
                displayAllBookmarks(bookmarks)
                bookmarkListAdapter.setBookmarkData(bookmarks)
            }
        }
        mapsViewModel.getBookmarkViews()?.observe(this, bookmarksObserver)
    }

    // Adds blue markers for bookmarks
    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkView>) {
        bookmarks.forEach { bookmark -> addPlaceMarker(bookmark) }
    }

    // Creates the blue marker
    private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = map.addMarker(
                 MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let { BitmapDescriptorFactory.fromResource(it) })
                .alpha(0.8f)
        )
        marker.tag = bookmark
        // This adds a new entry to markers when a new marker is added to the map.
        bookmark.id?.let { id -> markers.put(id, marker) }
        return marker
    }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        databinding.drawerLayout.closeDrawer(databinding.drawerViewMaps.drawerView)
        val marker = markers[bookmark.id]
        marker?.showInfoWindow()

        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }

    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        // Smoothly pans the map
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
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
            Place.Field.LAT_LNG,
            Place.Field.TYPES
        )

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

    private fun searchAtCurrentLocation() {
        // 1 What attributes the Autocomplete widget is going to return
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES
        )

        // 2 Computes the bounds of the currently visible region of the map
        val bounds = RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)

        try {
            // 3 Autocomplete provides an intent builder method to build up the intent to launch the autocomplete activity
            // Can overlay the current activity
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                placeFields
            )   // Look for places within the current map window before searching other places
                .setLocationBias(bounds)
                // Returns the intent
                .build(this)
            // 4 When the user finishes the search, the results are identified by this request code
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

        } catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, "Problems Searching", Toast.LENGTH_LONG).show()

        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "Problems Searching. Google Play Not available", Toast.LENGTH_LONG).show()
        }
    }

    // This method is linked to the method above
    // Called when the user completes the search
    // Data stores data in extras
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 1 Checks if the request code is correct
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE -> {
                // Result OK means operation succeeded
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 3 Takes the data intent and returns a populated Place object
                    val place = Autocomplete.getPlaceFromIntent(data)

                    // Converts the place object to a location object
                    val location = Location("")
                    location.latitude = place.latLng?.latitude ?: 0.0
                    location.longitude = place.latLng?.longitude ?: 0.0
                    updateMapToLocation(location)

                    // Loads the place photo and displays the place info window
                    displayPoiGetPhotoStep(place)
                }
            }
        }
    }

    /**
     * Feature: creates a bookmark from a place that does not show up on the map
     * Creates a bookmark from a map location
     * Called in setupMapListeners()
     */
    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let { id ->
                startBookmarkDetails(id)
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
        const val  EXTRA_BOOKMARK_ID = "com.raywenderlich.placebook.EXTRA_BOOKMARK_ID"
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
    }

    // This class is used to store data in the marker tag
    // I modified that because the place will not be null
    class PlaceInfo(val place: Place, val image: Bitmap? = null)
}