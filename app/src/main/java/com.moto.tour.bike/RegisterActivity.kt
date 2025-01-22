package com.moto.tour.bike

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var EnterTextView2: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)
        FirebaseApp.initializeApp(this)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        EnterTextView2 = findViewById(R.id.enterTextView2)
        FirebaseApp.initializeApp(this)
        setStatusBarColor()
        auth = FirebaseAuth.getInstance()

        EnterTextView2.setOnClickListener {
            registerUser()
        }
    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.app)
        }
    }

    private fun registerUser() {
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()

        // Проверка, что email и пароль не пустые
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        // Использование Firebase Authentication для регистрации пользователя
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Регистрация успешна, пользователь уже вошел в систему
                    val user: FirebaseUser? = auth.currentUser
                    if (user != null) {
                        // Переход на другой экран (например, SotrudnikActivity)
                        startActivity(Intent(this, LoginActivity::class.java))
                        Toast.makeText(this, "Аккаунт успешно создан.\nТеперь вы можете войти.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    // Ошибка регистрации
                    Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                }
            }
    }
}