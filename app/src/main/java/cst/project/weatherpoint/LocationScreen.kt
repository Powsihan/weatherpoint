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
import android.widget.EditText
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
import java.io.IOException
import java.util.Locale

class LocationScreen : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var lMap: GoogleMap //googleMap provided by android API represents object lMap

    //navigationBar
    private lateinit var imgHomeButton : ImageButton
    private lateinit var imgForecastButton : ImageButton
    private lateinit var imgWeatherButton : ImageButton
    private lateinit var imgLocationButton : ImageButton
    //for expandable content
    private lateinit var expandableContent : CardView
    private lateinit var constantLayout : LinearLayout
    private lateinit var cvExpand : CardView
    private lateinit var imgMenuButton : ImageView
    private lateinit var txtLocation : TextView
    private lateinit var txtDate : TextView
    private lateinit var txtLatitude : TextView
    private lateinit var txtLongitude : TextView
    private lateinit var txtCity : TextView
    private lateinit var txtCountry : TextView
    private lateinit var txtState : TextView

    private lateinit var latlngExpand : CardView //btn
    private lateinit var expandSearch : CardView //card to open
    private lateinit var imgArrow : ImageView
    private lateinit var btnSearchLatlng : CardView
    private lateinit var txtSearchLatitude : EditText
    private lateinit var txtSearchLongitude : EditText

    private lateinit var cvRefresh : CardView //refresh button

    var searchView: SearchView? = null //search bar

    //---------LocationVariables----------
    val locationClient : FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this) //Computed properties
    }
    var currentLocation : Location? = null
    val LOCATION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_screen)

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

        txtLocation = findViewById(R.id.txt_location)
        txtDate = findViewById(R.id.txt_date)
        txtLatitude = findViewById(R.id.txt_latitude)
        txtLongitude = findViewById(R.id.txt_longitude)
        txtCity = findViewById(R.id.txt_city)
        txtCountry = findViewById(R.id.txt_country)

        expandSearch = findViewById(R.id.expandable_search)
        latlngExpand = findViewById(R.id.cv_latlng_expand)
        imgArrow = findViewById(R.id.img_latlng)
        btnSearchLatlng = findViewById(R.id.btn_search_latlng)
        txtSearchLatitude = findViewById(R.id.txt_search_latitude)
        txtSearchLongitude = findViewById(R.id.txt_search_longitude)

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

        imgLocationButton.setBackgroundColor(ContextCompat.getColor(this,R.color.btn_focused))
        imgArrow.setBackgroundResource(R.drawable.arrowup)

        cvExpand.setOnClickListener {
            toggleLocationContent()
        }//when user click location details

        latlngExpand.setOnClickListener {
            toggleLatlngSearch()
        } //expand form to enter latlng

        cvRefresh.setOnClickListener{
            accessCurrentLocation()
        }//refreshes the map to show current location

        btnSearchLatlng.setOnClickListener{
            if((txtLatitude != null && txtLatitude.text == "") && (txtLongitude != null && txtLongitude.text == "")) {
                updateSearchMarker(txtSearchLatitude.text.toString(), txtSearchLongitude.text.toString())
                loadLatlngDetails(txtLatitude.text.toString(), txtLongitude.text.toString())
            }
            else
            {
                Toast.makeText(this, "Try again!", Toast.LENGTH_SHORT)
            }
        }

        //navigation
        imgHomeButton.setOnClickListener(){
            startActivity(Intent(this, MainScreen::class.java))
        }
        imgForecastButton.setOnClickListener(){
            startActivity(Intent(this, ForecastScreen::class.java))
        }
        imgWeatherButton.setOnClickListener(){
            startActivity(Intent(this, WeatherScreen::class.java))
        }
        imgLocationButton.setOnClickListener(){
            Toast.makeText(this, "Already in Location menu.", Toast.LENGTH_SHORT).show()
        }

        // current location----------------------------------------------------------------------------------------------
        createMap() //creates the map

        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd")
        val sdfTime = java.text.SimpleDateFormat("hh:mm:ss a")
        val currentDate = sdfDate.format(java.util.Date())
        val currentTime = sdfTime.format(java.util.Date())
        txtDate.text = "$currentDate  |  $currentTime"
    }

    @SuppressLint("MissingPermission")
    fun accessCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            return
        }
        lMap.isMyLocationEnabled = true
        locationClient.lastLocation.addOnSuccessListener(this) { location -> //actual current location
            if (location != null) {
                currentLocation = location
                val currentLatLng =
                    LatLng(location.latitude, location.longitude) //my location lat and long
                updateMarker(currentLatLng)
                lMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11f))
                val city = getCityNameFromLocation(
                    this,
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )

                txtLocation.text = city.first + ", " + city.second
                loadLocationDetails(city.first)
            }
        }
    }

    private fun getCityNameFromLocation(locationScreen: LocationScreen, latitude: Double, longitude: Double): Pair<String, String> {
        val geocoder = Geocoder(locationScreen, Locale.getDefault())
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

    private fun updateMarker(latLng: LatLng) {
        // Remove previous marker if exists
        lMap.clear()
        // Create a new marker at the current location
        val markerOptions = MarkerOptions().position(latLng).title("Current Location")
        lMap.addMarker(markerOptions)

        // Optionally move the camera to the updated location
        lMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private fun updateSearchMarker(latitude: String, longitude: String) {
        // Remove previous marker if exists
        lMap.clear()

        val latLng = LatLng(latitude.toDouble(), longitude.toDouble())

        // Create a new marker at the current location
        val markerOptions = MarkerOptions().position(latLng).title("Current Location")
        lMap.addMarker(markerOptions)

        // Optionally move the camera to the updated location
        lMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    //creates the map when screen opens
    private fun createMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.location_map) as SupportMapFragment
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
            val geocoder = Geocoder(this@LocationScreen)
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

            lMap?.clear()//remove existing markers

            //adding marker to that position.
            updateMarker(latLng)

            val searchCity = getCityNameFromLocation( //weather update after a search for city
                this,
                latLng!!.latitude,
                latLng!!.longitude
            )

            txtLocation.text = searchCity.first + ", " + searchCity.second
            loadLocationDetails(searchCity.first)
        }
    }

    private fun loadLatlngDetails(latitude : String, longitude: String){
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=${latitude}&lon=${longitude}&appid=6737838aeba7aa4a05c93bdc226b4ea1"
        val request = JsonObjectRequest(
            Request.Method.GET, url,null, {
                    data -> try {
                txtLatitude.text = data.getJSONObject("coord").getString("lat")
                txtLongitude.text = data.getJSONObject("coord").getString("lon")
                txtCity.text = data.getString("name")
                txtCountry.text = data.getJSONObject("sys").getString("country")
            }catch (e : Exception){
                Log.e("Error",e.toString())
            }
            },
            { error ->
                Log.e("Response", error.toString())
            })
        Volley.newRequestQueue(this).add(request)

    }

    private fun loadLocationDetails(city : String){
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=6737838aeba7aa4a05c93bdc226b4ea1"
        val request = JsonObjectRequest(
            Request.Method.GET, url,null, {
                data -> try {
                txtLatitude.text = data.getJSONObject("coord").getString("lat")
                txtLongitude.text = data.getJSONObject("coord").getString("lon")
                txtCity.text = data.getString("name")
                txtCountry.text = data.getJSONObject("sys").getString("country")
            }catch (e : Exception){
                    Log.e("Error",e.toString())
                }
            },
            { error ->
                Log.e("Response", error.toString())
            })
        Volley.newRequestQueue(this).add(request)
    }

    //opening and closing fragment
    private fun toggleLocationContent(){
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

    private fun toggleLatlngSearch() {
        val isVisible = expandSearch.visibility == View.VISIBLE

        if (!isVisible) {
            // Slide in animation
            imgArrow.setBackgroundResource(R.drawable.arrowdown)
            expandSearch.visibility = View.VISIBLE
            expandSearch.post {
                expandSearch.translationY = expandSearch.width.toFloat()
                expandSearch.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .start()
            }
        } else {
            // Slide out animation
            imgArrow.setBackgroundResource(R.drawable.arrowup)
            expandSearch.animate()
                .translationY(expandSearch.width.toFloat())
                .setDuration(500)
                .withEndAction {
                    expandSearch.visibility = View.GONE
                }
                .start()
        }
    }

    private var isCurrentLocationAccessed = false // to make this call only once when form load
    //initializes a marker in a location
    override fun onMapReady(googleMap: GoogleMap) {
        lMap = googleMap //googleMap object

        if (!isCurrentLocationAccessed) {
            accessCurrentLocation()
            isCurrentLocationAccessed = true
        }

        lMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {

            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragEnd(marker: Marker) {
                //get weather details and display
            }
        })
    }
}