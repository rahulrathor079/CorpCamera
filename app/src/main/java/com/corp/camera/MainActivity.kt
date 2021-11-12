package com.corp.camera

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.corp.camera.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var tempPath: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.camera.setOnClickListener {
            checkCameraPerMission()
        }
        binding.gallery.setOnClickListener {
            checkGalleryPerMission()
        }
    }

    private fun checkCameraPerMission() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissions.launch(arrayOf(Manifest.permission.CAMERA))
        } else {
            openCameraIntent()
        }
    }

    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                openCameraIntent()
            } else {
                Log.e("mtag", ": ")
            }
        }

    private fun openCameraIntent() {
        tempPath?.delete()
        tempPath = null
        tempPath = File(cacheDir, "image.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", tempPath!!)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

        launchCameraIntent.launch(cameraIntent)
    }

    private var launchCameraIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                binding.imgPreview.setImageURI(null)
                val uri = FileProvider.getUriForFile(this, "${packageName}.provider", tempPath!!)
                binding.imgPreview.setImageURI(uri)
                val editIntent = Intent(applicationContext, EditImageActivity::class.java)
                editIntent.putExtra("image", uri.toString())
                startActivity(editIntent)
            }
        }

    private fun checkGalleryPerMission() {

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launchStoragePermission.launch(arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE))
        } else {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        launchGalleryIntent.launch(intent) // GIVE AN INTEGER VALUE FOR IMAGE_PICK_CODE LIKE 1000
    }

    private var launchStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[READ_EXTERNAL_STORAGE] == true && permissions[WRITE_EXTERNAL_STORAGE] == true) {
                pickImageFromGallery()
            } else {
                Log.d("mtag", "Permission not granted")
            }
        }

    private var launchGalleryIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                Log.e("mtag", ": " + result.data)
                binding.imgPreview.setImageURI(result.data?.data)
                val editIntent = Intent(applicationContext, EditImageActivity::class.java)
                editIntent.putExtra("image", result.data?.data.toString())
                startActivity(editIntent)
            }
        }
}

