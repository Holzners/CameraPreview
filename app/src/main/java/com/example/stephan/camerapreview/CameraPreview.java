package com.example.stephan.camerapreview;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText destinationText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        destinationText = (EditText)findViewById(R.id.editText);
        destinationText.setVisibility(View.GONE);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        frameLayout.addView(mTextureView);
        Button button = (Button) findViewById(R.id.navigationButton);
        button.setBackground(this.getResources().getDrawable(R.drawable.navigation_icon));
        frameLayout.removeView(button);
        frameLayout.addView(button);

        buildGoogleApiClient();
    }

    public void newNavigation(View view){

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.contentPanel);
        frameLayout.removeView(destinationText);
        frameLayout.addView(destinationText);
        destinationText.setVisibility(View.VISIBLE);
    }

    public Location getLocation(){
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

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