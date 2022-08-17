/*
 * Copyright (c) 2016 - 2017 by Pacific Track, LLC
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property
 * of Pacific Track Incorporated and its suppliers, if any.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from
 * Pacific Track, LLC.
 */

package com.pt.devicemanager;

import com.pt.sdk.EventParam;
import com.pt.sdk.SPNEventParam;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.VehicleDiagTroubleCode;
import com.pt.ws.TrackerInfo;
import com.pt.ws.VehicleInfo;

public class AppModel {
    public static final String TAG = "TruckSpy";
    public static Boolean MODE_USB = false;


    public static final EventParam EV_ENGINE_ON = EventParam.EV_ENGINE_ON; // --- Engine is on
    public static final EventParam EV_ENGINE_OFF = EventParam.EV_ENGINE_OFF; // --- Engine is off
    public static final EventParam EV_IGNITION_ON = EventParam.EV_IGNITION_ON; // --- Ignition is on
    public static final EventParam EV_IGNITION_OFF = EventParam.EV_IGNITION_OFF; // --- Ignition is off
    public static final EventParam EV_TRIP_START = EventParam.EV_TRIP_START; // --- Driving started
    public static final EventParam EV_TRIP_END = EventParam.EV_TRIP_END; // --- Driving stopped
    public static final EventParam EV_BLE_ON = EventParam.EV_BLE_ON; // --- Bluetooth is on
    public static final EventParam EV_BLE_OFF = EventParam.EV_BLE_OFF; // --- Bluetooth is off
    public static final EventParam EV_BUS_ON = EventParam.EV_BUS_ON; // --- Tracker is connected to vehicle
    public static final EventParam EV_BUS_OFF = EventParam.EV_BUS_OFF; // --- Tracker is disconnected from vehicle
    private static AppModel ourInstance = new AppModel();

    public static AppModel getInstance() {
        return ourInstance;
    }



    // Per App Install
    public boolean privacyAccepted = false;
    public static boolean drivingStarted(boolean drivingStarted){
        
        return drivingStarted;
    }

    // Per tracker session
    public TelemetryEvent mLastEvent= null;
    public TelemetryEvent mLastSEvent= null;

    public Integer mLastSen = 0;
    public SPNEventParam mLastSPNEv = null;
    public VehicleDiagTroubleCode mLastDTC = null;
    public TrackerInfo mTrackerInfo = null;
    public VehicleInfo mVehicleInfo = null;
    public String mPT30Vin = "n/a";
    public Integer mLastSECount = 0;

    public boolean mUpgradefromFileSelected = false;
    public byte[] mFileContent;
    public int mFupType;

    public long mConnectTime = 0;
    public Boolean mTrackerLostLink = false;

    public boolean wereSERequested = false;

    // System variable tracking
    public String mPE = "n/a";



    private AppModel() {

    }

    public void invalidate()
    {
        mLastEvent= null;
        mLastSEvent = null;
        mLastSECount = 0;
        mLastDTC = null;
        mTrackerInfo = null;
        mVehicleInfo = null;
        mPT30Vin = "n/a";
        mUpgradefromFileSelected = false;
        mLastSen = 0;
        mLastSPNEv = null;
    }
}
