package com.gaurav.favdish.view.activities

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.gaurav.favdish.R
import com.gaurav.favdish.databinding.ActivityAddUpdateDishBinding
import com.gaurav.favdish.databinding.DialogImagePickerBinding
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class AddUpdateDishActivity : AppCompatActivity() {

    private lateinit var activityBinding: ActivityAddUpdateDishBinding
    private var mImagePath = ""
    private val TAG = "AddUpdateDishActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBinding = ActivityAddUpdateDishBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)

        activityBinding.toolbarAddDishActivity.title = "Add Dish"
        activityBinding.btnAddDish.setOnClickListener {
            Toast.makeText(this, "You have clicked add image", Toast.LENGTH_SHORT).show()
        }
        activityBinding.ivAddDishImage.setOnClickListener {
            customImageDialog()
        }
    }

    private fun customImageDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogIvCamera.setOnClickListener {

            Dexter.withContext(this).withPermissions(
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()) {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            startActivityForResult(intent, CAMERA)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    token?.continuePermissionRequest()
                    showDialogRationalForPermissions()
                }
            }).onSameThread().check()

            dialog.dismiss()
        }
        dialogBinding.dialogIvGallery.setOnClickListener {
            Dexter.withContext(this).withPermissions(
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, 2)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    token?.continuePermissionRequest()
                    showDialogRationalForPermissions()
                }
            }).onSameThread().check()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDialogRationalForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("You need to give all permissions to access full feature of the app")
            .setPositiveButton("Go to Settings") { _, _ ->
                Toast.makeText(this@AddUpdateDishActivity, "Go to settings", Toast.LENGTH_SHORT)
                    .show()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA && resultCode == RESULT_OK) {
            val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
            mImagePath = saveImageToInternalStorage(thumbnail)
            Glide.with(this@AddUpdateDishActivity).load(thumbnail).centerCrop()
                .into(activityBinding.ivDishImage)
            Log.i(TAG, "onActivityResult: mImagePath : $mImagePath")

            activityBinding.ivAddDishImage.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.ic_edit
                )
            )
        } else if (resultCode == RESULT_OK && requestCode == GALLERY) {
            data?.let {
                val imageURI = data.data
                Glide.with(activityBinding.root).load(imageURI).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                            Log.e(TAG, "onLoadFailed: Error in loading image")
                            return false
                        }

                        override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                            p0?.let {
                                val bitmap: Bitmap = p0.toBitmap()
                                mImagePath = saveImageToInternalStorage(bitmap)
                            }
                            return false
                        }
                    }).into(activityBinding.ivDishImage)
                activityBinding.ivAddDishImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        this, R.drawable.ic_edit
                    )
                )
            }
        } else if (resultCode == RESULT_CANCELED) {
            Snackbar.make(activityBinding.root, "Action Cancelled", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIR, MODE_PRIVATE)
        file = File(file, UUID.randomUUID().toString() + ".jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            Log.e("AddUpdateActivity", "saveImageToInternalStorage: " + e.printStackTrace())
        }
        return file.absolutePath
    }

    companion object {
        const val CAMERA = 1
        const val GALLERY = 2
        private const val IMAGE_DIR = "FavImagesFolder"
    }
}