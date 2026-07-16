package com.example.nfcautomation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val py = Python.getInstance()
        val module = py.getModule("bridge")
        val result = module.callAttr("execute", TEST_TAG_ID).toString()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = result)
            }
        }
    }

    companion object {
        /**
         * Temporary UID for testing. 
         * Replace this with the real UID from the NFC API in the next phase.
         */
        const val TEST_TAG_ID = "04AABBCCDD11"
    }
}