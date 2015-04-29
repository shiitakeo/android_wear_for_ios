package com.shiitakeo.android_wear_for_ios;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by shiitakeo on 15/03/15.
 */
public class BLEService extends Service{
//    private int api_level = Build.VERSION.SDK_INT;
    private int api_level = 20;

    private BluetoothAdapter bluetooth_adapter;
    private BluetoothGatt bluetooth_gatt;
    private static Boolean is_connect = false;


    private static final String TAG_LOG = "BLE_wear";
    //ANCS Profile
    private static final String service_ancs = "7905f431-b5ce-4e99-a40f-4b1e122d00d0";
    private static final String characteristics_notification_source = "9fbf120d-6301-42d9-8c58-25e699a21dbd";
    private static final String characteristics_data_source =         "22eac6e9-24d6-4bb5-be44-b36ace7c7bfb";
    private static final String characteristics_control_point =       "69d1d8f3-45e1-49a8-9821-9bbdfdaad9d9";

    private static final String descriptor_config = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String service_blank = "00001111-0000-1000-8000-00805f9b34fb";

    //AMS Profile
    private static final String service_ams = "89d3502b-0f36-433a-8ef4-c502ad55f8dc";
    private static final String characteristics_remote_command = "9b3c81d8-57b1-4a8a-b8df-0e56f7ca51c2";
    private static final String characteristics_entity_update = "2f7cabce-808d-411f-9a0c-bb92ba96c102";
    private static final String characteristics_entity_attribute = "c6b2f38c-23ab-46d8-a6ab-a3a870bbd5d7";

    //CTS Profile
    private static final String service_cts = "00001805-0000-1000-8000-00805f9b34fb";
    private static final String characteristics_current_time = "00002a2b-0000-1000-8000-00805f9b34fb";


    private Boolean is_subscribed_characteristics = false;

    private Vibrator vib;
    private long pattern[] = {200, 100, 200, 100};


    private BluetoothLeScanner le_scanner;

    private NotificationManagerCompat notificationManager;
    private int notification_id = 0;

    private PowerManager.WakeLock wake_lock;

    private PacketProcessor packet_processor;

    private IconImageManager icon_image_manager;

    private BroadcastReceiver message_receiver = new MessageReceiver();

    private byte[] uid = new byte[4];

    // intent action
    String action_positive = "com.shiitakeo.perform_notification_action_positive";
    String action_negative = "com.shiitakeo.perform_notification_action_negative";
    String action_delete = "com.shiitakeo.delete";
    String action_renotify = "com.shiitakeo.renotify";
    String action_set_clock = "com.shiitakeo.set_clock";
    String extra_uid = "com.shiitakeo.extra_uid";

    private static final long screen_time_out = 1000;
    private Boolean is_reconnect = false;
    private int skip_count = 0;
    BLEScanCallback scan_callback = new BLEScanCallback();
    String iphone_uuid;

