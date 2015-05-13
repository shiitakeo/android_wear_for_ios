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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.TextView;

/**
 * Created by shiitakeo on 15/04/03.
 */
public class MusicControlActivity extends Activity {
    private static final String TAG_LOG = "BLE_wear";

    // BluetoothLeScanner does not work properly in my environment...
//    private int api_level = Build.VERSION.SDK_INT;
    private int api_level = 20;

    private BluetoothLeScanner le_scanner;
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothGatt bluetooth_gatt;
    private BluetoothGattCharacteristic bluetooth_gatt_chara;
    private static Boolean is_connect = false;

    //ams Profile
    private static final String service_ams = "89d3502b-0f36-433a-8ef4-c502ad55f8dc";
    private static final String characteristics_remote_command = "9b3c81d8-57b1-4a8a-b8df-0e56f7ca51c2";
    private static final String characteristics_entity_update = "2f7cabce-808d-411f-9a0c-bb92ba96c102";
    private static final String descriptor_config = "00002902-0000-1000-8000-00805f9b34fb";

    String iphone_uuid = "";
    Boolean is_set_entity = false;
    private BroadcastReceiver message_receiver = new MessageReceiver();

    // intent action
    String set_artist_info = "com.shiitakeo.set_artist";
    String set_title_info = "com.shiitakeo.set_title";
    String extra_data = "com.shiitakeo.extra_data";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_control);

        Log.d(TAG_LOG, "-=-=-=-=-=-=-=-= onCreate @ MusicControlActivity -=-=-=-=-=-=-=-=-=");
        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction(set_artist_info);
        intent_filter.addAction(set_title_info);
        registerReceiver(message_receiver, intent_filter);

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
        Log.d(TAG_LOG, "-=-=-=-=-= next song -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x03});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

    }

    public void click_previous_button(View view){
        Log.d(TAG_LOG, "-=-=-=-=-= previous song -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x04});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

    }
    public void click_start_button(View view){
        Log.d(TAG_LOG, "-=-=-=-=-= start -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x01});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

    }

    public void click_stop_button(View view){
        Log.d(TAG_LOG, "-=-=-=-=-= stop -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x02});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

    }
    public void click_volume_up_button(View view){
        Log.d(TAG_LOG, "-=-=-=-=-= volume up -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x05});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

    }

    public void click_volume_down_button(View view){
        Log.d(TAG_LOG, "-=-=-=-=-= volume down -=-==-");
        bluetooth_gatt_chara.setValue(new byte[]{(byte) 0x06});
        bluetooth_gatt.writeCharacteristic(bluetooth_gatt_chara);

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
        Log.d(TAG_LOG, "~~~~~~~~ onDestroy @ Music Control Acitivity");
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
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_ams)).build();
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
                if(!is_set_entity){
                bluetooth_gatt = gatt;

                // for ams
                BluetoothGattService service = gatt.getService(UUID.fromString(service_ams));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristics_remote_command));

                if (characteristic == null) {
                    Log.d(TAG_LOG, " cant find chara");
                } else {
                    Log.d(TAG_LOG, " ** find chara :: " + characteristic.getUuid());
//                    if ("0001".equals(characteristic.getUuid().toString())) {
                    Log.d(TAG_LOG, " set notify:: " + characteristic.getUuid());
                    bluetooth_gatt_chara = characteristic;

                    request_media_info();
                }
            }else {
                try {
                    Log.d(TAG_LOG, "-=-=-=-=- set request -=-=-==-=-=-=");
                    BluetoothGattService service = bluetooth_gatt.getService(UUID.fromString(service_ams));
                    if(service != null) {
                        BluetoothGattCharacteristic chara = service.getCharacteristic(UUID.fromString(characteristics_entity_update));
                        if(chara != null) {
                            bluetooth_gatt.setCharacteristicNotification(chara, true);
                            BluetoothGattDescriptor desc = chara.getDescriptor(UUID.fromString(descriptor_config));
                            if(desc != null) {
                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                bluetooth_gatt.writeDescriptor(desc);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }
        }

        private void request_media_info(){
            is_set_entity = true;
            bluetooth_gatt.discoverServices();
            Log.d(TAG_LOG, "request media info");
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG_LOG, " onDescriptorWrite:: " + status);
            // Notification source
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG_LOG, "status: write success ");
                //find music controll service
                Log.d(TAG_LOG, "*+*+*+*+*+*+*+*+*+*+ find music control");
                BluetoothGattService service = bluetooth_gatt.getService(UUID.fromString(service_ams));
                if(service != null) {
                    BluetoothGattCharacteristic chara = service.getCharacteristic(UUID.fromString(characteristics_entity_update));
                    if(chara != null) {
                        chara.setValue(new byte[]{(byte) 0x02, (byte) 0x00, (byte) 0x02});
                        bluetooth_gatt.writeCharacteristic(chara);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG_LOG, "onCharacteristicRead:: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG_LOG, "+onCharacteristicRead:: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG_LOG, "onCharacteristicChanged:: " + characteristic.getUuid().toString());

            if (characteristics_entity_update.toString().equals(characteristic.getUuid().toString())) {
                Log.d(TAG_LOG, "entity update ");

                byte[] get_data = characteristic.getValue();
                String str = characteristic.getStringValue(3);
                Log.d(TAG_LOG, "new music info: " + str);

                if(get_data[1] == (byte)0x00) {
                    Log.d(TAG_LOG, "get artist info");

                    Intent _intent_positive = new Intent();
                    _intent_positive.setAction(set_artist_info);
                    _intent_positive.putExtra(extra_data, str);
                    sendBroadcast(_intent_positive);
                }else if(get_data[1] == (byte)0x02) {
                    Log.d(TAG_LOG, "get title info");

                    Intent _intent_positive = new Intent();
                    _intent_positive.setAction(set_title_info);
                    _intent_positive.putExtra(extra_data, str);
                    sendBroadcast(_intent_positive);
                }
            }
        }
    };

    @TargetApi(20)
    public class MessageReceiver extends BroadcastReceiver {
        private static final String TAG_LOG = "BLE_wear";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG_LOG, "onReceive");
            String action = intent.getAction();

            // perform notification action: immediately
            // delete intent: after 7~8sec.
            if (action.equals(set_artist_info)) {
                Log.d(TAG_LOG, "get action: " + action);

                String str = intent.getStringExtra(extra_data);
                Log.d(TAG_LOG, "get artist info: " + str);

                TextView textView = (TextView) findViewById(R.id.text_artist);
                textView.setText(str);
            }else if (action.equals(set_title_info)) {
                Log.d(TAG_LOG, "get action: " + action);

                String str = intent.getStringExtra(extra_data);
                Log.d(TAG_LOG, "get title info: " + str);

                TextView textView = (TextView) findViewById(R.id.text_title);
                textView.setText(str);
            }
        }
    }
}
