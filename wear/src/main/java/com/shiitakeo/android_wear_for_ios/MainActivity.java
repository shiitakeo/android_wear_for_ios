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
//        RootTools.isAccessGiven();

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
//            os.writeBytes("date -s 20120419.024012; \n");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2015, 04, 05, 16, 39, 00);


            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DATE);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
//        am.setTime(c.getTimeInMillis());
//            os.writeBytes("date -s 20120419.024012; \n");
            os.writeBytes("date -s 20150405.164600; \n");
//            os.writeBytes("date -s " + year + month + day + "." + hour + minute + second + "; \n");
        } catch (Exception e) {
            Log.d(TAG_LOG,"error=="+e.toString());
            e.printStackTrace();
        }
//        Command command = new Command(0, "chmod  777 /dev/alarm");
//        try {
//            RootTools.getShell(true).add(command);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        } catch (RootDeniedException e) {
//            e.printStackTrace();
//        }
////        CommandCapture command = new CommandCapture(0, "chmod  777 " + "\""+ MyFilePath+ "\"");
////        RootTools.getShell(true).add(command).waitForFinish();
//
//        Calendar c = Calendar.getInstance();
//        c.set(2013, 8, 15, 12, 34, 56);
//        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
//        am.setTime(c.getTimeInMillis());
//                SystemClock.setCurrentTimeMillis(c.getTimeInMillis());

//        Shell.
//            if (Sh.isSuAvailable()) {
//                ShellInterface.runCommand("chmod 666 /dev/alarm");
//                SystemClock.setCurrentTimeMillis(time);
//                ShellInterface.runCommand("chmod 664 /dev/alarm");
//            }


//        AlarmManager a = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//        long current_time_millies = System.currentTimeMillis();
//        try {
//            a.setTime((long)current_time_millies+10000);
//            SystemClock.setCurrentTimeMillis(current_time_millies + 10000);
//        } catch (Exception e) {
//// Why is this exception thrown?
//        }


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