    long start_time;
    Boolean is_time = false;
    Boolean is_music_control = false;
    int id_music_control = 99;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG_LOG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }

    @TargetApi(20)
    @Override
    public void onStart(Intent intent, int startID) {
        Log.d(TAG_LOG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction(action_positive);
        intent_filter.addAction(action_negative);
        intent_filter.addAction(action_delete);
        intent_filter.addAction(action_renotify);
        registerReceiver(message_receiver, intent_filter);




        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        packet_processor = new PacketProcessor();
        icon_image_manager = new IconImageManager();

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
        is_subscribed_characteristics = false;
        iphone_uuid = "";
        start_time = 0;
        is_time = false;


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


        Intent _intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent music_control_intent = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent, PendingIntent.FLAG_CANCEL_CURRENT);


// この MainActivity は Wear アプリの MainActivity
        Intent __intent = new Intent(this, MusicControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, __intent, 0);

// カードをタップしたときに表示される Activity
// ここでは ImageView 1つだけのレイアウト
        Intent displayIntent = new Intent(this, MusicControlActivity.class);
        PendingIntent displayPendingIntent = PendingIntent.getActivity(this,
                0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        Notification.Extender wearableExtender =
//                new Notification.Extender()
//                        .setDisplayIntent(displayPendingIntent);
        Intent _intent_delete = new Intent();
        _intent_delete.setAction(action_renotify);
        PendingIntent _delete_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_delete, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notificationBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.whatsapp)
                        .setContentTitle("music💿")
//                        .setContentTitle("tv📺")
                        .setContentIntent(pendingIntent)
//                        .extend(wearableExtender);
                .addAction(R.drawable.resume, "Controller Open", pendingIntent)
                        .setLocalOnly(true)
                .extend(new Notification.WearableExtender().setContentAction(0).setHintHideIcon(true))
//                        .addAction(R.drawable.decline, "delete music controller", _delete_action)
//                        .setLocalOnly(true)
//                        .setDeleteIntent(_delete_action)
//                .extend(new Notification.WearableExtender().setDisplayIntent(displayPendingIntent))
        ;

        Notification _notification = notificationBuilder.build();
//        _notification.flags = _notification.flags | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        _notification.flags = _notification.flags | Notification.FLAG_ONGOING_EVENT;
//        _notification.flags = _notification.flags | Notification.FLAG_NO_CLEAR ;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.notify(notification_id, notificationBuilder.build());
        notificationManager.notify(id_music_control, _notification);
        notification_id++;

    }

    @TargetApi(21)
    private void start_le_scanner(){
        le_scanner = bluetooth_adapter.getBluetoothLeScanner();

//        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
//        le_scanner.startScan(scan_fillters(), settings, scan_callback);
        le_scanner.startScan(scan_callback);
    }

    @TargetApi(21)
    private void stop_le_scanner(){
        le_scanner.stopScan(scan_callback);
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
        is_subscribed_characteristics = false;
        iphone_uuid = "";
        is_time = false;

        if(null != bluetooth_gatt){
            bluetooth_gatt.disconnect();
            bluetooth_gatt.close();
            bluetooth_gatt = null;
        }
        bluetooth_adapter = null;
        if(notificationManager != null) {
            notificationManager.cancel(id_music_control);
            notificationManager.cancelAll();
            notificationManager = null;
        }
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


            if (!is_connect) {
                Log.d(TAG_LOG, "is connect");
                if (device != null) {
                    Log.d(TAG_LOG, "device ");
                    if (!is_reconnect && device.getName() != null) {
                        if(device.getName().equals("Blank")) {
//                        if(device.getName().equals("BLE Utility")) {
                            Log.d(TAG_LOG, "getname ");
                            iphone_uuid = device.getAddress().toString();
                            is_connect = true;
                            bluetooth_gatt = result.getDevice().connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                        }
//                    } else if (is_reconnect && skip_count > 5 && device.getName() != null) {
//                    } else if (is_reconnect && skip_count > 5 && device.getAddress().toString().equals(iphone_uuid)) {
                    } else if (is_reconnect && device.getAddress().toString().equals(iphone_uuid)) {
                        if(!is_time) {
                            Log.d(TAG_LOG, "-=-=-=-=-=-=-=-=-=- timer start:: -=-==-=-=-=-=-=-==--=-=-=-==-=-=-=-=-=-=-=-");
                            start_time = System.currentTimeMillis();
                            is_time = true;
                        }else if(System.currentTimeMillis() - start_time > 5000){
                            Log.d(TAG_LOG, "-=-=-=-=-=-=-=-=-=- reconnect:: -=-==-=-=-=-=-=-==--=-=-=-==-=-=-=-=-=-=-=-");
                            is_connect = true;
                            is_reconnect = false;
                            bluetooth_gatt = device.connectGatt(getApplicationContext(), true, bluetooth_gattCallback);
//                            bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                        }
                    } else {
                        Log.d(TAG_LOG, "skip:: ");
                        skip_count++;
                    }
                }
            }
        }

        ;

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
            Log.i(TAG_LOG, "onLeScan");
//            if(!is_connect) {
//                Log.d(TAG_LOG, "is connect");
//                if (device != null) {
//                    Log.d(TAG_LOG, "device ");
//                    if (!is_reconnect && device.getName() != null) {
//                        Log.d(TAG_LOG, "getname ");
//                        is_connect = true;
//                        bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
//                    }else if(is_reconnect && skip_count > 5 && device.getName() != null){
//                        Log.d(TAG_LOG, "reconnect:: ");
//                        is_connect = true;
//                        is_reconnect = false;
//                        bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
//                    }else {
//                        Log.d(TAG_LOG, "skip:: ");
//                        skip_count++;
//                    }
//                }
//            }

            if (!is_connect) {
                Log.d(TAG_LOG, "is connect");
                if (device != null) {
                    Log.d(TAG_LOG, "device ");
                    if (!is_reconnect && device.getName() != null) {
//                        if(device.getName().equals("Blank")) {
                        if(device.getName().equals("BLE Utility")) {
                            Log.d(TAG_LOG, "getname ");
                            iphone_uuid = device.getAddress().toString();
                            Log.d(TAG_LOG, "iphone_uuid: " + iphone_uuid);
                            is_connect = true;
                            //----*

//                            // get paired devices
//                            Set<BluetoothDevice> _paired_devices = bluetooth_adapter.getBondedDevices();
//
//                            Log.d(TAG_LOG, "for1 *=*=8=*+*+8=8+*+8=*+*+*=8+*+*8+*+*=*+*+*=8+*+*+8+*+*+*=*+8=*+8+*+8=8+*=");
//                            for(BluetoothDevice _paired_device: _paired_devices) {
//                                Log.d(TAG_LOG, "paired device: " + _paired_device + " :: " + _paired_device.getName());
//                                Log.d(TAG_LOG, "++ " + _paired_device.getAddress() + " :: " + _paired_device.getUuids());
//                                if(_paired_device.getAddress().toString().equals(iphone_uuid)) {
//                                    Log.d(TAG_LOG, "find 1: *=*=8=*+*+8=8+*+8=*+*+*=8+*+*8+*+*=*+*+*=8+*+*+8+*+*+*=*+8=*+8+*+8=8+*=");
//                                    try {
//
//                                        BluetoothSocket b_s = _paired_device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//                                        b_s.connect();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//
//                                }
//                            }
                            //----*
                            Log.d(TAG_LOG, "1: connect gatt");
                            bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                        }

//                    } else if (is_reconnect && skip_count > 5 && device.getName() != null) {
//                    } else if (is_reconnect && skip_count > 5 && device.getAddress().toString().equals(iphone_uuid)) {
                    } else if (is_reconnect && device.getAddress().toString().equals(iphone_uuid)) {
                        if(!is_time) {
                            Log.d(TAG_LOG, "-=-=-=-=-=-=-=-=-=- timer start:: -=-==-=-=-=-=-=-==--=-=-=-==-=-=-=-=-=-=-=-");
                            start_time = System.currentTimeMillis();
                            is_time = true;
                        }else if(System.currentTimeMillis() - start_time > 5000){
                            Log.d(TAG_LOG, "-=-=-=-=-=-=-=-=-=- reconnect:: -=-==-=-=-=-=-=-==--=-=-=-==-=-=-=-=-=-=-=-");
                            is_connect = true;
                            is_reconnect = false;
                            //----*

                            // get paired devices
                            Set<BluetoothDevice> _paired_devices = bluetooth_adapter.getBondedDevices();

                            Log.d(TAG_LOG, "for1 *=*=8=*+*+8=8+*+8=*+*+*=8+*+*8+*+*=*+*+*=8+*+*+8+*+*+*=*+8=*+8+*+8=8+*=");
                            for(BluetoothDevice _paired_device: _paired_devices) {
                                Log.d(TAG_LOG, "paired device: " + _paired_device + " :: " + _paired_device.getName());
                                Log.d(TAG_LOG, "++ " + _paired_device.getAddress() + " :: " + _paired_device.getUuids());
                                if(_paired_device.getAddress().toString().equals(iphone_uuid)) {
                                    Log.d(TAG_LOG, "find 1: *=*=8=*+*+8=8+*+8=*+*+*=8+*+*8+*+*=*+*+*=8+*+*+8+*+*+*=*+8=*+8+*+8=8+*=");
                                    try {

                                        BluetoothSocket b_s = _paired_device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                        b_s.connect();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                            //----*
                            Log.d(TAG_LOG, "connect gatt");
//                            bluetooth_gatt = device.connectGatt(getApplicationContext(), true, bluetooth_gattCallback);
                            bluetooth_gatt = device.connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                        }
                    } else {
                        Log.d(TAG_LOG, "skip:: ");
                        skip_count++;
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

                if(api_level >= 21) {
                    if (le_scanner != null) {
                        Log.d(TAG_LOG, "status: ble reset");
                        stop_le_scanner();
                    }
                }
                if(bluetooth_gatt != null) {
                    bluetooth_gatt.disconnect();
                    bluetooth_gatt.close();
                    bluetooth_gatt = null;
                }
                if(bluetooth_adapter != null) {
                    bluetooth_adapter = null;
                }
                is_connect = false;
                is_subscribed_characteristics = false;
                skip_count = 0;
                is_time = false;


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


                is_reconnect = true;
                Log.d(TAG_LOG, "start BLE scan");
                if(api_level >= 21) {
                    start_le_scanner();
                }else {
                    bluetooth_adapter.startLeScan(le_scan_callback);
                }

                //execute success animation
//                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
//                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "airplane mode on -> off, after restart app.");
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);


//                Intent _intent = new Intent(Intent.ACTION_MAIN);
//                intent.addCategory(Intent.CATEGORY_HOME);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//                startActivity(_intent);




                // time intent
                // intent -> connect gatt -> discover time service -> read time service chara
                //
                Intent _intent_set_clock = new Intent();
                _intent_set_clock.setAction(action_set_clock);
                PendingIntent _set_clock_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_set_clock, PendingIntent.FLAG_CANCEL_CURRENT);
                notification_id++;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG_LOG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(service_ancs));
                if (service == null) {
                    Log.d(TAG_LOG, "cant find service");
                } else {
                    Log.d(TAG_LOG, "find service");
                    Log.d(TAG_LOG, String.valueOf(bluetooth_gatt.getServices()));

                    // subscribe data source characteristic
                    BluetoothGattCharacteristic data_characteristic = service.getCharacteristic(UUID.fromString(characteristics_data_source));

                    if (data_characteristic == null) {
                        Log.d(TAG_LOG, "cant find data source chara");
                    } else {
                        Log.d(TAG_LOG, "find data source chara :: " + data_characteristic.getUuid());
                        Log.d(TAG_LOG, "set notify:: " + data_characteristic.getUuid());
                        bluetooth_gatt.setCharacteristicNotification(data_characteristic, true);
                        BluetoothGattDescriptor descriptor = data_characteristic.getDescriptor(
                                UUID.fromString(descriptor_config));
                        if(descriptor == null){
                            Log.d(TAG_LOG, " ** cant find desc :: " + descriptor.getUuid());
                        }else{
                            Log.d(TAG_LOG, " ** find desc :: " + descriptor.getUuid());
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetooth_gatt.writeDescriptor(descriptor);
                            if(api_level >= 21) {
                                stop_le_scanner();
                            }else {
                                bluetooth_adapter.stopLeScan(le_scan_callback);
                            }
                        }
                    }
                }
                // get current time
//                Log.d(TAG_LOG, "get time+_=-=_=-+-+-+-=_=_=_+-=-=-=-=");
//                BluetoothGattService _service = gatt.getService(UUID.fromString(service_cts));
//                if (_service == null) {
//                    Log.d(TAG_LOG, "cant find service");
//                } else {
//                    Log.d(TAG_LOG, "find service");
//                    Log.d(TAG_LOG, String.valueOf(bluetooth_gatt.getServices()));
//
//                    // subscribe data source characteristic
//                    BluetoothGattCharacteristic data_characteristic = _service.getCharacteristic(UUID.fromString(characteristics_current_time));
//
//                    if (data_characteristic == null) {
//                        Log.d(TAG_LOG, "cant find data source chara");
//                    } else {
//                        Log.d(TAG_LOG, "find data source chara :: " + data_characteristic.getUuid());
//
//
//                    }
//                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG_LOG, "onCharacteristicRead:: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG_LOG, "8=8==8=8=8=8===8=8=8=8=8=8==8=8=8=8=8==88=8=8=8=");
                String month_prefix = "";
                String day_prefix = "";
                String hour_prefix = "";
                String min_prefix = "";
                String sec_prefix = "";

                int year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                Log.d(TAG_LOG, "year:: " + year);

                int month  = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 2);
                if(month < 10) {
                    month_prefix = new String("0" + month);
                    Log.d(TAG_LOG, "month_pre:: " + month_prefix);
                }else {
                    month_prefix = String.valueOf(month);
                    Log.d(TAG_LOG, "month_pre:: " + month_prefix);
                }

                int day = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 3);
                if(day < 10) {
                    day_prefix = new String("0" + day);
                    Log.d(TAG_LOG, "day_pre:: " + day_prefix);
                }else {
                    day_prefix = String.valueOf(day);
                    Log.d(TAG_LOG, "day_pre:: " + day_prefix);
                }
                int hour = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 4);
                if(hour < 10) {
                    hour_prefix = new String("0" + hour);
                    Log.d(TAG_LOG, "hour_pre:: " + hour_prefix);
                }else {
                    hour_prefix = String.valueOf(hour);
                    Log.d(TAG_LOG, "hour_pre:: " + hour_prefix);
                }

                int min = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 5);
                if(min < 10) {
                    min_prefix = new String("0" + min);
                    Log.d(TAG_LOG, "min_pre:: " + min_prefix);
                }else {
                    min_prefix = String.valueOf(min);
                    Log.d(TAG_LOG, "min_pre:: " + min_prefix);
                }

                int sec = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 6);
                if(sec < 10) {
                    sec_prefix = new String("0" + sec);
                    Log.d(TAG_LOG, "sec_pre:: " + sec_prefix);
                }else {
                    sec_prefix = String.valueOf(sec);
                    Log.d(TAG_LOG, "sec_pre:: " + sec_prefix);
                }

                int date = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 7);
                Log.d(TAG_LOG, "date:: " + date);

                try {
                    Process process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    String set_time = new String("date -s " + year + month_prefix + day_prefix + "." + hour_prefix + min_prefix + sec_prefix);
                    Log.d(TAG_LOG, "set_time: " + set_time);
                    os.writeBytes(set_time);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG_LOG, " onDescriptorWrite:: " + status);
            // Notification source
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG_LOG, "status: write success ");
                if (!is_subscribed_characteristics) {
                    //subscribe characteristic notification characteristic
                    BluetoothGattService service = gatt.getService(UUID.fromString(service_ancs));
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristics_notification_source));
//
                    if (characteristic == null) {
                        Log.d(TAG_LOG, " cant find chara");
                    } else {
                        Log.d(TAG_LOG, " ** find chara :: " + characteristic.getUuid());
                        if (characteristics_notification_source.equals(characteristic.getUuid().toString())) {
                            Log.d(TAG_LOG, " set notify:: " + characteristic.getUuid());
                            bluetooth_gatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor notify_descriptor = characteristic.getDescriptor(
                                    UUID.fromString(descriptor_config));
                            if (descriptor == null) {
                                Log.d(TAG_LOG, " ** not find desc :: " + notify_descriptor.getUuid());
                            } else {
                                Log.d(TAG_LOG, " ** find desc :: " + notify_descriptor.getUuid());
                                notify_descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                bluetooth_gatt.writeDescriptor(notify_descriptor);
                                is_subscribed_characteristics = true;
                            }
                        }
                    }
                }else {
                    //execute success animation
                    Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "success");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    // get current time
                    Log.d(TAG_LOG, ":get time+_=-=_=-+-+-+-=_=_=_+-=-=-=-=");
                    BluetoothGattService _service = gatt.getService(UUID.fromString(service_cts));
                    if (_service == null) {
                        Log.d(TAG_LOG, "cant find service");
                    } else {
                        Log.d(TAG_LOG, "find service");
                        Log.d(TAG_LOG, String.valueOf(bluetooth_gatt.getServices()));

                        // subscribe data source characteristic
                        BluetoothGattCharacteristic data_characteristic = _service.getCharacteristic(UUID.fromString(characteristics_current_time));

                        if (data_characteristic == null) {
                            Log.d(TAG_LOG, "cant find data source chara");
                        } else {
                            Log.d(TAG_LOG, "find data source chara :: " + data_characteristic.getUuid());
                            gatt.readCharacteristic(data_characteristic);
                        }
                    }


