package com.moto.tour.bike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var userName: String

    private val NOTIFICATION_CHANNEL_ID = "MyNotificationChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        createNotificationChannel()
        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Настройка параметров запроса геолокации
        locationRequest = LocationRequest.create().apply {
            interval = 1000 // Интервал запроса в миллисекундах (в данном случае 0.1 секунд)
            fastestInterval = 1000 // Наименьший интервал обновления в миллисекундах
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Настройка колбэка для обработки обновлений геолокации
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Обновление местоположения, например, отправка на сервер
                    updateLocationToServer(location.latitude, location.longitude, userName)
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userName = intent?.getStringExtra("userName") ?: "unknown"
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(userName))
        requestLocationUpdates()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager?.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }


    private fun createNotification(userName: String?): Notification {
        // Создайте Intent без назначения активности (null)
        val notificationIntent = Intent()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Service")
            .setContentText("Tracking user: $userName")
            .setSmallIcon(R.drawable.notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun requestLocationUpdates() {
        try {
            // Запрос разрешения на доступ к геоданным
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateLocationToServer(latitude: Double, longitude: Double, userName: String?) {
        try {
            val database = FirebaseDatabase.getInstance()
            val locationRef = database.getReference("locations")

            if (userName != null) {
                val locationData = LocationData(latitude, longitude)

                // Используйте userName в качестве ключа в базе данных
                locationRef.child(userName).setValue(locationData)

                // Отправить широту, долготу и имя пользователя через Broadcast
                val intent = Intent("LOCATION_UPDATE")
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
                intent.putExtra("userName", userName)
                sendBroadcast(intent)

                Log.d("FirebaseUpdate", "Location data for $userName updated successfully.")
            } else {
                Log.e("FirebaseUpdate", "userName is null. Cannot update location data.")
            }
        } catch (e: Exception) {
            Log.e("FirebaseUpdate", "Error updating location data: ${e.message}")
        }
    }
}
