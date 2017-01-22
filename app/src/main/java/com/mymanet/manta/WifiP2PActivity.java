package com.mymanet.manta;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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
    EditText mEdit;

    String filename = "";

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

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel,this, mPeerListListener, mConnectionInfoListener);


        mManager.discoverPeers(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("wifi p2p", "discover peers");
            }

            @Override
            public void onFailure(int i) {
                Log.d("wifi p2p", "not discover peers");
            }
        });

        /* Setup intent filter for activity*/
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        setContentView(R.layout.activity_wifi_p2_p);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEdit = (EditText)findViewById(R.id.fileName);


    }

    /***
     *  Register the broadcast receiver with the intent values to be matched
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /**
     * unregister the broadcast receiver when activity is paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }


    /**
     * Initial callback method for peer list listener.
     * Once the state of the peer list listener changes,
     * this method is called
     * @param deviceList
     */
    public void connectToFirstDevice(WifiP2pDeviceList deviceList) {

        // get first device
        WifiP2pDevice firstDevice = null;
        for(WifiP2pDevice device : deviceList.getDeviceList())
        {
            if(device.deviceName.equals("Lord of Darkness"))
                firstDevice = device;
                break;
        }

        // connect to device
        if(firstDevice != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = firstDevice.deviceAddress;
            config.groupOwnerIntent = 15;

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

            Context context = getApplicationContext();
            CharSequence text = "Not found";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }
    }

//    public void showListOfPeers(Collection<WifiP2pDevice> deviceList) {
//        Context context = getApplicationContext();
//        CharSequence text = "In Progress";
//        int duration = Toast.LENGTH_SHORT;
//        Toast.makeText(context, text, duration).show();
//    }

    /**
     * Method associated with button on wifi p2p activity to initiate
     * peer discovery
     * @param view
     */
    public void lookForPeers(View view)
    {
        filename = mEdit.getText().toString();

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

    /***
     * Method for group owner to start server to wait for file from not group owner
     * @param wifiP2pInfo
     */
    private void startServer(WifiP2pInfo wifiP2pInfo) {
        new OFileServerAsyncTask().execute();
    }

    /***
     * Method for not group owner to send file to group owner (person who initiated connection)
     */
    private void sendFile(WifiP2pInfo wifiP2pInfo) {
        InetAddress[] addresses = new InetAddress[1];
        addresses[0] = wifiP2pInfo.groupOwnerAddress;
        new OFileClientAsyncTask().execute(addresses);
    }

    /**
     * disconnect from stack overflow
     * http://stackoverflow.com/questions/18679481/wifi-direct-end-connection-to-peer-on-android
     * */
    protected void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("disconnect", "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d("disconnect", "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    /***
     * Class describing async task for client to transfer file to server
     */
     class OFileClientAsyncTask extends AsyncTask<InetAddress, Void, String> {

        private Context context;
        private TextView statusText;


        @Override
        protected String doInBackground(InetAddress[] params) {

            if(android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();

            Context context = getApplicationContext();

            InetAddress groupOwnerAddress = params[0];
            int port = 8888;
            int len;
            Socket socket = new Socket();
            byte[] buf = new byte[1024];
            OutputStream outputStream = null;
            InputStream inputStream = null;
            InputStream fileInputStream = null;
            try {
                /** Create a client socket with the host, port and timeout information
                 *
                 */
                socket.bind(null);
                socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 500);

                /**
                 * Get name of file that server wants
                 */
                inputStream = socket.getInputStream();
                BufferedReader in  = new BufferedReader(
                        new InputStreamReader(inputStream)
                );
                String filename = in.readLine();

                /**
                 * Send desired file
                 */
                outputStream = socket.getOutputStream();

                ContentResolver cr = context.getContentResolver();
                fileInputStream = null;

                //inputStream = cr.openInputStream(Uri.parse("/storage/emulated/0/DCIM/Camera/Desk.jpg"));
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String path1 = "file:////"+ picturesDir.getAbsolutePath() + "/"+ filename;
                //String path = "file://sdcard/Pictures/";

                fileInputStream = cr.openInputStream(Uri.parse(path1));
                while((len = fileInputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }

            }
            catch (FileNotFoundException e) {
                PrintWriter textOut = new PrintWriter(outputStream, true);
                textOut.println("not found");
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally
            {
                if(outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                }
                if(fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                }

                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                }

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
     * Class describing async task for server to accept file from client
     */

    class OFileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        @Override
        protected String doInBackground(Void ... params) {

            /** Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            PrintWriter out = null;
            ServerSocket serverSocket = null;
            Socket client = null;
            try {
                serverSocket = new ServerSocket(8888);

                client = serverSocket.accept();

                /**
                 * Send file name
                 */
                out = new PrintWriter(client.getOutputStream(), true);
                out.println(filename);
                // out.println("Desk.jpg");

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
                       filename);


//                File dirs = new File(f.getParent());
//                if(!dirs.exists())
//                {
//                    dirs.mkdirs();
//                }

                f.createNewFile();

                InputStream inputStream = client.getInputStream();
                copyFile(inputStream, new FileOutputStream(f));

                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("Wifi P2P activity", e.getMessage());
            }
            finally
            {
                try {
                    client.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

                try {
                    serverSocket.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

                out.close();
                disconnect();
                mManager.stopPeerDiscovery(mChannel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("wifi p2p", "stop peer discovery");
                    }

                    @Override
                    public void onFailure(int i) {
                        Log.d("wifi p2p", "not stop peer discovery");
                    }
                });
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

            try{
                fileOutputStream.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
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
