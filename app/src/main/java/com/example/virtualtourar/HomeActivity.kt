package com.example.virtualtourar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.example.virtualtourar.ar.ARTourActivity
import android.widget.Button
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Find views
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val instructionsText = findViewById<TextView>(R.id.instructionsText)
        val startArTourButton = findViewById<MaterialButton>(R.id.startArTourButton)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        val scaleUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left).apply {
            duration = 500
        }

        // Apply animations
        welcomeText.startAnimation(fadeIn)
        instructionsText.startAnimation(slideUp)
        startArTourButton.startAnimation(scaleUp)

        // Check for required permissions
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }

        // Set up start tour button
        startArTourButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                startActivity(Intent(this, ARTourActivity::class.java))
            } else {
                Toast.makeText(this, "Please grant all required permissions", Toast.LENGTH_LONG).show()
                requestPermissions()
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions are required for AR experience", Toast.LENGTH_LONG).show()
            }
        }
    }
} 