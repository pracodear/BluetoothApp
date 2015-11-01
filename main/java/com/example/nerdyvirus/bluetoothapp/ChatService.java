package com.example.nerdyvirus.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by prakhar on 19/9/15.
 */
public class ChatService {

    //private static final String LOG_TAG = ChatService.class.getSimpleName();
    private static final String LOG_TAG = "ChatBoxFragment"+ChatService.class.getSimpleName();
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mState;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    static public byte[] object2Bytes( Object o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        return baos.toByteArray();
    }

    public ChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }



    public synchronized void start() {
        Log.d(LOG_TAG, "Start");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(LOG_TAG, "Connecting to: " + device);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(LOG_TAG, "Connected to "+device);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(4);
        Bundle bundle = new Bundle();
        bundle.putString("device name", device.getName());
        Log.d(LOG_TAG,"device name "+device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(LOG_TAG, "Stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        Log.d(LOG_TAG,"setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(1, state, -1).sendToTarget();
    }
    public synchronized int getState() {
        return mState;
    }

    public void write(byte[] out,String flag) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        int x=out.length;

        Log.d(LOG_TAG,"Write 1 "+x);
        r.write(out, flag);

    }

    private void connectionFailed() {
        Log.d(LOG_TAG, "connectionFailed");
        Message msg = mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        ChatService.this.start();
    }

    private void connectionLost() {
        Log.d(LOG_TAG, "connectionLost");
        Message msg = mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        ChatService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type: listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.i(LOG_TAG, "Socket Type: BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Socket Type: accept() failed", e);
                    break;
                }

                if (socket != null) {
                    Log.d(LOG_TAG, "Socket!=null");
                    synchronized (ChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(LOG_TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(LOG_TAG, "END mAcceptThread, socket Type: ");
        }

        public void cancel() {
            Log.d(LOG_TAG, "Socket Type cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type: create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(LOG_TAG, "BEGIN mConnectThread SocketType:");
            setName("ConnectThread");
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(LOG_TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (ChatService.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(LOG_TAG, "create ConnectedThread: ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(LOG_TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(LOG_TAG, "BEGIN mConnectedThread");
            byte[] flag = new byte[5];
            byte[] buffer = new byte[1024];

            //byte[] buffer2 = new byte[10240];
            int bytes,fbytes=0,fileType=-1;

            while (true) {
                try {
                    fbytes = mmInStream.read(flag);
                    mHandler.obtainMessage(2, 1, fbytes, flag)
                            .sendToTarget();
                    String flagstr= new String(flag, 0, fbytes);
                    Log.d(LOG_TAG,"ReadData: "+flagstr);
                    if(flagstr.equals("text"))
                        fileType=1;
                    else if(flagstr.equals("image"))
                        fileType=2;

                    Log.d(LOG_TAG,"FileType "+fileType);

                    if(fileType==1)
                    {
                        bytes=mmInStream.read(buffer);
                        mHandler.obtainMessage(2, 2, bytes, buffer)
                                    .sendToTarget();
                    }
                    else if(fileType==2)
                    {
                        File photo;
                        String newFolder = "/NewFolder2/videos";
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        File newDirectory = new File(Environment
                                .getExternalStorageDirectory().toString() + newFolder);

                        newDirectory.mkdirs();
                        photo=new File(newDirectory,timeStamp);
                        Log.d(LOG_TAG,"Directory created");

                        FileOutputStream fos = new FileOutputStream(photo.getPath());
                        Log.d(LOG_TAG,"FOS created");
                        int total=0;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Log.d(LOG_TAG,"BAOS created");
                        BufferedInputStream bf=new BufferedInputStream(mmInStream);
                        while( (bytes=bf.read(buffer))>0)
                        {
                            Log.d(LOG_TAG, "total read  i" + bytes);

                            total+=bytes;
                           // fos.write(buffer);
                            String eof=buffer.toString();
                            if(eof.endsWith("end of file"))
                                break;

                           baos.write( buffer, 0, bytes );
                            //Log.d(LOG_TAG,"written data");
                        }
                        baos.writeTo(fos);
                        Log.d(LOG_TAG, "total read o" + baos.size());
                        Log.d(LOG_TAG,"BAOS filled");

                    }

                } catch (IOException e) {
                    Log.d(LOG_TAG, "disconnected", e);
                    connectionLost();
                    ChatService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer,String flag) {
            try {
                byte[] flagbuffer=flag.getBytes();
                mmOutStream.flush();
                mmOutStream.write(flagbuffer);
                mHandler.obtainMessage(3, 1, -1, flagbuffer)
                        .sendToTarget();

                mmOutStream.flush();
                mmOutStream.write(buffer);
                mHandler.obtainMessage(3, 2, -1, buffer)
                        .sendToTarget();


                //mmOutStream.flush();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "close() of connect socket failed", e);
            }
        }
    }
}
