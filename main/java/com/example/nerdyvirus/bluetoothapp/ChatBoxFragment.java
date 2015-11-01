package com.example.nerdyvirus.bluetoothapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Set;

public class ChatBoxFragment extends Fragment {
    public  static  final String  LOG_TAG=ChatBoxFragment.class.getSimpleName();
    ListView messageListView ;
    Spinner deviceListSpinner ;
    ArrayList<CustomPairedBluetoothDevice> deviceList ;
    ArrayList<String > messageList ;
    Button sendButton ;
    Button attachButton;
    EditText msgTextView;
    BluetoothAdapter mBluetoothAdapter;
    ArrayAdapter<CustomPairedBluetoothDevice> spinnerAdapter;
    ArrayAdapter<String> msgAdapter ;
    View rootView;
    ChatService mChatService ;
    Button connectButton ;
    TextView notifyTextView;
    String preamblercv,preamblesend;
    private String mConnectedDeviceName="xxxxxxxxxxxx";
    private boolean connectButtonState ;
    private  static final int SELECT_PHOTO=1;
    private ProgressDialog mProgressDialog,dialog;
    private ChatArrayAdapter chatArrayAdapter ;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_chat_box, container, false);
        messageListView= (ListView) rootView.findViewById(R.id.message_ListView);
        deviceListSpinner= (Spinner) rootView.findViewById(R.id.deviceListSpinner);
        sendButton= (Button) rootView.findViewById(R.id.message_send_button);
        attachButton=(Button)rootView.findViewById(R.id.attach_button);
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        msgTextView= (EditText) rootView.findViewById(R.id.send_message_txtView);
        connectButton= (Button) rootView.findViewById(R.id.connectButton);
        notifyTextView= (TextView) rootView.findViewById(R.id.notifyBox);
        dialog = new ProgressDialog(getActivity());
        chatArrayAdapter=new ChatArrayAdapter(getActivity(),R.layout.single_chat_message);
        connectButtonState=false ;
        registerReceivers();

        setUpSpinner();

        setUpListView();

        updateList();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomPairedBluetoothDevice btDevice = (CustomPairedBluetoothDevice) deviceListSpinner.getSelectedItem();
                Log.d(LOG_TAG, "Connect to button " + btDevice.getName());
                if(connectButtonState==false) {
                    connectDevice(btDevice.getDevice());
                    dialog.setMessage("Connecting to " + btDevice.getName());
                    dialog.show();
                }
                else
                    reset();

            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = msgTextView.getText().toString();
                SendMessage(message);
                msgTextView.setText("");
            }
        });
        attachButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO);*/
                Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(),R.drawable.big_pic);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();;
                icon.compress(Bitmap.CompressFormat.JPEG, 40, stream);

                byte[] image=stream.toByteArray();
                String eof=new String("end of file");
                byte[] endof=eof.getBytes();

                // create a destination array that is the size of the two arrays
                byte[] destination = new byte[endof.length + image.length];

