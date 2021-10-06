package com.raywenderlich.placebook.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.databinding.ActivityBookmarkDetailsBinding
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel

class BookmarkDetailsActivity : AppCompatActivity(), PhotoOptionDialogFragment.PhotoOptionDialogListener {

    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    private fun getIntentData() {

        val bookmarkId = intent.getLongExtra(MapsActivity.EXTRA_BOOKMARK_ID, 0)

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this, { bookmarkView ->
                bookmarkView?.let { bookmarkView ->
                    bookmarkDetailsView = bookmarkView
                    databinding.bookmarkDetailsView = bookmarkView
                    populateImageView()
                }
            }
        )
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let { placeImage ->
                databinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
        databinding.imageViewPlace.setOnClickListener {
            replaceImage()
        }
    }

    // Problem over here
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = databinding.editTextName.text.toString()
            bookmarkView.notes = databinding.editTextNotes.text.toString()
            bookmarkView.address = databinding.editTextAddress.text.toString()
            bookmarkView.phone = databinding.editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        // Closes the activity
        finish()
    }

    override fun onCaptureClick() {
        Toast.makeText(this, "Camera Update", Toast.LENGTH_SHORT).show()
    }

    override fun onPickClick() {
        Toast.makeText(this, "Gallery Pick", Toast.LENGTH_SHORT).show()
    }
}