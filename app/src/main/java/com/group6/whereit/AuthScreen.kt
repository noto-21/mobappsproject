package com.group6.whereit

// JETPACK COMPOSE - UI COMPONENTS
import androidx.compose.runtime.Composable // allows functions to create Compose UI
import androidx.compose.material3.Text // displays text on screen
import androidx.compose.material3.TextButton // creates clickable text button
import androidx.compose.material3.Button // creates regular button
import androidx.compose.material3.OutlinedTextField // creates outlined input field

// STATE MANAGEMENT
import androidx.compose.runtime.remember // preserves values across recompositions
import androidx.compose.runtime.mutableStateOf // creates observable state that triggers UI updates

// LAYOUT - ARRANGING UI ELEMENTS
import androidx.compose.foundation.layout.Column // arranges elements vertically
import androidx.compose.foundation.layout.Spacer // creates empty space between elements
import androidx.compose.foundation.layout.Arrangement // controls positioning of elements in layouts
import androidx.compose.ui.Alignment // controls alignment of elements
import androidx.compose.ui.Modifier // allows changing size, spacing, appearance, etc.

// LAYOUT - SIZING AND SPACING
import androidx.compose.foundation.layout.fillMaxSize // makes element fill available width and height
import androidx.compose.foundation.layout.fillMaxWidth // makes element fill available width
import androidx.compose.foundation.layout.height // sets element height
import androidx.compose.foundation.layout.width // sets element width
import androidx.compose.foundation.layout.padding // adds spacing around elements
import androidx.compose.ui.unit.dp // unit for layout dimensions and spacing
import androidx.compose.ui.unit.sp // unit for font sizes

// TEXT STYLING AND KEYBOARD INPUT
import androidx.compose.ui.text.font.FontWeight // controls text boldness
import androidx.compose.ui.text.input.PasswordVisualTransformation // hides password characters
import androidx.compose.ui.text.input.KeyboardType // specifies expected input type
import androidx.compose.foundation.text.KeyboardOptions // configures the on-screen keyboard

// FIREBASE AUTHENTICATION
import com.google.firebase.auth.FirebaseAuth // handles account creation and login through Firebase

// INPUT VALIDATION
import android.util.Patterns // provides patterns for checking email format

// ANDROID CONTEXT AND NOTIFICATIONS
import androidx.compose.ui.platform.LocalContext // provides Android context inside Compose
import android.widget.Toast // displays temporary messages on screen


@Composable // tells kotlin this creates part of a jetpack compose interface
// take function which has no parameters or meaningful return type as a parameter to
// used to notify MainActivity when authentication succeeds
fun AuthScreen(onAuthSuccess: () -> Unit){

    // stores whatever the user types into the email field
    // remember keeps the same state object across compose recompositions
    // when this value changes, compose updates the UI that reads it
    val email = remember { mutableStateOf("") } // mutableStateOf makes the state observable

    // stores what's typed in the password field across recompositions
    val password = remember {mutableStateOf("")}

    // gets the Android context, needed to display Toast messages
    val context = LocalContext.current

    // tracks whether the user is on the login or signup screen
    // false = login, true = signup
    val isSignUp = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),

        // center everything vertically on the screen
        verticalArrangement = Arrangement.Center,

        // center everything horizontally on the screen
        horizontalAlignment = Alignment.CenterHorizontally

    ) { // comlumn contents start under here


        Text(
            text = "WhereIt?",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )


        // space between the app title and form
        Spacer(modifier = Modifier.height(24.dp))

        // email input field
        OutlinedTextField( // creates text box with an outline and the below properties
            value = email.value, // read the state from email.value

            // runs whenever the user changes the text
            // "it" is the newly entered text which we store in email.value
            // Compose automatically detects the state change because we used 'mutableStateOf'
            // then re-executes the affected composable functions that read the updated email.value
            onValueChange = { email.value = it },

            label = { Text("Email") },
            singleLine = true, // prevents multiline input
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )


        // space between email and password fields
        Spacer(modifier = Modifier.height(16.dp))

        // password input field
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            singleLine = true,

            // hide password characters as dots while typing
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password // tell android that the field expects a password
            ),

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )



        // space between password field and login button
        Spacer(modifier = Modifier.height(24.dp))

        // login button
        Button(
            onClick = {

                val enteredEmail = email.value.trim() // remove spaces before and after email

                // check if either field is empty for both login and signup
                if (enteredEmail.isBlank() || password.value.isBlank()) {
                    // prevent sending empty input fields to firebase
                    Toast.makeText(context, "Enter email and password", Toast.LENGTH_SHORT).show()
                }
                else if (!Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()) {
                    // check that the email has a valid format
                    Toast.makeText(context, "Invalid email address", Toast.LENGTH_SHORT).show()
                }
                else if (isSignUp.value && password.value.length < 6) {
                    // check that signup password has at least 6 characters
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                else if (isSignUp.value) {

                    // create a new Firebase account using the entered email and password
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(enteredEmail, password.value)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {
                                // account successfully created
                                Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess() // call the function MainActivity passed to here

                            }
                            else {
                                // account creation failed, get the error message from Firebase
                                // if firebase error message can't be retrieved use default fallback message
                                val error = task.exception?.localizedMessage ?: "Sign Up failed!"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }

                }
                else {
                    // send the entered email and password to Firebase
                    FirebaseAuth.getInstance() // get the firebase object

                        // starts login attempt and returns a Task object
                        .signInWithEmailAndPassword(enteredEmail, password.value) // send credentials

                        // when the login attempt finishes, run the lambda function, sending task as parameter
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {
                                // Firebase successfully authenticated the user
                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess() // call the function MainActivity passed to here

                            }
                            else {
                                // Firebase could not authenticate the user, get the error message
                                val error = task.exception?.localizedMessage ?: "Login failed!"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                }

            },
            modifier = Modifier
                .width(220.dp)
                .padding(horizontal = 24.dp)
                .height(52.dp)
        ) {
            Text(

                text = if (isSignUp.value) "Sign Up" else "Log In",
                fontSize = 20.sp
                )
        }


        // space below the login/signup button
        Spacer(modifier = Modifier.height(16.dp))

        // button to switch between login and signup
        TextButton(
            onClick = {

                // toggle the boolean value when clicked
                isSignUp.value = !isSignUp.value
            }
        ) {
            Text(

                // show appropriate message depending on state
                text = if (isSignUp.value)
                    "Already have an account? Log In" // while signing up give option to login
                else
                    "Don't have an account? Sign Up" // while logging in give option to signup
            )

        }

    }
}