package com.group6.whereit

// ANDROID ACTIVITY - APP STARTUP
import android.os.Bundle // stores saved Activity state
import androidx.activity.ComponentActivity // base class for MainActivity
import androidx.activity.compose.setContent // displays Jetpack Compose UI inside the Activity

// JETPACK COMPOSE - UI COMPONENTS
import androidx.compose.material3.Text // displays text on the screen
import androidx.compose.material3.Button // creates a clickable button

// JETPACK COMPOSE - STATE MANAGEMENT
import androidx.compose.runtime.remember // preserves values across recompositions
import androidx.compose.runtime.mutableStateOf // creates observable state that triggers UI updates

// JETPACK COMPOSE - LAYOUT AND POSITIONING
import androidx.compose.foundation.layout.Column // arranges UI elements vertically
import androidx.compose.foundation.layout.Arrangement // controls vertical positioning within Column
import androidx.compose.foundation.layout.fillMaxSize // makes an element fill the available screen space
import androidx.compose.ui.Alignment // controls horizontal alignment within Column
import androidx.compose.ui.Modifier // allows changing UI element size, spacing, etc.

// FIREBASE AUTHENTICATION
import com.google.firebase.auth.FirebaseAuth // checks current user session and handles sign out


class MainActivity : ComponentActivity() {




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent{

            // check whether Firebase already has a signed-in user
            val isLoggedIn = remember {

                //.currentUser gets the currently signed-in user, or null if nobody is signed in.
                //!= null returns true if a signed-in user exists.
                // mutableStateOf(...) initializes our UI state using that result.
                // remember preserves this state during recompositions.

                // when reopening the app, users with an existing session will go straight
                // through without having to login again
                mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
            }


            if (isLoggedIn.value) {
                // temporary home screen until merge
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Welcome to WhereIt!")

                    Button(
                        onClick = {
                            // sign out the currently authenticated Firebase user
                            FirebaseAuth.getInstance().signOut()

                            // update the UI to display the login screen again
                            isLoggedIn.value = false
                        }
                    ) {
                        Text("Sign Out")
                    }
                }
            }
            else {
                // display authentication screen
                AuthScreen(
                    onAuthSuccess = {
                        // AuthScreen calls this function after successful authentication
                        isLoggedIn.value = true
                    }
                )
            }
        }








    }
}

