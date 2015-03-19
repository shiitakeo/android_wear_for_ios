package com.shiitakeo.android_wear_for_ios;

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
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by kusabuka on 15/03/15.
 */
public class BLEService extends Service{
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


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onStart(Intent intent, int startID) {
        Log.d("Service", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction(action_positive);
        intent_filter.addAction(action_negative);
        registerReceiver(message_receiver, intent_filter);

        if(le_scanner != null) {
            Log.d(TAG_LOG, "status: ble reset");
            le_scanner.stopScan(scan_callback);
            bluetooth_gatt.close();
            bluetooth_gatt = null;
            bluetooth_adapter = null;
            is_connect = false;
            is_subscribed_characteristics = false;
        }

        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        packet_processor = new PacketProcessor();
        icon_image_manager = new IconImageManager();

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

        Log.d(TAG_LOG, "start BLE scan");
        le_scanner = bluetooth_adapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        le_scanner.startScan(scan_fillters(), settings, scan_callback);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG_LOG, "~~~~~~~~ service onDestroy");
        le_scanner.stopScan(scan_callback);
        is_connect =false;
        is_subscribed_characteristics = false;

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
//        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_ancs)).build();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(service_blank)).build();
        List<ScanFilter> list = new ArrayList<ScanFilter>(1);
        list.add(filter);
        return list;
    }


    ScanCallback scan_callback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            Log.i(TAG_LOG, "scan result" + result.toString());
            BluetoothDevice device = result.getDevice();
            if(!is_connect) {
                Log.d(TAG_LOG, "is connect");
                if (device != null) {
                    Log.d(TAG_LOG, "device ");
                    if (device.getName() != null) {
                        Log.d(TAG_LOG, "getname ");
                        is_connect = true;
                        bluetooth_gatt = result.getDevice().connectGatt(getApplicationContext(), false, bluetooth_gattCallback);
                    }
                }
            }
        };
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG_LOG, "batchscan result" + results.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG_LOG, "onScanFailed" + errorCode);
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
                bluetooth_gatt.close();
                bluetooth_gatt = null;
                is_connect = false;
                is_subscribed_characteristics = false;


                //execute success animation
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "airplane mode on -> off, after restart app.");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
                            le_scanner.stopScan(scan_callback);
                        }
                    }
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
                }
            }else if(status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.d(TAG_LOG, "status: write not permitted");
                //execute not permission animation
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "please re-authorization paring");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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

                    //create perform notification action pending_intent
                    Intent _intent_positive = new Intent();
                    _intent_positive.setAction(action_positive);
                    PendingIntent _positive_action = PendingIntent.getBroadcast(getApplicationContext(), 0, _intent_positive, PendingIntent.FLAG_CANCEL_CURRENT);

                    Intent _intent_negative = new Intent();
                    _intent_negative.setAction(action_negative);
                    PendingIntent _negative_action = PendingIntent.getBroadcast(getApplicationContext(), 0, _intent_negative, PendingIntent.FLAG_CANCEL_CURRENT);

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setContentTitle(packet_processor.get_ds_title())
                            .setContentText(packet_processor.get_ds_message())
                            .setSmallIcon(app_logo)
                            .setLargeIcon(large_icon)
                            .setGroup(packet_processor.get_ds_app_id())
                            .addAction(android.R.drawable.ic_input_add, "positive", _positive_action)
                            .addAction(android.R.drawable.ic_input_add, "negative", _negative_action)
                            .build();
                    notificationManager.notify(notification_id, notification);
                    notification_id++;
                    vib.vibrate(pattern, -1);

                    // awake screen.
                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    wake_lock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "MyWakelockTag");
                    if (!wake_lock.isHeld()) {
                        Log.d(TAG_LOG, "acquire()");
                        wake_lock.acquire();
                    }
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

    public class MessageReceiver extends BroadcastReceiver{
        private static final String TAG_LOG = "BLE_wear";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG_LOG, "onReceive");
            String action = intent.getAction();

            if (action.equals(action_positive) | action.equals(action_negative)){
                Log.d(TAG_LOG, "get action: " + action);

                // set action id
                byte _action_id = 0x00;
                if(action.equals(action_negative)) {
                    _action_id = (byte) 0x01;
                }

                try {
                    Log.d(TAG_LOG, "get notify");
                    // perform notification action value setting.
                    byte[] get_notification_attribute = {
                            (byte)0x02,
                            //UID
                            uid[0], uid[1], uid[2], uid[3],
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
            }
        }
    }
}
