package com.moto.tour.bike

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SuggestItem
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.runtime.image.ImageProvider

class GPS_Activity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var suggestSession: SuggestSession
    private lateinit var searchEditText: AutoCompleteTextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var employeesRecyclerView: RecyclerView
    private lateinit var employeesAdapter: EmployeesAdapter
    private val currentRoutesMap = mutableMapOf<String, PolylineMapObject>()
    private lateinit var Enter: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("e9b3882f-b34a-49f0-ab16-9bca61f63e5d")
        MapKitFactory.initialize(this)
        setContentView(R.layout.gps_activity)
        FirebaseApp.initializeApp(this)
        setStatusBarColor()
        mapView = findViewById(R.id.mapview)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        Enter = findViewById(R.id.enter)
        searchEditText = findViewById(R.id.searchEditText)

        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
        searchEditText.setAdapter(adapter)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не нужно реагировать до изменения текста
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Вызывается при изменении текста
                if (s != null && s.length > 2) {
                    performSearch(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Вызывается после изменения текста
            }
        })
        Enter.setOnClickListener {
            // Переход на активность регистрации (например, RegisterActivity)
            startActivity(Intent(this, RouteActivity::class.java))
        }

        mapView.map.move(
            CameraPosition(Point(56.8519, 60.6122), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        readDataFromDatabase()

        employeesRecyclerView = findViewById(R.id.employeesRecyclerView)

        // Инициализация адаптера
        employeesAdapter = EmployeesAdapter(object : EmployeesAdapter.EmployeeClickListener {
            override fun onEmployeeClick(employee: Employee) {
                handleEmployeeClick(employee)
            }
        })

        // Настройка RecyclerView
        employeesRecyclerView.layoutManager = LinearLayoutManager(this)
        employeesRecyclerView.adapter = employeesAdapter

    }

    private fun handleEmployeeClick(employee: Employee) {
        // Получите координаты местоположения сотрудника
        val destinationPoint = Point(employee.latitude, employee.longitude)

        // Получите координаты текущего местоположения пользователя (можно использовать значения из поиска)
        val currentLocation = mapView.map.cameraPosition.target

        // Создайте запрос на построение маршрута
        val request = createRouteRequest(currentLocation, destinationPoint, employee.username)

        // Отправьте запрос
        sendRouteRequest(request, employee.username)

        // Установите зум на карте
        mapView.map.move(
            CameraPosition(Point(employee.latitude, employee.longitude), 14f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )

        // Скрыть RecyclerView
        employeesRecyclerView.visibility = View.GONE
    }

    private fun createRouteRequest(startPoint: Point, endPoint: Point, username: String): DrivingSession {
        val options = DrivingOptions()
        options.routesCount = 1
        val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
        val startRequestPoint = createRequestPoint(startPoint, RequestPointType.WAYPOINT)
        val endRequestPoint = createRequestPoint(endPoint, RequestPointType.WAYPOINT)

        val session = drivingRouter.requestRoutes(
            listOf(startRequestPoint, endRequestPoint),
            options,
            VehicleOptions(),
            object : DrivingSession.DrivingRouteListener {
                override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                    if (drivingRoutes.isNotEmpty()) {
                        val route = drivingRoutes[0]

                        // Удаление старого маршрута для конкретного пользователя
                        currentRoutesMap[username]?.let {
                            mapView.map.mapObjects.remove(it)
                        }

                        // Отображение нового маршрута на карте
                        currentRoutesMap[username] = mapView.map.mapObjects.addPolyline(route.geometry)

                    }
                }

                override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
                    // Обработка ошибок при построении маршрута
                }
            }
        )

        return session
    }


    private fun createRequestPoint(endPoint: Point, waypoint: RequestPointType): RequestPoint {
        return RequestPoint(endPoint, waypoint, null, null)
    }

    private fun sendRouteRequest(session: DrivingSession, username: String) {
        // Отправка запроса на построение маршрута
        // Этот метод вызывается автоматически после создания запроса, обработка результатов в onDrivingRoutes
        session.retry(object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                if (drivingRoutes.isNotEmpty()) {
                    val route = drivingRoutes[0]

                    // Удаление старого маршрута для конкретного пользователя
                    currentRoutesMap[username]?.let {
                        mapView.map.mapObjects.remove(it)
                    }

                    // Отображение нового маршрута на карте
                    val polylineMapObject = mapView.map.mapObjects.addPolyline(route.geometry)
                    currentRoutesMap[username] = polylineMapObject

                    // Добавление обработчика долгого нажатия для маршрута
                    polylineMapObject.addTapListener { _, _ ->
                        // Вызывается при долгом нажатии на маршрут
                        showDeleteRouteConfirmation(username)
                        true
                    }

                }

            }

            override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
                // Обработка ошибок при построении маршрута
            }
        })
    }

    private fun showDeleteRouteConfirmation(username: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("Удалить маршрут для байкера $username?")

        alertDialogBuilder.setPositiveButton("Да") { dialog, _ ->
            // Удаление маршрута при согласии пользователя
            currentRoutesMap[username]?.let {
                mapView.map.mapObjects.remove(it)
            }
            dialog.dismiss()
        }

        alertDialogBuilder.setNegativeButton("Отмена") { dialog, _ ->
            // Отмена удаления маршрута
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun performSearch(query: String) {
        val suggestOptions = SuggestOptions().setSuggestTypes(SuggestType.UNSPECIFIED.value)

        // Сессия поиска
        suggestSession = searchManager.createSuggestSession()

        // Используйте эту строку
        val boundingBox = BoundingBox(mapView.map.visibleRegion.bottomLeft, mapView.map.visibleRegion.topRight)
        suggestSession.suggest(query, boundingBox, suggestOptions, suggestListener)
    }

    private val suggestListener = object : SuggestSession.SuggestListener {
        override fun onResponse(suggestions: MutableList<SuggestItem>) {
            // Создаем список адресов для автодополнения
            val addresses = mutableListOf<String>()

            // Итерируем по предложениям и добавляем адреса в список
            for (suggestItem in suggestions) {
                addresses.add(suggestItem.title.text)
            }

            // Очищаем адаптер и добавляем новые адреса
            adapter.clear()
            adapter.addAll(addresses)

            // Обновляем AutoCompleteTextView
            adapter.notifyDataSetChanged()

            searchEditText.setOnItemClickListener { _, _, position, _ ->
                val selectedSuggestion = suggestions[position]
                val coordinates = selectedSuggestion.center

                if (coordinates != null) {
                    val addressCoordinates = Point(coordinates.latitude, coordinates.longitude)

                    // Перемещаем карту к выбранному месту
                    mapView.map.move(
                        CameraPosition(addressCoordinates, 18.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )

                    // Отображаем список ближайших сотрудников
                    getEmployeesFromFirebaseAndSort(coordinates.latitude, coordinates.longitude)

                    searchEditText.clearFocus()
                    employeesRecyclerView.visibility = View.VISIBLE
                } else {
                    // Обработка случая, если координаты недоступны
                }
            }
        }

        override fun onError(error: com.yandex.runtime.Error) {
            // Обработка ошибки поиска
            // Можете добавить здесь логирование или отображение сообщения об ошибке
        }
    }

    private fun getEmployeesFromFirebaseAndSort(searchLatitude: Double, searchLongitude: Double) {
        val employeesRef = FirebaseDatabase.getInstance().getReference("locations")


        employeesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employees = mutableListOf<Employee>()

                for (employeeSnapshot in snapshot.children) {
                    val username = employeeSnapshot.key
                    val latitude = employeeSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = employeeSnapshot.child("longitude").getValue(Double::class.java)

                    if (username != null && latitude != null && longitude != null) {
                        val employee = Employee(username, latitude, longitude)
                        employee.distance = calculateDistance(searchLatitude, searchLongitude, latitude, longitude)
                        employees.add(employee)
                    }
                }

                // Сортируем список сотрудников по близости к выбранному адресу
                val sortedEmployees = sortEmployeesByDistance(employees, searchLatitude, searchLongitude)

                employeesAdapter.submitList(sortedEmployees)
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки
            }
        })
    }

    private fun sortEmployeesByDistance(employees: List<Employee>, searchLatitude: Double, searchLongitude: Double): List<Employee> {
        return employees.sortedBy { employee ->
            calculateDistance(
                searchLatitude, searchLongitude,
                employee.latitude, employee.longitude
            )
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Ваш код для расчета расстояния между двумя точками
        // Например, можно использовать формулу гаверсинуса.

        val R = 6371 // Радиус Земли в километрах

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c // Расстояние в километрах
    }
    private fun readDataFromDatabase() {
        val database = FirebaseDatabase.getInstance()
        val locationRef = database.getReference("locations")

        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Очистим существующие метки на карте
                mapView.map.mapObjects.clear()

                for (dataSnapshot in snapshot.children) {
                    val userName = dataSnapshot.key
                    val locationData = dataSnapshot.getValue(LocationData::class.java)

                    if (userName != null && locationData != null) {
                        // Добавим метку на карту
                        addMapMarker(locationData.latitude, locationData.longitude, userName)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRead", "Error reading data from database: ${error.message}")
            }


        })
    }

    private fun addMapMarker(latitude: Double, longitude: Double, userName: String) {
        val mapObjects = mapView.map.mapObjects.addCollection()
        val point = Point(latitude, longitude)
        val placemark = mapObjects.addPlacemark(point)

        // Установка пользовательского изображения в качестве иконки метки
        placemark.setIcon(ImageProvider.fromResource(this, R.drawable.chelik))

        // Добавление балуна с именем пользователя
        placemark.userData = userName

        placemark.addTapListener { _, _ ->
            // Отобразить информацию о метке (например, имя пользователя)
            showMarkerInfo(userName)
            true
        }

        // Плавное перемещение метки с текущего положения до нового
        val animator = ObjectAnimator.ofObject(placemark, "geometry", PointEvaluator(), point)
        animator.duration = 1000 // Длительность анимации в миллисекундах
        animator.start()
    }

    private fun showMarkerInfo(userName: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("Байкер:  $userName")

        // Кнопка "OK", которая закрывает диалоговое окно
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        // Создаем и отображаем диалоговое окно
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    class PointEvaluator : TypeEvaluator<Point> {
        override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
            val x = startValue.latitude + (endValue.latitude - startValue.latitude) * fraction
            val y = startValue.longitude + (endValue.longitude - startValue.longitude) * fraction
            return Point(x, y)
        }
    }


    override fun onStart() {

        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {

        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.app)
        }
    }
}
