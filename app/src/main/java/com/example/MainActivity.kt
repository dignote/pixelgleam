package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.AppDatabase
import com.example.ui.PixelGleamApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PixelGleamViewModel

class MainActivity : ComponentActivity() {
  private var viewModel: PixelGleamViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Room Database
    val database = AppDatabase.getDatabase(applicationContext)
    
    // Obtain PixelGleamViewModel
    val factory = PixelGleamViewModel.Factory(application, database)
    val vm = ViewModelProvider(this, factory)[PixelGleamViewModel::class.java]
    this.viewModel = vm

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PixelGleamApp(viewModel = vm)
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel?.resumeAudio()
  }

  override fun onStop() {
    super.onStop()
    viewModel?.pauseAudio()
  }
}

