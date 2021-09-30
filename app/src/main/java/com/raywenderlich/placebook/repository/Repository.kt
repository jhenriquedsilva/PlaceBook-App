package com.raywenderlich.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook.db.BookmarkDao
import com.raywenderlich.placebook.db.PlaceBookDatabase
import com.raywenderlich.placebook.model.Bookmark

class Repository(context: Context) {

    private val db = PlaceBookDatabase.getInstance(context)
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()

    // The return may be null as the bookmark could not be saved
    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    // Gets all the bookmark from the repository
    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return bookmarkDao.loadAll()
        }

}