package com.shiitakeo.android_wear_for_ios;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

public class MainActivity extends Activity {

    private static final String TAG_LOG = "BLE_wear";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG_LOG, "-=-=-=-=-=-=-=-= onCreate -=-=-=-=-=-=-=-=-=");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG_LOG, "not supported ble");
            finish();
        }
        startService(new Intent(MainActivity.this, BLEService.class));
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG_LOG, "-=-=-=-=-=-=-=-= onDestroy -=-=-=-=-=-=-=-=-=");
        stopService(new Intent(MainActivity.this, BLEService.class));
    }
}
