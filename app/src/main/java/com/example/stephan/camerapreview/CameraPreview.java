package com.example.stephan.camerapreview;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.opengl.util.LowPassFilter;
import com.beyondar.android.util.location.BeyondarLocationManager;
import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.BeyondarObjectList;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CameraPreview extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener, LocationListener,
        OnClickBeyondarObjectListener{

    private static final long MIN_DISTANCE = 2;
    private static final long MIN_TIME = 1000;
    private static final String KEY_HISTORY = "History_Prefs_Key";

    private String destination;

    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    private boolean isNavigationInit = false;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationManager locationManager;
    private List<Location> locationsList;
    private World world;
    private HashMap<Location, GeoObject> locationGeoObjectHashMap;
    private List<GoogleRadarTask.PlaceRadarSearch> radarSearchList;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    private int countSelected = 0;
    private int countToSelect = 0;

    private BeyondarFragmentSupport mBeyondarFragment;
    private TextView collectedText;
    private ProgressDialog progressDialog;
    private ImageView mPointer;
    private Button backButton, refreshButton;

    private BroadcastReceiver broadcastReceiver;
    private GpsFilter gpsFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        buildGoogleApiClient();

        gpsFilter = new GpsFilter();
        mPointer = (ImageView) findViewById(R.id.imageView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        world = new World(this);
        world.setLocation(mLastLocation);
        world.setDefaultImage(R.drawable.ic_marker);

        locationGeoObjectHashMap = new HashMap<>();

        collectedText = (TextView) findViewById(R.id.pointText);
        backButton = (Button) findViewById(R.id.backButton);
        refreshButton = (Button) findViewById(R.id.revalButton);

        BeyondarLocationManager.setLocationManager((locationManager));
        BeyondarLocationManager.addWorldLocationUpdate(world);
        BeyondarLocationManager.addLocationListener(this);

        backToStartScreen(null);
        initLocationListener();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String tag = bundle.getString(getResources().getString(R.string.key_ar_event_caluclated));
                    if(tag.equals(getResources().getString(R.string.tag_broadcast_directions))){
                        updateLocations();
                    }else if(tag.equals(getResources().getString(R.string.tag_broadcast_radarsearch))){
                        processRadarData();
                    }
                }
            }
        };
    }

    /**
     * Initialisiere den Location Update Request
     */
    private void initLocationListener() {
        try {
            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGPSEnabled && !isNetworkEnabled) {
               Toast.makeText(this, "GPS disabled", Toast.LENGTH_SHORT).show();
            } else {
                this.canGetLocation = true;

                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
                    if (locationManager != null) {
                        mLastLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

                    if (locationManager != null) {
                        mLastLocation = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (canGetLocation) BeyondarLocationManager.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(GoogleRadarTask.RADAR_TASK_INTENTFILTER));
        registerReceiver(broadcastReceiver, new IntentFilter(DirectionFetcher.DIRECTION_TASK_INTENTFILTER));
        BeyondarLocationManager.enable();
        initLocationListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        BeyondarLocationManager.disable();
        locationManager.removeUpdates(this);
        cleanTempFolder();
    }

    /**
     * Neue Navigation wird direkt in Shared Preferences abgespeichert
     * @param destination
     */
    public void newNavigation(String destination) {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        Set<String> targets = new HashSet<>();
        targets.addAll(preferences.getStringSet(KEY_HISTORY, new HashSet<String>()));
        targets.add(destination);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_HISTORY,targets).commit();

        FrameLayout frame = (FrameLayout) findViewById(R.id.contentPanel);
        frame.addView(mPointer);
        backButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        collectedText.setVisibility(View.VISIBLE);
        collectedText.setText("0/0");
        collectedText.setVisibility(View.VISIBLE);
        this.destination = destination;

        if (mLastLocation != null) startNavigation(destination);

        progressDialog = ProgressDialog.show(this, "Processing",
                "calculate route", true);

        addBeyondArFragment();
    }

    /**
     * Initialisiert die Google Api Client Connection
     */
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        } else if (mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /**
     * Eigener Sensor Listener für Kompassrotation
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == mAccelerometer) {
            mLastAccelerometer = lowPass(event.values.clone(), mLastAccelerometer);
            // arraycopy (source, IndexBegin, target, IndexBegin, length)

            mLastAccelerometerSet = true;
            // catches changes registered by the Magnetometer
        } else if (event.sensor == mMagnetometer) {
            mLastMagnetometer = lowPass(event.values.clone(), mLastMagnetometer);
            mLastMagnetometerSet = true;
        }
        /*
        * RotationMatrix, Conversion to rotation of the 2-dimensional arrow
       */
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, event.values.clone(), mLastAccelerometer, mLastMagnetometer);
            SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X, SensorManager.AXIS_Z, mR);
            SensorManager.getOrientation(mR, mOrientation);

            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);
            if (mPointer != null) {
                mPointer.startAnimation(ra);
                mCurrentDegree = -azimuthInDegress;
            }
        }

    }

    /**
     * LowPass Filter zum glätten der Sensor Daten
     * @param input
     * @param output
     * @return
     */
    private float[] lowPass(float[] input, float[] output) {
        final float ALPHA = 0.1f;

        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            if (Math.abs(Math.toDegrees(output[i]) - Math.toDegrees(input[i])) > 9)
                output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * Setzt Liste an Routenpunkte für Zielführung
     * @param latLngs
     */
    public void setLatLng(List<LatLng> latLngs) {
        LatLng tmp = latLngs.get(0);
        Location location = new Location(LOCATION_SERVICE);
        location.setLatitude(tmp.latitude);
        location.setLongitude(tmp.longitude);

        locationsList = new ArrayList<>();
        for (int i = 0; i < latLngs.size(); i++) {
            Location nextLocation = new Location(LOCATION_SERVICE);
            nextLocation.setLatitude(latLngs.get(i).latitude);
            nextLocation.setLongitude(latLngs.get(i).longitude);
            splitDistanceToLowerThan10m(location, nextLocation, locationsList);
            location = nextLocation;
        }
        this.countToSelect = locationsList.size();
    }

    /**
     * Initialisiert die Geoobjekte in der 3D Welt
     */
    private void updateLocations() {
        collectedText.setText(0 + "/" + countToSelect);
        for (Location l : locationsList) {

            GeoObject go = new GeoObject(1l);

            if (locationsList.size() - 1 > locationsList.indexOf(l)) go.setImageResource(R.drawable.ic_marker);
            else go.setImageResource(R.drawable.pfeil);

            go.setName("position");
            go.setGeoPosition(l.getLatitude(), l.getLongitude());

            locationGeoObjectHashMap.put(l, go);
            world.addBeyondarObject(go);
        }
        LowPassFilter.ALPHA = 0.09f;
        mBeyondarFragment.setWorld(world);
        progressDialog.dismiss();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Prüft die Entfernung zwischen zwei Punkten und berechnet Einen neuen Punkt in der Mitte,
     * falls die beiden über 20 m entfernt sind
     * @param start
     * @param dest
     * @param result
     */
    private void splitDistanceToLowerThan10m(Location start, Location dest, List<Location> result) {
        if (!result.contains(start)) result.add(start);
        if (!result.contains(dest)) result.add(dest);
        if (start.distanceTo(dest) > 20) {
            Location midLocation = new Location(LOCATION_SERVICE);
            double lat3 = (start.getLatitude() + dest.getLatitude()) / 2;
            double lon3 = (start.getLongitude() + dest.getLongitude()) / 2;
            if (start.getAltitude() != 0 && dest.getAltitude() != 0) {
                midLocation.setAltitude((start.getAltitude() + dest.getAltitude()) / 2);
            }
            midLocation.setLatitude(lat3);
            midLocation.setLongitude(lon3);
            splitDistanceToLowerThan10m(start, midLocation, result);
            splitDistanceToLowerThan10m(midLocation, dest, result);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = gpsFilter.filterGpsData(location.getLatitude(), location.getLongitude(),
                location.getAccuracy(), System.currentTimeMillis());

        Location location1 = new Location(LOCATION_SERVICE);
        location1.setLongitude(latLng.longitude);
        location1.setLatitude(latLng.latitude);
        mLastLocation.setLongitude(latLng.longitude);
        mLastLocation.setLatitude(latLng.latitude);

        if (destination != null && !destination.equals("")) {
            if (!isNavigationInit) startNavigation(destination);

            world.setLocation(location);
            if (locationsList != null) {
                int index = -1;
                for (int i = 0; i < 10 && i < locationsList.size(); i++) {
                    if (location.distanceTo(locationsList.get(i)) < 10) {
                        index = i;
                        break;
                    }
                }
                for (Map.Entry<Location, GeoObject> e : locationGeoObjectHashMap.entrySet()) {
                    e.getValue().setLocation(e.getKey());
                }
                if (index != -1) {
                    for (int i = 0; i <= index; i++) {
                        world.remove(locationGeoObjectHashMap.get(locationsList.get(0)));
                        locationGeoObjectHashMap.remove(locationsList.get(0));
                        locationsList.remove(0);
                        countSelected++;
                        if (locationsList.isEmpty())
                            (findViewById(R.id.destinationText)).setVisibility(View.VISIBLE);
                        break;
                    }
                }
                collectedText.setText(countSelected + "/" + countToSelect);
            }
        }
    }

    /**
     * Fügt Ar Fragment an
     */
    public void addBeyondArFragment() {
        mBeyondarFragment = new BeyondarFragmentSupport();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.fragmentContainer, mBeyondarFragment).commit();

    }

    /**
     * Startet neue Navigationsanfrage
     * @param destination
     */
    public void startNavigation(String destination) {
        isNavigationInit = true;
        LatLng myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        DirectionFetcher directionFetcher = new DirectionFetcher(myLocation, destination, this);
        if (world != null) {
            world.clearWorld();
            locationGeoObjectHashMap.clear();
        }
        directionFetcher.execute();
    }

    /**
     * Zurückkehren zum Startmenü Fragment
     * @param view
     */
    public void backToStartScreen(View view) {
        destination = "";
        FrameLayout fl = (FrameLayout) findViewById(R.id.contentPanel);
        fl.removeView(mPointer);
        collectedText.setVisibility(View.GONE);
        backButton.setVisibility(View.INVISIBLE);
        refreshButton.setVisibility(View.INVISIBLE);
        world.clearWorld();

        if (findViewById(R.id.destinationText).getVisibility() == View.VISIBLE)
            findViewById(R.id.destinationText).setVisibility(View.INVISIBLE);

        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);


        StartScreenFragment startScreenFragment = StartScreenFragment.newInstance(mGoogleApiClient);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.fragmentContainer, startScreenFragment).commit();
    }

    /**
     * LocationListener neu setzten
     * @param view
     */
    public void refreshGps(View view) {
        locationManager.removeUpdates(this);
        initLocationListener();
    }

    /**
     * Zieht letzte Ziele aus Shared Preferences und lässt diese in neuem Fragment anzeigen
     * TODO SharedPreferences Einträge mit Integer und Strings loopen da Sets keine Sortierung kennen
     * @param view
     */
    public void lastTargets(View view){
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        Set<String> targets = prefs.getStringSet(KEY_HISTORY, new HashSet<String>());
        List<String> targetList = new ArrayList<>();
        targetList.addAll(targets);

        HistoryFragment fragment = HistoryFragment.newInstance(targetList);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }

    /**
     * Stellt suchanfrage nach Nutzereingabe
     * @param keyWord
     */
    public void radarSearch(String keyWord){
        if(mLastLocation != null) {
            addBeyondArFragment();
            FrameLayout frame = (FrameLayout) findViewById(R.id.contentPanel);
            frame.addView(mPointer);
            backButton.setVisibility(View.VISIBLE);
            refreshButton.setVisibility(View.VISIBLE);
            new GoogleRadarTask(keyWord, mLastLocation.getLatitude(), mLastLocation.getLongitude(),500, this).execute();
            progressDialog = ProgressDialog.show(this, "Processing",
                    "find places", true);;
        }else {
            Toast.makeText(this, "No Location found", Toast.LENGTH_SHORT).show();
        }
    }

    public void setRadarSearchList(List<GoogleRadarTask.PlaceRadarSearch> resultList){
        this.radarSearchList = resultList;
    }

    /**
     * Verarbeitet die Response der Google Places Api RadarSearch Requests
     * Erstellt für jeden Places Eintrag ein neues Geoobject
     */
    public void processRadarData(){
        world = new World(this);
        world.setLocation(mLastLocation);
        world.setDefaultImage(R.drawable.pfeil);
        locationGeoObjectHashMap.clear();
        locationsList = new ArrayList<>();

        for (GoogleRadarTask.PlaceRadarSearch p : radarSearchList) {
            Location l = new Location(LOCATION_SERVICE);
            l.setLatitude(p.geoLocation.location.lat);
            l.setLongitude(p.geoLocation.location.lng);
            locationsList.add(l);
            String title = p.name + "\n" + p.address + "\n"  + (int)l.distanceTo(mLastLocation) + " m";
            GeoObject go = new GeoObject(1l);
            go.setName(title);
            go.setGeoPosition(l.getLatitude(), l.getLongitude());
            locationGeoObjectHashMap.put(l,go);
            world.addBeyondarObject(go);
        }
        //den Objekte TextOverlays zuweisen
        replaceImagesByStaticViews(world);
        //Smothing der Sensordaten
        LowPassFilter.ALPHA = 0.08f;
        //Nur objekte bis 1500 meter entfernung rendern
        mBeyondarFragment.setMaxDistanceToRender(1000);
        //Alle Objekte sollen zwischen 15 und 20 Meter "entfernt wirken"
        mBeyondarFragment.setPullCloserDistance(20);
        mBeyondarFragment.setPushAwayDistance(20);
        mBeyondarFragment.setWorld(world);
        mBeyondarFragment.setOnClickBeyondarObjectListener(this);
        progressDialog.dismiss();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    /**
     * Erzeugt für jedes Geoobjekt aus einer Textview ein Bitmap Image da das Frame Work keine
     * TextOverlays unterstütz
     * @param world
     */
    private void replaceImagesByStaticViews(World world) {
        String path = getExternalFilesDir(null).getAbsoluteFile() + "/tmp/";

        for (BeyondarObjectList beyondarList : world.getBeyondarObjectLists()) {
            for (BeyondarObject beyondarObject : beyondarList) {

                TextView textView = new TextView(this);

                String[] split = beyondarObject.getName().split("\n");

                textView.setText(split[0] +"\n"+split[2] );
                textView.setTextSize(14);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                textView.setTextColor(getResources().getColor(R.color.material_deep_teal_500));
                textView.setBackgroundColor(getResources().getColor(R.color.dim_foreground_material_dark));
                textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
                textView.setGravity(Gravity.CENTER);


                try{
                    String[] nameSplit = beyondarObject.getName().split("\n");
                    String imageName = "viewImage_"+ nameSplit[0] + ".png";
                    FileOutputStream file = new FileOutputStream(new File(path, imageName));
                    Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    textView.draw(c);

                    b.compress(Bitmap.CompressFormat.PNG, 100, file);
                    file.close();
                    b.recycle();

                    beyondarObject.setImageUri(path+imageName);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * Löschen der zuvor erzeugten Bilder aus dem tmp folder bei onPause
     */
    private void cleanTempFolder() {
        File tmpFolder = new File(getExternalFilesDir(null).getAbsoluteFile() + "/tmp/");
        if (tmpFolder.isDirectory()) {
            String[] children = tmpFolder.list();
            for (int i = 0; i < children.length; i++) {
                if (children[i].startsWith("viewImage_")) {
                    new File(tmpFolder, children[i]).delete();
                }
            }
        }
    }

    /**
     * Click auf GeoObjekt im Radar Modus startet Navigation zum Ort
     * @param beyondarObjects
     */
    @Override
    public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
        if (beyondarObjects.size() > 0) {
            FrameLayout frame = (FrameLayout) findViewById(R.id.contentPanel);
            frame.removeView(mPointer);
            String[] splitString = beyondarObjects.get(0).getName().split("\n");
            newNavigation(splitString[1]);
        }
    }

    /**
     * einfacher Kalman Filter zum Glätten ungenauer Gps Daten
     */
    public class GpsFilter {
        private final float MIN_ACCURACY = 1;
        private float variance = -1;
        private float metres_per_second_walking = 2;
        private long time_inMilliseconds;
        private double lat;
        private double lng;

        /**
         * Methode zum filtern Ungenauer Gps Messungen
         * @param lat_measurement - latitude der letzen Messung
         * @param lng_measurement - longitude der letzen Messung
         * @param accuracy - Genauigkeit der letzten Messung
         * @param time_inMilliseconds - Genauer Zeitpunkt des Erhalts der Messdaten
         * @return
         */
        public LatLng filterGpsData(double lat_measurement, double lng_measurement, float accuracy,
                                    long time_inMilliseconds) {

            if (accuracy < MIN_ACCURACY) accuracy = MIN_ACCURACY;

            //init der Variablen wenn noch nicht gesetzt
            if (variance < 0) {
                this.time_inMilliseconds = time_inMilliseconds;
                lat=lat_measurement;
                lng = lng_measurement;
                variance = accuracy*accuracy;

            } else {
                long deltaTime_inMilliseconds = time_inMilliseconds - this.time_inMilliseconds;

                if (deltaTime_inMilliseconds > 0) {
                    //Berechnung der Varianz zur aktuellen Messung anhand der Konstante m/s /1000 für werte in Millis
                    variance += deltaTime_inMilliseconds * metres_per_second_walking * metres_per_second_walking / 1000;
                    this.time_inMilliseconds = time_inMilliseconds;
                }

                // k wird mit der Genauigkeit der aktuellen Messung und
                // der bekannten Varianz aus allen zuvor stattgefunden Messungen errechnet
                float K = variance / (variance + accuracy * accuracy);

                //smoothing der Location Daten
                lat += K * (lat_measurement - lat);
                lng += K * (lng_measurement - lng);

                //Berechnung der neuen Varianz
                variance = (1 - K) * variance;
            }
            return new LatLng(lat, lng);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onConnectionFailed(ConnectionResult result) { }

    @Override
    public void onConnected(Bundle bundle) { }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}