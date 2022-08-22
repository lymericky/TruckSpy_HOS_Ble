package com.pt.devicemanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pt.sdk.GeolocParam;
import com.pt.sdk.TSError;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManagerCallbacks;
import com.pt.sdk.VehicleDiagTroubleCode;
import com.pt.sdk.request.GetStoredEventsCount;
import com.pt.sdk.request.GetTrackerInfo;
import com.pt.sdk.request.GetVehicleInfo;
import com.pt.sdk.request.inbound.SPNEventRequest;
import com.pt.sdk.request.inbound.StoredTelemetryEventRequest;
import com.pt.sdk.request.inbound.TelemetryEventRequest;
import com.pt.sdk.response.ClearDiagTroubleCodesResponse;
import com.pt.sdk.response.ClearStoredEventsResponse;
import com.pt.sdk.response.ConfigureSPNEventResponse;
import com.pt.sdk.response.GetDiagTroubleCodesResponse;
import com.pt.sdk.response.GetStoredEventsCountResponse;
import com.pt.sdk.response.GetSystemVarResponse;
import com.pt.sdk.response.GetTrackerInfoResponse;
import com.pt.sdk.response.GetVehicleInfoResponse;
import com.pt.sdk.response.RetrieveStoredEventsResponse;
import com.pt.sdk.response.SetSystemVarResponse;
import com.pt.ws.TrackerInfo;
import com.pt.ws.VehicleInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/* This is the details view for the last stored event. It still requires re-organization and a
* method to scroll through recorded events */
public class EventListFragment extends Fragment implements TrackerServiceListener, TrackerManagerCallbacks {



    TrackerService trackerService;
    public static String VEH_INFO_TAG = "VEHICLE_INFO";
    public static String TRAC_INFO_TAG = "TRACKER_INFO";
    public static String DEV_INFO_TAG = "DEVICE_INFO";

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getLatLong() {
        return latLong;
    }

