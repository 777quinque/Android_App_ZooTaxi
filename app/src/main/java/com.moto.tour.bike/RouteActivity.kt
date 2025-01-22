package com.moto.tour.bike

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
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
import com.yandex.runtime.Error

class RouteActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var suggestSession: SuggestSession
    private lateinit var searchEditText: AutoCompleteTextView
    private lateinit var searchEdit: AutoCompleteTextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var drivingRouter: DrivingRouter
    private lateinit var drivingSession: DrivingSession

    private val currentRoutesMap = mutableMapOf<String, PolylineMapObject>()
    private lateinit var routeTextView: TextView
    private var startPoint: Point? = null
    private var endPoint: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        MapKitFactory.initialize(this)
        setStatusBarColor()
        mapView = findViewById(R.id.mapview)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        searchEditText = findViewById(R.id.searchEditText)
        searchEdit = findViewById(R.id.searchEdit)
        routeTextView = findViewById(R.id.routeTextView)
        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
        searchEditText.setAdapter(adapter)
        searchEdit.setAdapter(adapter)


        setupTextWatchers()
        setupRouteTextView()

        mapView.map.move(
            CameraPosition(Point(56.8519, 60.6122), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
    }

    private fun setupTextWatchers() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length > 2) {
                    performSearch(s.toString(), searchEditText)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length > 2) {
                    performSearch(s.toString(), searchEdit)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRouteTextView() {
        routeTextView.setOnClickListener {

            if (startPoint != null && endPoint != null) {
                removePreviousRoute()
                buildRoute(startPoint!!, endPoint!!)
                val currentCameraPosition = mapView.map.cameraPosition
                mapView.map.move(
                    CameraPosition(
                        currentCameraPosition.target,
                        currentCameraPosition.zoom - 2.0f,
                        currentCameraPosition.azimuth,
                        currentCameraPosition.tilt
                    ),
                    Animation(Animation.Type.SMOOTH, 0f),
                    null
                )
            }
        }
    }

    private fun removePreviousRoute() {
        // Удаляем предыдущий маршрут с карты
        currentRoutesMap.values.forEach { polylineMapObject ->
            mapView.map.mapObjects.remove(polylineMapObject)
        }
        currentRoutesMap.clear()
    }

    private fun buildRoute(start: Point, end: Point) {
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()

        val points = listOf(
            createRequestPoint(start, RequestPointType.WAYPOINT),
            createRequestPoint(end, RequestPointType.WAYPOINT)
        )

        val drivingOptions = DrivingOptions().apply {
            routesCount = 1 // Количество альтернативных маршрутов
        }

        val vehicleOptions = VehicleOptions() // Параметры транспортного средства

        drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener
        )
    }

    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            if (drivingRoutes.isNotEmpty()) {
                val route = drivingRoutes[0]
                val polyline = route.geometry
                val polylineMapObject = mapView.map.mapObjects.addPolyline(polyline)
                currentRoutesMap["current_route"] = polylineMapObject
            }
        }

        override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
            // Обработка ошибки построения маршрута
        }
    }

    private fun createRequestPoint(point: Point, type: RequestPointType): RequestPoint {
        return RequestPoint(point, type, null, null)
    }

    private fun performSearch(query: String, editText: AutoCompleteTextView) {
        val suggestOptions = SuggestOptions().setSuggestTypes(SuggestType.UNSPECIFIED.value)
        suggestSession = searchManager.createSuggestSession()
        val boundingBox = BoundingBox(mapView.map.visibleRegion.bottomLeft, mapView.map.visibleRegion.topRight)
        suggestSession.suggest(query, boundingBox, suggestOptions, suggestListener(editText))
    }

    private fun suggestListener(editText: AutoCompleteTextView) = object : SuggestSession.SuggestListener {
        override fun onResponse(suggestions: MutableList<SuggestItem>) {
            val addresses = suggestions.map { it.title.text }
            adapter.clear()
            adapter.addAll(addresses)
            adapter.notifyDataSetChanged()
            editText.setOnItemClickListener { _, _, position, _ ->
                val selectedSuggestion = suggestions[position]
                val coordinates = selectedSuggestion.center
                if (coordinates != null) {
                    val addressCoordinates = Point(coordinates.latitude, coordinates.longitude)
                    mapView.map.move(
                        CameraPosition(addressCoordinates, 18.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )
                    editText.clearFocus()
                    // Устанавливаем startPoint или endPoint в зависимости от editText
                    if (editText == searchEditText) {
                        startPoint = addressCoordinates
                    } else {
                        endPoint = addressCoordinates
                    }
                }
            }
        }

        override fun onError(error: Error) {}
    }

    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.app)
        }
    }
}