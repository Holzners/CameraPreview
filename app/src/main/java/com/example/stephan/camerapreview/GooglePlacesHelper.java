package com.example.stephan.camerapreview;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLngBounds;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stephan on 15.06.15.
 *
 * Adapter Klasse füllt ListView:
 * verbindet mit Google Places und gibt Auto Complete Vorschläge für Places
 */
public class GooglePlacesHelper extends ArrayAdapter<String> implements Filterable {

    public static final String PLACES_API_KEY = "AIzaSyBgduUQoaBJbTUmL9lOlUlaQmHbswuHNSk";

    private static final String PLACES_AUTOCOMPLETE_API = "https://maps.googleapis.com/maps/api/place/autocomplete/json";

    private GoogleApiClient mGoogleApiClient;

    private ArrayList<String> resultList;
    static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private LatLngBounds mBounds;

    public GooglePlacesHelper(Context context, int resource, GoogleApiClient mGoogleApiClient) {
        super(context, resource);
        this.mGoogleApiClient = mGoogleApiClient;
        Log.d("Adapter", "Adapter init");

    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the autocomplete results.
                    resultList = autocomplete(constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
    private ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = new ArrayList<String>();

        try {

            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(
                    new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) {
                            request.setParser(new JsonObjectParser(JSON_FACTORY));
                        }
                    }
            );

            GenericUrl url = new GenericUrl(PLACES_AUTOCOMPLETE_API);
            url.put("input", input);
            url.put("key", PLACES_API_KEY);
            url.put("sensor", false);

            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse httpResponse = request.execute();
            PlacesResult directionsResult = httpResponse.parseAs(PlacesResult.class);

            List<Prediction> predictions = directionsResult.predictions;
            for (Prediction prediction : predictions) {
                resultList.add(prediction.description);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultList;
    }


    public static class PlacesResult {

        @Key("predictions")
        public List<Prediction> predictions;

    }

    public static class Prediction {

        @Key("description")
        public String description;

        @Key("id")
        public String id;

    }



}