//                    //find music controll service
//                    if(!is_music_control) {
//                        Log.d(TAG_LOG, "*+*+*+*+*+*+*+*+*+*+ find music control");
//                        //subscribe characteristic notification characteristic
//                        BluetoothGattService service = gatt.getService(UUID.fromString(service_blank));
//                        Log.d(TAG_LOG, " ** find service :: " + service.getUuid());
//                        List <BluetoothGattCharacteristic> chs = service.getCharacteristics();
//                        for(BluetoothGattCharacteristic ch: chs) {
//                            Log.d(TAG_LOG, "ch: " + ch);
//                        }
//                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("00001111-0001-1000-8000-00805f9b34fb"));
//
//                        if (characteristic == null) {
//                            Log.d(TAG_LOG, " cant find chara");
//                        } else {
//                            Log.d(TAG_LOG, " ** find chara :: " + characteristic.getUuid());
//                            if ("0001".equals(characteristic.getUuid().toString())) {
//                                Log.d(TAG_LOG, " set notify:: " + characteristic.getUuid());
//                                characteristic.setValue(new byte[] { (byte) 0x03 });
//                                gatt.writeCharacteristic(characteristic);
//                                Log.d(TAG_LOG, "-=-=-=-=-= finish write data-=-==-");
////                                bluetooth_gatt.setCharacteristicNotification(characteristic, true);
////                                BluetoothGattDescriptor notify_descriptor = characteristic.getDescriptor(
////                                        UUID.fromString(descriptor_config));
////                                if (descriptor == null) {
////                                    Log.d(TAG_LOG, " ** not find desc :: " + notify_descriptor.getUuid());
////                                } else {
////                                    Log.d(TAG_LOG, " ** find desc :: " + notify_descriptor.getUuid());
////                                    notify_descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
////                                    bluetooth_gatt.writeDescriptor(notify_descriptor);
////                                    is_subscribed_characteristics = true;
////                                }
//                            }
//                        }
//                    }
                }
            }else if(status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.d(TAG_LOG, "status: write not permitted");
                //execute not permission animation
//                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
//                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "please re-authorization paring");
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
                Method method = null;
                try {
                    method = gatt.getDevice().getClass().getMethod("removeBond", (Class[]) null);
                    method.invoke(gatt.getDevice(), (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                gatt.disconnect();

                Log.d(TAG_LOG, "onDisconnect: ");

                if(api_level >= 21) {
                    if (le_scanner != null) {
                        Log.d(TAG_LOG, "status: ble reset");
                        stop_le_scanner();
                    }
                }
                if(bluetooth_gatt != null) {
                    bluetooth_gatt.disconnect();
                    bluetooth_gatt.close();
                    bluetooth_gatt = null;
                }
                if(bluetooth_adapter != null) {
                    bluetooth_adapter = null;
                }
                is_connect = false;
                is_subscribed_characteristics = false;
                skip_count = 0;
                is_time = false;


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


                is_reconnect = true;
                Log.d(TAG_LOG, "start BLE scan");
                if(api_level >= 21) {
                    start_le_scanner();
                }else {
                    bluetooth_adapter.startLeScan(le_scan_callback);
                }

                //execute success animation
//                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
//                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "airplane mode on -> off, after restart app.");
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);


//                Intent _intent = new Intent(Intent.ACTION_MAIN);
//                intent.addCategory(Intent.CATEGORY_HOME);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//                startActivity(_intent);
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

                packet_processor.processing(get_data);

                if(packet_processor.is_finish_processing()){
                    int app_logo = icon_image_manager.get_image_index(packet_processor.get_ds_app_id());

                    Bitmap large_icon = BitmapFactory.decodeResource(getResources(), app_logo);
                    Log.d(TAG_LOG, "in title: notification");



                    byte[] _uid = packet_processor.get_uid();
                    //create perform notification action pending_intent
                    Intent _intent_positive = new Intent();
                    _intent_positive.setAction(action_positive);
                    _intent_positive.putExtra(extra_uid, _uid);
                    PendingIntent _positive_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_positive, PendingIntent.FLAG_ONE_SHOT);

                    Intent _intent_negative = new Intent();
                    _intent_negative.setAction(action_negative);
                    _intent_negative.putExtra(extra_uid, _uid);
                    //0 => notification_idでインクリメントしてる
                    PendingIntent _negative_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_negative, PendingIntent.FLAG_ONE_SHOT);

                    Intent _intent_delete = new Intent();
                    _intent_delete.setAction(action_delete);
                    _intent_delete.putExtra(extra_uid, _uid);
                    PendingIntent _delete_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_delete, PendingIntent.FLAG_ONE_SHOT);

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setContentTitle(packet_processor.get_ds_title())
                            .setContentText(packet_processor.get_ds_message())
                            .setSmallIcon(app_logo)
                            .setLargeIcon(large_icon)
                            .setGroup(packet_processor.get_ds_app_id())
                            .addAction(R.drawable.ic_accept, "Accept", _positive_action)
                            .addAction(R.drawable.ic_decline, "Decline", _negative_action)
                            .setDeleteIntent(_delete_action)
                            .build();
                    //
                    //notification_id => mediaとかREGULARで固定してる
                    //UIDから生成するユニークなtagをつかっている．タグを使う場合，タグとIDのペアがユニークであればよい．
                    notificationManager.notify(notification_id, notification);
                    notification_id++;
                    vib.vibrate(pattern, -1);

                    // awake screen.
                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    wake_lock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "MyWakelockTag");
                    if (!wake_lock.isHeld()) {
                        Log.d(TAG_LOG, "acquire()");
                        wake_lock.acquire(screen_time_out);
                    }



                    // get current time
