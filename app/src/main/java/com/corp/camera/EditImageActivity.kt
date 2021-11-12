package com.corp.camera

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.corp.camera.databinding.ActivityEditImageBinding
import com.theartofdev.edmodo.cropper.CropImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream


class EditImageActivity : AppCompatActivity() {
    private var currentRotation = 0.0f
    private var fromRotation = 0.0f
    private var toRotation = 0.0f
    private var isRotate = false
    private var rotateBitmap: Bitmap? = null
    private var imageName: String? = ""
    private var uri: Uri? = null
    private var croppedBitmap: Bitmap? = null
    private var cropThenRotateBitmap: Bitmap? = null
    private var rotateThenCropBitmap: Bitmap? = null
    private var photoBmp: Bitmap? = null
    private lateinit var binding: ActivityEditImageBinding
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageName = intent.getStringExtra("image")
        binding.rotateImage.setImageURI(Uri.parse(imageName))
        photoBmp = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(imageName))
        uri = Uri.parse(imageName)
        binding.rotate.setOnClickListener {
            rotate(photoBmp)
        }
        binding.crop.setOnClickListener {
            cropBitmap()
        }
        binding.save.setOnClickListener {
            saveImage()
        }
    }

    private fun rotate(bitmap: Bitmap?) {
        isRotate = true
        if (currentRotation == 360f)
            currentRotation %= 360
        val matrix = android.graphics.Matrix()
        fromRotation = currentRotation
        toRotation += 90f
        currentRotation = toRotation

        val rotateAnimation = RotateAnimation(
            fromRotation, toRotation,
            (binding.rotateImage.width / 2).toFloat(), (binding.rotateImage.height / 2).toFloat()

        )

        rotateAnimation.duration = 500
        rotateAnimation.fillAfter = true
        matrix.setRotate(toRotation)

        if (croppedBitmap != null && bitmap != null) {

            cropThenRotateBitmap =
                croppedBitmap?.width?.let {
                    croppedBitmap?.height?.let { it1 ->
                        Bitmap.createBitmap(
                            bitmap, 0, 0, it,
                            it1, matrix, true
                        )
                    }
                };
        }
        rotateBitmap =
            bitmap?.let {
                Bitmap.createBitmap(
                    it,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
            };
        binding.rotateImage.startAnimation(rotateAnimation)

    }

    private fun cropBitmap() {
        if (rotateBitmap != null) {
            val byteOutputStream = ByteArrayOutputStream()
            rotateBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, byteOutputStream)
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                rotateBitmap,
                imageName,
                null
            )
            uri = Uri.parse(path)
        }
        CropImage.activity(uri).start(this)

    }

    private fun undo() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                binding.rotateImage.setImageURI(resultUri)
                //                Matrix matrix = new Matrix();
                croppedBitmap = binding.rotateImage.drawable.toBitmap()
                if (isRotate) {
                    rotateThenCropBitmap = croppedBitmap
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImage() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName.toString() + ".jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        var imageOutStream: OutputStream? = null

        try {
            if (uri !== null) {
                imageOutStream = resolver.openOutputStream(uri)
                if (cropThenRotateBitmap != null) {
                    if (cropThenRotateBitmap?.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            imageOutStream
                        ) == false
                    ) {
                        throw IOException("Failed to compress bitmap")
                    }
                } else if (rotateThenCropBitmap != null) {
                    if (rotateThenCropBitmap?.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            imageOutStream
                        ) == false
                    ) {
                        throw IOException("Failed to compress bitmap")
                    }
                } else if (croppedBitmap != null) {
                    if (!croppedBitmap!!.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            imageOutStream
                        )
                    ) {
                        throw IOException("Failed to compress bitmap")
                    }
                } else if (rotateBitmap != null) {
                    if (!rotateBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                        throw IOException("Failed to compress bitmap")
                    }
                } else {
                    if (photoBmp?.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            imageOutStream
                        ) == false
                    ) {
                        throw IOException("Failed to compress bitmap")
                    }
                }
                Toast.makeText(this, "Imave Saved", Toast.LENGTH_SHORT).show()
            }
            } finally {
                if (imageOutStream != null) {
                    imageOutStream.close()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    finish()
                    startActivity(intent)
                }
            }
        }

}