package com.example.stephan.camerapreview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;


public class StartScreenFragment extends Fragment{

    private AutoCompleteTextView destinationText;
    private GoogleApiClient mGoogleApiClient;


    public static StartScreenFragment newInstance(GoogleApiClient mGoogleApiClient) {
        StartScreenFragment fragment = new StartScreenFragment();
        fragment.mGoogleApiClient = mGoogleApiClient;
        return fragment;
    }

    public StartScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_screen, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        destinationText = (AutoCompleteTextView) getView().findViewById(R.id.editText);
        destinationText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });
        // Google Places Helper als Adapter setzten zur Autocomplete
        destinationText.setAdapter(new AutoComplete(getActivity(), android.R.layout.simple_dropdown_item_1line, mGoogleApiClient));
        Button button = (Button) getView().findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickStart();
            }
        });
        Button radarSearch = (Button) getView().findViewById(R.id.radarSearch);
        radarSearch.setOnClickListener(new View.OnClickListener() {
           // Startet RadarSearch der Google Places Api
            @Override
            public void onClick(View v) {
                if(!destinationText.getText().toString().equals("")){
                    ((CameraPreview)getActivity()).radarSearch(destinationText.getText().toString());
                }else{
                    Toast.makeText(getActivity(), "Destination can not be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Neue Navigation wird gestartet falls Edittext nicht leer
     */
    public void onClickStart(){
        if(!destinationText.getText().toString().equals("")){
            ((CameraPreview)getActivity()).newNavigation(destinationText.getText().toString());
        }else{
            Toast.makeText(getActivity(), "Destination can not be empty", Toast.LENGTH_SHORT).show();
        }
    }

}
