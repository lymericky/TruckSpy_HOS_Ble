/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pt.devicemanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;


import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.common.internal.TelemetryData;
import com.google.android.gms.common.internal.TelemetryLoggingClient;
import com.google.android.gms.common.internal.TelemetryLoggingOptions;
import com.google.android.gms.tasks.Task;
import com.pt.sdk.BaseRequest;
import com.pt.sdk.BaseResponse;
import com.pt.sdk.DateTimeParam;
import com.pt.sdk.EventParam;
import com.pt.sdk.GeolocParam;
import com.pt.sdk.BleuManager;
import com.pt.sdk.Sdk;
import com.pt.sdk.SystemVar;
import com.pt.sdk.TSError;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManager;
import com.pt.sdk.TrackerManagerCallbacks;
import com.pt.sdk.request.GetStoredEventsCount;
import com.pt.sdk.request.GetSystemVar;
import com.pt.sdk.request.GetVehicleInfo;
import com.pt.sdk.request.inbound.SPNEventRequest;
import com.pt.sdk.request.inbound.StoredTelemetryEventRequest;
import com.pt.sdk.response.ClearDiagTroubleCodesResponse;
import com.pt.sdk.response.ClearStoredEventsResponse;
import com.pt.sdk.response.ConfigureSPNEventResponse;
import com.pt.sdk.response.GetDiagTroubleCodesResponse;
import com.pt.sdk.response.GetStoredEventsCountResponse;
import com.pt.sdk.response.GetSystemVarResponse;
import com.pt.sdk.response.GetTrackerInfoResponse;
import com.pt.sdk.request.GetTrackerInfo;
import com.pt.sdk.request.inbound.TelemetryEventRequest;
import com.pt.sdk.response.GetVehicleInfoResponse;
import com.pt.sdk.response.RetrieveStoredEventsResponse;
import com.pt.sdk.response.SetSystemVarResponse;
import com.pt.sdk.response.outbound.AckEvent;
import com.pt.sdk.response.outbound.AckSPNEvent;
import com.pt.ws.TrackerInfo;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.log.Logger;

public class TrackerService extends BleProfileService implements TrackerManagerCallbacks, java.io.Serializable, java.lang.Comparable<EventParam>, TelemetryLoggingClient {

	double engineAge = 0; 	// Vehicle is off
	double engineHours = 0; // Engine is running

	public static final EventParam EV_ENGINE_ON = EventParam.EV_ENGINE_ON; // --- Engine is on
	public static final EventParam EV_ENGINE_OFF = EventParam.EV_ENGINE_OFF; // --- Engine is off
	public static final EventParam EV_IGNITION_ON = EventParam.EV_IGNITION_ON; // --- Ignition is on
	public static final EventParam EV_IGNITION_OFF = EventParam.EV_IGNITION_OFF; // --- Ignition is off
	public static final EventParam EV_TRIP_START = EventParam.EV_TRIP_START; // --- Driving started
	public static final EventParam EV_TRIP_END = EventParam.EV_TRIP_END; // --- Driving stopped
	public static final EventParam EV_BLE_ON = EventParam.EV_BLE_ON; // --- Bluetoothh is on
	public static final EventParam EV_BLE_OFF = EventParam.EV_BLE_OFF; // --- Bluetooth is off
	public static final EventParam EV_BUS_ON = EventParam.EV_BUS_ON; // --- Tracker is connected to vehicle
	public static final EventParam EV_BUS_OFF = EventParam.EV_BUS_OFF; // --- Tracker is disconnected from vehicle
	private static final String TAG = "TrackerService";

	public static double getEngineOdometer() {
		return ENGINE_ODOMETER;
	}

	public static void setEngineOdometer(double engineOdometer) {
		TrackerService.ENGINE_ODOMETER = engineOdometer;
	}
	public static double ENGINE_ODOMETER;

	public static double getEngineTripOdometer() {
		return ENGINE_TRIP_ODOMETER;
	}

