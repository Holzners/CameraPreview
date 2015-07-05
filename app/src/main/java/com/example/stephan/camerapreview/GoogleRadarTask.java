package com.example.stephan.camerapreview;

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

/**
 * Created by Stephan on 05.07.15.
 */
public class GoogleRadarTask extends AsyncTask {

    private static final String PLACES_API_KEY = "AIzaSyBgduUQoaBJbTUmL9lOlUlaQmHbswuHNSk";
    private static final String PLACES_SEARCH_API = "https://maps.googleapis.com/maps/api/place/radarsearch/json";
    private static final String PLACES_DETAILS_API = "https://maps.googleapis.com/maps/api/place/details/json";
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
                PlaceDetails det = details(p.reference);
                p.name = det.name + "";
                p.address = det.address;
            }

        } catch (IOException ie) {
            Log.e("Error", "Error processing Request results", ie);
        }

        main.processRadarData(resultList);

        return null;
    }

    public static PlaceDetails details(String place_id) {
        try {
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(
                    new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) {
                            request.setParser(new JsonObjectParser(JSON_FACTORY));
                        }
                    }
            );

            GenericUrl url = new GenericUrl(PLACES_DETAILS_API);
            url.put("sensor", false);
            url.put("key", PLACES_API_KEY);
            url.put("reference", place_id);

            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse httpResponse = request.execute();

            PlaceResults details = httpResponse.parseAs(PlaceResults.class);

            return details.result;


        }catch (IOException ie){
            ie.printStackTrace();
            return null;
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

        public String name;

        public String address;

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


    public static class PlaceResults {

        @Key("result")
        public PlaceDetails result;

    }

    public static class PlaceDetails {

        @Key("formatted_address")
        public String address;

        @Key("name")
        public String name;

        @Key("place_id")
        public String place_id;

    }

}
