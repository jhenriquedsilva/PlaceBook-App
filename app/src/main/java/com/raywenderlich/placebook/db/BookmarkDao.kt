package com.raywenderlich.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.raywenderlich.placebook.model.Bookmark

@Dao
interface BookmarkDao {

    // LiveData does its work in the background
    @Query("SELECT * FROM Bookmark ORDER BY name")
    fun loadAll(): LiveData<List<Bookmark>>

    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark

    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    @Delete
    fun deleteBookmark(bookmark: Bookmark)

}