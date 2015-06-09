package com.example.stephan.camerapreview;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;


public class CameraPreview extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                                                    TextureView.SurfaceTextureListener{

    private Camera camera=null;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private TextureView mTextureView;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera_preview);

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

        Camera.Parameters parameters=camera.getParameters();
        Camera.Size previewSize = getBestPreviewSize(width, height,
                parameters);

        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(
                previewSize.width, previewSize.height, Gravity.CENTER));

        try {
            camera.setPreviewTexture(surface);
        } catch (IOException t) {
            t.printStackTrace();
        }

        camera.startPreview();

    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
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