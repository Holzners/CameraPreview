package com.example.stephan.camerapreview;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements Renderer {

	private Path path;

	protected static float[] rotationMatrix;
	protected static float[] orientation;

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
        gl.glTranslatef(0, 0, -10);

        if(orientation!= null) {
            float pi = (float) Math.PI;
            float rad2deg = 180 / pi;

            if (rotationMatrix != null) gl.glLoadMatrixf(rotationMatrix, 0);
            gl.glPushMatrix();
            // Get the pitch, yaw and roll from the sensor.

            float yaw = orientation[0] * rad2deg;
            float pitch = orientation[1] * rad2deg;
            float roll = orientation[2] * rad2deg;

            // Convert pitch, yaw and roll to a vector

            float x = (float) (Math.cos(yaw) * Math.cos(pitch));
            float y = (float) (Math.sin(yaw) * Math.cos(pitch));
            float z = (float) (Math.sin(pitch));

            Log.d("Yaw", yaw + "");
            Log.d("Roll", roll + "");
            Log.d("Pitch", pitch + "");

            GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);
            gl.glRotatef(yaw, 1.0f, 0.0f, 0.0f);
            gl.glRotatef(roll, 0.0f, 1.0f, 0.0f);
            gl.glRotatef(pitch, 0.0f, 0.0f, 1.0f);
            gl.glPushMatrix();
        }

		path.draw(gl);
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

	public float[] transformLatLngToOpenGL(LatLng latLng){
      //  X = (lat / 1) * ( (min(w,h) / 2) / tan(α) ) and Y = (lng / 1) * ( (min(w,h) / 2) / tan(α)

        return null;
	}

}
