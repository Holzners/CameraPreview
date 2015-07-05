package com.example.stephan.camerapreview;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beyondar.android.view.BeyondarViewAdapter;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.GeoObject;

import java.util.HashMap;

/**
 * Created by Stephan on 05.07.15.
 */
public class CustomBeyondarViewAdapter extends BeyondarViewAdapter {

    LayoutInflater inflater;
    HashMap<Location, GeoObject> locationGeoObjectHashMap;

    public CustomBeyondarViewAdapter(Context context, HashMap<Location, GeoObject> locationGeoObjectHashMap) {
        super(context);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.locationGeoObjectHashMap = locationGeoObjectHashMap;
    }

    @Override
    public View getView(BeyondarObject beyondarObject, View view, ViewGroup viewGroup) {
        if (!locationGeoObjectHashMap.containsValue(beyondarObject)) {
            return null;
        }
        if (view == null) {
            view = inflater.inflate(R.layout.beyondar_image_text, null);
        }
        TextView textView = (TextView) view.findViewById(R.id.titleText);
        textView.setText(beyondarObject.getName());
        Log.d("DEBUG", beyondarObject.getName());
        return view;
    }
}
