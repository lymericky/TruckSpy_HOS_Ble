package com.pt.devicemanager;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pt.sdk.BaseResponse;
import com.pt.sdk.DateTimeParam;
import com.pt.sdk.GeolocParam;
import com.pt.sdk.SPNEventDefinitionParam;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManager;
import com.pt.sdk.TrackerModel;
import com.pt.sdk.VehicleDiagTroubleCode;
import com.pt.sdk.request.ClearDiagTroubleCodes;
import com.pt.sdk.request.ClearStoredEvents;
import com.pt.sdk.request.ConfigureSPNEvent;
import com.pt.sdk.request.GetDiagTroubleCodes;
import com.pt.sdk.request.GetStoredEventsCount;
import com.pt.sdk.request.GetTrackerInfo;
import com.pt.sdk.request.GetVehicleInfo;
import com.pt.sdk.request.RetrieveStoredEvents;
import com.pt.sdk.response.outbound.AckEvent;
import com.pt.sdk.response.outbound.AckStoredEvent;
import com.pt.ws.TrackerInfo;
import com.pt.ws.VehicleInfo;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackerViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackerViewFragment extends Fragment implements TrackerServiceListener {
    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "tracker_view";
    protected static final int REQUEST_SELECT_CTRL_FW = 3;
    protected static final int REQUEST_SELECT_BLE_FW = 4;
    protected static final int REQUEST_READ_STORAGE_PERMISSION = 5;

    private final Handler mHandler = new Handler();
    private TrackerService.TrackerBinder mBinder = null;

    public static String getMacAddr() {
        return MAC_ADDR;
    }
    public static void setMacAddr(String macAddr) {
        MAC_ADDR = macAddr;
    }
    public static String MAC_ADDR;
    public static String getVehIdent() {
        return VEH_IDENT;
    }
    public static void setVehIdent(String vehIdent) {
        VEH_IDENT = vehIdent;
    }
    public static String VEH_IDENT;

    // Device tile
    TextView tvModel;
    TextView tvSerial;
    TextView tvMac;
    TextView tvVersion;
    TextView tvVin;
    TextView tvRssi; // Received Signal Strength
    TextView tvUpTime;

    // Stored Events tile
    View vSETile;
    TextView tvSEventCount;
    TextView tvSESeq;
    TextView tvSEvent;
    Boolean requestedSEvents = false;

    // Telemetery tile
    TextView tvEvent;
    TextView tvSeq;
    TextView tvDateTime;
    TextView tvGeoloc;
    TextView tvGeolocExtra;
    TextView tvSatStatus;
    TextView tvOdo;
    TextView tvVelo;
    TextView tvEh;
    TextView tvRpm;
    ExtendedFloatingActionButton hos_fab_btn;


    String vin;

    Switch swOdoUnit;   // Default (off) = Km

    final IntentFilter tmIf = new IntentFilter();
    final IntentFilter trackerIf = new IntentFilter();
    final IntentFilter tupIf = new IntentFilter();
    final IntentFilter dtcIf = new IntentFilter();
    final IntentFilter tviIf = new IntentFilter();
    final IntentFilter seIf = new IntentFilter();
    final IntentFilter spnIf = new IntentFilter();

    BroadcastReceiver tmRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTelemetryInfo();
        }
    };

    BroadcastReceiver tiRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTrackerInfo();
        }
    };

    BroadcastReceiver tupRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer action = intent.getIntExtra(TrackerService.EXTRA_RESP_ACTION_KEY, 0);

            switch (action) {
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_UPDATED:
                    Toast.makeText(getContext(), "Tracker was successfully updated.", Toast.LENGTH_SHORT).show();
                    break;
                default: // FAILED
            }
        }
    };

    BroadcastReceiver dtcRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(TrackerService.EXTRA_RESP_ACTION_KEY);

            if (action.equals("GET")) {
                VehicleDiagTroubleCode dtc = AppModel.getInstance().mLastDTC;
                StringBuilder sb = new StringBuilder();
                sb.append("Malfunction Indicator:").append(dtc.mDtc.mil).append("\n")
                        .append("Bus:").append(dtc.mDtc.busType.name()).append("\n");
                if (dtc.mDtc.codes.size() != 0) {
                    for (String code: dtc.mDtc.codes) {
                        sb.append(code).append(",");
                    }
                    // remove the trailing comma
                    int sz = sb.length();
                    sb.deleteCharAt(sz-1);
                } else {
                    sb.append("No codes.");
                }

                Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), sb.toString(), Snackbar.LENGTH_LONG);
                TextView textView = (TextView) snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setMaxLines(4);  
                snackbar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbSuccess));
                snackbar.show();

            } else if (action.equals("CLEAR")) {
                Integer status = intent.getIntExtra(TrackerService.EXTRA_RESP_STATUS_KEY, 0);
                if (status == 0) {
                    Toast.makeText(context, "DTC cleared!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "DTC clear failed!", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    BroadcastReceiver viRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVehicleInfo();
        }
    };

    BroadcastReceiver spnRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            StringBuilder sb = new StringBuilder();
            Integer fl = Math.round(AppModel.getInstance().mLastSPNEv.value * 0.4f);
            sb.append("Tank 1 level: ").append(fl).append("%");

            Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), sb.toString(), Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbSuccess));
            snackbar.show();
        }
    };

    void updateTrackerInfo() {

        if (getBinder() != null) {
            tvMac.setText(getBinder().getDeviceAddress());
            setMacAddr(getBinder().getDeviceAddress());
        }

        if (AppModel.getInstance().mTrackerInfo != null) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

            if (ti.product != null) {
                tvModel.setText(ti.product);
            } else {
                tvModel.setText("Generic");
            }
            tvSerial.setText(ti.SN);
            String ver = "F/W:" + ti.mvi.toString() + "  BLE:" + ti.bvi.toString();
            tvVersion.setText(ver);

            if (ti.product.contains("30")) {                                                        // -- if using PT-30 device
                tvVin.setText(AppModel.getInstance().mPT30Vin);
                getActivity().invalidateOptionsMenu();
            }
        } else if (getBinder() != null) {
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }

        tvUpTime.setText(upTimeToString());
    }

    void updateVehicleInfo() {

        if (AppModel.getInstance().mVehicleInfo != null) {
            VehicleInfo vi = AppModel.getInstance().mVehicleInfo;
            tvVin.setText(vi.VIN);
            setVehIdent(vi.VIN);

        }
    }

    BroadcastReceiver seRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            updateSE();
        }
    };

    void updateSE()
    {
        if (AppModel.getInstance().mLastSECount == 0) {
            vSETile.setVisibility(View.GONE);
        } else {
            vSETile.setVisibility(View.VISIBLE);
            tvSEventCount.setText(AppModel.getInstance().mLastSECount.toString());
        }

        // Update Event
        if (AppModel.getInstance().mLastSEvent != null) {
            tvSESeq.setText(AppModel.getInstance().mLastSEvent.mSeq.toString());
            tvSEvent.setText(AppModel.getInstance().mLastSEvent.mEvent.toString());
        }
    }

    void getStoredEventsCount()
    {
        // Get Stored Events count
        Log.i(TAG, "Get Stored Events count ...");
        GetStoredEventsCount gsec = new GetStoredEventsCount();
        if (getBinder() != null) {
            getBinder().getTracker().sendRequest(gsec, null, null);
        }
    }

    void ackStoredEvent(TelemetryEvent tm)
    {
        // Ack
        GeolocParam geoloc = tm.mGeoloc;
        DateTimeParam dt = tm.mDateTime;
        StringBuilder params = new StringBuilder();
        String id = "";
        params.append("id=").append(id)
                .append("&lat=").append(geoloc.latitude.toString())
                .append("&lon=").append(geoloc.longitude.toString())
                .append("&sat=").append(geoloc.satCount)
                .append("&speed=").append(geoloc.speed)
                .append("&head=").append(geoloc.heading)
                .append("&date=").append(dt.date)
                .append("&time=").append(dt.time);

        // Do something
        Log.i(TAG, "ack EVENT:" + tm.mEvent.toString() + ":" + tm.mSeq);

        // ACK the event
        AckStoredEvent ack = new AckStoredEvent(0, tm.mSeq.toString(), dt.toDateString());
        getBinder().getTracker().sendResponse(ack, null, null);

        // Refresh count
        getStoredEventsCount();
    }


    String upTimeToString()
    {
        long et = (System.currentTimeMillis() - AppModel.getInstance().mConnectTime);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long ed = et / daysInMilli;
        et = et % daysInMilli;

        long eh = et / hoursInMilli;
        et = et % hoursInMilli;

        long em = et / minutesInMilli;
        et = et % minutesInMilli;

        long elapsedSeconds = et / secondsInMilli;

        StringBuffer sb = new StringBuffer().append(em+" mins");
        if (eh > 0) {
            sb.insert(0, eh+ " hrs,");
        }

        if (ed > 0) {
           sb.insert(0, ed+ " days,");
        }

        return sb.toString();
    }

    // Gets called from a different thread
    @SuppressLint("SetTextI18n")
    void updateTelemetryInfo()
    {
        if (AppModel.getInstance().mLastEvent != null) {
            try {
                final TelemetryEvent te = AppModel.getInstance().mLastEvent;
                tvEvent.setText(te.mEvent.toString());
                tvSeq.setText(te.mSeq.toString());
                tvDateTime.setText(te.mDateTime.toString());
                GeolocParam gp = te.mGeoloc;
                tvGeoloc.setText(gp.latitude + "/" + gp.longitude);
                tvGeolocExtra.setText(gp.heading.toString());
                tvSatStatus.setText("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);

                BigDecimal km = new BigDecimal(te.mOdometer);
                BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                miles = miles.setScale(2, RoundingMode.FLOOR);
                tvOdo.setText(miles.toString());



               /* if (swOdoUnit.isChecked()) {    // to Mile
                    BigDecimal km = new BigDecimal(te.mOdometer);
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.FLOOR);
                    tvOdo.setText(miles.toString());
                    String tvodo = tvOdo.getText().toString();
                    //setVehOdometer();
                } else {
                    tvOdo.setText(te.mOdometer);
                }*/

                tvVelo.setText(te.mVelocity);

                tvEh.setText(te.mEngineHours);

                tvRpm.setText(te.mRpm.toString());


            } catch (Exception e) {
                Log.e(TAG, e.fillInStackTrace().toString());
            }
        }
    }


    public TrackerViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment PrivacyFragment.
     */

    public static TrackerViewFragment newInstance() {
        TrackerViewFragment fragment = new TrackerViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TrackerViewFragment init(TrackerService.TrackerBinder binder)
    {
        AppModel.getInstance().mConnectTime = System.currentTimeMillis();
        mBinder = binder;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.v(TAG, "TVF: onCreate: "+this);
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SIS_DA", getBinder().getDeviceAddress());
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TVF: onDestroy:"+this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view =  inflater.inflate(R.layout.fragment_tracker_view, container, false);
        hos_fab_btn = (ExtendedFloatingActionButton)view.findViewById(R.id.hos_fab_btn);
        tvModel = (TextView)view.findViewById(R.id.tvModel);                                        // textview device model
        tvSerial = (TextView)view.findViewById(R.id.tvSerial);                                      // textview device serial number
        tvMac = (TextView)view.findViewById(R.id.tvMac);                                            // textview MAC address
        tvVersion = (TextView)view.findViewById(R.id.tvVersion);                                    // textview App version
        tvVin = (TextView)view.findViewById(R.id.tvVIN);                                            // textView Vehicle Identifier Number
        tvRssi = (TextView)view.findViewById(R.id.tvRssi);                                          // textview Received Signal Strength
        tvUpTime = (TextView)view.findViewById(R.id.tvUpTime);                                        // active time -- why is this not used?
        tvEvent = (TextView)view.findViewById(R.id.tvEvent);                                        // textView Stored Event
        tvSeq = (TextView)view.findViewById(R.id.tvSeq);                                            // textview Stored Event Sequence
        tvDateTime = (TextView)view.findViewById(R.id.tvDateTime);                                  // textView dateTime
        tvGeoloc = (TextView)view.findViewById(R.id.tvGeoloc);                                      // textview Geolocation
        tvGeolocExtra = (TextView)view.findViewById(R.id.tvGeolocExtra);                            // textview GeoLocation extra?
        tvSatStatus = (TextView)view.findViewById(R.id.tvSatStatus);                                // textview satellite status
        tvOdo = (TextView)view.findViewById(R.id.tvOdo);                                            // textview odometer
        tvVelo = (TextView)view.findViewById(R.id.tvVelo);                                          // textview velocity
        tvEh = (TextView)view.findViewById(R.id.tvEh);                                              // textview Engine Hours
        tvRpm = (TextView)view.findViewById(R.id.tvRpm);                                            // textview engine RPM

        setMacAddr(tvMac.getText().toString());

        hos_fab_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent hosIntent = new Intent(getContext(), HoursOfServiceActivity.class);
                startActivity(hosIntent);

            }
        });

        swOdoUnit = (Switch)view.findViewById(R.id.swOdoUnit);                                      // switch odometer mph/kph
        swOdoUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swOdoUnit.setChecked(true);
                swOdoUnit.setEnabled(true);
                if (swOdoUnit.isChecked()) {    // to Mile
                    BigDecimal km = new BigDecimal(tvOdo.getText().toString());
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.CEILING);
                    tvOdo.setText(miles.toString());
                } else {
                    BigDecimal miles = new BigDecimal(tvOdo.getText().toString());
                    BigDecimal km = miles.divide(BigDecimal.valueOf(0.621371), BigDecimal.ROUND_HALF_DOWN);
                    tvOdo.setText(km.toString());
                }
            }
        });

        // VIN refresh
        Button refreshVin = (Button)view.findViewById(R.id.refresh_vin);
        refreshVin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getBinder() == null) {
                    Toast.makeText(getContext(),"Error fetching VIN", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tracker Info was retrieved during onSync
                if (AppModel.getInstance().mTrackerInfo != null) {
                    TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

                    if (ti.product.contains("30")) {
                        GetTrackerInfo gti = new GetTrackerInfo();
                        getBinder().getTracker().sendRequest(gti, null, null);
                    } else {
                        GetVehicleInfo gvi = new GetVehicleInfo();
                        getBinder().getTracker().sendRequest(gvi, null, null);
                    }
                } else {
                    GetTrackerInfo gti = new GetTrackerInfo();
                    getBinder().getTracker().sendRequest(gti, null, null);
                }
            }
        });


        // Stored Events tile
        vSETile = view.findViewById(R.id.stored_events_tile);
        vSETile.setVisibility(View.GONE);
        tvSEventCount = (TextView)view.findViewById(R.id.tvSEventCount);                         // TextView Stored Event Count
        tvSESeq = (TextView)view.findViewById(R.id.tvSESeq);                                     // TextView Stored Event Sequence
        tvSEvent = (TextView)view.findViewById(R.id.tvSEvent);                                   // TextView Stored Event

        Button clearSE = (Button)view.findViewById(R.id.clear_stored_events);
        clearSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getBinder() == null) {
                    return;
                }

                ClearStoredEvents cse = new ClearStoredEvents();
                getBinder().getTracker().sendRequest(cse, null, null);
            }
        });

        Button detailsSE = (Button)view.findViewById(R.id.details_stored_event);
        detailsSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getBinder() == null) {
                    return;
                }

                if (AppModel.getInstance().mLastSEvent == null){
                    return;
                }

                TelemetryEvent te = AppModel.getInstance().mLastSEvent;

                LayoutInflater inflater = requireActivity().getLayoutInflater();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                final View dialog_se_view = getLayoutInflater().inflate(R.layout.dialog_se_view, null);


                builder.setTitle("Stored Event #: "+AppModel.getInstance().mLastSEvent.mSeq.toString())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setView(dialog_se_view);
                AlertDialog dialog = builder.create();

                TextView tvDateTime = (TextView)dialog_se_view.findViewById(R.id.tv_se_DateTime);
                TextView  tvGeoloc = (TextView)dialog_se_view.findViewById(R.id.tv_se_Geoloc);
                TextView  tvGeolocExtra = (TextView)dialog_se_view.findViewById(R.id.tv_se_GeolocExtra);
                TextView  tvSatStatus = (TextView)dialog_se_view.findViewById(R.id.tv_se_SatStatus);
                TextView  tvOdo = (TextView)dialog_se_view.findViewById(R.id.tv_se_Odo);
                TextView  tvVelo = (TextView)dialog_se_view.findViewById(R.id.tv_se_Velo);
                TextView tvEh = (TextView)dialog_se_view.findViewById(R.id.tv_se_Eh);
                TextView tvRpm = (TextView)dialog_se_view.findViewById(R.id.tv_se_Rpm);

                tvEvent.setText(te.mEvent.toString());

                tvSeq.setText(te.mSeq.toString());
                tvDateTime.setText(te.mDateTime.toString());

                GeolocParam gp = te.mGeoloc;
                tvGeoloc.setText(gp.latitude + "/" + gp.longitude);

                tvGeolocExtra.setText(gp.heading.toString());

                tvSatStatus.setText("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);

                if (swOdoUnit.isChecked()) {    // to Mile
                    BigDecimal km = new BigDecimal(te.mOdometer);
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.FLOOR);
                    tvOdo.setText(miles.toString());

                } else {
                    tvOdo.setText(te.mOdometer);

                }

                tvVelo.setText(te.mVelocity);

                tvEh.setText(te.mEngineHours);

                tvRpm.setText(te.mRpm.toString());


                dialog.show();

            }
        });
        Button ackSE = (Button)view.findViewById(R.id.ack_stored_event);
        ackSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getBinder() == null) {
                    return;
                }

                if (AppModel.getInstance().mLastSEvent != null) {
                    ackStoredEvent(AppModel.getInstance().mLastSEvent);
                }
            }
        });

        Button retrieveSE = (Button)view.findViewById(R.id.retrieve_stored_events);
        retrieveSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getBinder() == null) {
                    return;
                }

                RetrieveStoredEvents rse = new RetrieveStoredEvents();
                getBinder().getTracker().sendRequest(rse, null, null);
                // Hide the button after initiating the retrieval
                if (!AppModel.getInstance().wereSERequested) {
                    view.setVisibility(View.GONE);
                    AppModel.getInstance().wereSERequested = true;
                }
            }
        });

        // End Stored Events tile

        /*---------------------------------- The "TRACKER" is the PT-40 Device ---------------------*/
        tmIf.addAction("REFRESH");                                                              // TRACKER MANAGER
        trackerIf.addAction("TRACKER-REFRESH");                                                 // refresh TRACKER
        tupIf.addAction("TRACKER-UPDATE");                                                      // tu = TRACKER update
        dtcIf.addAction("TRACKER-DTC-REFRESH");                                                 // DTC = Diagnostic Trouble Code
        dtcIf.addAction("TRACKER-DTC-CLEAR");                                                   // Clear Vehicle Trouble Codes
        tviIf.addAction("TRACKER-VIN-REFRESH");                                                 // tvi = TRACKER vehicle info
        seIf.addAction("TRACKER-SE-REFRESH");                                                   // se = Stored event
        spnIf.addAction("TRACKER-SPN-REFRESH");

        // If Debug is ON, change panel bkg color
        View diPanel = view.findViewById(R.id.device_info_panel);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.perf_file), Context.MODE_PRIVATE);
        Boolean devmode = sharedPref.getBoolean("dev_mode_switch", false);
        if (devmode) {
            diPanel.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbWarn));
        }

        // Restore view
        if (savedInstanceState != null) {
            tvMac.setText(savedInstanceState.getString("SIS_DA"));
            setMacAddr(savedInstanceState.getString("SIS_DA"));
            updateTrackerInfo();
            updateVehicleInfo();
            updateTelemetryInfo();
        }

        Log.v(TAG, "TVF: view initialized");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "TVF: onResume:"+this);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(tmRefresh, tmIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(tiRefresh, trackerIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(tupRefresh, tupIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(dtcRefresh, dtcIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(viRefresh, tviIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(seRefresh, seIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(spnRefresh, spnIf);

        if (getBinder() != null) {
            updateTrackerInfo();
            updateTelemetryInfo();
            updateSE();

        } else {
            Log.e(TAG, "TVF: >>>>>>>>>>>>> Binder null");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "TVF: onPause:"+this);

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tmRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tiRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tupRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(dtcRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(viRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(seRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(spnRefresh);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tmRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tiRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tupRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(dtcRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(viRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(seRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(spnRefresh);

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_tracker_view, menu);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {

        // FIXME
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        boolean isNetAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (isNetAvailable ){
            menu.findItem(R.id.action_fup).setEnabled(true);
            menu.findItem(R.id.action_chk_fup).setEnabled(true);
        }else {
            menu.findItem(R.id.action_fup).setEnabled(false);
            menu.findItem(R.id.action_chk_fup).setEnabled(false);
        }

        menu.findItem(R.id.action_fup_file).setEnabled(true);

        // SPN only supported on PT30
        // Invalidate menu on TrackerInfo refresh
        TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
        if ((ti != null) && (ti.product != null) && ti.product.contains("30")) {
            menu.findItem(R.id.action_sv).setEnabled(true);
            menu.findItem(R.id.action_demo_spn).setEnabled(true);
        } else {
            menu.findItem(R.id.action_sv).setEnabled(true);
            menu.findItem(R.id.action_demo_spn).setEnabled(false);
        }


        menu.findItem(R.id.action_get_dtc).setEnabled(true);
        menu.findItem(R.id.action_clear_dtc).setEnabled(true);
    }

    @Override
    public void onServiceBound(TrackerService.TrackerBinder binder) {
        // note: Binder may not be present, if this fragment was added in response to a broadcast from the existing service
        mBinder = binder;
        Log.v(TAG, "TVF: onServiceBound:"+this);
    }

    @Override
    public void onServiceUnbound() {
        Log.v(TAG, "TVF: onServiceUnbound:"+this);
    }


    class ChkUpdateTask extends AsyncTask<Void, Void, Long> {

        boolean mIsAvailable = false;
        boolean mError = false;
        String msg;

        protected Long doInBackground(Void... voids ) {

            if (getBinder() == null) {
                mError = true;
                msg = "Try again";
                return 0L;
            }

            try {
                mIsAvailable = getBinder().getTracker().isUpdateAvailable(getContext());

            } catch (IllegalStateException ise) {
                mError = true;
                msg = ise.getLocalizedMessage();
            } catch (IllegalArgumentException iae) {
                mError = true;
                msg = iae.getLocalizedMessage();
            }

            return 0L;
        }


        protected void onPostExecute(Long result) {
            if (!mError) {
                if (mIsAvailable) {
                    Toast.makeText(getContext(), "An update is available!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "No updates available.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Error:"+msg, Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        int id = item.getItemId();
        TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

        if (getBinder() == null) {
            super.onOptionsItemSelected(item);
        }
        //final WeakReference<Activity> activityRef = new WeakReference<Activity>(TrackerViewActivity.this);
        if (id == R.id.action_fup) {
            if (getBinder().getTracker() != null) {
                getBinder().getTracker().update(getContext());
                showUpdateProgressDialog();
            }
            return true;
        } else if (id == R.id.action_chk_fup) {
            new ChkUpdateTask().execute();
            return true;
        }  else if (id == R.id.action_fup_file_ctrl) {

            String permission = "android.permission.READ_EXTERNAL_STORAGE";
            int res = getActivity().checkCallingOrSelfPermission(permission);
            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Storage permission not available!", Toast.LENGTH_LONG).show();
                this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_READ_STORAGE_PERMISSION);
            } else {

                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select Firmware"), REQUEST_SELECT_CTRL_FW);
            }
            return true;
        } else if (id == R.id.action_fup_file_ble) {

            String permission = "android.permission.READ_EXTERNAL_STORAGE";
            int res = getActivity().checkCallingOrSelfPermission(permission);
            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Storage permission not available!", Toast.LENGTH_LONG).show();
                this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_READ_STORAGE_PERMISSION);
            } else {

                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select Firmware"), REQUEST_SELECT_BLE_FW);
            }
            return true;
        } else if (id == R.id.action_sv) {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            TrackerPrefFragment frag = TrackerPrefFragment.newInstance();
            ft.replace(R.id.fragment_container,frag );
            ft.addToBackStack("tracker_perf");
            ft.commit();
            return true;
        } else if (id == R.id.action_get_dtc) {

            GetDiagTroubleCodes getDTC = new GetDiagTroubleCodes();
            if (getBinder().getTracker() != null) {
                getBinder().getTracker().sendRequest(getDTC, null, null);
            }
            return true;
        } else if (id == R.id.action_clear_dtc) {

            ClearDiagTroubleCodes clearDTC = new ClearDiagTroubleCodes();
            if (getBinder().getTracker() != null) {
                getBinder().getTracker().sendRequest(clearDTC, null, null);
            }
            return true;
        } else if (id == R.id.action_demo_spn) {
            // Fuel gauge example
            SPNEventDefinitionParam.DefinitionBuilder builder = new SPNEventDefinitionParam.DefinitionBuilder(0)
                    .setSpn(96)
                    .setMode(0)
                    .setTimer(15)
                    .setValue(50)
                    .setPgn(65276)
                    .setAddress(255)
                    .setStartByte(2)
                    .setStartBit(0)
                    .setLength(8);
            ConfigureSPNEvent config_spn = new ConfigureSPNEvent(builder.build());
            if ((ti != null) && (ti.product != null) && ti.product.contains("30")) {
                config_spn.enableLegacy();
            }
            if (getBinder().getTracker() != null) {
                getBinder().getTracker().sendRequest(config_spn, null, null);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showUpdateProgressDialog() {
        final TrackerUpdateProgressFragment dialog = TrackerUpdateProgressFragment.getInstance();
        dialog.show(getActivity().getSupportFragmentManager(), TrackerUpdateProgressFragment.FRAG_TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getContext(), "Permission granted. Pl. try again.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode==REQUEST_SELECT_CTRL_FW || requestCode==REQUEST_SELECT_BLE_FW )  && resultCode==Activity.RESULT_OK) {

            String fn = data.getData().getLastPathSegment();
            Log.i(TAG, "File Uri: "+ data.getDataString());

            try {

                InputStream iss = getActivity().getContentResolver().openInputStream(data.getData());
                AppModel.getInstance().mFileContent = new byte[(int) iss.available()];
                Log.i(TAG, "File size: "+ iss.available());

                DataInputStream dis = new DataInputStream(getActivity().getContentResolver().openInputStream(data.getData()));
                dis.readFully(AppModel.getInstance().mFileContent);
                dis.close();

                AppModel.getInstance().mUpgradefromFileSelected = true;
                upgradeFromFile(requestCode==REQUEST_SELECT_BLE_FW ? TrackerManager.FT_BLE : TrackerManager.FT_CTRL);

            } catch (FileNotFoundException e) {
                Log.e(TAG, "FNF", e.fillInStackTrace());
            } catch (IOException e) {
                Log.e(TAG, "IO",e.fillInStackTrace());
            }
        }
    }

    public void upgradeFromFile(Integer filetype) {
        if (getBinder() != null) {
            getBinder().getTracker().update(getContext(), "file", (long)filetype, AppModel.getInstance().mFileContent);
            showUpdateProgressDialog();
            AppModel.getInstance().mUpgradefromFileSelected = false;
        }
    }

     TrackerService.TrackerBinder getBinder() {
         if (mBinder != null) {
             return mBinder;
         } else {
             //Toast.makeText(getContext(), "TVF:TNB!", Toast.LENGTH_SHORT).show();
             return null;
         }
     }
}
