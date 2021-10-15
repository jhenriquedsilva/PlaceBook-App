package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.Repository
import com.raywenderlich.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application): AndroidViewModel(application) {

    private val repository = Repository(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }

    // Updated to conform with the deleteBookmark() feature
    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = repository.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark) { repoBookmark ->
            repoBookmark?.let { repoBookmark ->
                bookmarkToBookmarkView(repoBookmark)
            }
        }
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
            bookmark.longitude,
            bookmark.latitude,
            bookmark.placeId
        )
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let { bookmark -> repository.updateBookmark(bookmark) }
        }
    }

    /**
     * Feature: deleting a bookmark
     * Deletes a bookmark from the database
     * Called by BookmarkDetailsActivity in deleteBookmark()
     */
    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let { id ->
                repository.getBookmark(id)
            }
            bookmark?.let { bookmark ->
                repository.deleteBookmark(bookmark)
            }
        }
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let { id ->
            repository.getBookmark(id)
        }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }

    fun getCategoryResourceId(category: String): Int? {
       return repository.getCategoryResourceId(category)
    }

    fun getCategories(): List<String> {
        return repository.categories
    }

    // Longitude, latitude, and placeId were added to implement the sharing bookmarks feature

    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = "",
        var category: String = "",
        var longitude: Double = 0.0,
        var latitude: Double = 0.0,
        var placeId: String? = null
    )
    {
        fun getImage(context: Context): Bitmap? {
            return id?.let { id -> ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(id)) }
        }

        fun setImage(context: Context, image: Bitmap) {
            id?.let { id ->
                ImageUtils.saveBitmapToFile(context, image, Bookmark.generateImageFilename(id))
            }
        }
    }
}