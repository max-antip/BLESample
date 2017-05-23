package metashack.com.myblue;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import metashack.com.myblue.services.BLEService;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BLEActivity extends AppCompatActivity {
    private static final String TAG = "BLEActivity";
    public static final int SCAN_PRIOD = 5000;
    public static final String NONE = "NONE";
    private DeviceListAdapter deviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private BluetoothLeScanner bluetoothLeScanner;
    private MyScanCallback scanCallback = new MyScanCallback();
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 11;

    private BLEService bleService;
    private BluetoothGatt bluetoothGatt;
    private LinearLayout devicesList;
    private ProgressBar progressBar;
    private Button scanBut;
    private Button stopScanBut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.scanning_process);

        devicesList = (LinearLayout) findViewById(R.id.devices);
        initButtons();


        deviceListAdapter = new DeviceListAdapter();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        turnOnBLE();

    }

    private void turnOnBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(GONE);
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PRIOD);


            progressBar.setVisibility(VISIBLE);
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            progressBar.setVisibility(GONE);
            bluetoothLeScanner.stopScan(scanCallback);
        }

    }

    private class MyScanCallback extends ScanCallback {


        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Got device " + result.getDevice().getName());
            BluetoothDevice device = result.getDevice();
            addDevice(device);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.d(TAG, sr.getDevice().getName());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scna faild with err code " + errorCode);
        }

    }

    private void addDevice(final BluetoothDevice device) {

        if (deviceListAdapter.hasDevice(device)) return;
        deviceListAdapter.addDevice(device);

        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        final LinearLayout deviceView = (LinearLayout) inflater.inflate(R.layout.device, null, false);

        deviceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BLEActivity.this, BLEService.class);
                intent.putExtra("device", device);
                startService(intent);
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//                Intent intentAct = new Intent(BLEActivity.this, DeviceActivity.class);
//                startActivity(intentAct);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 15, 5, 15);


        TextView nameText = (TextView) deviceView.findViewById(R.id.name);
        TextView macText = (TextView) deviceView.findViewById(R.id.mac);
        nameText.setText(getString(R.string.name_lable));
        String name = device.getName() == null ? NONE : device.getName();
        nameText.append(name);
        macText.setText(getString(R.string.MAC_lable));
        macText.append(device.getAddress());

        devicesList.addView(deviceView, layoutParams);

    }

    private void initButtons() {
        scanBut = (Button) findViewById(R.id.scan);

        scanBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceListAdapter.clear();
                devicesList.removeAllViews();
                scanLeDevice(true);
            }
        });

        stopScanBut = (Button) findViewById(R.id.stop_scan);

        stopScanBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
            }
        });
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BLEService.MyBinder binder = (BLEService.MyBinder) service;
            bleService = binder.getService();
            System.out.println("test");
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;

    }

}
