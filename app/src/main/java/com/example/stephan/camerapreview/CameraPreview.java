package com.example.stephan.camerapreview;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.util.location.BeyondarLocationManager;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CameraPreview extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener, LocationListener, android.location.LocationListener,
        TextView.OnEditorActionListener{

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private AutoCompleteTextView destinationText;
    private ImageView mPointer;
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
    int countSelected = 0;
    int countToSelect = 0;

    private boolean isNavigationInit = false;

    private List<Location> locationsList;
    private World world;

    private BeyondarFragmentSupport mBeyondarFragment;
    private HashMap<Location, GeoObject> locationGeoObjectHashMap;
    private TextView collectedText;

    LocationRequest locationRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        mPointer = (ImageView) findViewById(R.id.imageView);
        destinationText = (AutoCompleteTextView) findViewById(R.id.editText);

         buildGoogleApiClient();
        destinationText.setVisibility(View.GONE);
        destinationText.setOnEditorActionListener(this);
        destinationText.setAdapter(new PlacesAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, mGoogleApiClient));

        mBeyondarFragment = (BeyondarFragmentSupport) getSupportFragmentManager().findFragmentById(R.id.beyondarFragment);

        Button button = (Button) findViewById(R.id.navigationButton);
        button.setBackground(this.getResources().getDrawable(R.drawable.navigation_icon));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        world = new World(this);
        world.setLocation(mLastLocation);
        world.setDefaultImage(R.drawable.ic_marker);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10);

        locationGeoObjectHashMap = new HashMap<>();

        collectedText = (TextView) findViewById(R.id.pointText);
        collectedText.setVisibility(View.VISIBLE);

        BeyondarLocationManager.setLocationManager((LocationManager) getSystemService(LOCATION_SERVICE));
        BeyondarLocationManager.addWorldLocationUpdate(world);
        BeyondarLocationManager.addLocationListener(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        BeyondarLocationManager.enable();
        if(mGoogleApiClient.isConnected())startLocationUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        BeyondarLocationManager.disable();
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    public void newNavigation(View view) {

            FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
            frameLayout.removeView(destinationText);
            frameLayout.addView(destinationText);
            destinationText.setVisibility(View.VISIBLE);

    }

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
        } else if(mGoogleApiClient.isConnecting()){
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("GoogleApiConnection", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle bundle) {

        startLocationUpdate();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            Log.d("CameraPreview", (String.valueOf(mLastLocation.getLatitude())));
            Log.d("CameraPreview", (String.valueOf(mLastLocation.getLongitude())));
            world.setLocation(mLastLocation);
            BeyondarLocationManager.disable();
        } else {
            Log.d("CameraPreview", "Could not get Location!");
        }
    }

    private void startLocationUpdate(){
        LocationServices.FusedLocationApi.
                requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        mGoogleApiClient.connect();
    }



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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float[] lowPass(float[] input, float[] output) {
        final float ALPHA = 0.1f;

        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            if (Math.abs(Math.toDegrees(output[i]) - Math.toDegrees(input[i])) > 9)
                output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void setLatLng(List<LatLng> latLngs) {
        LatLng tmp = latLngs.get(0);
        Location location = new Location(LOCATION_SERVICE);
        location.setLatitude(tmp.latitude);
        location.setLongitude(tmp.longitude);

        //location.setAltitude(getElevationFromGoogleMaps(tmp.longitude,tmp.latitude));

       locationsList = new ArrayList<>();
        for (int i = 0; i < latLngs.size(); i++) {
            Location nextLocation = new Location(LOCATION_SERVICE);
            nextLocation.setLatitude(latLngs.get(i).latitude);
            nextLocation.setLongitude(latLngs.get(i).longitude);
            //  nextLocation.setAltitude(getElevationFromGoogleMaps(latLngs.get(i).longitude, latLngs.get(i).latitude));
            splitDistanceToLowerThan10m(location, nextLocation, locationsList);
            location = nextLocation;
        }this.countToSelect = locationsList.size();
        Log.d("Locations:", "up to date");
        updateLocations();
    }

    private void updateLocations(){
        for (int i = 0; i < locationsList.size(); i++) {

            GeoObject go = new GeoObject(1l);

            go.setImageResource(R.drawable.ic_marker);
            go.setName("position");
            go.setGeoPosition(locationsList.get(i).getLatitude(), locationsList.get(i).getLongitude());

            //   allLocationPoints.get(i).getAltitude());
            locationGeoObjectHashMap.put(locationsList.get(i), go);
            world.addBeyondarObject(go);

        }
        mBeyondarFragment.setWorld(world);

    }


    private double getElevationFromGoogleMaps(double longitude, double latitude) {
        double result = Double.NaN;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://maps.googleapis.com/maps/api/elevation/"
                + "xml?locations=" + String.valueOf(latitude)
                + "," + String.valueOf(longitude)
                + "&sensor=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = (Double.parseDouble(value)); // convert from meters to feet
                }
                instream.close();
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        return result;
    }

    private void splitDistanceToLowerThan10m(Location start, Location dest, List<Location> result) {
        if (!result.contains(start)) result.add(start);
        if (!result.contains(dest)) result.add(dest);
        if (start.distanceTo(dest) > 15) {
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
        Log.d("New Location" , location.getLatitude() +" " + location.getLongitude());
        mLastLocation = location;
        if(!isNavigationInit) {
            startNavigation(destinationText.getText().toString());
            isNavigationInit = true;
        }
       if(mGoogleApiClient.isConnected()) world.setLocation(location);
       if(locationsList != null) {
           int index = -1;
           for (int i = 0; i < 10; i++) {
               if (location.distanceTo(locationsList.get(i)) < 10) {
                   index = i;
                   break;
               }
           }
           if (index != -1) {
               for (int i = 0; i <= index; i++) {
                   world.remove(locationGeoObjectHashMap.get(locationsList.get(0)));
                   locationGeoObjectHashMap.remove(locationsList.get(i));
                   locationsList.remove(0);
                   countSelected++;
               }
           }
           collectedText.setText(countSelected + "/" + countToSelect);
       }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        destinationText.setVisibility(View.INVISIBLE);
        if(mLastLocation != null) {
            startNavigation(destinationText.getText().toString());

            isNavigationInit = true;
            return true;
        }
        return false;
    }

    public void startNavigation(String destination){
        LatLng myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        DirectionFetcher directionFetcher = new DirectionFetcher(myLocation,destination, this);
        if (world != null) {
            world.clearWorld();
            locationGeoObjectHashMap.clear();
        }
        directionFetcher.execute();
    }
}