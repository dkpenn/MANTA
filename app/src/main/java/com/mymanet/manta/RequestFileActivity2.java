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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

/**
 * allows user to request a file in the network
 */

public class RequestFileActivity2 extends AppCompatActivity {

    int TIME_TO_LIVE = 10;
    EditText mEdit;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    WifiP2pManager.PeerListListener mPeerListListener;
    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_file);
        mEdit = (EditText)findViewById(R.id.requested_file);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                connectToAllPeers(wifiP2pDeviceList);
            }
        };

        // TODO what's the point of this section?
        mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
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

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel,this, mPeerListListener, mConnectionInfoListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void startServer(WifiP2pInfo wifiP2pInfo) {
        new RequestFileActivity2.OFileServerAsyncTask().execute();
    }

    private void sendFile(WifiP2pInfo wifiP2pInfo) {
        InetAddress[] addresses = new InetAddress[1];
        addresses[0] = wifiP2pInfo.groupOwnerAddress;
        new RequestFileActivity2.OFileClientAsyncTask().execute(addresses);
    }

    // TODO needed?
//    /* register the broadcast receiver with the intent values to be matched */
//    @Override
//    protected void onResume() {
//        super.onResume();
//        registerReceiver(mReceiver, mIntentFilter);
//    }
//    /* unregister the broadcast receiver */
//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver(mReceiver);
//    }

    /**
     * When user clicks to request a file, tries to make connections with neighbors
     * @param view
     */
    public void requestFile(View view) {
        String filename = mEdit.getText().toString();

        // get request src id
        TelephonyManager tManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        String uid = tManager.getDeviceId();

        RequestPacket packet = new RequestPacket(filename, TIME_TO_LIVE, uid);

        if (containsFile()) {
            Context context = getApplicationContext();
            CharSequence text = "Requested file exists on this device";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
            return;
        }

        setContentView(R.layout.activity_propagate_request);

        propagateRequest();
    }

    public void propagateRequest() {
        lookForPeers();
        // TODO connect to all of the devices around
    }

    public boolean containsFile() {
        // TODO implement
        return false;
    }

    public void lookForPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                // TODO decide on success scenario
//                Context context = getApplicationContext();
//                CharSequence text = "Discovery succeeded";
//                int duration = Toast.LENGTH_SHORT;
//
//                Toast toast = Toast.makeText(context, text, duration);
//                toast.show();
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

    public void connectToAllPeers(WifiP2pDeviceList deviceList) {

        for (WifiP2pDevice device : deviceList.getDeviceList()) {

            // connect to device
            if (device != null) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
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
    }

    /**
     * client side of connection
     */
    // TODO sender of file (request)
    // turn request packet into a file
    // send request to each other device
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

            // TODO check if request has been seen before; if so, ignore it

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
     * server side of connection
     */
    // TODO receiver of file (request)
    // receive request packet and parse it
    // if contains the file, send back acknowledgement
    // if not, repeat process
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
                 */

                InputStream inputStream = client.getInputStream();

                // parse request packet


                serverSocket.close();
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