// copy ciphertext into start of destination (from pos 0, copy ciphertext.length bytes)
                System.arraycopy(image, 0, destination, 0, image.length);
                System.arraycopy(endof, 0, destination, image.length, endof.length);
                Toast.makeText(getActivity(),image.length+"",Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG,""+image.length);
                mChatService.write(destination, "image");
                Log.d(LOG_TAG, "SelectPhoto");
            }
        });

        return  rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode) {
            case SELECT_PHOTO:
                //Toast.makeText(getActivity(),"SentPicture",Toast.LENGTH_SHORT);
                Log.d(LOG_TAG,"SelectPhoto");
                if(resultCode == getActivity().RESULT_OK){
                    Uri selectImage = intent.getData();

                    Toast.makeText(getActivity(),""+ selectImage.getPath(),Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "intentdata");

                    Bitmap selectedImage = null;

                        selectedImage =BitmapFactory.decodeFile(selectImage.getPath());
                    Toast.makeText(getActivity(), "ho gaya bhai ", Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "intentdata2");

                   ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Log.d(LOG_TAG, "intentdata3");
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    Log.d(LOG_TAG, "intentdata4");
                    byte[] byteArray =stream.toByteArray();
                    Log.d(LOG_TAG, "intentdata5");

                    mChatService.write(byteArray, "image");

                }
            break;
        }
    }

    private void setUpSpinner() {
        deviceList = new ArrayList<CustomPairedBluetoothDevice>();
        spinnerAdapter= new ArrayAdapter<CustomPairedBluetoothDevice>(
                getActivity(),
                android.R.layout.simple_spinner_dropdown_item,deviceList);
        deviceListSpinner.setAdapter(spinnerAdapter);
    }

    private void setUpListView() {
        messageList= new ArrayList<>();
        msgAdapter=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,messageList);
        messageListView.setAdapter(msgAdapter);
        //messageListView.setAdapter(chatArrayAdapter);
    }
    private final BroadcastReceiver adapterStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            action=intent.getAction();
            Log.d(LOG_TAG,"Listened "+action);
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
                updateList();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                updateList();
        }
    };


    private void updateList() {

        Log.d(LOG_TAG, "updateList");
        Set<BluetoothDevice> deviceSet=mBluetoothAdapter.getBondedDevices();
        deviceList.clear();
        spinnerAdapter.notifyDataSetChanged();
        for(BluetoothDevice device:deviceSet) {
            deviceList.add(new CustomPairedBluetoothDevice(device));

        }
        spinnerAdapter.notifyDataSetChanged();

        if(deviceList.size()==0)
        {
            connectButton.setVisibility(View.GONE);
            deviceListSpinner.setVisibility(View.GONE);
            notifyTextView.setVisibility(View.VISIBLE);

            if(mBluetoothAdapter.isEnabled())
                notifyTextView.setText("No paired Device Found");
            else
                notifyTextView.setText("Enable Bluetooth");
        }
        else {
            notifyTextView.setVisibility(View.GONE);
            connectButton.setVisibility(View.VISIBLE);
            deviceListSpinner.setVisibility(View.VISIBLE);
        }
    }

     private void registerReceivers() {
         Log.d(LOG_TAG,"registerReceivers");
         IntentFilter filter;
         filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
         getActivity().registerReceiver(adapterStateChangeReceiver, filter);
         filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
         getActivity().registerReceiver(adapterStateChangeReceiver, filter);
     }

    private void reset()
    {
        connectButtonState=false;
        connectButton.setText("Connect");
        mChatService.stop();
        mChatService.start();
        sendButton.setEnabled(false);
        attachButton.setEnabled(false);
        msgTextView.setEnabled(false);
    }




    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String deviceName=mConnectedDeviceName;
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case 1:
                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            Log.d(LOG_TAG, "Connected to " + mConnectedDeviceName);
                            msgAdapter.clear();
                            //setStatus("Connected to " + mConnectedDeviceName);
                            break;
                        case ChatService.STATE_CONNECTING:
                            Log.d(LOG_TAG,"Connecting ...");
                            //setStatus("Connecting ...");
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            Log.d(LOG_TAG,"Not Connected");
                            //setStatus("Not Connected");
                            break;
                    }
                    break;
                case 2:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readData= new String(readBuf, 0, msg.arg2);
                    //Log.d(LOG_TAG,"ReadData: "+readData);
                    if(msg.arg1==1) {
                        preamblercv = readData;
                    }
                    else {
                        if(preamblercv.equals("text")) {
                            String readMessage = readData;
                            msgAdapter.add(mConnectedDeviceName + ":\n" + readMessage + "\n");
                        }
                        else{
                            //SavePhoto savePhoto=new SavePhoto();
                            //savePhoto.execute(readBuf);
                        }
                    }
                    //chatArrayAdapter.add(new ChatMessage(false,mConnectedDeviceName + ":\n" + readMessage+"\n"));
                    break;
                case 3:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeData=new String(writeBuf);
                    //Log.d(LOG_TAG,"WriteData: "+writeData);
                    if(msg.arg1==1){
                        preamblesend = writeData;
                    }
                    else {
                        if(preamblesend.equals("text")) {
                            String writeMessage = writeData;
                            msgAdapter.add("Me: \n" + writeMessage + "\n");
                        }
                        else{
                            Toast.makeText(getActivity(),"Sent the Attachement",Toast.LENGTH_SHORT).show();
                        }
                    }
                    //chatArrayAdapter.add(new ChatMessage(true,"Me:\n"+writeMessage+"\n"));
                    break;
                case 4:
                    dialog.dismiss();
                    connectButtonState=true;
                    connectButton.setText("Disconnect");
                    mConnectedDeviceName = msg.getData().getString("device name");
                    int idx=findIndex(mConnectedDeviceName);
                    if(idx!=-1)
                         deviceListSpinner.setSelection(idx);
                    Log.d(LOG_TAG,"case 4 "+mConnectedDeviceName);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    msgTextView.setEnabled(true);
                    sendButton.setEnabled(true);
                    attachButton.setEnabled(true);

                    break;
                case 5:
                    dialog.dismiss();
                    reset();
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(String.valueOf("toast")),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private int  findIndex(String deviceName) {
        int count=0;
        for(CustomPairedBluetoothDevice device:deviceList)
            if(device.getName().equals(deviceName))
                return count ;
            else
                count++;
        return  -1;
    }

    private void setUpChat() {
        Log.d(LOG_TAG,"setUpChat");
        if(mChatService==null)
            mChatService = new ChatService(getActivity(), mHandler);
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d(LOG_TAG,"connectDevice");
        if(device!=null)
            mChatService.connect(device);
        else
            Log.d(LOG_TAG,"Device NULL");
    }

    public void SendMessage(String message) {
        Log.d(LOG_TAG, "SendMessage");
        byte[] send = message.getBytes();
        mChatService.write(send,"text");
        //messageList.add("Me:\n" + message + "\n");
    }

    class SavePhoto extends AsyncTask<byte[],String,String>{
        File photo;
        @Override
        protected String doInBackground(byte[]... jpeg) {
            String newFolder = "/NewFolder/videos";

            File newDirectory = new File(Environment
                    .getExternalStorageDirectory().toString() + newFolder);

            newDirectory.mkdirs();


            photo=new File(newDirectory,"photo.jpg");
         //   FileOutputStream out = new FileOutputStream(photo);
           // finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
           // out.flush();
            //out.close();


            /*try {
                photo.createNewFile();


            } catch (IOException e) {
                Log.d(LOG_TAG,"File not created");
                e.printStackTrace();
            }*/

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());


                fos.write(jpeg[0]);
                fos.close();
            } catch (IOException e) {
                Log.e(LOG_TAG,"Exception On Storing2");
            }

            return  null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(getActivity(), "Received the Attachement", Toast.LENGTH_SHORT).show();
            Toast.makeText(getActivity(), photo.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        Log.d(LOG_TAG, "onStart");
        super.onStart();
        if (mBluetoothAdapter.isEnabled()) {
            if (mChatService == null) {
                setUpChat();
            }
        }
    }


    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
        if(mBluetoothAdapter.isEnabled()) {
            if (mChatService != null) {
                Log.d(LOG_TAG, "mChatService!=null");
                if (mChatService.getState() == ChatService.STATE_NONE) {
                    mChatService.start();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG,"onDestroy");
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
        getActivity().unregisterReceiver(adapterStateChangeReceiver);
    }
}