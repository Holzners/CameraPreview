package com.example.stephan.camerapreview;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

/**
 * Created by Stephan on 05.07.15.
 */
public class GoogleRadarTask extends AsyncTask {

    private static final String PLACES_API_KEY = "AIzaSyBgduUQoaBJbTUmL9lOlUlaQmHbswuHNSk";
    private static final String PLACES_SEARCH_API = "https://maps.googleapis.com/maps/api/place/radarsearch/json";
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String keyword;
    private double lat, lng;
    private int radius;
    private CameraPreview main;

    public GoogleRadarTask(String keyWord, double lat, double lng , int radius, CameraPreview main){
        this.keyword = keyWord;
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.main = main;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        List<PlaceRadarSearch> resultList = null;
        try {
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(
                    new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) {
                            request.setParser(new JsonObjectParser(JSON_FACTORY));
                        }
                    }
            );

            GenericUrl url = new GenericUrl(PLACES_SEARCH_API);
            url.put("sensor", false);
            url.put("key", PLACES_API_KEY);
            url.put("location", String.valueOf(lat) + "," + String.valueOf(lng));
            url.put("radius", String.valueOf(radius));
            url.put("types", URLEncoder.encode(keyword, "utf8"));


            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse httpResponse = request.execute();
            PlaceRadarSearchList placeRadarSearchList = httpResponse.parseAs(PlaceRadarSearchList.class);
            resultList = placeRadarSearchList.placeRadarSearchesList;
            for(PlaceRadarSearch p : resultList){
                Log.d("Place Loc", p.geoLocation.location.lat +" " + p.geoLocation.location.lng);
                getAddress(p.geoLocation.location.lat, p.geoLocation.location.lng);
            }

        } catch (IOException ie) {
            Log.e("Error", "Error processing Request results", ie);
        }



        return null;
    }

    public void getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(main, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getLocality();

            Log.v("IGA", "Address" + add);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static class PlaceRadarSearchList {

        @Key("results")
        public List<PlaceRadarSearch> placeRadarSearchesList;

    }


    public static class PlaceRadarSearch {

        @Key("geometry")
        public GeoLocation geoLocation;

        @Key("place_id")
        public String place_id;

        @Key("reference")
        public String reference;
    }

    public static class GeoLocation {

        @Key("location")
        public GeoLatLng location;

    }

    public static class GeoLatLng {

        @Key("lat")
        public double lat;

        @Key("lng")
        public double lng;

    }

}
