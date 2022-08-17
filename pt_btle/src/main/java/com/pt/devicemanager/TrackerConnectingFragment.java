package com.pt.devicemanager;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackerConnectingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackerConnectingFragment extends Fragment implements TrackerServiceListener {
    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "tracker_connecting";

    private TrackerService.TrackerBinder mBinder = null;
    TextView tvConnecting;

    public TrackerConnectingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment PrivacyFragment.
     */

    public static TrackerConnectingFragment newInstance() {
        TrackerConnectingFragment fragment = new TrackerConnectingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TrackerConnectingFragment init(TrackerService.TrackerBinder binder)
    {
        mBinder = binder;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "TCF: onCreate:"+this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracker_connecting, container, false);
        tvConnecting = (TextView) view.findViewById(R.id.tracker_connecting);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "TCF: onResume:"+this);

        if (getBinder() != null) {
            if (AppModel.MODE_USB) {
                if (getBinder().getTracker().getUsbDevice() != null)
                    tvConnecting.setText("Connected to " + getBinder().getTracker().getUsbDevice().getDeviceName());
                else
                    tvConnecting.setText("Connected to UNKNOWN!"  );
            } else {
                if (getBinder().getTracker().getBluetoothDevice() != null)
                    tvConnecting.setText("Waiting for " + getBinder().getTracker().getBluetoothDevice().getName() + "...");
                else
                    tvConnecting.setText("Connected to UNKNOWN!"  );
            }
        } else {
            //Log.e(TAG, "TCF: >>>>>>>>>>>>> Binder null");
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "TCF: onPause:"+this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TCF: onDestroy:"+this);
    }

    @Override
    public void onServiceBound(TrackerService.TrackerBinder binder) {
        // note: Binder may not be present, if this fragment was added in response to a broadcast from the existing service
        // i.e. not during the init time
        mBinder = binder;
    }

    @Override
    public void onServiceUnbound() {
    }

    TrackerService.TrackerBinder getBinder() {
        if (mBinder != null) {
            return mBinder;
        } else {
            //Toast.makeText(getContext(), "TCF:TNB!", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