//                    Log.d(TAG_LOG, ":get time+_=-=_=-+-+-+-=_=_=_+-=-=-=-=");
//                    BluetoothGattService _service = gatt.getService(UUID.fromString(service_cts));
//                    if (_service == null) {
//                        Log.d(TAG_LOG, "cant find service");
//                    } else {
//                        Log.d(TAG_LOG, "find service");
//                        Log.d(TAG_LOG, String.valueOf(bluetooth_gatt.getServices()));
//
//                        // subscribe data source characteristic
//                        BluetoothGattCharacteristic data_characteristic = _service.getCharacteristic(UUID.fromString(characteristics_current_time));
//
//                        if (data_characteristic == null) {
//                            Log.d(TAG_LOG, "cant find data source chara");
//                        } else {
//                            Log.d(TAG_LOG, "find data source chara :: " + data_characteristic.getUuid());
//                            gatt.readCharacteristic(data_characteristic);
//                        }
//                    }
                }
            }

            //notify from characteristic notification characteristic
            if (characteristics_notification_source.toString().equals(characteristic.getUuid().toString())) {
                Log.d(TAG_LOG, "get notify from notification source chara");

                //init  packet processing flag;
                packet_processor.init();

                byte[] data = characteristic.getValue();
                if(data != null && data.length > 0) {
//                    if(DEBUG) {
//                        Log.d(TAG_LOG, "*******");
//                        String type = String.format("%02X", data[0]);
//                        Log.d(TAG_LOG, "type: " + type);
//                        String priority = String.format("%02X", data[1]);
//                        Log.d(TAG_LOG, "priority: " + priority);
//                        String category = String.format("%02X", data[2]);
//                        Log.d(TAG_LOG, "category: " + category);
//                        String count = String.format("%02X", data[3]);
//                        Log.d(TAG_LOG, "category count: " + count);
//
//                        StringBuilder stringBuilder = new StringBuilder();
//                        for(byte byteChar: data){
//                            stringBuilder.append(String.format("%02X", byteChar));
//                        }
//                        Log.d(TAG_LOG, "notify value:: " + stringBuilder.toString());
//                    }

                    try {
                        if (String.format("%02X", data[0]).equals("00")) {
                            Log.d(TAG_LOG, "get notify");
                            // notification value setting.
                            //current, hard coding
                            uid[0] = data[4];
                            uid[1] = data[5];
                            uid[2] = data[6];
                            uid[3] = data[7];
                            byte[] get_notification_attribute = {
                                    (byte)0x00,
                                    //UID
                                    data[4], data[5], data[6], data[7],
                                    //app id
                                    (byte)0x00,
                                    //title
                                    (byte)0x01, (byte)0xff, (byte)0xff,
                                    //message
                                    (byte)0x03, (byte)0xff, (byte)0xff
                            };

                            BluetoothGattService service = gatt.getService(UUID.fromString(service_ancs));
                            if (service == null) {
                                Log.d(TAG_LOG, "cant find service");
                            } else {
                                Log.d(TAG_LOG, "find service");
                                characteristic = service.getCharacteristic(UUID.fromString(characteristics_control_point));
                                if (characteristic == null) {
                                    Log.d(TAG_LOG, "cant find chara");
                                } else {
                                    Log.d(TAG_LOG, "find chara");
                                    characteristic.setValue(get_notification_attribute);
                                    gatt.writeCharacteristic(characteristic);
                                }
                            }
                        }
                    }catch(ArrayIndexOutOfBoundsException e){
                        Log.d(TAG_LOG, "error");
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @TargetApi(20)
    public class MessageReceiver extends BroadcastReceiver{
        private static final String TAG_LOG = "BLE_wear";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG_LOG, "onReceive");
            String action = intent.getAction();

            // perform notification action: immediately
            // delete intent: after 7~8sec.
        if (action.equals(action_positive) | action.equals(action_negative) | action.equals(action_delete)){
                Log.d(TAG_LOG, "get action: " + action);

                //get notification uid
                byte[] _uid = intent.getByteArrayExtra(extra_uid);
                for(int i = 0; i < _uid.length; i++) {
                    Log.d(TAG_LOG, "@@uid@@ :: " + Integer.toHexString(_uid[i]));
                }
                Log.d(TAG_LOG, "*+*+*++*+*+*+*+*+*+*+*+*+");

                // set action id
                byte _action_id = 0x00;
                if(action.equals(action_negative) | action.equals(action_delete)) {
                    _action_id = (byte) 0x01;
                }

                try {
                    Log.d(TAG_LOG, "get notify");
                    // perform notification action value setting.
                    byte[] get_notification_attribute = {
                            (byte)0x02,
                            //UID
                            _uid[0], _uid[1], _uid[2], _uid[3],
                            //action
                            _action_id
                    };

                    BluetoothGattService service = bluetooth_gatt.getService(UUID.fromString(service_ancs));
                    if (service == null) {
                        Log.d(TAG_LOG, "cant find service @ BR");
                    } else {
                        Log.d(TAG_LOG, "find service @ BR");
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristics_control_point));
                        if (characteristic == null) {
                            Log.d(TAG_LOG, "cant find chara @ BR");
                        } else {
                            Log.d(TAG_LOG, "find chara @ BR");
                            characteristic.setValue(get_notification_attribute);
                            bluetooth_gatt.writeCharacteristic(characteristic);
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    Log.d(TAG_LOG, "error");
                    e.printStackTrace();
                }
                Log.d(TAG_LOG, "onReceive");
            }else if(action.equals(action_set_clock)) {
            }else if(action.equals(action_renotify)) {
                Log.d(TAG_LOG, "action_renotify");
                Intent _intent = new Intent(getApplicationContext(), MainActivity.class);
                PendingIntent music_control_intent = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent, PendingIntent.FLAG_CANCEL_CURRENT);


    // この MainActivity は Wear アプリの MainActivity
                Intent __intent = new Intent(getApplicationContext(), MusicControlActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, __intent, 0);

    // カードをタップしたときに表示される Activity
    // ここでは ImageView 1つだけのレイアウト
                Intent displayIntent = new Intent(getApplicationContext(), MusicControlActivity.class);
                PendingIntent displayPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    //        Notification.Extender wearableExtender =
    //                new Notification.Extender()
    //                        .setDisplayIntent(displayPendingIntent);
                Intent _intent_delete = new Intent();
                _intent_delete.setAction(action_renotify);
                PendingIntent _delete_action = PendingIntent.getBroadcast(getApplicationContext(), notification_id, _intent_delete, PendingIntent.FLAG_ONE_SHOT);

                Notification.Builder notificationBuilder =
                        new Notification.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.whatsapp)
                            .setContentTitle("music 💿")
//                                .setContentTitle("tv📺")
                                .setContentIntent(pendingIntent)
    //                        .extend(wearableExtender);
                                .addAction(R.drawable.resume, "Controller Open", pendingIntent)
//                                .setLocalOnly(true)
                                .extend(new Notification.WearableExtender().setContentAction(0).setHintHideIcon(true))
    //                .extend(new Notification.WearableExtender().setDisplayIntent(displayPendingIntent))
                        //can't work.
//                                .setDeleteIntent(_delete_action)
                        ;

                Notification _notification = notificationBuilder.build();
//            _notification.flags = _notification.flags | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            _notification.flags = _notification.flags | Notification.FLAG_ONGOING_EVENT;
//            _notification.flags = _notification.flags | Notification.FLAG_NO_CLEAR ;

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //        notificationManager.notify(notification_id, notificationBuilder.build());
                notificationManager.notify(id_music_control, _notification);
                notification_id++;
            }
        }
    }
}