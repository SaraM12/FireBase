package com.saram.ruvxposed

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {

    val AUTH_REQUEST_CODE = 100
    private lateinit var auth: FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener
    private lateinit var  providers:List<AuthUI.IdpConfig>

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        signIn()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(listener)
    }

    override fun onStop() {
        FirebaseAuth.getInstance().signOut()
        auth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun signIn() {
        providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        auth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { p0 ->
            val user = p0.currentUser
            if(user != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setLogo(R.mipmap.ic_launcher_sun)
                    .setTheme(R.style.LoginTheme)
                    .build(),AUTH_REQUEST_CODE)

            }
        }
    }

}