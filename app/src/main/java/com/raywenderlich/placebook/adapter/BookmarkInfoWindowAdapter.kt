package com.raywenderlich.placebook.adapter

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.raywenderlich.placebook.databinding.ContentBookmarkInfoBinding

// Once this object is assigned, the map will call getInfoWindow() whenever it needs to display
// an info window for a particular marker
class BookmarkInfoWindowAdapter(context: Activity) : GoogleMap.InfoWindowAdapter {
    private val binding = ContentBookmarkInfoBinding.inflate(context.layoutInflater)

    override fun getInfoWindow(marker: Marker): View? {
    // This function is required, but can return null if
    // not replacing the entire info window
        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        binding.title.text = marker.title ?: "Information not available"
        binding.phone.text = marker.snippet ?: "Information not available"
        // A buc occurs when there is no image
        binding.photo.setImageBitmap(marker.tag as Bitmap)
        return binding.root
    }
}