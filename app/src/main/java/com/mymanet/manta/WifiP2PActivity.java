package com.mymanet.manta;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/*
Template from https://developer.android.com/guide/topics/connectivity/wifip2p.html#creating-app
 */
public class WifiP2PActivity extends AppCompatActivity {

    WifiP2pManager mManager;
    Channel mChannel;
    BroadcastReceiver mReceiver;
    PeerListListener mPeerListListener;
    ConnectionInfoListener mConnectionInfoListener;
    IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mPeerListListener = new PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                //showListOfPeers(wifiP2pDeviceList.getDeviceList());
                connectToFirstDevice(wifiP2pDeviceList);
            }
        };

        mConnectionInfoListener = new ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

                Context context = getApplicationContext();
                CharSequence text = "Connection Successful:" + wifiP2pInfo.groupOwnerAddress +"\n";
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, text, duration).show();

                //if not group owner, then send file as client
                if(!wifiP2pInfo.isGroupOwner) {
                    CharSequence text2 = "not group owner\n";
                    Toast.makeText(context, text2, duration).show();
                    sendFile(wifiP2pInfo);
                }
                // if group owner, then receive file like server
                else {
                    CharSequence text2 = "group owner\n";
                    Toast.makeText(context, text2, duration).show();
                    startServer(wifiP2pInfo);
                }
            }
        };

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel,this, mPeerListListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        setContentView(R.layout.activity_wifi_p2_p);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void startServer(WifiP2pInfo wifiP2pInfo) {
        new OFileServerAsyncTask().execute();
    }

    private void sendFile(WifiP2pInfo wifiP2pInfo) {
        InetAddress[] addresses = new InetAddress[1];
        addresses[0] = wifiP2pInfo.groupOwnerAddress;
        new OFileClientAsyncTask().execute(addresses);
    }

    public void connectToFirstDevice(WifiP2pDeviceList deviceList) {

        // get first device
        WifiP2pDevice firstDevice = null;
        for(WifiP2pDevice device : deviceList.getDeviceList())
        {
            firstDevice = device;
            break;
        }

        // connect to device
        if(firstDevice != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = firstDevice.deviceAddress;

            mManager.connect(mChannel, config, new ActionListener() {
                @Override
                public void onSuccess() {
                    Context context = getApplicationContext();
                    CharSequence text = "Connection Successful: In Progress";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }

                @Override
                public void onFailure(int i) {
                    Context context = getApplicationContext();
                    CharSequence text = "Connection Failed: In Progress";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }
            });
        }
    }

//    public void showListOfPeers(Collection<WifiP2pDevice> deviceList) {
//        Context context = getApplicationContext();
//        CharSequence text = "In Progress";
//        int duration = Toast.LENGTH_SHORT;
//        Toast.makeText(context, text, duration).show();
//    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void lookForPeers(View view)
    {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                Context context = getApplicationContext();
                CharSequence text = "Discovery succeeded";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }

            @Override
            public void onFailure(int i) {
                Context context = getApplicationContext();
                CharSequence text = "Discovery failed";
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, text, duration).show();
                Log.d("Discovery Failure", "reason " + i);
            }
        });
    }

     class OFileClientAsyncTask extends AsyncTask<InetAddress, Void, String> {

        private Context context;
        private TextView statusText;


        @Override
        protected String doInBackground(InetAddress[] params) {

            Context context = getApplicationContext();

            InetAddress groupOwnerAddress = params[0];
            int port = 8888;
            int len;
            Socket socket = new Socket();
            byte[] buf = new byte[1024];

            try {
                /** Create a client socket with the host, port and timeout information
                 *
                 */
                socket.bind(null);
                socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 500);
                OutputStream outputStream = socket.getOutputStream();

                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = null;

                //inputStream = cr.openInputStream(Uri.parse("/storage/emulated/0/DCIM/Camera/Desk.jpg"));
                File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String path1 = "file:////"+ root.getAbsolutePath() + "/Desk.jpg";
                String path = "file://sdcard/Pictures/";
                inputStream = cr.openInputStream(Uri.parse(path1));
                while((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }

                outputStream.close();
                inputStream.close();

            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

         protected void onProgressUpdate() {
            Context context = getApplicationContext();
            CharSequence text = "Sending...";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }

         protected void onPostExecute() {
             Context context = getApplicationContext();
             CharSequence text = "Sent";
             int duration = Toast.LENGTH_SHORT;
             Toast.makeText(context, text, duration).show();
         }

    }

    /**
     * Created by dk on 11/1/16.
     * From Wifi Peer-to-Peer Tutorial
     */

    class OFileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        @Override
        protected String doInBackground(Void ... params) {

            /** Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            try {
                ServerSocket serverSocket = new ServerSocket(8888);

                Socket client = serverSocket.accept();

                /**
                 *  If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */
//            final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                +  ".jpg");

//                final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                        +  ".jpg");

                final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        System.currentTimeMillis() + ".jpg");


//                File dirs = new File(f.getParent());
//                if(!dirs.exists())
//                {
//                    dirs.mkdirs();
//                }

                f.createNewFile();

                InputStream inputStream = client.getInputStream();
                copyFile(inputStream, new FileOutputStream(f));

                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("Wifi P2P activity", e.getMessage());
            }
            return null;
        }

        private void copyFile(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1)
            {
                fileOutputStream.write(buffer, 0, read);
            }
        }

        protected void onProgressUpdate() {
            Context context = getApplicationContext();
            CharSequence text = "Sending...";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }

        protected void onPostExecute() {
            Context context = getApplicationContext();
            CharSequence text = "Sent";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }

    }

}
