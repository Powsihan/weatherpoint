package cst.project.weatherpoint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import cst.project.weatherpoint.databinding.MainScreenBinding
import java.io.IOException
import java.util.Locale

class MainScreen : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap //googleMap provided by android API represents object mMap
    private lateinit var binding: MainScreenBinding //used for data binding

    //for expandable content
    private lateinit var expandableContent : CardView
    private lateinit var constantLayout : LinearLayout
    private lateinit var cvExpand : CardView
    private lateinit var imgMenuButton : ImageView
    private lateinit var imgHomeButton : ImageButton
    private lateinit var imgForecastButton : ImageButton
    private lateinit var imgWeatherButton : ImageButton
    private lateinit var imgLocationButton : ImageButton

    private lateinit var cvRefresh : CardView //refresh button

    var searchView: SearchView? = null //search bar

    //---------LocationVariables----------
    val locationClient : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this) //Computed properties
    }
    var currentLocation : Location? = null
    val LOCATION_REQUEST_CODE = 1

    //location details views
    private lateinit var imgMainWeather : ImageView
    private lateinit var txtMainDesc : TextView
    private lateinit var txtLocation : TextView
    private lateinit var txtTemp : TextView
    private lateinit var txtHumidity : TextView
    private lateinit var txtPressure : TextView
    private lateinit var txtWindSpeed : TextView
    private lateinit var txtCloudiness : TextView
    private lateinit var txtDate : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //declaring expandable content views
        cvRefresh = findViewById(R.id.cv_refresh)
        expandableContent = findViewById(R.id.expandable_card)
        constantLayout = findViewById(R.id.ll_constant)
        cvExpand = findViewById(R.id.cv_expand)
        imgMenuButton = findViewById(R.id.img_btn_menu_out)
        imgHomeButton = findViewById(R.id.img_btn_home)
        imgForecastButton = findViewById(R.id.img_btn_forecast)
        imgWeatherButton = findViewById(R.id.img_btn_weather)
        imgLocationButton = findViewById(R.id.img_btn_location)

        //location views declaration
        imgMainWeather = findViewById(R.id.img_main_weather)
        txtDate = findViewById(R.id.txt_date)
        txtMainDesc = findViewById(R.id.txt_main_desc)
        txtLocation = findViewById(R.id.txt_location)
        txtTemp = findViewById(R.id.txt_temp)
        txtHumidity = findViewById(R.id.txt_humidity)
        txtPressure = findViewById(R.id.txt_pressure)
        txtWindSpeed = findViewById(R.id.txt_wind_speed)
        txtCloudiness = findViewById(R.id.txt_cloudiness)