	public static void setEngineTripOdometer(double engineTripOdometer) {
		ENGINE_TRIP_ODOMETER = engineTripOdometer;
	}

	public static double ENGINE_TRIP_ODOMETER;

	public static double getVehicleSpeed() {
		return VEHICLE_SPEED;
	}

	public static void setVehicleSpeed(double vehicleSpeed) {
		VEHICLE_SPEED = vehicleSpeed;
	}

	public static double VEHICLE_SPEED;

	public static double getVehicleMph() {
		return VEHICLE_MPH;
	}

	public static void setVehicleMph(double vehicleMph) {
		VEHICLE_MPH = vehicleMph;
	}

	public static double VEHICLE_MPH;

	public static double getStartTime() {
		return START_TIME;
	}

	public static void setStartTime(double startTime) {
		START_TIME = startTime;
	}

	public static double START_TIME;



	public static double DIFFERENCE;

	long startTime;
	long toMinutes;
	long toHours;


	/** A broadcast message with this action and the message in {@link Intent#EXTRA_TEXT} will be sent t the UART device. */
	public final static String ACTION_SEND = "no.nordicsemi.android.nrftoolbox.uart.ACTION_SEND";
	/** A broadcast message with this action is triggered when a message is received from the UART device. */
	private final static String ACTION_RECEIVE = "no.nordicsemi.android.nrftoolbox.uart.ACTION_RECEIVE";
	/** Action send when user press the DISCONNECT button on the notification. */
	public final static String ACTION_DISCONNECT = "no.nordicsemi.android.nrftoolbox.uart.ACTION_DISCONNECT";
	/** A source of an action. */
	public final static String EXTRA_SOURCE = "no.nordicsemi.android.nrftoolbox.uart.EXTRA_SOURCE";
	public final static int SOURCE_NOTIFICATION = 0;
	public final static int SOURCE_WEARABLE = 1;
	public final static int SOURCE_3RD_PARTY = 2;

	private final static int CONNECTION_NOTI_ID = 151; // random
	private final static int UPDATE_NOTI_ID = 171; // random

	private final static int OPEN_ACTIVITY_REQ = 67; // random
	private final static int DISCONNECT_REQ = 97; // random

	final String API_KEY = "ETktZrCVlk7h9pMxiNkyn1XbxqHVq3JN4HCtZDsB";

	private TrackerManager mTracker;
	private final LocalBinder mBinder = new TrackerBinder();

	public static String getVehVinNum() {
		return VEH_VIN_NUM;
	}

	public static void setVehVinNum(String vehVinNum) {
		VEH_VIN_NUM = vehVinNum;
	}

	public static String VEH_VIN_NUM;

