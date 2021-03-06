package com.example.stephan.camerapreview;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.maps.android.PolyUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stephan on 15.06.15.
 */
public class DirectionFetcher extends AsyncTask<URL, Integer, String> {

    private List<LatLng> latLngs = new ArrayList<LatLng>();

    static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static final String HTTP_REQUEST ="http://maps.googleapis.com/maps/api/directions/json";

    private LatLng origin;
    private String destination;
    private CameraPreview preview;

    public DirectionFetcher(LatLng origin , String destination, CameraPreview preview ) {
        this.destination = destination;
        this.origin = origin;
        this.preview = preview;
    }


    protected String doInBackground(URL... urls) {
        try {
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
            });

            GenericUrl url = new GenericUrl(HTTP_REQUEST);
            url.put("origin", origin.latitude + "," + origin.longitude);
            url.put("destination", destination);
            url.put("sensor",false);
            url.put("mode", "walking");

            HttpRequest request = requestFactory.buildGetRequest(url);

            HttpResponse httpResponse = request.execute();
            DirectionsResult directionsResult = httpResponse.parseAs(DirectionsResult.class);
                String encodedPoints = directionsResult.routes.get(0).overviewPolyLine.points;
            latLngs = PolyUtil.decode(encodedPoints);

            Location myLoc= new Location(LocationManager.GPS_PROVIDER);
            myLoc.setLongitude(origin.longitude);
            myLoc.setLatitude(origin.latitude);

            preview.setLatLng(latLngs);

            for(LatLng ll :latLngs){
                Log.d("Point" , ll.latitude + " " +ll.longitude);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static class DirectionsResult {
        @Key("routes")
        public List<Route> routes;
    }

    public static class Route {
        @Key("overview_polyline")
        public OverviewPolyLine overviewPolyLine;
    }

    public static class OverviewPolyLine {
        @Key("points")
        public String points;
    }

}
