package com.example.stephan.camerapreview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;


public class StartScreenFragment extends Fragment implements TextView.OnEditorActionListener{

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
        destinationText.setOnEditorActionListener(this);
        destinationText.setAdapter(new PlacesAutoCompleteAdapter(getActivity(), android.R.layout.simple_dropdown_item_1line, mGoogleApiClient));
        Button button = (Button) getView().findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickStart();
            }
        });
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {

            return true;
        }
        return false;
    }

    public void onClickStart(){
        if(destinationText.getText().toString() != ""){
            ((CameraPreview)getActivity()).newNavigation(destinationText.getText().toString());
        }else{
            Toast.makeText(getActivity(), "Destination can not be empty", Toast.LENGTH_SHORT).show();
        }
    }

}
