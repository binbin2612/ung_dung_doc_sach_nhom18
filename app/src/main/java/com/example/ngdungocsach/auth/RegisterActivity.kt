package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.User
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.ui.BaseActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider

class RegisterActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        firebaseHelper = FirebaseHelper()
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        setupGoogleSignIn()
        setupFacebookLogin()

        val edtEmail = findViewById<EditText>(R.id.edtNewEmail)
        val edtUser = findViewById<EditText>(R.id.edtNewUsername)
        val edtPass = findViewById<EditText>(R.id.edtNewPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnBackToLogin = findViewById<MaterialButton>(R.id.btnBackToLogin)
        val btnGoogleRegister = findViewById<MaterialButton>(R.id.btnGoogleRegister)
        val btnFacebookRegister = findViewById<MaterialButton>(R.id.btnFacebookRegister)
        val btnPhoneRegister = findViewById<MaterialButton>(R.id.btnPhoneRegister)

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val username = edtUser.text.toString().trim()
            val password = edtPass.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseHelper.registerWithAuth(email, password, username) { success, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    finish()
                }
            }
        }

        btnGoogleRegister.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        btnFacebookRegister.setOnClickListener {
            LoginManager.getInstance().logOut()
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        btnPhoneRegister.setOnClickListener {
            startActivity(Intent(this, PhoneLoginActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    signInWithFirebase(credential)
                } catch (e: ApiException) {
                    Log.w("RegisterActivity", "Google sign in failed", e)
                }
            }
        }
    }

    private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                signInWithFirebase(credential)
            }

            override fun onCancel() {}

            override fun onError(error: FacebookException) {
                Log.e("RegisterActivity", "Facebook login error", error)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun signInWithFirebase(credential: com.google.firebase.auth.AuthCredential) {
        firebaseHelper.signInWithCredential(credential) { success, message ->
            if (success) {
                val user = firebaseHelper.getCurrentUser()
                user?.let {
                    firebaseHelper.getUserData(it.uid) { userData ->
                        val editor = sharedPreferences.edit()
                        editor.putString("username", userData?.username ?: it.displayName)
                        editor.putString("role", userData?.role ?: "user")
                        editor.apply()

                        Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
