package com.example.nerdyvirus.bluetoothapp;

import android.bluetooth.BluetoothDevice;

/**
 * Created by nerdyvirus on 15/9/15.
 */
public class CustomPairedBluetoothDevice {

        private BluetoothDevice device;
        private String macAddress,name ;

        public CustomPairedBluetoothDevice(BluetoothDevice device)
        {
            this.device=device;
            this.macAddress=device.getAddress();
            this.name=device.getName();
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
                return this.macAddress  ;
            else
                return  this.name ;
        }

}


