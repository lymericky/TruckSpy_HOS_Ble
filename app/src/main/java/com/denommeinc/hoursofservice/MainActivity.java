package com.denommeinc.hoursofservice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pt.devicemanager.TrackerManagerActivity;

public class MainActivity extends AppCompatActivity {

    private Button go_btn;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishAffinity();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        go_btn = findViewById(R.id.button);

        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, com.pt.devicemanager.R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, com.pt.devicemanager.R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        go_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goIntent = new Intent(MainActivity.this, TrackerManagerActivity.class);
                startActivity(goIntent);
            }
        });


    }


}