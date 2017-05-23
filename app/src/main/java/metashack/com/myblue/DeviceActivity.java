package metashack.com.myblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import metashack.com.myblue.services.BLEService;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static metashack.com.myblue.services.BLEService.ACTION_DATA_AVAILABLE;
import static metashack.com.myblue.services.BLEService.ACTION_GATT_SERVICES_DISCOVERED;

public class DeviceActivity extends AppCompatActivity {

    private static final String TAG = DeviceActivity.class.getSimpleName();
    private BLEService bleService;

    private LinearLayout contentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_devices_list_layout);
        registerReceiver(new Reciever(), BLEService.getIntentFilter());
        contentView = (LinearLayout) findViewById(R.id.content);

        Intent mIntent = new Intent(this, BLEService.class);
        bindService(mIntent, new Connection(), BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        bleService.stopSelf();
    }

    private void loadServicesStuff() {
        if (bleService != null && bleService.getBluetoothServices().size() > 0) {
            List<BluetoothGattService> services = bleService.getBluetoothServices();
            for (BluetoothGattService serv : services) {

                LinearLayout.LayoutParams servicesMargins = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                servicesMargins.setMargins(5, 20, 5, 20);

                LinearLayout.LayoutParams chracMargins = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                servicesMargins.setMargins(5, 10, 5, 10);


                LayoutInflater inflater = LayoutInflater.from(getBaseContext());
                final LinearLayout servicesLayout = (LinearLayout) inflater.inflate(R.layout.service_layout, null, false);

                TextView servName = (TextView) servicesLayout.findViewById(R.id.service_name);
                servName.setText(serv.getUuid().toString());


                List<BluetoothGattCharacteristic> characs = serv.getCharacteristics();

                contentView.addView(servicesLayout, servicesMargins);
                final List<View> charsViews = new ArrayList<>();
                for (final BluetoothGattCharacteristic cha : characs) {
                    final LinearLayout characLayout = (LinearLayout) inflater.inflate(R.layout.characteristic_layout, null, false);
                    TextView writableView = (TextView) characLayout.findViewById(R.id.writable);
                    TextView charachName = (TextView) characLayout.findViewById(R.id.charac_name);
                    charachName.setText(cha.getUuid().toString());

                    if ((cha.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                        writableView.setVisibility(VISIBLE);
                    } else {
                        writableView.setVisibility(GONE);
                    }
                    servicesLayout.addView(characLayout, chracMargins);
                    characLayout.setVisibility(GONE);


                    charsViews.add(characLayout);


                }

                servicesLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (View cc : charsViews) {
                            if (cc.getVisibility() == GONE) {
                                cc.setVisibility(VISIBLE);
                            } else if (cc.getVisibility() == VISIBLE) {
                                cc.setVisibility(GONE);
                            }
                        }
                    }
                });
            }
        }
    }


    private class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BLEService.MyBinder binder = (BLEService.MyBinder) service;
            bleService = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    class Reciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_DATA_AVAILABLE:
                    Log.d(TAG, "Gatt action data available");
                    //todo update chractericticx
                    break;
                case ACTION_GATT_SERVICES_DISCOVERED:
                    loadServicesStuff();
                    break;
            }
        }
    }


}
