package com.dec.app

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This command instantly closes the screen so the app stays invisible
        finish()
    }
}
