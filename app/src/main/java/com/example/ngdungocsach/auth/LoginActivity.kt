package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ngdungocsach.R
import com.example.ngdungocsach.admin.AdminActivity
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.user.SubscriptionActivity
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseHelper = FirebaseHelper()
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        setupGoogleSignIn()
        setupFacebookLogin()

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val layoutLogin = findViewById<LinearLayout>(R.id.layoutLogin)
        val layoutUserInfo = findViewById<ScrollView>(R.id.layoutUserInfo)

        // Login views
        val txtUser = findViewById<EditText>(R.id.txtUser)
        val txtPass = findViewById<EditText>(R.id.txtPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)
        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        val btnFacebookLogin = findViewById<MaterialButton>(R.id.btnFacebookLogin)
        val btnPhoneLogin = findViewById<MaterialButton>(R.id.btnPhoneLogin)

        // User info views
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val btnManageBooks = findViewById<MaterialButton>(R.id.btnManageBooks)
        val btnManageUsers = findViewById<MaterialButton>(R.id.btnManageUsers)
        val btnStatistics = findViewById<MaterialButton>(R.id.btnStatistics)
        val btnSubscription = findViewById<MaterialButton>(R.id.btnSubscription)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val btnSettings = findViewById<MaterialButton>(R.id.btnSettings)
        val btnEditDisplayName = findViewById<MaterialButton>(R.id.btnEditDisplayName)
        val btnPaymentSettings = findViewById<MaterialButton>(R.id.btnPaymentSettings)

        // Kiểm tra trạng thái đăng nhập
        val savedUsername = sharedPreferences.getString("username", null)
        val savedRole = sharedPreferences.getString("role", null)

        if (savedUsername != null && savedRole != null) {
            showUserInfo(layoutLogin, layoutUserInfo, savedUsername, savedRole, tvWelcome, tvUserRole, 
                btnManageBooks, btnManageUsers, btnStatistics, btnSubscription, btnPaymentSettings)
        } else {
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val emailOrUser = txtUser.text.toString().trim()
            val password = txtPass.text.toString().trim()

            if (emailOrUser.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ưu tiên tài khoản admin
            if (emailOrUser.lowercase() == "admin") {
                firebaseHelper.login("admin", password) { role, message ->
                    if (role != null) {
                        saveSessionAndFinish("admin", "admin", role)
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
                return@setOnClickListener
            }

            val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrUser).matches()
            if (isEmail) {
                firebaseHelper.loginWithAuth(emailOrUser, password) { role, message ->
                    if (role != null) {
                        val uid = firebaseHelper.getCurrentUser()?.uid ?: ""
                        firebaseHelper.getUserData(uid) { userData ->
                            saveSessionAndFinish(uid, userData?.username ?: emailOrUser, role)
                        }
                    } else {
                        firebaseHelper.login(emailOrUser, password) { legacyRole, _ ->
                            if (legacyRole != null) {
                                saveSessionAndFinish(emailOrUser, emailOrUser, legacyRole)
                            } else {
                                Toast.makeText(this, "Lỗi: $message", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } else {
                firebaseHelper.login(emailOrUser, password) { role, message ->
                    if (role != null) {
                        saveSessionAndFinish(emailOrUser, emailOrUser, role)
                    } else {
                        Toast.makeText(this, "Lỗi: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnManageBooks.setOnClickListener { startActivity(Intent(this, AdminActivity::class.java)) }
        btnManageUsers.setOnClickListener { startActivity(Intent(this, com.example.ngdungocsach.admin.ManageUsersActivity::class.java)) }
        btnStatistics.setOnClickListener { startActivity(Intent(this, com.example.ngdungocsach.admin.StatisticsActivity::class.java)) }
        btnPaymentSettings.setOnClickListener { startActivity(Intent(this, com.example.ngdungocsach.admin.PaymentSettingsActivity::class.java)) }
        btnSubscription.setOnClickListener { startActivity(Intent(this, SubscriptionActivity::class.java)) }
        btnSettings.setOnClickListener { showFontSizeDialog() }
        btnEditDisplayName.setOnClickListener { showEditDisplayNameDialog() }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            googleSignInClient.signOut()
            LoginManager.getInstance().logOut()

            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        txtRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }

        btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        btnFacebookLogin.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        btnPhoneLogin.setOnClickListener { startActivity(Intent(this, PhoneLoginActivity::class.java)) }
    }

    private fun saveSessionAndFinish(uid: String, username: String, role: String) {
        val editor = sharedPreferences.edit()
        editor.putString("uid", uid)
        editor.putString("username", username)
        editor.putString("role", role)
        editor.apply()

        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
                    val errorMsg = when (e.statusCode) {
                        10 -> "Lỗi 10: Sai mã SHA-1 trên Firebase Console!"
                        else -> "Lỗi Google (${e.statusCode}): Hãy kiểm tra lại cấu hình."
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
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
            override fun onCancel() { Toast.makeText(this@LoginActivity, "Hủy đăng nhập", Toast.LENGTH_SHORT).show() }
            override fun onError(error: FacebookException) {
                Toast.makeText(this@LoginActivity, "Lỗi Facebook: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (::callbackManager.isInitialized) callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun signInWithFirebase(credential: com.google.firebase.auth.AuthCredential) {
        firebaseHelper.signInWithCredential(credential) { success, message ->
            if (success) {
                val user = firebaseHelper.getCurrentUser()
                user?.let {
                    firebaseHelper.getUserData(it.uid) { userData ->
                        saveSessionAndFinish(it.uid, userData?.username ?: it.displayName ?: "User", userData?.role ?: "user")
                    }
                }
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUserInfo(layoutLogin: View, layoutUserInfo: View, username: String, role: String, 
                             tvWelcome: TextView, tvUserRole: TextView, 
                             btnManageBooks: MaterialButton, btnManageUsers: MaterialButton, 
                             btnStatistics: MaterialButton, btnSubscription: MaterialButton,
                             btnPaymentSettings: MaterialButton) {
        layoutLogin.visibility = View.GONE
        layoutUserInfo.visibility = View.VISIBLE
        tvWelcome.text = "Chào mừng bạn, $username!"
        tvUserRole.visibility = View.GONE
        
        // Đồng bộ màu trắng tinh cho tất cả icon để tránh "cái rõ cái mờ"
        val whiteTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        val buttons = listOf(btnManageBooks, btnManageUsers, btnStatistics, btnSubscription, btnPaymentSettings)
        buttons.forEach { 
            it.iconTint = whiteTint
            it.alpha = 1.0f 
        }

        if (role == "admin") {
            btnManageBooks.visibility = View.VISIBLE
            btnManageUsers.visibility = View.VISIBLE
            btnStatistics.visibility = View.VISIBLE
            btnPaymentSettings.visibility = View.VISIBLE
            btnSubscription.visibility = View.GONE
        } else {
            btnManageBooks.visibility = View.GONE
            btnManageUsers.visibility = View.GONE
            btnStatistics.visibility = View.GONE
            btnPaymentSettings.visibility = View.GONE
            btnSubscription.visibility = View.VISIBLE
        }
    }

    private fun showLoginForm(layoutLogin: View, layoutUserInfo: View) {
        layoutLogin.visibility = View.VISIBLE
        layoutUserInfo.visibility = View.GONE
    }

    private fun showFontSizeDialog() {
        val sizes = arrayOf("Nhỏ", "Trung bình", "Lớn")
        val currentSize = sharedPreferences.getInt("font_size", 1)
        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn cỡ chữ")
            .setSingleChoiceItems(sizes, currentSize) { dialog, which ->
                val editor = sharedPreferences.edit()
                editor.putInt("font_size", which)
                editor.apply()
                recreate()
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditDisplayNameDialog() {
        val input = EditText(this)
        input.setPadding(50, 20, 50, 20)
        val currentName = sharedPreferences.getString("username", "")
        input.setText(currentName)
        input.hint = "Nhập tên hiển thị mới"

        MaterialAlertDialogBuilder(this)
            .setTitle("Đổi tên hiển thị")
            .setView(input)
            .setPositiveButton("Lưu") { dialog, _ ->
                val newName = input.text.toString().trim()
                val uid = sharedPreferences.getString("uid", null)

                if (newName.isNotEmpty() && uid != null) {
                    firebaseHelper.updateUsername(uid, newName) { success ->
                        if (success) {
                            sharedPreferences.edit().putString("username", newName).apply()
                            findViewById<TextView>(R.id.tvWelcome).text = "Chào mừng bạn, $newName!"
                            Toast.makeText(this, "Đổi tên thành công", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Lỗi cập nhật tên", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ -> dialog.cancel() }
            .show()
    }
}
