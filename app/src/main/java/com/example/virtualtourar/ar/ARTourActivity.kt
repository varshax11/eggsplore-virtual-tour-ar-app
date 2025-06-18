package com.example.virtualtourar.ar

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import android.view.View
import com.example.virtualtourar.databinding.ActivityArtourBinding
import android.widget.TextView
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class ARTourActivity : AppCompatActivity() {

    data class TourPoint(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val name: String
    )

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val MIN_DISTANCE_CHANGE = 10.0 // Only update if moved more than 10 meters
        private const val MIN_TIME_BETWEEN_UPDATES = 5000L // 5 seconds between updates
        private const val BUFFER_ZONE = 20.0 // Points within 20 meters are considered equally close
    }

    private lateinit var binding: ActivityArtourBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationDetectedText: TextView
    private lateinit var latLongText: TextView
    private lateinit var nearestLocationText: TextView
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    private val tourPoints = listOf(
        TourPoint("computer_center", 26.5136764, 80.2346992, 125.0, "Computer Centre"),
        TourPoint("main_auditorium", 26.5129113, 80.2358415, 125.0, "Main Auditorium"),
        TourPoint("sculpture_fountain", 26.5119375, 80.2330625, 125.0, "Sculpture Fountain"),
        TourPoint("faculty_bridge", 26.5128303, 80.2331605, 125.0, "Faculty Bridge"),
        TourPoint("kargil_chauraha", 26.5104932, 80.2353768, 125.0, "Kargil Chauraha")
    )

    private var lastUpdateTime: Long = 0
    private var lastKnownLocation: Location? = null
    private var lastDisplayedPoint: TourPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtourBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationDetectedText = binding.locationDetectedText
        latLongText = binding.latLongText
        nearestLocationText = binding.nearestLocationText
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permissions if not granted
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }

        // Set up location request for FusedLocationProviderClient
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    updateDistance()
                    updateLatLongDisplay()
                }
            }
        }

        // Initialize camera
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        surfaceView = binding.surfaceView
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                openCamera()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                closeCamera()
            }
        })
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCameraPreviewSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                    }
                }, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surface = surfaceHolder.surface
            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@ARTourActivity, "Failed to configure camera session", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (::captureSession.isInitialized) {
            captureSession.close()
        }
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
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

    private fun updateDistance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("LocationDebug", "Location permission not granted")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < MIN_TIME_BETWEEN_UPDATES) {
            return
        }

        // Validate coordinates
        if (!isValidCoordinate(userLatitude, userLongitude)) {
            android.util.Log.e("LocationDebug", "Invalid coordinates received: ($userLatitude, $userLongitude)")
            return
        }

        try {
            android.util.Log.d("LocationDebug", """
                Location Update:
                - Current coordinates: ($userLatitude, $userLongitude)
                - Time since last update: ${currentTime - lastUpdateTime}ms
            """.trimIndent())

            val closestPoint = getClosestTourPoint()
            val distance = calculateDistance(closestPoint)

            // Only update if the closest point has changed significantly
            if (lastDisplayedPoint != closestPoint) {
                val lastDistance = lastDisplayedPoint?.let { calculateDistance(it) } ?: Double.MAX_VALUE
                if (Math.abs(distance - lastDistance) > MIN_DISTANCE_CHANGE) {
                    if (distance <= 100) {
                        showLocationDetected(closestPoint)
                    } else {
                        locationDetectedText.visibility = View.GONE
                    }
                    updateNearestLocationDisplay(closestPoint, distance)
                    lastDisplayedPoint = closestPoint
                    lastUpdateTime = currentTime
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationDebug", "SecurityException while updating location: ${e.message}")
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        // Check if coordinates are within reasonable bounds for the tour area
        val isValid = latitude in 26.50..26.52 && longitude in 80.23..80.24
        if (!isValid) {
            android.util.Log.e("LocationDebug", """
                Invalid coordinates detected:
                - Latitude: $latitude (should be between 26.50 and 26.52)
                - Longitude: $longitude (should be between 80.23 and 80.24)
            """.trimIndent())
        }
        return isValid
    }

    private fun getClosestTourPoint(): TourPoint {
        android.util.Log.d("LocationDebug", """
            ===== Distance Calculation Debug =====
            Current location: ($userLatitude, $userLongitude)
        """.trimIndent())
        
        // Calculate and log distances to all points
        val distances = tourPoints.map { point ->
            val distance = calculateDistance(point)
            android.util.Log.d("LocationDebug", """
                Distance to ${point.name}:
                - Coordinates: (${point.latitude}, ${point.longitude})
                - Raw distance: $distance meters
                - Rounded distance: ${Math.round(distance)} meters
            """.trimIndent())
            point to distance
        }
        
        // Find points within buffer zone of the closest point
        val sortedDistances = distances.sortedBy { it.second }
        val closestDistance = sortedDistances.first().second
        val pointsInBufferZone = sortedDistances.filter { 
            Math.abs(it.second - closestDistance) <= BUFFER_ZONE 
        }
        
        // If we have multiple points in buffer zone, keep the current one if possible
        val selectedPoint = if (pointsInBufferZone.size > 1 && lastDisplayedPoint != null) {
            pointsInBufferZone.find { it.first == lastDisplayedPoint }?.first 
                ?: pointsInBufferZone.first().first
        } else {
            pointsInBufferZone.first().first
        }
        
        android.util.Log.d("LocationDebug", """
            ===== Closest Point Selection =====
            All distances (sorted):
            ${sortedDistances.joinToString("\n") { "- ${it.first.name}: ${Math.round(it.second)} meters" }}
            
            Points within buffer zone (${BUFFER_ZONE} meters):
            ${pointsInBufferZone.joinToString("\n") { "- ${it.first.name}: ${Math.round(it.second)} meters" }}
            
            Selected point:
            - Name: ${selectedPoint.name}
            - Distance: ${Math.round(closestDistance)} meters
            - Coordinates: (${selectedPoint.latitude}, ${selectedPoint.longitude})
            =================================
        """.trimIndent())
        
        return selectedPoint
    }

    private fun calculateDistance(target: TourPoint): Double {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("LocationDebug", "Location permission not granted")
            return Double.MAX_VALUE
        }

        try {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLatitude,
                userLongitude,
                target.latitude,
                target.longitude,
                results
            )
            return results[0].toDouble()
        } catch (e: SecurityException) {
            android.util.Log.e("LocationDebug", "SecurityException while calculating distance: ${e.message}")
            return Double.MAX_VALUE
        }
    }

    private fun showLocationDetected(tourPoint: TourPoint) {
        locationDetectedText.visibility = View.VISIBLE
        locationDetectedText.text = "🎯 Location Detected! You are within 100 meters of ${tourPoint.name}"
        locationDetectedText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        Toast.makeText(this, "You are within 100 meters of ${tourPoint.name}!", Toast.LENGTH_SHORT).show()
    }

    private fun updateLatLongDisplay() {
        latLongText.text = "Latitude: $userLatitude, Longitude: $userLongitude"
    }

    private fun updateNearestLocationDisplay(tourPoint: TourPoint, distance: Double) {
        val roundedDistance = Math.round(distance)
        android.util.Log.d("LocationDebug", """
            Updating display:
            - Location: ${tourPoint.name}
            - Distance: $roundedDistance meters
            - Coordinates: (${tourPoint.latitude}, ${tourPoint.longitude})
        """.trimIndent())
        nearestLocationText.text = "Nearest Location: ${tourPoint.name}, Distance: $roundedDistance meters"
        
        // Update text color based on distance
        val textColor = if (distance <= 100) {
            android.R.color.holo_green_light
        } else {
            android.R.color.black
        }
        nearestLocationText.setTextColor(ContextCompat.getColor(this, textColor))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Snackbar.make(binding.root, "Location permission is required for this feature", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant") { requestPermissions() }
                    .show()
            } else {
                // Permissions granted, start location updates
                startLocationUpdates()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                android.util.Log.e("LocationDebug", "SecurityException while starting location updates: ${e.message}")
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        closeCamera()
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            startLocationUpdates()
        }
    }
}