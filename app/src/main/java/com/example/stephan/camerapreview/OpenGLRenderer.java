package com.example.stephan.camerapreview;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements Renderer {

	private Path path;

	protected static float[] rotationMatrix;
	protected static float[] orientation;

	private float[] mViewMatrix = new float[16];

	float eyeX = 0.0f;
	float eyeY = 0.0f;
	float eyeZ = 1.5f;
	float lookX = 0.0f;
	float lookY = 0.0f;
	float lookZ = -5.0f;

	private float[] mModelMatrix = new float[16];
	float upX = 0.0f;
	float upY = 1.0f;
	float upZ = 0.0f;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition
	 * .khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
	 */
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background color to black ( rgba ).


		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
				GL10.GL_FASTEST);

		gl.glClearColor(0, 0, 0, 0);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.
	 * khronos.opengles.GL10)
	 */
	public void onDrawFrame(GL10 gl) {

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();





			//GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f, (float) Math.cos(betaX), (float) Math.cos(betaY), (float) Math.cos(betaZ), 0.0f, 1.0f, 0.0f);
			GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);


    if(path != null)path.draw(gl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition
	 * .khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// Sets the current view port to the new size.
		gl.glViewport(0, 0, width, height);
		// Select the projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		// Reset the projection matrix
		gl.glLoadIdentity();
		// Calculate the aspect ratio of the window
		GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f,
                100.0f);
		// Select the modelview matrix
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		// Reset the modelview matrix
		gl.glLoadIdentity();
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public void transformLngLatToCoordinate(List<LatLng> latLngs, Location myLocation){
        //MyLocation == x = 0.0f ; y = 0.0f ; z = 1.0f;
        List<Float> verticesList = new ArrayList<>();
        float azimuth =0;
        float pitch = 0;
        float roll = 0;

        //Blickrichtung
        if(orientation != null){
            azimuth = (float)(Math.toDegrees(orientation[0]));
            pitch = (float)(Math.toDegrees(orientation[1]));
            roll = (float)(Math.toDegrees(orientation[2]));

             Log.d("Azimuth", azimuth+"");
             Log.d("Pitch", pitch+"");
              Log.d("Roll", roll + "");

        }
        float baseAzimuth = azimuth;

        for(LatLng l : latLngs) {
            GeomagneticField geomagneticField = new GeomagneticField(
                    Double.valueOf(myLocation.getLatitude()).floatValue(),
                    Double.valueOf(myLocation.getLongitude()).floatValue(),
                    Double.valueOf(1.0).floatValue(),
                    System.currentTimeMillis());

            azimuth -= geomagneticField.getDeclination();
            Location destLoc = new Location(LocationManager.GPS_PROVIDER);
            destLoc.setLatitude(l.latitude);
            destLoc.setLongitude(l.longitude);

            float bearTo = myLocation.bearingTo(destLoc);
            if (bearTo < 0){
                bearTo += 360;
            }

            double latDelta = myLocation.getLatitude() - l.latitude;
            double lngDelta = myLocation.getLongitude() - l.longitude;

            double yTmp = Math.sin(lngDelta)  *  Math.cos(l.latitude);
            double xTmp =  Math.cos( myLocation.getLatitude()) *  Math.sin(l.latitude) -
                    Math.sin(myLocation.getLatitude()) *  Math.cos(l.latitude)*  Math.cos(lngDelta);

            double angle = Math.atan2(xTmp,yTmp);
            double angleDeg = angle * 180/Math.PI;
            double heading = azimuth*Math.PI/180;
            angle = Math.IEEEremainder(angleDeg + 360, 360) * Math.PI/180; //normalize to 0 to 360 (instead of -180 to 180), then convert back to radians
            angleDeg = angle * 180/Math.PI;

            float direction = bearTo - azimuth;

            double distance = myLocation.distanceTo(destLoc);
            float x =(float)( Math.sin(direction) * distance);
            float z =(float)( Math.cos(direction) * distance);

            verticesList.add(x);
            verticesList.add(0.5f);
            verticesList.add(z);

        }


        float [] vertices = new float[verticesList.size()];
        for(int i = 0 ; i < vertices.length ; i++){
            vertices[i] = verticesList.get(i);
        }
        setPath(new Path(vertices));
    }

}
