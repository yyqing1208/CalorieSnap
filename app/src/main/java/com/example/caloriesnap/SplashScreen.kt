package com.example.caloriesnap

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView

class SplashScreen : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var imgSplash : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        imgSplash = findViewById(R.id.img_splash)
        imgSplash.setImageResource(R.drawable.logo1)
        supportActionBar?.hide()

        handler = Handler()
        handler.postDelayed({
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}