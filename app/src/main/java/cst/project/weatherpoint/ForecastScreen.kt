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
import android.widget.ImageButton
import android.widget.ImageView
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
import java.io.IOException
import java.util.Locale

class ForecastScreen : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var fMap: GoogleMap //googleMap provided by android API represents object mMap

    private lateinit var imgHomeButton : ImageButton
    private lateinit var imgForecastButton : ImageButton
    private lateinit var imgWeatherButton : ImageButton
    private lateinit var imgLocationButton : ImageButton

    private lateinit var cvRefresh : CardView //refresh button

    private lateinit var cityName : TextView

    private lateinit var weatherIcon1 : ImageView
    private lateinit var weatherIcon2 : ImageView
    private lateinit var weatherIcon3 : ImageView
    private lateinit var weatherIcon4 : ImageView
    private lateinit var weatherIcon5 : ImageView

    private lateinit var weatherDate1 : TextView
    private lateinit var weatherDate2 : TextView
    private lateinit var weatherDate3 : TextView
    private lateinit var weatherDate4 : TextView
    private lateinit var weatherDate5 : TextView

    private lateinit var forecastDis1 : TextView
    private lateinit var forecastDis2 : TextView
    private lateinit var forecastDis3 : TextView
    private lateinit var forecastDis4 : TextView
    private lateinit var forecastDis5 : TextView

    private lateinit var temperature1 : TextView
    private lateinit var temperature2 : TextView
    private lateinit var temperature3 : TextView
    private lateinit var temperature4 : TextView
    private lateinit var temperature5 : TextView

    var searchView: SearchView? = null

    val locationClient : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this) //Computed properties
    }
    var currentLocation : Location? = null
    val LOCATION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forcast_screen)

        searchView = findViewById(R.id.idSearchView)
        imgHomeButton = findViewById(R.id.img_btn_home)
        imgForecastButton = findViewById(R.id.img_btn_forecast)
        imgWeatherButton = findViewById(R.id.img_btn_weather)
        imgLocationButton = findViewById(R.id.img_btn_location)

        cvRefresh = findViewById(R.id.cv_refresh)

        cityName = findViewById(R.id.cityLable)

        weatherIcon1 = findViewById(R.id.wIcon1)
        weatherIcon2 = findViewById(R.id.wIcon2)
        weatherIcon3 = findViewById(R.id.wIcon3)
        weatherIcon4 = findViewById(R.id.wIcon4)
        weatherIcon5 = findViewById(R.id.wIcon5)

        weatherDate1 = findViewById(R.id.day1)
        weatherDate2 = findViewById(R.id.day2)
        weatherDate3 = findViewById(R.id.day3)
        weatherDate4 = findViewById(R.id.day4)
        weatherDate5 = findViewById(R.id.day5)

        forecastDis1 = findViewById(R.id.forcastDis1)
        forecastDis2 = findViewById(R.id.forcastDis2)
        forecastDis3 = findViewById(R.id.forcastDis3)
        forecastDis4 = findViewById(R.id.forcastDis4)
        forecastDis5 = findViewById(R.id.forcastDis5)

        temperature1 = findViewById(R.id.celcius1)
        temperature2 = findViewById(R.id.celcius2)
        temperature3 = findViewById(R.id.celcius3)
        temperature4 = findViewById(R.id.celcius4)
        temperature5 = findViewById(R.id.celcius5)

        searchView?.getQuery().toString()

        imgHomeButton.setOnClickListener(){
            startActivity(Intent(this, MainScreen::class.java))
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchInMap()
                createMap() //to sync the map with the seach location
                loadForecast(searchView?.query.toString())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        imgForecastButton.setBackgroundColor(ContextCompat.getColor(this,R.color.btn_focused))

        //navigation
        imgHomeButton.setOnClickListener(){
            startActivity(Intent(this, MainScreen::class.java))
        }
        imgForecastButton.setOnClickListener(){
            Toast.makeText(this, "Already in forecast menu.", Toast.LENGTH_SHORT).show()
        }
        imgWeatherButton.setOnClickListener(){
            startActivity(Intent(this, WeatherScreen::class.java))
        }
        imgLocationButton.setOnClickListener(){
            startActivity(Intent(this, LocationScreen::class.java))
        }

        cvRefresh.setOnClickListener{
            accessCurrentLocation()
        }//refreshes the map to show current location

        createMap()
    }

    private fun createMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment2 = supportFragmentManager.findFragmentById(R.id.forecast_map) as SupportMapFragment
        mapFragment2.getMapAsync(this)
    }

    fun accessCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        fMap.isMyLocationEnabled = true
        locationClient.lastLocation.addOnSuccessListener(this) { location -> //actual current location
            if (location != null) {
                currentLocation = location
                val currentLatLng =
                    LatLng(location.latitude, location.longitude) //my location lat and long
                updateMarker(currentLatLng)
                fMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))
                val city = getCityNameFromLocation(
                    this,
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
                loadForecast(city = city.first)
                cityName.text = city.first + ", " + city.second+"."
            }
        }
    }

    private fun searchInMap() {
        // on below line we are getting the location name from search view.
        val location = searchView?.getQuery().toString()
        // below line is to create a list of address where we will store the list of all address.
        var addressList: List<Address>? = null

        // checking if the entered location is null or not.
        if (location != null || location == "") {
            // on below line we are creating and initializing a geo coder.
            val geocoder = Geocoder(this@ForecastScreen)
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

            fMap?.clear()//remove existing markers

            //adding marker to that position.
            updateMarker(latLng)

            val searchCity = getCityNameFromLocation( //weather update after a search for city
                this,
                latLng!!.latitude,
                latLng!!.longitude
            )
            loadForecast(city = searchCity.first) //cont
            cityName.text = searchCity.first + ", " + searchCity.second
        }
    }

    private fun updateMarker(latLng: LatLng) {
        // Remove previous marker if exists
        fMap.clear()
        // Create a new marker at the current location
        val markerOptions = MarkerOptions().position(latLng).title("Current Location")
        fMap.addMarker(markerOptions)

        // Optionally move the camera to the updated location
        fMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private fun getCityNameFromLocation(forecastScreen: ForecastScreen, latitude: Double, longitude: Double): Pair<String, String> {
        val geocoder = Geocoder(forecastScreen, Locale.getDefault())
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
        return Pair(cityName, countryName)
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    fun loadForecast(city : String){
        val url = "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=6737838aeba7aa4a05c93bdc226b4ea1"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { data->
                Log.e("Response", data.toString())

                try{
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(data.getJSONArray("list").getJSONObject(1).getString("dt_txt"))
                    weatherDate1.text = "${java.text.SimpleDateFormat("yyyy-MM-dd").format(date)}  |  ${java.text.SimpleDateFormat("hh:mm:ss a").format(date)}"
                    //split where there is a space then we format it
                    weatherDate2.text = data.getJSONArray("list").getJSONObject(9).getString("dt_txt").let { it.split(" ")[0] }
                    weatherDate3.text = data.getJSONArray("list").getJSONObject(17).getString("dt_txt").let { it.split(" ")[0] }
                    weatherDate4.text = data.getJSONArray("list").getJSONObject(25).getString("dt_txt").let { it.split(" ")[0] }
                    weatherDate5.text = data.getJSONArray("list").getJSONObject(33).getString("dt_txt").let { it.split(" ")[0] }

                    forecastDis1.text = data.getJSONArray("list").getJSONObject(1).getJSONArray("weather").getJSONObject(0).getString("description")
                    forecastDis2.text = data.getJSONArray("list").getJSONObject(9).getJSONArray("weather").getJSONObject(0).getString("description")
                    forecastDis3.text = data.getJSONArray("list").getJSONObject(17).getJSONArray("weather").getJSONObject(0).getString("description")
                    forecastDis4.text = data.getJSONArray("list").getJSONObject(25).getJSONArray("weather").getJSONObject(0).getString("description")
                    forecastDis5.text = data.getJSONArray("list").getJSONObject(33).getJSONArray("weather").getJSONObject(0).getString("description")

                    temperature1.text = String.format("%.1f °C", data.getJSONArray("list").getJSONObject(1).getJSONObject("main").getDouble("temp") - 273.15)
                    temperature2.text = String.format("%.0f °C", data.getJSONArray("list").getJSONObject(9).getJSONObject("main").getDouble("temp") - 273.15)
                    temperature3.text = String.format("%.0f °C", data.getJSONArray("list").getJSONObject(17).getJSONObject("main").getDouble("temp") - 273.15)
                    temperature4.text = String.format("%.0f °C", data.getJSONArray("list").getJSONObject(25).getJSONObject("main").getDouble("temp") - 273.15)
                    temperature5.text = String.format("%.0f °C", data.getJSONArray("list").getJSONObject(33).getJSONObject("main").getDouble("temp") - 273.15)

                    val iconUrl1 = "https://openweathermap.org/img/wn/" + data.getJSONArray("list").getJSONObject(1).getJSONArray("weather").getJSONObject(0).getString("icon")+"@4x.png"  //here @4x to increase resolution of the icon
                    Picasso.get().load(iconUrl1).into(weatherIcon1)
                    val iconUrl2 = "https://openweathermap.org/img/wn/${data.getJSONArray("list").getJSONObject(9).getJSONArray("weather").getJSONObject(0).getString("icon")}@4x.png"
                    Picasso.get().load(iconUrl2).into(weatherIcon2)
                    val iconUrl3 = "https://openweathermap.org/img/wn/${data.getJSONArray("list").getJSONObject(17).getJSONArray("weather").getJSONObject(0).getString("icon")}@4x.png"
                    Picasso.get().load(iconUrl3).into(weatherIcon3)
                    val iconUrl4 = "https://openweathermap.org/img/wn/${data.getJSONArray("list").getJSONObject(25).getJSONArray("weather").getJSONObject(0).getString("icon")}@4x.png"
                    Picasso.get().load(iconUrl4).into(weatherIcon4)
                    val iconUrl5 = "https://openweathermap.org/img/wn/${data.getJSONArray("list").getJSONObject(33).getJSONArray("weather").getJSONObject(0).getString("icon")}@4x.png"
                    Picasso.get().load(iconUrl5).into(weatherIcon5)

                }
                catch (e : Exception){
                    Log.e("Error",e.toString())
                }
            },
            { error ->
                Log.e("Response", error.toString())
            })

        Volley.newRequestQueue(this).add(request)
    }

    private var isCurrentLocationAccessed = false

    override fun onMapReady(googleMap: GoogleMap) {
        fMap = googleMap //googleMap object

        if (!isCurrentLocationAccessed) {
            accessCurrentLocation()
            isCurrentLocationAccessed = true
        }

        fMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {

            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragEnd(marker: Marker) {
                //get weather details and display
            }
        })
    }
}