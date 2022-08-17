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

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.encoders.json.BuildConfig;
import com.pt.sdk.Sdk;
//import com.squareup.leakcanary.LeakCanary;

public class App extends Application {

    public static final String CONNECTED_DEVICE_CHANNEL = "connected_device_channel";
    public static final String UPDATE_CHANNEL = "update_channel";

    AppModel mModel;

    @Override
        public void onCreate()
        {
        super.onCreate();

//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
        // PT sdk init
        Sdk.getInstance().setLogger(new DefaultLogger());
        Sdk.getInstance().initialize(this);
        // Note: For PT managed service user, provide your API key here or update the app/build.gradle
        //Sdk.getInstance().setApiKey();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //DfuServiceInitiator.createDfuNotificationChannel(this);

            final NotificationChannel channel = new NotificationChannel(CONNECTED_DEVICE_CHANNEL, getString(R.string.channel_connected_devices_title), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_connected_devices_description));
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            final NotificationChannel upd_channel = new NotificationChannel(UPDATE_CHANNEL, getString(R.string.channel_update_title), NotificationManager.IMPORTANCE_LOW);
            upd_channel.setDescription(getString(R.string.channel_update_description));
            upd_channel.setShowBadge(false);
            upd_channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(upd_channel);
        }


        Log.d(AppModel.TAG,"App created ...");

        mModel = AppModel.getInstance();


    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        Log.d(AppModel.TAG,"App terminated. ----------");
    }
}
