package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.Repository
import com.raywenderlich.placebook.util.ImageUtils

class MapsViewModel(application: Application): AndroidViewModel(application) {

    private val TAG = "MapsViewModel"
    private val repository: Repository = Repository(getApplication())
    private var bookmarks: LiveData<List<BookmarkMarkerView>>? = null

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {

        val bookmark = repository.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        val newId = repository.addBookmark(bookmark)
        image?.let { image -> bookmark.setImage(image, getApplication()) }

        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    // Data for places that are bookmarked
    data class BookmarkMarkerView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0,0.0),
        var name: String = "",
        var phone: String = ""
    ) {
        fun getImage(context: Context): Bitmap? {
            return id?.let { id ->
                ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(id))
            }
        }
    }

    private fun bookmarkToMarkerView(bookmark: Bookmark): BookmarkMarkerView {
        return BookmarkMarkerView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone
        )
    }

    private fun mapBookmarksToMarkerView() {
        bookmarks = Transformations.map(repository.allBookmarks) { repoBookmarks ->
            repoBookmarks.map { bookmark -> bookmarkToMarkerView(bookmark) }
        }
    }

    fun getBookmarkMarkerViews() : LiveData<List<BookmarkMarkerView>>? {
        if (bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }

}