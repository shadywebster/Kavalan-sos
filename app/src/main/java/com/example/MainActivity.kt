package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.KavalanRepository
import com.example.ui.KavalanApp
import com.example.ui.KavalanViewModel
import com.example.ui.KavalanViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database, Repository, and ViewModel using local constructor injection
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = KavalanRepository(
            database.emergencyContactDao(),
            database.safetyTipDao(),
            database.audioRecordDao(),
            database.policeStationDao()
        )
        val viewModel: KavalanViewModel by viewModels {
            KavalanViewModelFactory(repository, application)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KavalanApp(viewModel)
                }
            }
        }
    }
}

