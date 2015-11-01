package com.example.nerdyvirus.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by nerdyvirus on 15/9/15.
 */
public class CustomAvailableBluetoothDevice {

    private BluetoothDevice device;
    private String macAddress,name ;

    public CustomAvailableBluetoothDevice(BluetoothDevice device)
    {
        this.device=device;
        this.macAddress=device.getAddress();
        this.name=device.getName();

    }
    public CustomAvailableBluetoothDevice(String name)
    {
        macAddress=null;
        this.name=name ;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getName() {
        return name;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public String toString() {
        if(name==null)
            return macAddress  ;
        else if (macAddress==null)
            return name;
        else
            return  name+"\n"+macAddress ;
    }


}
