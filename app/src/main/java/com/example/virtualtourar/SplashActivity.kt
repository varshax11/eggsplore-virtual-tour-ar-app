package com.example.virtualtourar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import com.example.virtualtourar.HomeActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Use the animated egg vector drawable
        val animatedEgg = findViewById<ImageView>(R.id.eggImageView)
        animatedEgg.setImageResource(R.drawable.animated_egg)

        // Create a more natural floating animation
        val floatAnimator = ObjectAnimator.ofFloat(animatedEgg, "translationY", 0f, -30f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Gentle rotation animation
        val rotateAnimator = ObjectAnimator.ofFloat(animatedEgg, "rotation", -5f, 5f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Subtle scale animation for bounce effect
        val scaleXAnimator = ObjectAnimator.ofFloat(animatedEgg, "scaleX", 1f, 1.05f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = BounceInterpolator()
        }

        val scaleYAnimator = ObjectAnimator.ofFloat(animatedEgg, "scaleY", 1f, 1.05f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = BounceInterpolator()
        }

        // Play all animations together
        AnimatorSet().apply {
            playTogether(floatAnimator, rotateAnimator, scaleXAnimator, scaleYAnimator)
            start()
        }

        // Set app name
        val appNameText = findViewById<TextView>(R.id.appNameText)
        appNameText.text = "Eggsplore IITK"

        // Add loading messages
        val loadingText = findViewById<TextView>(R.id.loadingText)
        val loadingMessages = arrayOf(
            "Loading AR Experience...",
            "Initializing Camera...",
            "Setting up Location Services...",
            "Preparing Easter Eggs..."
        )

        var currentMessageIndex = 0
        val handler = Handler(Looper.getMainLooper())

        val updateMessage = object : Runnable {
            override fun run() {
                if (currentMessageIndex < loadingMessages.size) {
                    loadingText.text = loadingMessages[currentMessageIndex]
                    currentMessageIndex++
                    handler.postDelayed(this, 1000)
                }
            }
        }

        handler.post(updateMessage)

        // Navigate to HomeActivity after delay
        handler.postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, 5000) // 5 seconds total splash screen duration
    }
}
