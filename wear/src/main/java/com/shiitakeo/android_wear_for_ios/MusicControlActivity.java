package com.shiitakeo.android_wear_for_ios;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.view.View.OnClickListener;

/**
 * Created by shiitakeo on 15/04/03.
 */
public class MusicControlActivity extends Activity {
    private int api_level = 20;

    private BluetoothLeScanner le_scanner;
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothGatt bluetooth_gatt;
    private BluetoothGattCharacteristic bluetooth_gatt_chara;
    private static Boolean is_connect = false;


    private static final String TAG_LOG = "BLE_wear";
    //ANCS Profile
    private static final String service_ancs = "7905f431-b5ce-4e99-a40f-4b1e122d00d0";
    private static final String characteristics_notification_source = "9fbf120d-6301-42d9-8c58-25e699a21dbd";
    private static final String characteristics_data_source =         "22eac6e9-24d6-4bb5-be44-b36ace7c7bfb";
    private static final String characteristics_control_point =       "69d1d8f3-45e1-49a8-9821-9bbdfdaad9d9";

    private static final String descriptor_config = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String service_blank = "00001111-0000-1000-8000-00805f9b34fb";
    String iphone_uuid = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_control);

        Log.d(TAG_LOG, "-=-=-=-=-=-=-=-= onCreate -=-=-=-=-=-=-=-=-=");


//        //イメージボタンの取得
//        ImageButton imageBtn=(ImageButton)findViewById(R.id.volume_down);
//        imageBtn.setOnClickListener(new OnClickListener() {
//            //クリック時のイベント処理
//            @Override
//            public void onClick(View view) {
//                bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x03});
//                bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
//                Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");
//            }
//        });
//
//        ImageButton button_prev =(ImageButton)findViewById(R.id.previous);
//        button_prev.setOnClickListener(new OnClickListener() {
//            //クリック時のイベント処理
//            @Override
//            public void onClick(View view) {
//                bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x04});
//                bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
//                Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");
//            }
//        });


        if(bluetooth_gatt != null) {
            bluetooth_gatt.disconnect();
            bluetooth_gatt.close();
            bluetooth_gatt = null;
        }
        if(bluetooth_adapter != null) {
            bluetooth_adapter = null;
        }
        if(api_level >= 21) {
            if (le_scanner != null) {
                Log.d(TAG_LOG, "status: ble reset");
                stop_le_scanner();
            }
        }
        is_connect = false;
        iphone_uuid = "";


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetooth_adapter == null) {
            Log.d(TAG_LOG, "ble adapter is null");
            return;
        }

        if(api_level >= 21) {
            Log.d(TAG_LOG, "start BLE scan @ lescan");
            start_le_scanner();
        }else {
            Log.d(TAG_LOG, "start BLE scan @ BluetoothAdapter");
            bluetooth_adapter.startLeScan(le_scan_callback);
        }















    }

    public void click_next_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x03});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }

    public void click_previous_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x04});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }
    public void click_start_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x01});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }

    public void click_stop_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x02});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }
    public void click_volume_up_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x05});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }

    public void click_volume_down_button(View view){
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x06});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);
        Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");

    }

    @TargetApi(21)
    private void start_le_scanner(){
        le_scanner = bluetooth_adapter.getBluetoothLeScanner();

//        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
//        le_scanner.startScan(scan_fillters(), settings, scan_callback);
//        le_scanner.startScan(scan_callback);
    }

    @TargetApi(21)
    private void stop_le_scanner(){
//        le_scanner.stopScan(scan_callback);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG_LOG, "~~~~~~~~ service onDestroy");
        if(api_level >= 21) {
            stop_le_scanner();
        }else {
            bluetooth_adapter.stopLeScan(le_scan_callback);
        }
        is_connect =false;
        iphone_uuid = "";

        if(null != bluetooth_gatt){
            bluetooth_gatt.disconnect();
            bluetooth_gatt.close();
            bluetooth_gatt = null;
        }
        bluetooth_adapter = null;
        super.onDestroy();
    }

    private List<ScanFilter> scan_fillters() {
        // can't find ancs service
        return create_scan_filter();
    }

    @TargetApi(21)
    private List<ScanFilter> create_scan_filter(){
//        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_ancs)).build();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_blank)).build();
        List<ScanFilter> list = new ArrayList<ScanFilter>(1);
        list.add(filter);
        return list;
    }


//    ScanCallback scan_callback = new ScanCallback() {

    @TargetApi(21)
    private class BLEScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            Log.d(TAG_LOG, "scan result" + result.toString());
//            Log.d(TAG_LOG, "device address" + result.getDevice().getAddress());
//            Log.d(TAG_LOG, "iphone address" + iphone_uuid);
//            Log.d(TAG_LOG, skip_count + "judge: " + result.getDevice().getAddress().toString().equals(iphone_uuid));
            BluetoothDevice device = result.getDevice();


        }


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG_LOG, "batchscan result" + results.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG_LOG, "onScanFailed" + errorCode);
        }
    }
