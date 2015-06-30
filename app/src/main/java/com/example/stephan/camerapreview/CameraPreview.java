package com.example.stephan.camerapreview;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import java.util.List;


public class CameraPreview extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        TextureView.SurfaceTextureListener, SensorEventListener {

    private Camera camera = null;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationManager mLocationManager;
    private TextureView mTextureView;
    private AutoCompleteTextView destinationText;
    private String provider;
    private ImageView mPointer;
    private SensorManager mSensorManager;
    private LocationListener mLocationListener;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    private boolean isNewNavigationTextUp = false;

    //private OpenGLRenderer renderer;

    private List<Location> locationsList;
    World world;

    BeyondarFragmentSupport mBeyondarFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        mPointer = (ImageView) findViewById(R.id.imageView);
        mTextureView = (TextureView) findViewById(R.id.preview);
        // mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        destinationText = (AutoCompleteTextView) findViewById(R.id.editText);

        buildGoogleApiClient();
        destinationText.setVisibility(View.GONE);

        destinationText.setAdapter(new PlacesAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, mGoogleApiClient));

        mTextureView.setSurfaceTextureListener(this);

        mBeyondarFragment = (BeyondarFragmentSupport) getSupportFragmentManager().findFragmentById(R.id.beyondarFragment);

        Button button = (Button) findViewById(R.id.navigationButton);
        button.setBackground(this.getResources().getDrawable(R.drawable.navigation_icon));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Criteria criteria = new Criteria();
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        provider = mLocationManager.getBestProvider(criteria, false);
        //mLastLocation = mLocationManager.getLastKnownLocation(provider);
        mLocationListener = new CPLocationListener();
        world = new World(this);
        world.setLocation(mLastLocation);
        world.setDefaultImage(R.drawable.ic_marker);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        mLocationManager.requestLocationUpdates(provider, 40000, 1, mLocationListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        mLocationManager.requestLocationUpdates(provider, 40000, 1, mLocationListener);
        camera.release();
    }


    public void newNavigation(View view) {
        if (!isNewNavigationTextUp) {
            FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
            frameLayout.removeView(destinationText);
            frameLayout.addView(destinationText);
            destinationText.setVisibility(View.VISIBLE);
            isNewNavigationTextUp = true;
        } else {

            LatLng myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            DirectionFetcher directionFetcher = new DirectionFetcher(myLocation, destinationText.getText().toString(), this);

            destinationText.setVisibility(View.GONE);
            isNewNavigationTextUp = false;
            directionFetcher.execute();
        }
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

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            Log.d("CameraPreview", (String.valueOf(mLastLocation.getLatitude())));
            Log.d("CameraPreview", (String.valueOf(mLastLocation.getLongitude())));
            world.setLocation(mLastLocation);

        } else {
            Log.d("CameraPreview", "Could not get Location!");
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        mGoogleApiClient.connect();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        camera = Camera.open();

        //initPreview(surface, width , height);
        //    camera.startPreview();

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {

        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {

            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    private void initPreview(SurfaceTexture surface, int width, int height) {
        try {
            camera.setPreviewTexture(surface);
        } catch (Throwable t) {
            Log.e("CameraManager", "Exception in setPreviewTexture()", t);
        }

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size previewSize = getBestPreviewSize(width, height, parameters);

        float ratioSurface = width > height ? (float) width / height : (float) height / width;
        float ratioPreview = (float) previewSize.width / previewSize.height;

        int scaledHeight = 0;
        int scaledWidth = 0;
        float scaleX = 1f;
        float scaleY = 1f;

        Display display = getWindowManager().getDefaultDisplay();

        boolean isPortrait = false;

        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) {
                camera.setDisplayOrientation(display.getRotation() == Surface.ROTATION_0 ? 90 : 270);
                isPortrait = true;
            } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
                camera.setDisplayOrientation(display.getRotation() == Surface.ROTATION_90 ? 0 : 180);
                isPortrait = false;
            }
            if (isPortrait && ratioPreview > ratioSurface) {
                scaledHeight = (int) (((float) previewSize.width / previewSize.height) * width);
                scaleX = 1f;
                scaleY = (float) scaledHeight / height;
            } else if (isPortrait && ratioPreview < ratioSurface) {
                scaledWidth = (int) (height / ((float) previewSize.width / previewSize.height));
                scaleX = (float) scaledWidth / width;
                scaleY = 1f;
            } else if (!isPortrait && ratioPreview < ratioSurface) {
                scaledHeight = (int) (width / ((float) previewSize.width / previewSize.height));
                scaleX = 1f;
                scaleY = (float) scaledHeight / height;
            } else if (!isPortrait && ratioPreview > ratioSurface) {
                scaledWidth = (int) (((float) previewSize.width / previewSize.height) * width);
                scaleX = (float) scaledWidth / width;
                scaleY = 1f;
            }
            camera.setParameters(parameters);
        }

        // calculate transformation matrix
        Matrix matrix = new Matrix();

        matrix.setScale(scaleX, scaleY);
        mTextureView.setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.stopPreview();
        camera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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
        }
        updateLocations();

    }

    private void updateLocations(){
        for (int i = 0; i < locationsList.size() && i < 12; i++) {

            GeoObject go = new GeoObject(1l);
            go.setImageResource(R.drawable.ic_marker);
            go.setName("position");
            go.setGeoPosition(locationsList.get(i).getLatitude(), locationsList.get(i).getLongitude());
            //   allLocationPoints.get(i).getAltitude());

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
        if (start.distanceTo(dest) > 10) {
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

    public class CPLocationListener implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {
            Log.d("New Location" , location.getLatitude() +" " + location.getLongitude());
            mLastLocation = location;
            world.setLocation(mLastLocation);

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
    }
}