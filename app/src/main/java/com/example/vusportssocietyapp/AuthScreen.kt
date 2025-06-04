package com.example.vusportssocietyapp

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: (String) -> Unit
) {
    var selectedRole by remember { mutableStateOf("Participant") }
    val roles = listOf("Participant", "Coach", "Admin")

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Login" else "Sign Up",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Role") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { expanded = true }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                selectedRole = role
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    if (isLogin) {
                        // LOGIN
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    val db = FirebaseFirestore.getInstance()

                                    db.collection("users").document(uid!!)
                                        .get()
                                        .addOnSuccessListener { document ->
                                            val role = document.getString("role")
                                            if (role != null) {
                                                onLoginSuccess(role)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Role not found.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Failed to fetch role",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Login Failed: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        // SIGN UP
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                    val db = FirebaseFirestore.getInstance()

                                    val userData = mapOf(
                                        "name" to name,
                                        "email" to email,
                                        "role" to selectedRole
                                    )

                                    db.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Signup Successful! Please login.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            isLogin = true
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Failed to save user data.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Signup Failed: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(context, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text(text = if (isLogin) "Login" else "Sign Up")
        }

        TextButton(onClick = {
            isLogin = !isLogin
        }) {
            Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login")
        }
    }
}
