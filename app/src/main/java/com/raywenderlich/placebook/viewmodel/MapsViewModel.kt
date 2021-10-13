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
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {

        val bookmark = repository.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)

        val newId = repository.addBookmark(bookmark)

        // Stores the image in the Android filesystem
        image?.let { image -> bookmark.setImage(image, getApplication()) }

        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    // Data for places that are bookmarked
    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0,0.0),
        var name: String = "",
        var phone: String = "",
        val categoryResourceId: Int? = null
    )
    {
        fun getImage(context: Context): Bitmap? {
            return id?.let { id ->
                ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(id))
            }
        }
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkView {
        return BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone,
            repository.getCategoryResourceId(bookmark.category)
        )
    }

    private fun mapBookmarksToBookmarkView() {
        bookmarks = Transformations.map(repository.allBookmarks) { repoBookmarks ->
            repoBookmarks.map { bookmark -> bookmarkToBookmarkView(bookmark) }
        }
    }

    fun getBookmarkViews() : LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    private fun getPlaceCategory(place: Place): String {
        // One place can be assigned different types
        var category = "Other"
        val types = place.types

        // Security is important
        types?.let { placeTypes ->
            if (placeTypes.size > 0) {
                val placeType = placeTypes[0]
                category = repository.placeTypeToCategory(placeType)
            }
        }
        return category
    }

}