//        val currentDate = LocalDate.now().nowformat(DateTimeFormatter.ofPattern("dd/mm/yyyy"))
//        txtDate.text = currentDate

        //setting initial image
        imgMenuButton.setBackgroundResource(R.drawable.madmenuout)

        //declaring search bar
        searchView = findViewById(R.id.idSearchView)

        //when user submits a search
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchInMap()
                createMap() //to sync the map with the search location
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        cvExpand.setOnClickListener {
            toggleWeatherContent()
        }//when user click location details
        cvRefresh.setOnClickListener{
            accessCurrentLocation()
        }//refreshes the map to show current location

        imgHomeButton.setBackgroundColor(ContextCompat.getColor(this,R.color.btn_focused))

        //navigation
        imgHomeButton.setOnClickListener(){
            Toast.makeText(this, "Already in home screen.", Toast.LENGTH_SHORT).show()
        }
        imgForecastButton.setOnClickListener(){
            startActivity(Intent(this, ForecastScreen::class.java))
        }
        imgWeatherButton.setOnClickListener(){
            startActivity(Intent(this, WeatherScreen::class.java))
        }
        imgLocationButton.setOnClickListener(){
            startActivity(Intent(this, LocationScreen::class.java))
        }

        // current location----------------------------------------------------------------------------------------------
        createMap() //creates the map
    }

    @SuppressLint("MissingPermission")
    fun accessCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        locationClient.lastLocation.addOnSuccessListener(this) { location -> //actual current location
            if (location != null) {
                currentLocation = location
                val currentLatLng =
                    LatLng(location.latitude, location.longitude) //my location lat and long
                updateMarker(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))
                val city = getCityNameFromLocation(
                    this,
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
                loadWeatherInfo(city = city.first)
                txtLocation.text = city.first + ", " + city.second
            }
        }
    }

    private fun getCityNameFromLocation(mainScreen: MainScreen, latitude: Double, longitude: Double): Pair<String, String> {
        val geocoder = Geocoder(mainScreen, Locale.getDefault())
        var cityName = ""
        var countryName = ""

        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses!!.isNotEmpty()) {
                cityName = addresses[0].locality
                countryName = addresses[0].countryName
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Pair(cityName,countryName)
    }

    private fun loadWeatherInfo(city: String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=6737838aeba7aa4a05c93bdc226b4ea1"
        val request = JsonObjectRequest(
            Request.Method.GET, url,null, {
                data -> try {
                txtMainDesc.text = data.getJSONArray("weather").getJSONObject(0).getString("description")
                txtTemp.text = String.format("%.1f °C", data.getJSONObject("main").getDouble("temp") - 273.15)
                txtPressure.text = data.getJSONObject("main").getString("pressure")
                txtHumidity.text = data.getJSONObject("main").getString("humidity")
                txtWindSpeed.text = data.getJSONObject("wind").getDouble("speed").toString()
                txtCloudiness.text = data.getJSONObject("clouds").getInt("all").toString() + "%"

                val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd")
                val sdfTime = java.text.SimpleDateFormat("hh:mm:ss a")
                val currentDate = sdfDate.format(java.util.Date())
                val currentTime = sdfTime.format(java.util.Date())
                txtDate.text = "$currentDate  |  $currentTime"

                val imageurl = "https://openweathermap.org/img/wn/"+ data.getJSONArray("weather").getJSONObject(0).getString("icon")+"@4x.png"
                Picasso.get().load(imageurl).into(imgMainWeather)
            }catch (e : Exception){
        }
        },
            {error->
                Log.e("Error", error.toString())
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun updateMarker(latLng: LatLng) {
        // Remove previous marker if exists
        mMap.clear()
        // Create a new marker at the current location
        val markerOptions = MarkerOptions().position(latLng).title("Current Location")
        mMap.addMarker(markerOptions)

        // Optionally move the camera to the updated location
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    //creates the map when screen opens
    private fun createMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.home_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    //searches for the location when user submits a search
    private fun searchInMap() {
        // on below line we are getting the location name from search view.
        val location = searchView?.getQuery().toString()
        // below line is to create a list of address where we will store the list of all address.
        var addressList: List<Address>? = null

        // checking if the entered location is null or not.
        if (location != null || location == "") {
            // on below line we are creating and initializing a geo coder.
            val geocoder = Geocoder(this@MainScreen)
            try {
                // on below line we are getting location from the location name and adding that location to address list.
                addressList = geocoder.getFromLocationName(location, 1)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //getting the location from our list at first position.
            val address = addressList!![0]

            //search locations latitude and longitude.
            val latLng = LatLng(address.latitude, address.longitude)

            mMap?.clear()//remove existing markers

            //adding marker to that position.
            updateMarker(latLng)

            val searchCity = getCityNameFromLocation( //weather update after a search for city
                this,
                latLng!!.latitude,
                latLng!!.longitude
            )
            loadWeatherInfo(city = searchCity.first) //cont
            txtLocation.text = searchCity.first + ", " + searchCity.second
        }
    }

    //opening and closing fragment
    private fun toggleWeatherContent(){
        val isVisible = expandableContent.visibility == View.VISIBLE

        if (!isVisible) {
            // Slide in animation
            constantLayout.setBackgroundColor(ContextCompat.getColor(this,R.color.btn_focused))
            imgMenuButton.setBackgroundResource(R.drawable.madmenuin)
            expandableContent.visibility = View.VISIBLE
            expandableContent.post {
                expandableContent.translationX = expandableContent.width.toFloat()
                expandableContent.animate()
                    .translationX(0f)
                    .setDuration(500)
                    .start()
            }
        } else {
            // Slide out animation
            imgMenuButton.setBackgroundResource(R.drawable.madmenuout)
            constantLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_unfocused))
            expandableContent.animate()
                .translationX(expandableContent.width.toFloat())
                .setDuration(500)
                .withEndAction {
                    expandableContent.visibility = View.GONE
                }
                .start()
        }
    }

    private var isCurrentLocationAccessed = false // to make this call only once when form load

    //initializes a marker in a location
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap //googleMap object

        if (!isCurrentLocationAccessed) {
            accessCurrentLocation()
            isCurrentLocationAccessed = true
        }

        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {

            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragEnd(marker: Marker) {
                //get weather details and display
            }
        })
    }
}