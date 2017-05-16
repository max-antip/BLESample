package metashack.com.myblue.connections;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectionBluetoothThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothSocket socket;
    BluetoothAdapter bluetoothAdapter;
    private static final UUID UIID = new UUID(11L, 22L);

    public ConnectionBluetoothThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
        BluetoothSocket tmp = null;
        this.bluetoothAdapter = bluetoothAdapter;
        try {
//            tmp = device.createInsecureRfcommSocketToServiceRecord(UIID);
            tmp = device.createRfcommSocketToServiceRecord(UIID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        socket = tmp;
    }

    public void run() {
        bluetoothAdapter.cancelDiscovery();

        try {
            socket.connect();
        } catch (IOException connectException) {
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

//        manageMyConnectedSocket(socket);
    }


    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}