    public void setLatLong(String latLong) {
        this.latLong = latLong;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getSatStatus() {
        return satStatus;
    }

    public void setSatStatus(String satStatus) {
        this.satStatus = satStatus;
    }

    public String getOdometer() {
        return odometer;
    }

    public void setOdometer(String odometer) {
        this.odometer = odometer;
    }

    public String getVelocity() {
        return velocity;
    }

    public void setVelocity(String velocity) {
        this.velocity = velocity;
    }

    public String getEngineHours() {
        return engineHours;
    }

    public void setEngineHours(String engineHours) {
        this.engineHours = engineHours;
    }

    public String getRpm() {
        return rpm;
    }

    public void setRpm(String rpm) {
        this.rpm = rpm;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public String getEventCount() {
        return eventCount;
    }

    public void setEventCount(String eventCount) {
        this.eventCount = eventCount;
    }

    public String getTrackerModel() {
        return trackerModel;
    }

    public void setTrackerModel(String trackerModel) {
        this.trackerModel = trackerModel;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }


    String eventId;
    String dateTime;
    String latLong;
    String heading;
    String satStatus;
    String odometer;
    String velocity;
    String engineHours;
    String rpm;
    String driver;
    String vehicle;
    String trailer;
    String eventCount;
    String macAddress;
    String trackerModel;
    String serial;
    String version;
    String vin;
    String sequence;
    String speed;

    ExtendedFloatingActionButton getData_btn;
    ImageButton refreshData_btn;

    TextView eventID_tv;
    TextView datetime_tv;
    TextView latLong_tv;
    TextView heading_tv;
    TextView satStatus_tv;
    TextView odometer_tv;
    TextView velocity_tv;
    TextView engineHours_tv;
    TextView rpm_tv;
    TextView driver_tv;
    TextView vehicle_tv;
    TextView trailer_tv;
    TextView eventCount_tv;
    TextView MacAddress_tv;
    TextView trackerModel_tv;
    TextView serial_tv;
    TextView version_tv;
    TextView vin_tv;
    TextView sequence_tv;
    TextView speed_tv;

    VehicleInfo vi;
    GeolocParam gp;
    TrackerInfo ti;



    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "tracker_view";
    protected static final int REQUEST_SELECT_CTRL_FW = 3;
    protected static final int REQUEST_SELECT_BLE_FW = 4;
    protected static final int REQUEST_READ_STORAGE_PERMISSION = 5;
    private TelemetryEvent telemetryEvent;
    private final Handler mHandler = new Handler();

    TrackerViewFragment trackerViewFragment = new TrackerViewFragment();

    public TrackerViewFragment getTrackerViewFragment() {
        return trackerViewFragment;
    }

    private TrackerService.TrackerBinder binder = null;

    TrackerService.TrackerBinder getBinder() {

        if (binder != null) {
            return binder;
        } else {
            Toast.makeText(getContext(), "TVF:TNB!", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    public EventListFragment() {
        // Required empty public constructor
    }


    public static EventListFragment newInstance(String param1, String param2) {
        EventListFragment fragment = new EventListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public EventListFragment init(TrackerService.TrackerBinder binder){
        AppModel.getInstance().mConnectTime = System.currentTimeMillis();
        this.binder = binder;
        return this;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SIS_DA", getBinder().getDeviceAddress());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        telemetryEvent = AppModel.getInstance().mLastEvent;
        init(binder);

        eventID_tv = (TextView) view.findViewById(R.id.eventID_tv);
        datetime_tv = (TextView) view.findViewById(R.id.dateTime_tv);
        latLong_tv = (TextView) view.findViewById(R.id.latLong_tv);
        heading_tv = (TextView) view.findViewById(R.id.heading_tv);
        satStatus_tv = (TextView) view.findViewById(R.id.satStatus_tv);
        odometer_tv = (TextView) view.findViewById(R.id.odometer_tv);
        velocity_tv = (TextView) view.findViewById(R.id.velocity_tv);
        engineHours_tv = (TextView) view.findViewById(R.id.engineHours_tv);
        rpm_tv = (TextView) view.findViewById(R.id.rpm_tv);
        driver_tv = (TextView) view.findViewById(R.id.driver_tv);
        vehicle_tv = (TextView) view.findViewById(R.id.vehicle_tv);
        trailer_tv = (TextView) view.findViewById(R.id.trailer_tv);
        eventCount_tv = (TextView) view.findViewById(R.id.eventCount_tv);
        MacAddress_tv = (TextView) view.findViewById(R.id.MacAddress_tv);
        trackerModel_tv = (TextView) view.findViewById(R.id.trackerModel_tv);
        serial_tv = (TextView) view.findViewById(R.id.serial_tv);
        version_tv = (TextView) view.findViewById(R.id.version_tv);
        vin_tv = (TextView) view.findViewById(R.id.vin_tv);
        sequence_tv = (TextView) view.findViewById(R.id.sequence_tv);
        getData_btn = (ExtendedFloatingActionButton) view.findViewById(R.id.getData_btn);
        refreshData_btn = (ImageButton) view.findViewById(R.id.refreshData_btn);
        speed_tv = (TextView) view.findViewById(R.id.speed_tv);

        setDriver(HoursOfServiceActivity.getDriverId());
        setVehicle(HoursOfServiceActivity.getTruckId());
        setTrailer(HoursOfServiceActivity.getTrailerId());

        tmIf.addAction("REFRESH");                                                              // TRACKER MANAGER
        trackerIf.addAction("TRACKER-REFRESH");                                                 // refresh TRACKER
        tupIf.addAction("TRACKER-UPDATE");                                                      // tu = TRACKER update
        dtcIf.addAction("TRACKER-DTC-REFRESH");                                                 // DTC = Diagnostic Trouble Code
        dtcIf.addAction("TRACKER-DTC-CLEAR");                                                   // Clear Vehicle Trouble Codes
        tviIf.addAction("TRACKER-VIN-REFRESH");                                                 // tvi = TRACKER vehicle info
        seIf.addAction("TRACKER-SE-REFRESH");                                                   // se = Stored event
        spnIf.addAction("TRACKER-SPN-REFRESH");

        vi = AppModel.getInstance().mVehicleInfo;
        setVin(vi.VIN);
        Log.i(VEH_INFO_TAG, getVin());                                                      // Set the VIN WORKS!!!

        getStoredEventsCount();
        setEventCount(AppModel.getInstance().mLastSECount.toString());                              // Set the event count WORKS!!!

        gp = telemetryEvent.mGeoloc;
        setSpeed(gp.speed.toString());                                                              // set speed WORKS!!!

        ti = AppModel.getInstance().mTrackerInfo;
        MacAddress_tv.setText(TrackerViewFragment.getMacAddr());




        if(telemetryEvent != null) {
            try{
                eventID_tv.setText(getEventId());
                datetime_tv.setText(getDateTime());
                latLong_tv.setText(getLatLong());
                heading_tv.setText(getHeading());
                satStatus_tv.setText(getSatStatus());
                odometer_tv.setText(getOdometer());
                velocity_tv.setText(getVelocity());
                engineHours_tv.setText(getEngineHours());
                rpm_tv.setText(getRpm());
                eventCount_tv.setText(getEventCount());
                driver_tv.setText(getDriver());
                vehicle_tv.setText(getVehicle());
                trailer_tv.setText(getTrailer());
                MacAddress_tv.setText(TrackerViewFragment.getMacAddr());
                trackerModel_tv.setText(getTrackerModel());
                serial_tv.setText(getSerial());
                version_tv.setText(getVersion());
                vin_tv.setText(getVin());
                sequence_tv.setText(getSequence());
                speed_tv.setText(getSpeed());

            } catch (Exception e){
                Log.i("TELE_ERROR", e.getMessage());
            }

        }

        getData_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try{
                    updateTelemetryInfo();
                    updateTrackerInfo();
                    updateVehicleInfo();
                    updateSE();
                } catch (Exception e){
                    Log.i("TELE_ERROR", e.getMessage());
                }
            }
        });

        refreshData_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    updateTelemetryInfo();
                    updateTrackerInfo();
                    updateVehicleInfo();
                    updateSE();
                } catch (Exception e){
                    Log.i("TELE_UPDATE_ERROR", e.getMessage());
                }
            }
        });

