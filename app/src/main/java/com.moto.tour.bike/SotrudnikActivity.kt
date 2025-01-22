package com.moto.tour.bike

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase

class SotrudnikActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var imageView2: ImageView
    private lateinit var imageView4: ImageView
    private val LOCATION_PERMISSION_REQUEST_CODE = 123 // Любое уникальное значение
    private lateinit var checkImageView: ImageView
    private val sharedPreferencesKey = "user_name"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userName: String
    private var isTrackingServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sotrudnik_activity)
        FirebaseApp.initializeApp(this)
        setStatusBarColor()

        sharedPreferences = getSharedPreferences("new_pref_name", Context.MODE_PRIVATE)

        editTextName = findViewById(R.id.editTextName)
        imageView2 = findViewById(R.id.imageView2)
        imageView4 = findViewById(R.id.imageView4)
        checkImageView = findViewById(R.id.checkImageView)

        val savedUserName = sharedPreferences.getString(sharedPreferencesKey, "")
        editTextName.setText(savedUserName)

        editTextName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                userName = editTextName.text.toString()
                true
            } else {
                false
            }
        }

        imageView2.setOnClickListener {
            val enteredText = editTextName.text.toString()
            if (enteredText.isNotEmpty()) {
                startLocationTracking()
            } else {
                Toast.makeText(this, "Введите имя пользователя", Toast.LENGTH_SHORT).show()
            }
        }

        imageView4.setOnClickListener {
            stopLocationTrackingService()

        }

        checkImageView.setOnClickListener {
            // Сохранение имени в SharedPreferences при нажатии на checkImageView
            val userName = editTextName.text.toString()
            saveUserName(userName)

            // Скрытие клавиатуры и снятие фокуса с EditText
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(editTextName.windowToken, 0)
            editTextName.clearFocus()

        }

        // Проверяем, запущена ли служба отслеживания при старте приложения
        isTrackingServiceRunning = isLocationTrackingServiceRunning()
    }

    // Проверка, запущена ли служба отслеживания местоположения
    private fun isLocationTrackingServiceRunning(): Boolean {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            serviceIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    private fun showLocationPermissionInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Важно")
            .setMessage("Для корректной работы приложения требуется разрешение на геолокацию и энергопотребление.\n\nЗайдите в Настройки - Разрешения приложений - Местоположение - \"Разрешить в любом режиме\".\n\nА также в настройках зайдите в Расход заряда батареи (он может называться \"контроль активности\") - поставьте \"Нет Ограничений\".\n\nПосле всего этого просто нажмите кнопку назад в настройках и вы вернетесь в приложение.")
            .setPositiveButton("Перейти к настройкам") { _, _ ->
                // Открываем настройки приложения
                openAppSettings()
            }
            .setNegativeButton("Отмена", null)
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun deleteUserDataFromFirebase(userName: String) {
        val database = FirebaseDatabase.getInstance()
        val locationRef = database.getReference("locations")

        // Используйте userName в качестве ключа и удаляйте данные
        locationRef.child(userName).removeValue()
            .addOnSuccessListener {
                // Успешное удаление
                Toast.makeText(this, "Данные пользователя удалены", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Обработка ошибки при удалении данных
                Toast.makeText(this, "Ошибка при удалении данных пользователя", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserName(userName: String) {
        // Сохранение имени в SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString(sharedPreferencesKey, userName)
        editor.apply()
        Toast.makeText(this, "Имя сохранено: $userName", Toast.LENGTH_SHORT).show()
    }

    private fun startLocationTracking() {
        // Получаем значение из EditText
        userName = editTextName.text.toString()
        // Проверяем, есть ли у нас разрешение на ACCESS_BACKGROUND_LOCATION
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение на ACCESS_BACKGROUND_LOCATION уже предоставлено
            // Проверяем, не запущен ли уже трекинг
            if (!isTrackingServiceRunning) {
                // Теперь можно начать отслеживание местоположения
                startLocationTrackingService(userName)
                isTrackingServiceRunning = true
            }
        } else {
            // Пользователь отклонил разрешение
            // Выведем информацию и инструкции о том, как включить разрешение через настройки устройства
            showLocationPermissionInfoDialog()
        }
    }

    private fun startLocationTrackingService(userName: String) {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        serviceIntent.putExtra("userName", userName)
        ContextCompat.startForegroundService(this, serviceIntent)
        imageView4.visibility = View.VISIBLE
    }

    private fun stopLocationTrackingService() {
        if (isTrackingServiceRunning) {
            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            stopService(serviceIntent)
            deleteUserDataFromFirebase(userName)
            imageView4.visibility = View.GONE
            isTrackingServiceRunning = false

            // Завершаем все активности приложения и системно завершаем приложение
            finishAffinity()
            System.exit(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // При закрытии активности останавливаем службу, если она работает
        stopLocationTrackingService()
        finishAffinity()
        System.exit(0)
    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.app)
        }
    }
}