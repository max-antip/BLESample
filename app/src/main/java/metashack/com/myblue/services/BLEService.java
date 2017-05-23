package metashack.com.myblue.services;


import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.Serializable;
import java.util.List;

public class BLEService extends Service implements Serializable {
    private final static String TAG = BLEService.class.getSimpleName();
    public static final long SEND_TIME_OUT_MILLIS = 10000;

    private BluetoothGatt gatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;

    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    private List<BluetoothGattService> bluetoothServices;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private boolean writeInProgress;

    private IBinder myBinder = new MyBinder();

    public BLEService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = getIntentFilter();
        getApplicationContext().registerReceiver(broadcastUpdReciever, filter);
        device = intent.getParcelableExtra("device");
        if (!connectGatt()) {
            Log.e(TAG, "Gatt server not connected");
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public BLEService(BluetoothDevice device) {
        this.device = device;
    }

    @NonNull
    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        filter.addAction(EXTRA_DATA);
        return filter;
    }

    public boolean connectGatt() {
        gatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
        return gatt.connect();
    }


    public BluetoothGatt getGatt() {
        return gatt;
    }

    // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());

                        broadcastUpdate(intentAction);

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        bluetoothServices = gatt.getServices();
                        Log.d(TAG, "Gatt service discovered");
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "Write characteristic " + characteristic.getUuid() + " successfull");
                    }
                    writeInProgress = false;
                }
            };

    // for test only
    private final BroadcastReceiver broadcastUpdReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "Gatt Connecred");

            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
                Log.d(TAG, "Gatt disconnected");
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
              /*  bluetoothServices = getGatt().getServices();
                Log.d(TAG, "Gatt service discovered");*/
               /* for (BluetoothGattService s : services) {
                    List<BluetoothGattCharacteristic> characteristics = s.getCharacteristics();

                    for (BluetoothGattCharacteristic cha : characteristics) {
                        if ((cha.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                            send(cha, "test");
                        }
                    }
                }
*/
//                displayGattServices(bleService.getSupportedGattServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "Gatt action data available");
//                displayData(intent.getStringExtra(BLEService.EXTRA_DATA));
            }
        }

    };

    public List<BluetoothGattService> getBluetoothServices() {
        return bluetoothServices;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        getApplicationContext().sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        int flag = characteristic.getProperties();
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Heart rate format UINT16.");
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Heart rate format UINT8.");
        }
        final int heartRate = characteristic.getIntValue(format, 1);
        Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

        sendBroadcast(intent);
    }

    public void send(BluetoothGattCharacteristic charac, byte[] data) {
        long beginMillis = System.currentTimeMillis();
        if (charac == null || data == null || data.length == 0) {
            return;
        }
        charac.setValue(data);
        writeInProgress = true;
        gatt.writeCharacteristic(charac);
        while (writeInProgress) {
            if (System.currentTimeMillis() - beginMillis > SEND_TIME_OUT_MILLIS) {
                break;
            }
        }

    }

    public void send(BluetoothGattCharacteristic charac,
                     String string) {
        int len = string.length();
        int pos = 0;
        StringBuilder stringBuilder = new StringBuilder();

        while (len != 0) {
            stringBuilder.setLength(0);
            if (len >= 20) {
                stringBuilder.append(string.toCharArray(), pos, 20);
                len -= 20;
                pos += 20;
            } else {
                stringBuilder.append(string.toCharArray(), pos, len);
                len = 0;
            }
            send(charac, stringBuilder.toString().getBytes());
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }


    public class MyBinder extends Binder {

        public BLEService getService() {
            return BLEService.this;
        }

    }


}