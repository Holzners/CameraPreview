package com.example.stephan.camerapreview;


import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.List;


public class HistoryFragment extends ListFragment {
    List<String>targets;

    public static HistoryFragment newInstance(List<String>targets) {
        HistoryFragment fragment = new HistoryFragment();
        fragment.targets = targets;
        return fragment;
    }

    public HistoryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.history_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        HistoryFragmentAdapter adapter = new HistoryFragmentAdapter(getActivity(), 0, targets);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(new HistoryOnItemClickListener());

    }


    public class HistoryOnItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            view.setSelected(true);
            ((CameraPreview)getActivity()).newNavigation(targets.get(position));
        }
    }

}
