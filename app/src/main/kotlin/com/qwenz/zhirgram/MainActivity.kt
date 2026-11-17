package com.qwenz.zhirgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qwenz.zhirgram.ui.ZhirRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ZhirApp
        app.td.start()
        setContent { ZhirRoot(app.td, app.settings) }
    }
}