        return view;
    }

    @Override
    public void onServiceBound(TrackerService.TrackerBinder binder) {
        this.binder = binder;

    }

    @Override
    public void onServiceUnbound() {

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

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



    void updateTrackerInfo() {

        if (getBinder() != null) {
            // Set MAC Address
            MacAddress_tv.setText(getBinder().getDeviceAddress());
        }

        if (AppModel.getInstance().mTrackerInfo != null) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

            if (ti.product != null) {
                trackerModel_tv.setText(ti.product);
                setTrackerModel(ti.product);                                                        // Set Tracker Model
            } else {
                trackerModel_tv.setText("Generic");
            }
            setSerial(ti.SN);                                                                       // Set Serial
            serial_tv.setText(ti.SN);
            String ver = "F/W:" + ti.mvi.toString() + "  BLE:" + ti.bvi.toString();
            setVersion(ver);                                                                        // Set Version
            version_tv.setText(ver);

            if (ti.product.contains("30")) {                                                        // -- if using PT-30 device
                vin_tv.setText(AppModel.getInstance().mPT30Vin);
                setVin(AppModel.getInstance().mPT30Vin);
                getActivity().invalidateOptionsMenu();
            }
        } else if (getBinder() != null) {
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }

//        tvUpTime.setText(upTimeToString());
    }

    void updateVehicleInfo() {
        GetVehicleInfo getVehicleInfo = new GetVehicleInfo();
        getBinder().getTracker().sendRequest(getVehicleInfo, null, null);
        try{

            if(AppModel.getInstance().mTrackerInfo == null){
                Log.i("TRACKER", "\n TRACKER IS NULL");
            }

            if(AppModel.getInstance().mVehicleInfo == null){
                Log.i("VEH_INFO", "\n mVehicleInfo IS NULL");
            }

            if(AppModel.getInstance().mTrackerInfo != null){
                setVin(AppModel.getInstance().mTrackerInfo.mvi.toString());
                vin_tv.setText(getVin());
                Log.i("TRACKER", "\n VIN: \t" + getVin());
            }

            else if (AppModel.getInstance().mVehicleInfo != null) {
                VehicleInfo vi = AppModel.getInstance().mVehicleInfo;

                vin_tv.setText(vi.VIN);
                setVin(vi.VIN);
                Log.i("VEHICLE", "\n VIN: \t" + getVin());

                    String vehInfo = AppModel.getInstance().mVehicleInfo.VIN;
                    Log.i("INFO_TAG", vehInfo);
                }

        } catch (Exception e){
            Log.e("ERROR", e.getMessage());
        }

    }

    BroadcastReceiver seRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            updateSE();
        }
    };

    @SuppressLint("SetTextI18n")
    void updateSE()
    {
        if (AppModel.getInstance().mLastSECount == 0) {
            eventCount_tv.setText("No Events");                                                     // Set Event Count
            setEventCount("No Events");
        } else {
            eventCount_tv.setText(AppModel.getInstance().mLastSECount.toString());
            setEventCount(AppModel.getInstance().mLastSECount.toString());
        }

        // Update Event
        if (AppModel.getInstance().mLastSEvent != null) {
            sequence_tv.setText(AppModel.getInstance().mLastSEvent.mSeq.toString());
            setSequence(AppModel.getInstance().mLastSEvent.mSeq.toString());                        // Set Sequence
            eventID_tv.setText(AppModel.getInstance().mLastSEvent.mEvent.toString());
            setEventId(AppModel.getInstance().mLastSEvent.mEvent.toString());                       // Set Last Stored Event ID

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


    void updateTelemetryInfo()
    {
        if (AppModel.getInstance().mLastEvent != null) {
            try {
                final TelemetryEvent te = AppModel.getInstance().mLastEvent;
                eventID_tv.setText(te.mEvent.toString());
                setEventId(te.mEvent.toString() + " Sequence: " + te.mSeq);                         // Set Event ID
                sequence_tv.setText(te.mSeq.toString());
                setSequence(te.mSeq.toString());                                                    // Set Sequence
                datetime_tv.setText(te.mDateTime.toString());
                setDateTime(te.mDateTime.toString());                                               // Set DateTime

                GeolocParam gp = te.mGeoloc;
                latLong_tv.setText(gp.latitude + "/" + gp.longitude);
                setLatLong(gp.latitude + "/" + gp.longitude);                                       // Set Latitude and Longitude
                heading_tv.setText(gp.heading.toString());
                setHeading(gp.heading.toString());                                                  // Set Heading

                satStatus_tv.setText("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);
                setSatStatus("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);         // Set Sat Status

                    BigDecimal km = new BigDecimal(te.mOdometer);
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.FLOOR);
                    odometer_tv.setText(miles.toString());
                    setOdometer(miles.toPlainString());                                             // Set Odometer


                velocity_tv.setText(te.mVelocity);
                setVelocity(te.mVelocity);                                                          // Set Velocity
                engineHours_tv.setText(te.mEngineHours);
                setEngineHours(te.mEngineHours);                                                    // Set Engine Hours
                rpm_tv.setText(te.mRpm.toString());
                setRpm(te.mEngineHours);                                                            // Set RPM
                speed_tv.setText(te.mVelocity);
                setSpeed(te.mVelocity.toString());                                                  // Set Speed (Velocity is speed)


            } catch (Exception e) {
                Log.e(TAG, e.fillInStackTrace().toString());
            }
        }

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


        if (getBinder() != null) {
            updateTrackerInfo();
            updateTelemetryInfo();
            updateVehicleInfo();
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


    }

    @Override
    public void onRequest(String s, TelemetryEventRequest telemetryEventRequest) {

    }

    @Override
    public void onRequest(String s, StoredTelemetryEventRequest storedTelemetryEventRequest) {



    }

    @Override
    public void onRequest(String s, SPNEventRequest spnEventRequest) {


    }

    @Override
    public void onResponse(String s, ClearDiagTroubleCodesResponse clearDiagTroubleCodesResponse) {

    }

    @Override
    public void onResponse(String s, ClearStoredEventsResponse clearStoredEventsResponse) {

    }

    @Override
    public void onResponse(String s, GetDiagTroubleCodesResponse getDiagTroubleCodesResponse) {

    }

    @Override
    public void onResponse(String s, GetStoredEventsCountResponse getStoredEventsCountResponse) {

    }

    @Override
    public void onResponse(String s, GetSystemVarResponse getSystemVarResponse) {

    }

    @Override
    public void onResponse(String s, GetTrackerInfoResponse getTrackerInfoResponse) {

    }

    @Override
    public void onResponse(String s, GetVehicleInfoResponse gvi) {
        Intent broadcast = new Intent("TRACKER-VIN-REFRESH");
        AppModel.getInstance().mVehicleInfo = gvi.mVi;
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
        String VIN = gvi.mVi.VIN;
        setVin(VIN);
    }

    @Override
    public void onResponse(String s, RetrieveStoredEventsResponse retrieveStoredEventsResponse) {

    }

    @Override
    public void onResponse(String s, SetSystemVarResponse setSystemVarResponse) {

    }

    @Override
    public void onResponse(String s, ConfigureSPNEventResponse configureSPNEventResponse) {

    }

    @Override
    public void onFileUpdateStarted(String s, String s1) {

    }

    @Override
    public void onFileUpdateCompleted(String s) {

    }

    @Override
    public void onFileUpdateFailed(String s, TSError tsError) {

    }

    @Override
    public void onFileUpdateProgress(String s, int i) {

    }

    @Override
    public void onFwUptodate(String s) {

    }

    @Override
    public void onFwUpdated(String s, TrackerInfo trackerInfo) {

    }
}