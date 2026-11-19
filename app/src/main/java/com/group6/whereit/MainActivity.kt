package com.group6.whereit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.group6.whereit.data.SpotRepository
import com.group6.whereit.data.WhereItDatabase
import com.group6.whereit.ui.items.ItemsListScreen
import com.group6.whereit.ui.theme.WhereItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = WhereItDatabase.getInstance(this)
        val repository = SpotRepository(db.spotItemDao())

        // Real Firebase user once Essa's login is merged; test ID until then.
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test-user"

        setContent {
            WhereItTheme {
                ItemsListScreen(repository = repository, userId = userId)
            }
        }
    }
}