//    };

    private BluetoothAdapter.LeScanCallback le_scan_callback = new BluetoothAdapter.LeScanCallback(){
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.i(TAG_LOG, "onLeScan: " );

            if (!is_connect) {
                Log.d(TAG_LOG, "is connect");
                if (device != null) {
                    Log.d(TAG_LOG, "device ");
                    if (device.getName() != null) {
//                        if(device.getName().equals("Blank")) {
                        if(device.getName().equals("BLE Utility")) {
                            Log.d(TAG_LOG, "getname: " + device.getName());
                            iphone_uuid = device.getAddress().toString();
                            is_connect = true;
                            bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                            bluetooth_adapter.stopLeScan(le_scan_callback);
                        }
                    }
                }
            }


        }
    };

    private final BluetoothGattCallback bluetooth_gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG_LOG, "onConnectionStateChange: " + status + " -> " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // success, connect to gatt.
                // find service
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG_LOG, "onDisconnect: ");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG_LOG, "onServicesDiscovered received: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetooth_gatt = gatt;

                //subscribe characteristic notification characteristic
                BluetoothGattService service = gatt.getService(UUID.fromString(service_blank));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("00001111-0001-1000-8000-00805f9b34fb"));

                if (characteristic == null) {
                    Log.d(TAG_LOG, " cant find chara");
                } else {
                    Log.d(TAG_LOG, " ** find chara :: " + characteristic.getUuid());
//                    if ("0001".equals(characteristic.getUuid().toString())) {
                    Log.d(TAG_LOG, " set notify:: " + characteristic.getUuid());
                    bluetooth_gatt_chara = characteristic;
//                        characteristic.setValue(new byte[]{(byte) 0x03});
//                        gatt.writeCharacteristic(characteristic);
                    Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");
//                                bluetooth_gatt.setCharacteristicNotification(characteristic, true);
//                                BluetoothGattDescriptor notify_descriptor = characteristic.getDescriptor(
//                                        UUID.fromString(descriptor_config));
//                                if (descriptor == null) {
//                                    Log.d(TAG_LOG, " ** not find desc :: " + notify_descriptor.getUuid());
//                                } else {
//                                    Log.d(TAG_LOG, " ** find desc :: " + notify_descriptor.getUuid());
//                                    notify_descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                    bluetooth_gatt.writeDescriptor(notify_descriptor);
//                                    is_subscribed_characteristics = true;
//                                }
//                    }
                }



//                BluetoothGattService service = gatt.getService(UUID.fromString(service_ancs));
//                if (service == null) {
//                    Log.d(TAG_LOG, "cant find service");
//                } else {
//                    Log.d(TAG_LOG, "find service");
//                    Log.d(TAG_LOG, String.valueOf(bluetooth_gatt.getServices()));
//
//                    // subscribe data source characteristic
//                    BluetoothGattCharacteristic data_characteristic = service.getCharacteristic(UUID.fromString(characteristics_data_source));
//
//                    if (data_characteristic == null) {
//                        Log.d(TAG_LOG, "cant find data source chara");
//                    } else {
//                        Log.d(TAG_LOG, "find data source chara :: " + data_characteristic.getUuid());
//                        Log.d(TAG_LOG, "set notify:: " + data_characteristic.getUuid());
//                        bluetooth_gatt.setCharacteristicNotification(data_characteristic, true);
//                        BluetoothGattDescriptor descriptor = data_characteristic.getDescriptor(
//                                UUID.fromString(descriptor_config));
//                        if(descriptor == null){
//                            Log.d(TAG_LOG, " ** cant find desc :: " + descriptor.getUuid());
//                        }else{
//                            Log.d(TAG_LOG, " ** find desc :: " + descriptor.getUuid());
//                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                            bluetooth_gatt.writeDescriptor(descriptor);
//                            if(api_level >= 21) {
//                                stop_le_scanner();
//                            }else {
//                                bluetooth_adapter.stopLeScan(le_scan_callback);
//                            }
//                        }
//                    }
//                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG_LOG, " onDescriptorWrite:: " + status);
            // Notification source
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG_LOG, "status: write success ");
                //find music controll service
                Log.d(TAG_LOG, "*+*+*+*+*+*+*+*+*+*+ find music control");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG_LOG, "onCharacteristicChanged:: " + characteristic.getUuid().toString());
            //notify from data source characteristic
            //process get notitification packet from iphone.
            if (characteristics_data_source.toString().equals(characteristic.getUuid().toString())) {
                byte[] get_data = characteristic.getValue();
//                if(DEBUG){
//                StringBuilder stringBuilder = new StringBuilder();
//                for (byte byteChar : get_data) {
//                    stringBuilder.append(String.format("%02X", byteChar));
//                }
//
//                Log.d(TAG_LOG, "notify value:: " + stringBuilder.toString());
//                }

            }
        }
    };
}
