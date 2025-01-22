package com.moto.tour.bike

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.google.firebase.FirebaseApp
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var EnterTextView: TextView
    private lateinit var EnterTextView2: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_acitivity)
        FirebaseApp.initializeApp(this)
        setStatusBarColor()
        // Инициализация Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Получение ссылок на элементы макета
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        EnterTextView = findViewById(R.id.EnterTextView)
        EnterTextView2 = findViewById(R.id.enterTextView2)

        EnterTextView.setOnClickListener {
            loginUser()
        }

        EnterTextView2.setOnClickListener {
            // Переход на активность регистрации (например, RegisterActivity)
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Проверяем состояние входа
        if (isLoggedIn()) {
            // Если пользователь залогинен, открываем соответствующую активность
            val superUserEmail = "xchaos228@gmail.com"
            val userEmail = auth.currentUser?.email
            if (userEmail == superUserEmail) {
                startActivity(Intent(this, GPS_Activity::class.java))
            } else {
                startActivity(Intent(this, SotrudnikActivity::class.java))
            }
            finish()
        }

    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.app)
        }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()

        // Проверка, что email и пароль не пустые
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        // Использование Firebase Authentication для входа пользователя
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Вход успешен
                    val user: FirebaseUser? = auth.currentUser
                    if (task.isSuccessful) {
                        // Вход успешен
                        if (user != null) {
                            // Сохраняем информацию о входе пользователя
                            saveLoginStatus(true)

                            // Проверка для суперпользователя
                            val superUserEmail = "xchaos228@gmail.com"
                            if (email == superUserEmail) {
                                // Если входит суперпользователь, переход на GPSActivity
                                startActivity(Intent(this, GPS_Activity::class.java))
                            } else {
                                // Вход для обычного пользователя
                                startActivity(Intent(this, SotrudnikActivity::class.java))
                            }
                            finish()
                        }
                    }
                } else {
                    // Пользователь не существует или неверный пароль
                    Toast.makeText(this, "Неверный Email или пароль", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("LoginStatus3", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun saveLoginStatus(isLoggedIn: Boolean) {
        val sharedPreferences = getSharedPreferences("LoginStatus3", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.apply()
    }

}