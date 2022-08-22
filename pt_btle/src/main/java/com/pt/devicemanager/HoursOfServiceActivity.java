package com.pt.devicemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.common.internal.TelemetryData;
import com.google.android.gms.common.internal.TelemetryLoggingClient;
import com.google.android.gms.common.internal.TelemetryLoggingOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.pt.sdk.BaseRequest;
import com.pt.sdk.BaseResponse;
import com.pt.sdk.BleuManager;
import com.pt.sdk.TSError;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManager;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

public class HoursOfServiceActivity extends AppCompatActivity implements View.OnClickListener, TrackerServiceListener,
        TelemetryLoggingClient, TrackerManagerCallbacks {

    BleProfileService bleProfileService;
    TrackerService trackerService;
    private TrackerManager mTrackerManager;
    private TelemetryEvent telemetryEvent;
    private Handler mHandler = new Handler();
    private TrackerService.TrackerBinder binder = null;

    private ImageView dutyStatus_IV, countryFlag_IV;
    private TextView driverId_TV, truckId_TV, trailerId_TV, status_tv, trip_tv, timeRemaining_TV;
    private Button dataTransfer_BTN, remark_BTN, options_BTN, logs_BTN, roadsideInspection_BTN, dvir_BTN,
            startBreak_BTN, endBreak_BTN, vehicleError_BTN, events_BTN;

    private Chronometer runtime;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private final long START_TIME_IN_MILLIS = 8;
    private long mTimeLeftInMillis = TimeUnit.HOURS.toMillis(START_TIME_IN_MILLIS);
    double original_Odometer;
    double trip_Odometer;
    double trip = 0;
    private LinearLayoutCompat primaryLinearLayout;
    private long lastPause;


    public static String getDriverId() {
        return DRIVER_ID;
    }

    public static void setDriverId(String driverId) {
        DRIVER_ID = driverId;
    }

    public static String getTruckId() {
        return TRUCK_ID;
    }

    public static void setTruckId(String truckId) {
        TRUCK_ID = truckId;
    }

    public static String getTrailerId() {
        return TRAILER_ID;
    }

    public static void setTrailerId(String trailerId) {
        TRAILER_ID = trailerId;
    }

    public static String DRIVER_ID;
    public static String TRUCK_ID;
    public static String TRAILER_ID;

    private TrackerService.TrackerBinder hosBinder = null;

    TrackerService.TrackerBinder getBinder() {
        if (hosBinder != null) {
            return hosBinder;
        } else {
            Toast.makeText(HoursOfServiceActivity.this, "TVF:TNB!", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public HoursOfServiceActivity init(TrackerService.TrackerBinder binder){
        AppModel.getInstance().mConnectTime = System.currentTimeMillis();
        hosBinder = binder;
        this.binder = binder;
        return this;
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

    private void updateTelemetryInfo() {

        if(AppModel.getInstance().mLastEvent != null){
            final TelemetryEvent te = AppModel.getInstance().mLastEvent;
        }

    }

    BroadcastReceiver tiRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTrackerInfo();
        }
    };

    private void updateTrackerInfo() {
        if(AppModel.getInstance().mTrackerInfo != null){
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
        }
        if(getBinder() != null){
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }


    }

    BroadcastReceiver tupRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer action = intent.getIntExtra(TrackerService.EXTRA_RESP_ACTION_KEY, 0);

            switch (action) {
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_UPDATED:
                    Toast.makeText(HoursOfServiceActivity.this, "Tracker was successfully updated.", Toast.LENGTH_SHORT).show();
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

    private void updateVehicleInfo() {
        GetVehicleInfo gvi = new GetVehicleInfo();
        getBinder().getTracker().sendRequest(gvi, null, null);

    }

    void updateSE(){
        if(AppModel.getInstance().mLastSECount == 0){
            Log.i("HOS_EVENT_COUNT", "Last Stored Event Count was 0");
        }
    }
    void getStoredEventCount(){
        GetStoredEventsCount gsec = new GetStoredEventsCount();
        if(getBinder() != null){
            getBinder().getTracker().sendRequest(gsec, null, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_of_service);
        init(hosBinder);
        telemetryEvent = AppModel.getInstance().mLastSEvent;

        /*---------------------------------------------------- Initializing Interface Variables --------------------------------------*/

        dutyStatus_IV = findViewById(R.id.dutyStatus_IV); // ------- Change image from/to on/off Duty
        countryFlag_IV = findViewById(R.id.countryFlag_IV); // ----- Change with regional flag
        driverId_TV = findViewById(R.id.driverId_TV);   // --------- Get from database
        truckId_TV = findViewById(R.id.truckId_TV); //  ------------ Get from database
        trailerId_TV = findViewById(R.id.trailerId_TV);// ---------- Get from database
        timeRemaining_TV = findViewById(R.id.timeRemaining_TV); // - Time remaining till required break
        runtime = findViewById(R.id.runtime_TV); // ------------- Chronometer activates with timer
        status_tv = findViewById(R.id.status_tv);   // ------------- Duty Status / Connected to tracker status
        trip_tv = findViewById(R.id.trip_tv); // ------------------- Trip Odometer

        dataTransfer_BTN = findViewById(R.id.dataTransfer_BTN);
        dataTransfer_BTN.setOnClickListener(this);
        remark_BTN = findViewById(R.id.remark_BTN);
        remark_BTN.setOnClickListener(this);
        options_BTN = findViewById(R.id.options_BTN);
        options_BTN.setOnClickListener(this);
        logs_BTN = findViewById(R.id.logs_BTN);
        logs_BTN.setOnClickListener(this);
        roadsideInspection_BTN = findViewById(R.id.roadsideInspection_BTN);
        roadsideInspection_BTN.setOnClickListener(this);
        dvir_BTN = findViewById(R.id.dvir_BTN);
        dvir_BTN.setOnClickListener(this);
        startBreak_BTN = findViewById(R.id.startBreak_BTN);
        startBreak_BTN.setOnClickListener(this);
        endBreak_BTN = findViewById(R.id.endBreak_BTN);
        endBreak_BTN.setOnClickListener(this);
        vehicleError_BTN = findViewById(R.id.vehicleError_BTN);
        vehicleError_BTN.setOnClickListener(this);
        events_BTN = findViewById(R.id.events_BTN);
        events_BTN.setOnClickListener(this);
        primaryLinearLayout = findViewById(R.id.primaryLinearLayout);
        trackerService = new TrackerService();

        setDriverId(driverId_TV.getText().toString());
        setTruckId(truckId_TV.getText().toString());
        setTrailerId(trailerId_TV.getText().toString());

        TrackerInfo ti = AppModel.getInstance().mTrackerInfo; // --- Displays the device info in status textView
        if(ti != null){
            status_tv.setText("Connected to:\t" + ti.product.toString());
        } else {
            status_tv.setText("Device not detected");
        }


    }

    /*---------------- Button Click Events ----------------------*/
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.dataTransfer_BTN) {
            dataTransfer();
        } else if (id == R.id.remark_BTN) {
            remark();
        } else if (id == R.id.options_BTN) {
            options();
        } else if (id == R.id.logs_BTN) {
            logs();
        } else if (id == R.id.roadsideInspection_BTN) {
            roadsideInspection();
        } else if (id == R.id.dvir_BTN) {
            dvir();
        } else if (id == R.id.startBreak_BTN) {
            startBreak();
        } else if (id == R.id.endBreak_BTN) {
            endBreak();
        } else if (id == R.id.vehicleError_BTN) {
            vehicleError();
        } else if (id == R.id.events_BTN) {
            events();
        }
    }

    private void events() { // ------ Inflates the EventListFragment ******************************** VISIBLE TO USER FOR TESTING PURPOSES ONLY ***********
        primaryLinearLayout.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, EventListFragment.class, null)
                .commit();
    }

    /*---------------------------------------------------------- Button Click Methods ------------------------------------*/


    private void vehicleError() {
        /*-- This will view a log of vehicle error codes *** NOTE: When there's a malfunction, you need to send a notification and alert the driver using a snackbar--*/
        Toast.makeText(this, "Vehicle Malfunction Log", Toast.LENGTH_SHORT).show();
    }

    private void endBreak() {
        /*-- End break then activate driving timer and switch to active driving status Note: You will likely need to re-validate the driver--*/
        Toast.makeText(this, "Switching to Active Duty Status", Toast.LENGTH_SHORT).show();
        runtime.setBase(SystemClock.elapsedRealtime()); // Set the timer to the system clock
        runtime.start();    // Timer has started
        if(mTimerRunning){
            return;
        } else {
            startTimer();
        }
    }

    private void startBreak() {
        /*-- Switch to inactive duty status and deactivate driving timer NOTE: the vehicle must be stationary --*/
        Toast.makeText(this, "Switching to Off Duty Status", Toast.LENGTH_SHORT).show();
        runtime.stop();
        runtime.setBase(SystemClock.elapsedRealtime());
        stopTimer();
    }

    private void dvir() {
        /*-- Drivers Daily Vehicle Inspection Report --*/
        Toast.makeText(this, "Drivers Daily Vehicle Inspection Report Form", Toast.LENGTH_SHORT).show();
    }

    private void roadsideInspection() {
        /*-- View the driver daily logs for the past one (1) week. It includes information on the vehicle used, shipments and driving statuses --*/
        Toast.makeText(this, "Viewing Driver Daily Logs", Toast.LENGTH_SHORT).show();
    }

    private void logs() {
        /*--  Display the current days log and provide the ability to navigate backwards to view the previous seven logs --*/
        Toast.makeText(this, "Today's Log", Toast.LENGTH_SHORT).show();
    }

    private void options() {
        /*-- Options module allows the user to switch vehicles, view manifests, add exceptions, input remarks, activate off road/toll roadmap
         login or switch to a co-driver, and transfer data --*/
        Toast.makeText(this, "Options", Toast.LENGTH_SHORT).show();
    }

    private void remark() {
        /*-- Remarks --*/
        Toast.makeText(this, "Remarks", Toast.LENGTH_SHORT).show();
    }

    private void dataTransfer() {
        /*-- standardized single-step compliation for the driver’s ELD records and initiation of the data transfer to authorized safety officials when requested during a roadside inspection --*/
        Toast.makeText(this, "Data Transfer", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceBound(TrackerService.TrackerBinder binder) {
        hosBinder = binder;
    }

    @Override
    public void onServiceUnbound() {

    }

    public void startTimer(){
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long l) {
                mTimeLeftInMillis = l;
                NumberFormat f = new DecimalFormat("00");
                long hour = (mTimeLeftInMillis / 3600000) % 24;
                long min = (mTimeLeftInMillis / 60000) % 60;
                long sec = (mTimeLeftInMillis / 1000) % 60;
                timeRemaining_TV.setText(f.format(hour) + ":" + f.format(min) + ":" + f.format(sec));
                double theDifference = Double.parseDouble(String.valueOf(TrackerService.DIFFERENCE));
                DecimalFormat df = new DecimalFormat("0.00");
                trip_tv.setText(String.valueOf(df.format(theDifference)));
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mTimeLeftInMillis = TimeUnit.HOURS.toMillis(START_TIME_IN_MILLIS);


            }
        }.start();
        mTimerRunning = true;

    }

    public void stopTimer(){
        mCountDownTimer.cancel();
        mTimerRunning = false;
        mTimeLeftInMillis = TimeUnit.HOURS.toMillis(START_TIME_IN_MILLIS);

    }

    @NonNull
    @Override
    public Task<Void> log(@NonNull TelemetryData telemetryData) {
        return null;
    }

    @NonNull
    @Override
    public ApiKey<TelemetryLoggingOptions> getApiKey() {
        return null;
    }

    @Override
    public void onRequest(String s, TelemetryEventRequest telemetryEventRequest) {

        trip = 0;
        original_Odometer = TrackerService.getEngineOdometer();
        trip_Odometer = Double.parseDouble(telemetryEventRequest.mTm.mOdometer);
        trip = original_Odometer - trip_Odometer;
        Log.i("HOS_REQUEST", String.valueOf(trip));
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
    public void onResponse(String s, GetVehicleInfoResponse getVehicleInfoResponse) {

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