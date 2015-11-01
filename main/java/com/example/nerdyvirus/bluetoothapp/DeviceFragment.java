package com.example.nerdyvirus.bluetoothapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment ;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class DeviceFragment extends Fragment {

    Button scanButton ;
    List<String> deviceList ;
    ListView deviceListView ;
    private final String LOG_TAG=DeviceFragment.class.getSimpleName();
    private static int REQUEST_ENABLE_BT = 0;
    BluetoothAdapter mBluetoothAdapter;
    ArrayAdapter< String > deviceAdapter ;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView= inflater.inflate(R.layout.fragment_device, container, false);
        deviceListView= (ListView) rootView.findViewById(R.id.lvDeviceList);
        scanButton= (Button) rootView.findViewById(R.id.btnScanDevices);
        Log.e(LOG_TAG, "Got here");

        deviceList= new ArrayList<String>();

        deviceAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_view_devices,
                R.id.device_textView,
                deviceList
        );

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListView.setAdapter(deviceAdapter);
        getActivity().registerReceiver(myBluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    //updateList();

                }
                else
                    REQUEST_ENABLE_BT = 1;
                if(REQUEST_ENABLE_BT == 1)
                    updateList();
            }
        });
        return  rootView ;
    }

    private void updateList() {
        deviceAdapter.clear();
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver myBluetoothReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()==null) {
                    deviceAdapter.add(device.getAddress());
                }
                else{
                    deviceAdapter.add(device.getName());
                }
                deviceAdapter.notifyDataSetChanged();
            }
        }
    };
}
