package com.mymanet.manta;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RequestFileActivity extends AppCompatActivity {

    int TIME_TO_LIVE = 10;
    EditText mEdit;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    WifiP2pManager.PeerListListener mPeerListListener;
    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    IntentFilter mIntentFilter;
    String toConnectDevice;
    Packet packet;
    Handler mBroadcastHandler;

    String statusText;
    private TextView mStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_request_file);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStatus = (TextView) findViewById(R.id.status);

        updateUI();

        packet = null;

        // TODO don't hard code toConnectDevice value
        toConnectDevice = "SIRIUS";

        mEdit = (EditText) findViewById(R.id.requested_file);

        /** Initialize Wifi P2P manager and channel */
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        /** Set up listeners for wifi p2p related signals*/

        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                connectToSpecificDevice(wifiP2pDeviceList);
            }
        };

        /**Invariant is that the person who initiated the connection will act as the server in
         * this TCP connection. This is done for convenience, as the group owner's address
         * is available all those in the group, so the device being connected to can initiate a
         * connection with the group owner. */
        mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

                //if not group owner, start the client
                if (!wifiP2pInfo.isGroupOwner) {
                    startClient(wifiP2pInfo);
                }
                // if group owner, then start the server
                else {
                    startServer(wifiP2pInfo);
                }

                updateUI();
            }

        };


        /**Set up Receiver to intercept Wifi P2P related signals sent by the system */
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListener,
                mConnectionInfoListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(mReceiver, mIntentFilter);
        /**Handler to be used for performing delayed actions/runnable */
        mBroadcastHandler = new Handler();

    }

    private Handler uiHandler = new Handler()
    {
        // This method should be implemented in order to update the UI. Any data
        // that must be passed, should be put in the attribute Message msg.
        public void handleMessage(Message msg)
        {
            updateUI();
        };
    };

    /**
     *  register the broadcast receiver with the intent values to be matched
     *  TODO: understand why we are registering and unregistering the server*/
    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(mReceiver, mIntentFilter);
    }

    /**
     *  unregister the broadcast receiver on pause
     *  */
    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     *  runnable instance of lookForPeers so it can be done with a delay
     *  https://stackoverflow.com/questions/26300889/wifi-p2p-service-discovery-works-intermittently
     */
    private Runnable mServiceBroadcastingRunnable = new Runnable() {
        @Override
        public void run() {
            lookForPeers();
        }
    };

    /***
     * Action taken by the device acting as the server in the connection
     * @param wifiP2pInfo
     */
    private void startServer(WifiP2pInfo wifiP2pInfo) {
        new OFileServerAsyncTask().execute();
    }

    /**
     * Action taken by the device acting as the client in the connection
     * @param wifiP2pInfo
     */
    private void startClient(WifiP2pInfo wifiP2pInfo) {
        InetAddress[] addresses = new InetAddress[1];
        addresses[0] = wifiP2pInfo.groupOwnerAddress;
        new OFileClientAsyncTask().execute(addresses);
    }

    /**
     * Deleting data about requests, requests seen, and responses
     * @param view
     */
    public void deleteTempData(View view) {
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        db.deleteAllRequests();
        db.deleteAllFilterRequests();
        db.deleteAllResponses();
        db.deleteAllStatusMsgs();
        updateUI();
        Log.d("deleteTempData", "finished");
    }

    /**
     * Begins process of scanning for peers
     * @param view
     */
    public void scanForPeers(View view) {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("Discovery Succeeded", "scan for peers button");
            }

            @Override
            public void onFailure(int i) {
                Log.d("Discovery Failure", "reason " + i);
            }
        });
    }

    /**
     * When user clicks to request a file, broadcast request for file to
     * neighbors
     * @param view
     */
    public void requestByFilename(View view) {
        String filename = mEdit.getText().toString();

        // if file exists locally, terminate request
        if (containsFile(filename)) {
            // TODO move to next request
            return;
        }

        //retrieve device name from wifi broadcast receiver
        String deviceName = WifiDirectBroadcastReceiver.mDevice.deviceName;

        //set up request packet to be broadcast
        packet = new Packet(filename, TIME_TO_LIVE, deviceName, PacketType.REQUEST);
        packet.addToPath(deviceName);

        //add request to database
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        db.addRequest(filename, 0);
        db.addFilterRequest(filename, deviceName);
        db.updateStatusMessage("Send REQUEST " + filename);
        updateUI();
        //change screens
        //setContentView(R.layout.activity_propagate_request);

        lookForPeers();
    }

    /***
     * containsFile - check whether file available to the app locally
     * @param filename
     * @return
     */
    public boolean containsFile(String filename) {
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        return db.containsFile(filename);
    }

    public void updateUI() {
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        statusText = db.getStatusMessage();
        if (statusText != null) {

            mStatus.setText(statusText);
            statusText = null;
        }
        else {
            mStatus.setText("");
        }
    }


    /**
     * start peer discovery if there is a packet to process
     */
    public void lookForPeers() {
        if (this.packet != null) {
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // TODO do something here?
                }

                @Override
                public void onFailure(int i) {
                    Log.d("Discovery Failure", "reason " + i);
                }
            });
        } else {
            Log.d("LookforPeers", "packet is null; no request to process");
        }
    }

    /**
     * Once the state of the peer list listener changes,
     * this method is called and executed.
     *
     * Only will initiate connection if packet is not null and specified device can be found
     *
     * @param deviceList : list of wifi p2p devices discoverable from this device
     */
    public void connectToSpecificDevice(WifiP2pDeviceList deviceList) {

        if(this.packet != null) {

            WifiP2pDevice specificDevice = null;

            for (WifiP2pDevice device : deviceList.getDeviceList()) {
                if (device.deviceName.equals(this.toConnectDevice)) {
                    specificDevice = device;
                    break;
                }
            }

            if (specificDevice == null) {
                Log.d("connectToSpecificDevice", "device is not found");
                return;
            }

            // connect to device
            if (specificDevice != null) {

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = specificDevice.deviceAddress;

                /** Setting the group owner intent to highest number makes the device requesting the
                 * connection the most likely to be the group owner.
                 * This is important for the invariant that we have regarding who the group owner is
                 * (See ConnectionInfoListener in onCreate for more details)
                 */
                config.groupOwnerIntent = 15;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // TODO do something here?
                    }

                    @Override
                    public void onFailure(int i) {
                        // TODO do something here?
                    }
                });
            }
        }
        else {
            Log.d("connectToSpecificDevice", "packet is null");
        }
    }

    /**
     * Disconnect from a wifi p2p connection
     * http://stackoverflow.com/questions/18679481/wifi-direct-end-connection-to-peer-on-android
     */
    protected void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    //apparently only remove if you are group owner (server is always group owner)
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("disconnect", "removeGroup onSuccess -");
                                // TODO do something here?
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


    /**
     * client side of connection - responds to requests
     */

    class OFileClientAsyncTask extends AsyncTask<InetAddress, Void, String> {

        private Context context;
        private CharSequence progress = "not connected";

        @Override
        protected String doInBackground(InetAddress[] params) {
            /* busy wait to get server hopefully running,
             * TODO: check if we can remove this and it still works */
            unregisterReceiver(mReceiver);
            if(packet != null) {
                Log.d("Client", "packet is supplied, should not be client");
            }
            else {
                //unregisterReceiver(mReceiver);

                System.out.println("client started");

                for(int i = 0; i < 10000; i++) {
                    System.out.print("");
                }

                context = getApplicationContext();

                InetAddress groupOwnerAddress = params[0];
                int port = 8888;
                Socket socket = new Socket();

                OutputStream outputStream = null;
                InputStream inputStream = null;
                InputStream fileInputStream = null;
                PrintWriter out = null;
                String file = null;

                try {
                    /** Create a client socket with the host, port and timeout information
                     *
                     */
                    socket.bind(null);

                /*changed timeout to 5500ms so connection has time to happen */
                    //try connecting three times
                    boolean notConnected = true;

                    for(int i  = 0; i < 1 && notConnected; i++) {
                        try {
                            socket.close();
                            socket = new Socket();
                            socket.bind(null);
                            socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 5000);
                            notConnected = false;
                        } catch (SocketTimeoutException e) {
                            Log.e("client connection", "fail to make connection with server");
                            e.printStackTrace();
                        } catch (IOException e) {
                            Log.e("error connecting" , e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    if(notConnected) {
                        throw new IOException("didn't connect");
                    }

                    progress = "connected";

                /* get read and write ends of stream socket */
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();

                    /** Read information sent by server about packet */

                    String packetTypeString = null;
                    String srcDevice = null;
                    String filename = null;
                    int ttl = 0;
                    String path = null;
                    int pathPosition = 0;

                    packetTypeString = nextToken(inputStream);

                    if (packetTypeString == null) {
                        Log.e("client", "no packet supplied");
                    } else {
                        Log.v("client", packetTypeString);

                        PacketType packetType = PacketType.fromInt(Integer.parseInt(packetTypeString));

                        srcDevice = nextToken(inputStream);
                        filename = nextToken(inputStream);
                        ttl = Integer.parseInt(nextToken(inputStream));
                        path = nextToken(inputStream);
                        pathPosition = Integer.parseInt(nextToken(inputStream));

                        Packet pkt = new Packet(filename, ttl, srcDevice, packetType, path, pathPosition);

                        System.out.println("got packet");



                        switch (packetType) {
                            case REQUEST:

                                System.out.println("request packet for: " + filename + " from: " + srcDevice);
                                statusText = "Received request packet for: " + filename + " from: " + srcDevice;
                                progress = "received packet";

                                if(true) {
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage(statusText);
                                    uiHandler.sendEmptyMessage(0);
                                }


                                //This packet will be processed
                                String deviceName = WifiDirectBroadcastReceiver.mDevice.deviceName;
                                //Add self to packet path

                                pkt.addToPath(deviceName);

                                // if the request has reached a fileowner, unicast this back along the path
                                // otherwise, broadcast request to other peers
                                if (containsFile(filename)) {
                                    progress = "send ack pack";
                                    System.out.println("to connect device: " +
                                            RequestFileActivity.this.toConnectDevice + "\nsrc: " + srcDevice);

                                    System.out.println("found file: " +
                                            filename);
                                    //change packet to ACK
                                    sendAck(pkt);
                                } else {
                                    progress = "to broadcast packet";
                                    broadcastRequest(pkt);
                                }

                                break;
                            case ACK:

                                System.out.println("ack packet for: " + filename + " from: " + srcDevice);
                                progress = "received packet";
                                statusText = "Received ack packet for: " + filename + " from: " + srcDevice;
                                if(true) {
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage(statusText);
                                    uiHandler.sendEmptyMessage(0);
                                }
                                // if the ack has reached the requester, send a request for the file itself
                                // otherwise continue
                                if (WifiDirectBroadcastReceiver.mDevice.deviceName.equals(srcDevice)) {
                                    sendSend(pkt);
                                } else {
                                    pkt.decrPathPosition();
                                    RequestFileActivity.this.toConnectDevice = pkt.getNodeAtPathPosition();
                                    RequestFileActivity.this.packet = pkt;
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage("SEND ACK for " + filename);
                                    uiHandler.sendEmptyMessage(0);
                                }

                                System.out.println("ack sending to " + RequestFileActivity.this.toConnectDevice);

                                break;
                            case SEND:

                                System.out.println("send packet for: " + filename + " from: " + srcDevice);
                                progress = "received packet";
                                statusText = "Received send packet for: " + filename + " from: " + srcDevice;
                                if(true) {
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage(statusText);
                                    uiHandler.sendEmptyMessage(0);
                                }
                                // if fileowner has been reached, send the file back
                                if (pkt.isLast(
                                        WifiDirectBroadcastReceiver.mDevice.deviceName)) {
                                    sendFilePacket(pkt);
                                } else {
                                    pkt.incrPathPosition();
                                    RequestFileActivity.this.toConnectDevice = pkt.getNodeAtPathPosition();
                                    RequestFileActivity.this.packet = pkt;
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage("SEND SEND for " + filename);
                                    uiHandler.sendEmptyMessage(0);
                                }
                                break;
                            case FILE:

                                System.out.println("file packet for: " + filename + " from: " + srcDevice);
                                progress = "received packet";
                                statusText = "Received file packet for: " + filename + " from: " + srcDevice;

                                // if requester has been reached, stop because transaction is complete
                                // otherwise continue
                                final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                        filename);

                                f.createNewFile();

                                System.out.println("enter copy file for: packet for: " + filename + " from: " + srcDevice);
                                copyFile(inputStream, new FileOutputStream(f));

                                if (!WifiDirectBroadcastReceiver.mDevice.deviceName.equals(srcDevice)) {
                                    pkt.decrPathPosition();
                                    RequestFileActivity.this.toConnectDevice = pkt.getNodeAtPathPosition();
                                    RequestFileActivity.this.packet = pkt;
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage("SEND FILE for " + filename);
                                    uiHandler.sendEmptyMessage(0);
                                }
                                else {
                                    file = filename;
                                    final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
                                    db.updateStatusMessage("Downloading " + filename);
                                    uiHandler.sendEmptyMessage(0);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                } catch (FileNotFoundException e) {
                    Log.e("request file", e.getMessage());
                } catch (SocketException e) {
                    Log.e("error with socket: ", e.getMessage());
                }
                    catch (IOException e) {
                    Log.e("request file", e.getMessage());
                } catch (NumberFormatException e) {
                    Log.e("request file ", e.getMessage());
                } finally {
                    System.out.println("leaving client task");
                    //registerReceiver(mReceiver, mIntentFilter);

                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ioex) {
                            ioex.printStackTrace();
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException ioex) {
                            ioex.printStackTrace();
                        }
                    }

                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioex) {
                            ioex.printStackTrace();
                        }
                    }
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ioex) {
                            ioex.printStackTrace();
                        }
                    }

                }
                registerReceiver(mReceiver, mIntentFilter);
                return file;
            }
            registerReceiver(mReceiver, mIntentFilter);
            return null;
        }

        /* http://stackoverflow.com/questions/8488433/reading-lines-from-an-inputstream-without-buffering */
        String nextToken(InputStream is) throws IOException {
            int c;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (c = is.read(); c != '\n' && c != -1; c = is.read()) {
                byteArrayOutputStream.write(c);
            }
            if (c == -1 && byteArrayOutputStream.size() == 0) {
                return null;
            }
            String line = byteArrayOutputStream.toString("UTF-8");
            return line;
        }

        /**
         * broadcast a request to friends if not already sent
         */
        void broadcastRequest(Packet pkt) {
            Context context = getApplicationContext();
            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);

            if (!db.requestSeen(pkt.getFilename(), pkt.getSrc())) {

                //filter request; ignore subsequent packets
                db.addFilterRequest(pkt.getFilename(), pkt.getSrc());
                db.updateStatusMessage("Broadcast REQUEST " + pkt.getFilename() + "from " +
                pkt.getSrc());
                uiHandler.sendEmptyMessage(0);

                // TODO broadcast to friends (not sender)
                // here just unicasting to one friend
                List<String> peers = db.getTrustedPeers();
                RequestFileActivity.this.toConnectDevice = "Pia";
                RequestFileActivity.this.packet = pkt;


                System.out.println("BROADCAST: changed peer to connect to to be Pia");

                mBroadcastHandler
                        .postDelayed(mServiceBroadcastingRunnable, 2000);

            } else {
                //stop processing packet (ignore)
                //make sure it is null
                RequestFileActivity.this.packet = null;
                return;
            }



        }

        /**
         * send ACK backwards on path if not already done
         */
        void sendAck(Packet pkt) {

            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);

            //check if correct state
            if(!db.responseExists(pkt.getFilename(), pkt.getSrc())) {

                //setup packet and next-hop device
                pkt.changeToACK();
                String node = pkt.getNodeAtPathPosition();

                //add to database
                db.addResponse(pkt.getFilename(), pkt.getSrc());
                db.updateStatusMessage("SEND ACK for " + pkt.getFilename() + " from " + pkt.getSrc());
                uiHandler.sendEmptyMessage(0);

                db.addFilterRequest(pkt.getFilename(), pkt.getSrc());

                RequestFileActivity.this.packet = pkt;
                RequestFileActivity.this.toConnectDevice = node;


            } else {
                //ignore this
                RequestFileActivity.this.packet = null;
            }


        }

        /**
         * Send send packet only if not done so already
         * @param pkt
         */
        void sendSend(Packet pkt) {
            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);

            if (db.requestHasStatus(pkt.getFilename(), 0)) {
                pkt.changeToSEND();
                String node = pkt.getNodeAtPathPosition();
                db.updateRequest(pkt.getFilename(), 1);
                db.updateStatusMessage("SEND SEND packet for " + pkt.getFilename());
                uiHandler.sendEmptyMessage(0);


                RequestFileActivity.this.packet = pkt;
                RequestFileActivity.this.toConnectDevice = node;

            } else {
                //ignore the acknowledgement
                RequestFileActivity.this.packet = null;
            }
        }

        /**
         * Send file if response is in right state
         * @param pkt
         */
        void sendFilePacket(Packet pkt) {
            pkt.changeToFILE();

            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);

            if (db.responseHasStatus(pkt.getFilename(), pkt.getSrc(), 0)) {
                db.updateResponse(pkt.getFilename(), pkt.getSrc(), 1);
                db.updateStatusMessage("SEND FILE for " + pkt.getFilename() + " for " + pkt.getSrc());
                uiHandler.sendEmptyMessage(0);
                String node = pkt.getNodeAtPathPosition();

                RequestFileActivity.this.toConnectDevice = node;
                RequestFileActivity.this.packet = pkt;
            } else {
                //ignore if not in right state
                RequestFileActivity.this.packet = null;
            }
        }

        /**
         * copies file from input stream to output stream
         * @param inputStream from server
         * @param fileOutputStream local
         * @throws IOException
         */
        private void copyFile(InputStream inputStream, FileOutputStream fileOutputStream)
                throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }

            try {
                fileOutputStream.close();
            } catch (SocketException e) {
                Log.e("copy file", e.getMessage());
            } catch(IOException e) {
                Log.e("copy file", e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(String results) {
            if(RequestFileActivity.this.packet != null) {
                mBroadcastHandler
                        .postDelayed(mServiceBroadcastingRunnable, 2000);
            }
            else if (results != null) {
                Context context = getApplicationContext();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);

                //https://stackoverflow.com/questions/12585747/how-to-open-a-file-in-android-via-an-intent#12585945
                String ext = results.substring(results.indexOf('.')+1);
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                System.out.println("extention : " + ext + "mime " + mime);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String path = "file://" + picturesDir.getAbsolutePath() + "/" + results;
                intent.setDataAndType(Uri.parse(path), mime);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

            }
        }

    }

    /**
     * server side of connection -
     * sender of various packets
     */

    class OFileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        @Override
        protected String doInBackground(Void... params) {
            unregisterReceiver(mReceiver);
            if(RequestFileActivity.this.packet == null) {
                Log.d("ServerAsyncTask", "no packet is supplied");
            }
            else {
                System.out.println("server started");
                context = getApplicationContext();

                /** Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */

                PrintWriter out = null;
                ServerSocket serverSocket = null;
                Socket client = null;
                OutputStream outputStream = null;

                try {

                    serverSocket = new ServerSocket(8888);
                    System.out.println("server accepting connections");
                    client = serverSocket.accept();

                    outputStream = client.getOutputStream();

                    // send packet data to connected device
                    switch (packet.getPacketType()) {
                        case REQUEST:
                            out = new PrintWriter(outputStream, true);
                            packet.packetToStream(out, "1");
                            break;
                        case ACK:
                            out = new PrintWriter(outputStream, true);
                            packet.packetToStream(out, "2");
                            break;
                        case SEND:
                            out = new PrintWriter(outputStream, true);
                            packet.packetToStream(out, "3");
                            break;
                        case FILE:
                            packet.packetToStream(outputStream, "4");
                            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            String path = "file:////" + picturesDir.getAbsolutePath() + "/" + packet.getFilename();
                            ContentResolver cr = context.getContentResolver();
                            byte[] buf = new byte[1024];
                            int len;
                            InputStream fileInputStream = cr.openInputStream(Uri.parse(path));
                            System.out.println("start sending file");
                            while ((len = fileInputStream.read(buf)) != -1) {
                                System.out.println("sending..");
                                outputStream.write(buf, 0, len);
                                outputStream.flush();
                            }
                            outputStream.close();
                            System.out.println("finish sending file");
                        default:
                            break;
                    }

                    /**
                     *  If this code is reached, a client has connected and transferred data
                     */

                /* ONLY FOR DEMO PURPOSES (ALSO ASSUMING ONLY SEND TO ONE PEER) */
                    RequestFileActivity.this.packet = null;
                    
                } catch (IOException e) {
                    Log.e("Request File", e.getMessage());
                } finally {
                    //registerReceiver(mReceiver, mIntentFilter);
                    RequestFileActivity.this.packet = null;
                    disconnect();

                    if (client != null) {
                        try {
                            client.close();
                        } catch (IOException e) {
                            Log.e("client closing error", "");
                            e.printStackTrace();
                        }
                    }

                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            Log.e("requestFile", "error closing servers socket");
                            e.printStackTrace();
                        }
                    }
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            Log.e("requestFile", "error closing output stream");
                            e.printStackTrace();
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                /* Stop Peer discovery if it is still going */
                    mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
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
            }
            registerReceiver(mReceiver, mIntentFilter);
            return null;
        }

        @Override
        protected void onPostExecute(String results) {
            Context context = getApplicationContext();
        }

    }

}
