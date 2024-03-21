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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import java.io.IOException
import java.util.Locale

class WeatherScreen : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fMap: GoogleMap //googleMap provided by android API represents object mMap

    //navigationBar
    private lateinit var imgHomeButton : ImageButton
    private lateinit var imgForecastButton : ImageButton
    private lateinit var imgWeatherButton : ImageButton
    private lateinit var imgLocationButton : ImageButton

    //for location details
    private lateinit var redLayout : LinearLayout
    private lateinit var greenLayout : LinearLayout
    //red marker
    private lateinit var txtRedCity : TextView
    private lateinit var txtRedDesc : TextView
    private lateinit var txtRedTemp : TextView
    private lateinit var txtRedHumid : TextView
    private lateinit var txtRedWind : TextView
    private lateinit var imgRedIcon : ImageView
    //green marker
    private lateinit var txtGreenCity : TextView
    private lateinit var txtGreenDesc : TextView
    private lateinit var txtGreenTemp : TextView
    private lateinit var txtGreenHumid : TextView
    private lateinit var txtGreenWind : TextView
    private lateinit var imgGreenIcon : ImageView

    private lateinit var txtDate : TextView

    var marker = "red"

    //Location initializers
    var searchView: SearchView? = null

    val locationClient : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this) //Computed properties
    }
    var currentLocation : Location? = null
    val LOCATION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_screen)

        redLayout = findViewById(R.id.ll_red_marker)
        greenLayout = findViewById(R.id.ll_green_marker)
        imgHomeButton = findViewById(R.id.img_btn_home)
        imgForecastButton = findViewById(R.id.img_btn_forecast)
        imgWeatherButton = findViewById(R.id.img_btn_weather)
        imgLocationButton = findViewById(R.id.img_btn_location)
        txtDate = findViewById(R.id.txt_date)

        //Red Marker
        txtRedCity = findViewById(R.id.txt_red_city)
        txtRedDesc = findViewById(R.id.txt_red_desc)
        txtRedTemp = findViewById(R.id.txt_red_temp)
        txtRedHumid = findViewById(R.id.txt__red_humid)
        txtRedWind = findViewById(R.id.txt_red_wind)
        imgRedIcon = findViewById(R.id.img_red_icon)
        //Green Marker
        txtGreenCity = findViewById(R.id.txt_green_city)
        txtGreenDesc = findViewById(R.id.txt_green_desc)
        txtGreenTemp = findViewById(R.id.txt_green_temp)
        txtGreenHumid = findViewById(R.id.txt_green_humid)
        txtGreenWind = findViewById(R.id.txt_green_wind)
        imgGreenIcon = findViewById(R.id.img_green_icon)

        //declaring search bar
        searchView = findViewById(R.id.idSearchView)

        //when user submits a search
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchInMap(marker)//sends which marker to update
                createMap() //to sync the map with the search location
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        redLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_focused))

        redLayout.setOnClickListener {
            redLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_focused))
            greenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_unfocused))
            marker = "red"
        }
        greenLayout.setOnClickListener{
            greenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_focused))
            redLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_unfocused))
            marker = "green"
        }

        imgWeatherButton.setBackgroundColor(ContextCompat.getColor(this,R.color.btn_focused))

        //navigation
        imgHomeButton.setOnClickListener(){
            startActivity(Intent(this, MainScreen::class.java))
        }
        imgForecastButton.setOnClickListener(){
            startActivity(Intent(this, ForecastScreen::class.java))
        }
        imgWeatherButton.setOnClickListener(){
            Toast.makeText(this, "Already in weather menu.", Toast.LENGTH_SHORT).show()
        }
        imgLocationButton.setOnClickListener(){
            startActivity(Intent(this, LocationScreen::class.java))
        }

        createMap()

        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd")
        val sdfTime = java.text.SimpleDateFormat("hh:mm:ss a")
        val currentDate = sdfDate.format(java.util.Date())
        val currentTime = sdfTime.format(java.util.Date())
        txtDate.text = "$currentDate  |  $currentTime"
    }

    private fun createMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment2 = supportFragmentManager.findFragmentById(R.id.weather_map) as SupportMapFragment
        mapFragment2.getMapAsync(this)
    }

    private fun searchInMap(marker: String) {
        // on below line we are getting the location name from search view.
        val location = searchView?.getQuery().toString()
        // below line is to create a list of address where we will store the list of all address.
        var addressList: List<Address>? = null

        // checking if the entered location is null or not.
        if (location != null || location == "") {
            val geocoder = Geocoder(this@WeatherScreen)
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

            //adding marker to that position.
            updateMarker(latLng, marker)

            val searchCity = getCityNameFromLocation( //weather update after a search for city
                this,
                latLng!!.latitude,
                latLng!!.longitude
            )

            if(marker == "red"){
                txtRedCity.text = searchCity.first + ", " + searchCity.second
                loadWeatherDetails(searchCity.first, marker)
            }
            else{
                txtGreenCity.text = searchCity.first + ", " + searchCity.second
                loadWeatherDetails(searchCity.first, marker)
            }
        }
    }

    private var redMarker: Marker? = null
    private var greenMarker: Marker? = null
    private var initialMarker = false
    private fun initialMarker(){
        //beruwula
        val initialRedLat = 6.4738
        val initialRedLon = 79.9920
        //bentota
        val initialGreenLat = 6.4189
        val initialGreenLon = 80.0060
        //marker color
        val redIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        val greenIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

        //initial marker setup
        if(initialMarker == false){
            val initialRedLatlng = LatLng(initialRedLat, initialRedLon)
            val initialGreenLatlng = LatLng(initialGreenLat, initialGreenLon)

            val initialRedMarker = MarkerOptions().position(initialRedLatlng).icon(redIcon).title("Red Marker")
            redMarker = fMap.addMarker(initialRedMarker)

            val initialGreenMarker = MarkerOptions().position(initialGreenLatlng).icon(greenIcon).title("Green Marker")
            greenMarker = fMap.addMarker(initialGreenMarker)

            txtRedCity.text = "Beruwala, Sri Lanka"
            txtGreenCity.text = "Bentota, Sri Lanka"
            loadWeatherDetails("beruwala", "red")
            loadWeatherDetails("bentota", "green")

            fMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initialRedLatlng, 11f))

            initialMarker = true //this will make sure this initial marker setup only runs once
        }
    }
    private fun updateMarker(latLng: LatLng, marker : String) {
        //marker color
        val redIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        val greenIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

        //marker setup after user search
        if (marker == "red") {
            redMarker?.remove()// Remove any existing markers of the same color
            val markerOptions = MarkerOptions().position(latLng).icon(redIcon).title("Red Marker")
            redMarker = fMap.addMarker(markerOptions)
        } else if (marker == "green") {
            greenMarker?.remove()
            val markerOptions = MarkerOptions().position(latLng).icon(greenIcon).title("Green Marker")
            greenMarker = fMap.addMarker(markerOptions)
        }
        fMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n") //annotations to avoid date format warning
    fun loadWeatherDetails(city : String, marker : String){
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=6737838aeba7aa4a05c93bdc226b4ea1"
        val request = JsonObjectRequest(
            Request.Method.GET, url,null, {
                    data -> try {
                    if(marker == "red"){
                        txtRedDesc.text = data.getJSONArray("weather").getJSONObject(0).getString("description")
                        txtRedTemp.text = String.format("%.1f °C", data.getJSONObject("main").getDouble("temp") - 273.15)
                        txtRedHumid.text = data.getJSONObject("main").getString("humidity")
                        txtRedWind.text = data.getJSONObject("wind").getDouble("speed").toString()
                        val iconUrl1 = "https://openweathermap.org/img/wn/" + data.getJSONArray("weather").getJSONObject(0).getString("icon")+"@4x.png"  //here @4x to increase resolution of the icon
                        Picasso.get().load(iconUrl1).into(imgRedIcon)
                    }else if (marker == "green"){
                        txtGreenDesc.text = data.getJSONArray("weather").getJSONObject(0).getString("description")
                        txtGreenTemp.text = String.format("%.1f °C", data.getJSONObject("main").getDouble("temp") - 273.15)
                        txtGreenHumid.text = data.getJSONObject("main").getString("humidity")
                        txtGreenWind.text = data.getJSONObject("wind").getDouble("speed").toString()
                        val iconUrl1 = "https://openweathermap.org/img/wn/" + data.getJSONArray("weather").getJSONObject(0).getString("icon")+"@4x.png"  //here @4x to increase resolution of the icon
                        Picasso.get().load(iconUrl1).into(imgGreenIcon)
                    }
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

    private fun getCityNameFromLocation(weatherScreen: WeatherScreen, latitude: Double, longitude: Double): Pair<String, String> {
        val geocoder = Geocoder(weatherScreen, Locale.getDefault())
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

    @SuppressLint("MissingPermission")
    fun accessCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        fMap.isMyLocationEnabled = true
        locationClient.lastLocation.addOnSuccessListener(this) { location -> //actual current location
            if (location != null) {
                currentLocation = location
                initialMarker()
            }
        }
    }

    private var isCurrentLocationAccessed = false // to make this call only once when form load
    //initializes a marker in a location
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