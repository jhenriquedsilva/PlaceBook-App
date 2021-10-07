package com.raywenderlich.placebook.ui

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog

class PhotoOptionDialogFragment: DialogFragment() {

    interface PhotoOptionDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }

    private lateinit var listener: PhotoOptionDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        listener = activity as PhotoOptionDialogListener

        var captureSelectIdx = -1
        var pickSelectIdx = -1

        val options = ArrayList<String>()

        val context = activity as Context

        if (canCapture(context)) {
            options.add("Camera")
            captureSelectIdx = 0
        }

        if (canPick(context)) {
            options.add("Gallery")
            pickSelectIdx = if (captureSelectIdx == 0) 1 else 0
        }

        return AlertDialog.Builder(context)
            .setTitle("Photo Option")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == captureSelectIdx) {
                    listener.onCaptureClick()
                } else if (which == pickSelectIdx) {
                    listener.onPickClick()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {

        fun canPick(context: Context) : Boolean {
            val pickIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(context.packageManager) != null)
        }

        fun canCapture(context: Context) : Boolean {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(context.packageManager) != null)
        }

        fun newInstance(context: Context) =
            if (canPick(context) || canCapture(context)) {
                PhotoOptionDialogFragment()
            } else {
                null
            }

    }
}