package com.pt.devicemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
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
import com.pt.ws.TrackerInfo;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

public class HoursOfServiceActivity extends AppCompatActivity implements View.OnClickListener, TrackerServiceListener, TelemetryLoggingClient {

    TrackerService trackerService;

    private ImageView dutyStatus_IV, countryFlag_IV;
    private TextView driverId_TV, truckId_TV, trailerId_TV, status_tv, trip_tv, timeRemaining_TV;
    private Button dataTransfer_BTN, remark_BTN, options_BTN, logs_BTN, roadsideInspection_BTN, dvir_BTN,
            startBreak_BTN, endBreak_BTN, vehicleError_BTN, events_BTN;

    private Chronometer runtime;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private final long START_TIME_IN_MILLIS = 8;
    private long mTimeLeftInMillis = TimeUnit.HOURS.toMillis(START_TIME_IN_MILLIS);

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hours_of_service);

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
        runtime.setBase(0);
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

                trip_tv.setText(String.valueOf(theDifference));
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
}