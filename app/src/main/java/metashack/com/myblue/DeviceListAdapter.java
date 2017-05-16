package metashack.com.myblue;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

class DeviceListAdapter {
    private ArrayList<BluetoothDevice> devices;

    public DeviceListAdapter() {
        devices = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        if (!devices.contains(device)) {
            devices.add(device);
        }
    }

    public boolean hasDevice(BluetoothDevice dev) {
        return devices.contains(dev);
    }

    public BluetoothDevice getDevice(int position) {
        return devices.get(position);
    }

    public void clear() {
        devices.clear();
    }

}