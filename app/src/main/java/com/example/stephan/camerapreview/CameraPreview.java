package com.example.stephan.camerapreview;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


public class CameraPreview extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                                                    TextureView.SurfaceTextureListener{

    private Camera camera=null;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private TextureView mTextureView;
    private AutoCompleteTextView destinationText;
    private GLSurfaceView mGLSurfaceView;
    private float vertices[]  = {
                -1.0f,  1.0f, -1.0f,  //
                -1.0f, -1.0f, 1.0f,  //
                1.0f, 2.0f, 2.0f,  //
    };
    private boolean isNewNavigationTextUp = false;

    private OpenGLRenderer renderer;
   // private SensorManager sensorManager;
   // private float[] rotationMatrix;
   // private Sensor accSensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        buildGoogleApiClient();

        destinationText = (AutoCompleteTextView)findViewById(R.id.editText);
        //destinationText.setVisibility(View.GONE);

        destinationText.setAdapter(new PlacesAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, mGoogleApiClient));

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        frameLayout.addView(mTextureView);
        Button button = (Button) findViewById(R.id.navigationButton);
        button.setBackground(this.getResources().getDrawable(R.drawable.navigation_icon));


        this.renderer = new OpenGLRenderer();
        renderer.setPath(new Path(vertices));

        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLSurfaceView.setRenderer(renderer);
        mGLSurfaceView.setZOrderOnTop(true);
        frameLayout.addView(mGLSurfaceView);
        frameLayout.removeView(button);
        frameLayout.addView(button);

    }

    public void newNavigation(View view){
        if(!isNewNavigationTextUp) {
            FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
            frameLayout.removeView(destinationText);
            frameLayout.addView(destinationText);
            destinationText.setVisibility(View.VISIBLE);
            isNewNavigationTextUp = true;
        }else{
            String myLocation = "Frauenkirche, München";
            DirectionFetcher directionFetcher = new DirectionFetcher(myLocation, destinationText.getText().toString());
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
        }else{
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
        camera = Camera.open();

        initPreview(surface, width , height);
        camera.startPreview();

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {

        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {

            // printing for debugging
            System.out.println("width: "+size.width+", height: "+size.height+"\n");

            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
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
                scaledWidth = width;
                scaledHeight = (int) (((float) previewSize.width / previewSize.height) * width);
                scaleX = 1f;
                scaleY = (float) scaledHeight / height;
            } else if (isPortrait && ratioPreview < ratioSurface) {
                scaledWidth = (int) (height / ((float) previewSize.width / previewSize.height));
                scaledHeight = height;
                scaleX = (float) scaledWidth / width;
                scaleY = 1f;
            } else if (!isPortrait && ratioPreview < ratioSurface) {
                scaledWidth = width;
                scaledHeight = (int) (width / ((float) previewSize.width / previewSize.height));
                scaleX = 1f;
                scaleY = (float) scaledHeight / height;
            } else if (!isPortrait && ratioPreview > ratioSurface) {
                scaledWidth = (int) (((float) previewSize.width / previewSize.height) * width);
                scaledHeight = height;
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

}