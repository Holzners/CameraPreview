package com.example.stephan.camerapreview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter zur Darstellung der einzelnen Zeilen Der History Liste
 */
public class HistoryFragmentAdapter extends ArrayAdapter {

    List<String> lastTargets;

    public HistoryFragmentAdapter(Context context, int resource,  List<String>lastTargets) {
        super(context, resource);
        this.lastTargets = lastTargets;
    }

    @Override
    public String getItem(int position){
        return this.lastTargets.get(position);
    }

    @Override
    public int getCount(){
        return lastTargets.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parentGroup){
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View newRow = inflater.inflate(R.layout.history_row, null);

        TextView tv = (TextView) newRow.findViewById(R.id.destinationText);
        tv.setText(getItem(position));

        return newRow;
    }

}
