package com.example.nerdyvirus.bluetoothapp;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class AvailableDeviceFragment extends Fragment {

    public  static  final String  LOG_TAG=AvailableDeviceFragment.class.getSimpleName();
    ListView deviceListView ;
    ArrayList<CustomAvailableBluetoothDevice> deviceList;
    ArrayAdapter<CustomAvailableBluetoothDevice> deviceAdapter ;
    BluetoothAdapter mBluetoothAdapter ;
    Button scanButton ;
    View rootView;
    TextView notifyBox;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView= inflater.inflate(R.layout.fragment_available_device, container, false);

        configureBluetooth();

        registerReceivers();

        setUpListView();

        notifyBox= (TextView) rootView.findViewById(R.id.notifyView);
        scanButton= (Button) rootView.findViewById(R.id.btnScanDevices);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                else {
                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                    mBluetoothAdapter.startDiscovery();
                    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400);
                        startActivity(discoverableIntent);
                    }
                }
            }
        });

        if(!mBluetoothAdapter.isEnabled()) {
            notifyBox.setVisibility(View.VISIBLE);
            notifyBox.setText("Enable Bluetooth");
            scanButton.setVisibility(View.INVISIBLE);
        }
        else
        {
            notifyBox.setVisibility(View.GONE);
        }
        return  rootView ;
    }

    private void registerReceivers() {
        IntentFilter filter;

        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(adapterStateChangeReceiver,filter);

        filter=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        getActivity().registerReceiver(adapterStateChangeReceiver,filter);

        filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(adapterStateChangeReceiver,filter);

        filter=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(adapterStateChangeReceiver, filter);
    }

    private void configureBluetooth() {
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity,getString(R.string.NotSupported), Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpListView() {
        deviceListView= (ListView) rootView.findViewById(R.id.lvAvailableDeviceList);


        deviceList= new ArrayList<CustomAvailableBluetoothDevice>();

        deviceAdapter = new ArrayAdapter<CustomAvailableBluetoothDevice>(
                getActivity(),
                R.layout.list_view_devices,
                R.id.device_textView,
                deviceList
        );

        deviceListView.setAdapter(deviceAdapter);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceList.get(position).getDevice();
                boolean isPaired = false;
                try {
                    isPaired = createBond(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!isPaired) {
                    Toast.makeText(getActivity(), "Pairing Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private  final BroadcastReceiver adapterStateChangeReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action=intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if(state == BluetoothAdapter.STATE_OFF) {
                    scanButton.setVisibility(View.INVISIBLE);
                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                    notifyBox.setVisibility(View.VISIBLE);
                    notifyBox.setText("Enable Bluetooth");
                    //Log.e(LOG_TAG, "BTON");
                }
                else if(state == BluetoothAdapter.STATE_ON){
                    notifyBox.setVisibility(View.GONE);
                    scanButton.setVisibility(View.VISIBLE);
                    scanButton.setText("Start Scan");
                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                    //Log.e(LOG_TAG, "BTOFF");
                }
            }
            else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                scanButton.setText("Stop Scan");
            }
            else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                scanButton.setText("Start Scan");
                if(deviceList.size()==0) {
                    deviceAdapter.add(new CustomAvailableBluetoothDevice("No Device"));
                }
            }

            else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(new CustomAvailableBluetoothDevice(device));
                deviceAdapter.notifyDataSetChanged();
            }
        }
    };


    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }


    public void onDestroy()
    {
        super.onDestroy();
        getActivity().unregisterReceiver(adapterStateChangeReceiver);
    }

}