	@Override
	public int compareTo(EventParam eventParam) {

		return 0;
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

	public Sdk setApiKey(String API_KEY){
		API_KEY = "ETktZrCVlk7h9pMxiNkyn1XbxqHVq3JN4HCtZDsB";
		return Sdk.getInstance();
	}


	public class TrackerBinder extends LocalBinder  {
		public void sendResponse(@NonNull final BaseResponse response)
		{
			mTracker.sendResponse(response, null, null);
		}

		public TrackerManager getTracker()
		{
			return mTracker;
		}

		void cancelUpdateNotifications()
		{
			_cancelUpdateNotifications();
		}
	}

	@Override
	protected LocalBinder getBinder() {
		return mBinder;
	}

	@Override
	protected BleuManager initializeManager() {
		Log.i(TAG, "initializeManager: ");
		mTracker = new TrackerManager(this);
		mTracker.setTrackerManagerCallbacks(this);
		return mTracker;

	}

	@Override
	protected boolean shouldAutoConnect() {
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		registerReceiver(mDisconnectActionBroadcastReceiver, new IntentFilter(ACTION_DISCONNECT));
		registerReceiver(mIntentBroadcastReceiver, new IntentFilter(ACTION_SEND));
		setApiKey(API_KEY);



	}

	@Override
	public void onDestroy() {
		// when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
		cancelNotifications();
		unregisterReceiver(mDisconnectActionBroadcastReceiver);
		unregisterReceiver(mIntentBroadcastReceiver);

		super.onDestroy();
	}


	/*private void calculateMPH(){
		double speed = getVehicleSpeed();
		double mph = speed * 0.621371;
		setVehicleMph(mph);
	}*/

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void calculateTrip(){
		LocalTime localTime = LocalTime.now();
		Log.i("LOCAL_TIME", localTime.toString());
		startTime = (long) Math.round(localTime.getHour());
		/*toMinutes = TimeUnit.MILLISECONDS.toMinutes(startTime); // changes startTime from millisecond to minutes
		toHours = toMinutes / 60;*/
		setStartTime(startTime);
		Log.i("START_TIME", String.valueOf(getStartTime()));




		double currentSpeed = getVehicleSpeed();
		Log.i("CURRENT_KMH", String.valueOf(getVehicleSpeed()));
		double currentOdometer = getEngineOdometer();
		Log.i("ODOMETER", String.valueOf(currentOdometer));
		double tripDistance = (toHours * currentSpeed);
		setEngineTripOdometer(tripDistance);
		Log.i("TRIP_ODOMETER", String.valueOf(getEngineTripOdometer()));
	}

	/*public void startTimer(){
		timerTask = new TimerTask() {
			@Override
			public void run() {
				COUNTER++;
				Log.i("TIMER", String.valueOf(COUNTER));
				setCOUNTER(COUNTER);
			}
		};
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, 0, 1000);
	}*/



	@Override
	protected void onRebind() {
		stopForegroundService();
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	protected void onUnbind() {
		startForegroundService();
	}


	private String notNull(final String name) {
		if (!TextUtils.isEmpty(name))
			return name;
		return getString(R.string.not_available);
	}

	void syncTracker()
	{
		Log.i(TAG, "Getting Tracker info ...");
		// Get the Tracker Info
		GetTrackerInfo gti = new GetTrackerInfo();
		mTracker.sendRequest(gti, null, null);

		// Get Stored Events count
		Log.i(TAG, "Getting Stored Events count ...");
		GetStoredEventsCount gsec = new GetStoredEventsCount();
		mTracker.sendRequest(gsec, null, null);

		// Get Vehicle Info for VIN
		Log.i(TAG, "Getting Vehicle Info ...");
		GetVehicleInfo gvi = new GetVehicleInfo();
		mTracker.sendRequest(gvi, null, null);

	}

	@Override
	public void onDeviceReady(@NonNull BluetoothDevice device) {
		super.onDeviceReady(device);
		syncTracker();
	}

	@Override
	public void onSerialConnected(@NonNull UsbDevice device) {
		super.onSerialConnected(device);
		syncTracker();
	}

	@Override
	public void onDeviceDisconnected(@NonNull BluetoothDevice device, int code) {
		super.onDeviceDisconnected(device, code);
		cancelNotifications();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public void onRequest(final String address,TelemetryEventRequest tmr)
	{
		// Update model
		AppModel.getInstance().mLastEvent = tmr.mTm;
		class TEventFlag {
			Boolean flag = false;
			Boolean isAvailable = false;
			TEventFlag (BaseRequest req)
			{
				if (req.containsKey(BaseRequest.Key.LIVE_EVENT)) {
					isAvailable = true;
					flag = req.getValue(BaseRequest.Key.LIVE_EVENT).equals("1") ? true : false;
				}
			}
		}

		TEventFlag isLive = new TEventFlag(tmr);
		Intent broadcast = new Intent("REFRESH");
		broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

		// Ack
		GeolocParam geoloc = tmr.mTm.mGeoloc;
		DateTimeParam dt = tmr.mTm.mDateTime;
		String timeParam = tmr.mTm.mDateTime.time;
		String dateParam = tmr.mTm.mDateTime.date;
		StringBuilder params = new StringBuilder();
		String id = EXTRA_DEVICE_NAME;

		params.append("id=").append(id)
				.append("&lat=").append(geoloc.latitude.toString())
				.append("&lon=").append(geoloc.longitude.toString())
				.append("&sat=").append(geoloc.satCount)
				.append("&speed=").append(geoloc.speed)
				.append("&head=").append(geoloc.heading)
				.append("&date=").append(dt.date)
				.append("&time=").append(dt.time)
				.append("&odometer").append(tmr.mTm.mOdometer);


		if (isLive.isAvailable) {
			params.append("&live=").append(isLive.flag);
		}

		START_TIME = Double.parseDouble(dt.time);
		Log.i("START_TIME", String.valueOf(dateParam) + " : " + timeParam);




		setEngineOdometer(Double.parseDouble(tmr.mTm.mOdometer));
		engineAge = tmr.mTm.mEngineAge; 															// Vehicle is off
		engineHours = Double.parseDouble(tmr.mTm.mEngineHours);									// Engine is running

		Double velocity = Double.parseDouble(tmr.mTm.mVelocity);
		setVehicleSpeed(velocity);
		//double speed = tmr.mTm.mGeoloc.speed;
		//double speed = getVehicleMph();
		//double odometer = Double.parseDouble(tmr.mTm.mOdometer);

		//calculateMPH();
		//calculateTrip();

		Log.i("TRIP_ODO", String.valueOf(getEngineTripOdometer()));
		Log.i("LIVE_ODO", String.valueOf(getEngineOdometer()));


		if(tmr.mTm.mEvent.toString().equals(EventParam.EV_TRIP_START.toString())){
			Log.i("TRIP_LOG", "******************* Trip Started!!!*********************------------------------------------" + " engine hours:" + engineHours);
			//startTimer();
			//Log.i("START_TIME", formattedDate);
			setEngineTripOdometer(Double.parseDouble(tmr.mTm.mOdometer));
		}
		DIFFERENCE = Double.parseDouble(tmr.mTm.mOdometer) - getEngineTripOdometer();
		DecimalFormat df = new DecimalFormat("0.00");
		Log.i("DIFF", String.valueOf(df.format(DIFFERENCE)));


		if(tmr.mTm.mEvent.toString().equals(EventParam.EV_TRIP_END.toString())){
			Log.i("TRIP_LOG", "***************** Trip Ended!!!***************************----------------------------------------" + "Engine Hours : " + engineHours + " Time : " + getStartTime());
			/*if(timerTask != null){
				timerTask.cancel();
				COUNTER = 0;
			}*/
		}

		Log.i(TAG, "NEW EVENT:" + tmr.mTm.mEvent.toString() + ":" + tmr.mTm.mSeq);			// ***** THIS DISPLAYS EACH EVENT IN THE LOGCAT BY EVENT ID AND SEQUENCE NUMBER ******

		if(tmr.mTm.mEvent.toString().equals(EventParam.EV_ENGINE_ON.toString())){
			Log.i("EVENT_LOG", "Engine is running ----");
		}
		if(tmr.mTm.mEvent.toString().equals(EventParam.EV_ENGINE_OFF.toString())){
			Log.i("EVENT_LOG", "Engine turned off!!! ");
		}

		// ACK the event																			// Event to be used for acknowledgement of actions done, such as having received an event and/or having carried out an order
		AckEvent ack = new AckEvent(0, tmr.mTm.mSeq.toString(), dt.toDateString());
		mTracker.sendResponse(ack, null, null);

	}



	@Override
	public void onRequest(final String address, StoredTelemetryEventRequest stmr) {

		// Update model
		Intent broadcast = new Intent("TRACKER-SE-REFRESH");
		AppModel.getInstance().mLastSEvent = stmr.mTm;												// mLastSEvent / mtm = Last Telemetry Event
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	@Override
	public void onRequest(String address, SPNEventRequest spner) {

		Intent broadcast = new Intent("TRACKER-SPN-REFRESH");
		AppModel.getInstance().mLastSPNEv = spner.mSPNEv;											// mLastSPNEv / mSPNEv = *** ? ***

		AppModel.getInstance().mLastSen = spner.mSen;
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

		// ACK the SPN
		AckSPNEvent ack = new AckSPNEvent(0, spner.mSen);
		mTracker.sendResponse(ack, null, null);
	}

	@Override
	public void onResponse(final String address,final GetTrackerInfoResponse tir)
	{
		if (tir.getStatus() != 0) {
			Log.w(TAG, "GetTrackerInfoResponse: S="+tir.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-REFRESH");
		AppModel.getInstance().mTrackerInfo = tir.mTi;												// mTrackerInfo / mTi = TRACKER info (if it's PT30 or PT40)
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		// PT30 Compatibility - Extract VIN, for PT30
		if (tir.mTi.product.contains("30") && tir.containsKey(BaseResponse.Key.VIN)) {
			// PT30 workaround - sends a null tag, if VIN is not present
			String vin = tir.getValue(BaseResponse.Key.VIN);
			Log.i("INFO", "\n VIN: \t" + vin);
			if (!TextUtils.isEmpty(vin)) {
				AppModel.getInstance().mPT30Vin = vin;
			} else {
				AppModel.getInstance().mPT30Vin = "";
			}
		}

		if (tir.mTi.product.contains("30")) {
			// Get Tracker system vars
			Log.i(TAG, "Get Tracker SV:PE ...");
			GetSystemVar gsv = new GetSystemVar(SystemVar.PERIODIC_EVENT_GAP);
			mTracker.sendRequest(gsv, null, null);
		} else {
			// Get Tracker system vars
			Log.i(TAG, "Get Tracker SV:HUC ...");
			GetSystemVar gsv = new GetSystemVar("HUC");
			mTracker.sendRequest(gsv, null, null);
		}

	}

	@Override
	public void onResponse(final String address,final GetVehicleInfoResponse vir)
	{
		if (vir.getStatus() != 0) {
			Log.w(TAG, "GetVehicleInfoResponse: S="+vir.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-VIN-REFRESH");
		AppModel.getInstance().mVehicleInfo = vir.mVi;												// mVehicleInfo / mVi.VIN = Vehicle VIN
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		Log.i("VEH_INFO", vir.mVi.VIN);
		setVehVinNum(vir.mVi.VIN);
		Log.i("VEH_VIN_NUM", getVehVinNum());



	}

	@Override
	public void onResponse(final String address, RetrieveStoredEventsResponse rser) {
		if (rser.getStatus() != 0) {
			Log.w(TAG, "RetrieveStoredEventsResponse: S="+rser.getStatus());
			return;
		}
		// NOP - The events shall be updated in the Stored events tile
	}


	@Override
	public void onResponse(final String address,final GetDiagTroubleCodesResponse dtcr)
	{

		if (dtcr.getStatus() != 0) {
			Log.w(TAG, "GetDiagTroubleCodesResponse: S="+dtcr.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-DTC-REFRESH");
		broadcast.putExtra(EXTRA_RESP_ACTION_KEY, "GET" );
		AppModel.getInstance().mLastDTC = dtcr.mDTC;												// mLastDTC / mDTC = Last Diagnostic Trouble Code
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	@Override
	public void onResponse(final String address, GetStoredEventsCountResponse gsecr) {

		if (gsecr.getStatus() != 0) {
			Log.w(TAG, "GetStoredEventsCountResponse: S="+gsecr.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-SE-REFRESH");
		AppModel.getInstance().mLastSECount= gsecr.mCount;											// mLastSECount / mCount = Stored Event Count
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	public static final String EXTRA_RESP_STATUS_KEY = "status";
	public static final String EXTRA_RESP_ACTION_KEY = "action";


	@Override
	public void onResponse(final String address,final ClearDiagTroubleCodesResponse cdtcr)
	{
		if (cdtcr.getStatus() != 0) {
			Log.w(TAG, "GetDiagTroubleCodesResponse: S="+cdtcr.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-DTC-REFRESH");
		broadcast.putExtra(EXTRA_RESP_ACTION_KEY, "CLEAR" );
		broadcast.putExtra(EXTRA_RESP_STATUS_KEY, cdtcr.getStatus() );
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	@Override
	public void onResponse(final String address, ClearStoredEventsResponse cser) {
		if (cser.getStatus() != 0) {
			Log.w(TAG, "ClearStoredEventsResponse: S="+cser.getStatus());
			return;
		}

		Intent broadcast = new Intent("TRACKER-SE-REFRESH");
		AppModel.getInstance().mLastSECount= 0;
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	@Override
	public void onResponse(final String address,GetSystemVarResponse gsvr) {

		if (gsvr.getStatus() != 0) {
			Log.w(TAG, "GetSystemVarResponse: S="+gsvr.getStatus());
			return;
		}


        if (!TextUtils.isEmpty(gsvr.mTag) && gsvr.mTag.equals(SystemVar.PERIODIC_EVENT_GAP.mVal)) {
			// App model and shared pref
            AppModel.getInstance().mPE = gsvr.mVal;
            Log.d(TAG, "SV: PE = "+gsvr.mVal);
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString("sv_pe", gsvr.mVal);
			editor.commit();
        } else if (!TextUtils.isEmpty(gsvr.mTag) && gsvr.mTag.equals("HUC")) { //PT-40
			// App model and shared pref
			AppModel.getInstance().mPE = gsvr.mVal;
			Log.d(TAG, "SV: HUC = "+gsvr.mVal);
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString("sv_pe", gsvr.mVal);
			editor.commit();
		}
	}

	@Override
	public void onResponse(final String address,SetSystemVarResponse ssvr) {
        // NOP
	}

	@Override
	public void onResponse(String s, ConfigureSPNEventResponse configureSPNEventResponse) {
		// NOP
	}


	public static final String EXTRA_TRACKER_UPDATE_ACTION_KEY = "action";
	public static final String EXTRA_TRACKER_UPDATE_ARG_KEY = "arg";

	public static final int EXTRA_TRACKER_UPDATE_ACTION_UPTODATE = 0;
	public static final int EXTRA_TRACKER_UPDATE_ACTION_STARTED = 1;
	public static final int EXTRA_TRACKER_UPDATE_ACTION_PROG = 2;
	public static final int EXTRA_TRACKER_UPDATE_ACTION_COMPLETED = 3;
	public static final int EXTRA_TRACKER_UPDATE_ACTION_UPDATED = 4;
	public static final int EXTRA_TRACKER_UPDATE_ACTION_FAILED = -1;

	@Override
	public void onFwUptodate(final String address) {
		final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_UPTODATE ); // UPTODATE
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}



	@Override
	public void onFileUpdateStarted(final String address, final String fn) {
		createUpdateNotification("Updating "+fn+" ...");

        final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_STARTED ); // STARTED
		intent.putExtra("arg", fn );
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

	}

	@Override
	public void onFileUpdateProgress(final String address, final int percentage) {
		final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_PROG ); // PROGRESS
		intent.putExtra("arg", percentage );
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}


	@Override
	public void onFileUpdateCompleted(final String address) {
		_cancelUpdateNotifications();
		final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_COMPLETED ); // COMPLETED
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

	}

	@Override
	public void onFileUpdateFailed(final String address, final TSError tsError) {
		_cancelUpdateNotifications();
		final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_FAILED ); // FAILED
		intent.putExtra(TSError.KEY_CODE, tsError.mCode );
		intent.putExtra(TSError.KEY_CAUSE, tsError.mCause );

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public void onFwUpdated(final String address,TrackerInfo ti) {
		final Intent intent = new Intent("TRACKER-UPDATE");
		intent.putExtra("action", EXTRA_TRACKER_UPDATE_ACTION_UPDATED ); // UPDATED
		AppModel.getInstance().mTrackerInfo = ti;
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	/**
	 * Sets the service as a foreground service
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	private void startForegroundService() {
		// when the activity closes we need to show the notification that user is connected to the peripheral sensor
		// We start the service as a foreground service as Android 8.0 (Oreo) onwards kills any running background services
		final Notification notification = createNotification(R.string.tracker_running, 0);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForeground(CONNECTION_NOTI_ID, notification);
		} else {
			final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(CONNECTION_NOTI_ID, notification);
		}
	}

	/**
	 * Stops the service as a foreground service
	 */
	private void stopForegroundService() {
		// when the activity rebinds to the service, remove the notification and stop the foreground service
		// on devices running Android 8.0 (Oreo) or above
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopForeground(true);
		} else {
			cancelNotifications();
		}
	}


	/**
	 * Creates the notification
	 * 
	 * @param messageResId
	 *            message resource id. The message must have one String parameter,<br />
	 *            f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
	 * @param defaults
	 *            signals that will be used to notify the user
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	private Notification createNotification(final int messageResId, final int defaults) {
		final Intent parentIntent = new Intent(this, TrackerManagerActivity.class);
		parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		final Intent disconnect = new Intent(ACTION_DISCONNECT);
		disconnect.putExtra(EXTRA_SOURCE, SOURCE_NOTIFICATION);
		//final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

		// activity has launchMode="singleTask" , so if the task is already running, it will be resumed
		final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[] { parentIntent }, PendingIntent.FLAG_IMMUTABLE);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, App.CONNECTED_DEVICE_CHANNEL);
		builder.setContentIntent(pendingIntent);
		builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
		builder.setSmallIcon(R.drawable.ic_app_icon);
		builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
		// FIXME - Handle disconnect from noti
		//builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.disconnect), disconnectAction));
		return builder.build();
	}

	/**
	 * Cancels the existing notification. If there is no active notification this method does nothing
	 */
	private void cancelNotifications() {
		final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(CONNECTION_NOTI_ID);
		nm.cancel(UPDATE_NOTI_ID);
	}

	private void createUpdateNotification(final String msg) {

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, App.UPDATE_CHANNEL);
		builder.setSmallIcon(R.drawable.ic_app_icon);
		builder.setContentTitle(getString(R.string.app_name)).setContentText(msg);

		final Notification notification = builder.build();

		final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(UPDATE_NOTI_ID, notification);
	}

	private void _cancelUpdateNotifications() {
		final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(UPDATE_NOTI_ID);
	}


	/**
	 * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
	 */
	private final BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_NOTIFICATION);
			switch (source) {
				case SOURCE_NOTIFICATION:
					Logger.i(getLogSession(), "[Notification] Disconnect action pressed");
					break;
				case SOURCE_WEARABLE:
					Logger.i(getLogSession(), "[WEAR] '" + "Constants.ACTION_DISCONNECT" + "' message received");
					break;
			}
			if (isConnected())
				getBinder().disconnect(context);
			else
				stopSelf();
		}
	};

	/**
	 * Broadcast receiver that listens for {@link #ACTION_SEND} from other apps. Sends the String or int content of the {@link Intent#EXTRA_TEXT} extra to the remote device.
	 * The integer content will be sent as String (65 -> "65", not 65 -> "A").
	 */
	private BroadcastReceiver mIntentBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean hasMessage = intent.hasExtra(Intent.EXTRA_TEXT);
			if (hasMessage) {
				String message = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (message == null) {
					final int intValue = intent.getIntExtra(Intent.EXTRA_TEXT, Integer.MIN_VALUE); // how big is the chance of such data?
					if (intValue != Integer.MIN_VALUE)
						message = String.valueOf(intValue);
				}

				if (message != null) {
					final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_3RD_PARTY);
					switch (source) {
						case SOURCE_WEARABLE:
							Logger.i(getLogSession(), "[WEAR] '" + "Constants.UART.COMMAND" + "' message received with data: \"" + message + "\"");
							break;
						case SOURCE_3RD_PARTY:
						default:
							Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received with data: \"" + message + "\"");
							break;
					}
					//mTracker.send(message);
					return;
				}
			}
			// No data od incompatible type of EXTRA_TEXT
			if (!hasMessage)
				Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received no data.");
			else
				Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received incompatible data type. Only String and int are supported.");
		}
	};
}
