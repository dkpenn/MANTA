package com.mymanet.manta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
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
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class RequestFileActivity extends AppCompatActivity {

    int TIME_TO_LIVE = 10;
    EditText mEdit;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    WifiP2pManager.PeerListListener mPeerListListener;
    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    IntentFilter mIntentFilter;
    String mDeviceName;
    String toConnectDevice;
    Packet packet;
//    List<Packet> packets; // packets that need to be processed

    Handler mBroadcastHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        packets = new ArrayList<>();
        System.out.println("onCreate entered");
        setContentView(R.layout.activity_request_file);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        packet = null;
        //mDeviceName = getHostName("");

        // TODO don't hard code toConnectDevice value
        toConnectDevice = "SIRIUS";

        mEdit = (EditText) findViewById(R.id.requested_file);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                connectToFirstDevice(wifiP2pDeviceList);
            }
        };

        mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

                Context context = getApplicationContext();
                CharSequence text = "Connection Successful:" + wifiP2pInfo.groupOwnerAddress + "\n";
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, text, duration).show();

                //if not group owner, then send file as client
                if (!wifiP2pInfo.isGroupOwner) {
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


        mBroadcastHandler = new Handler();
        //mBroadcastHandler.postDelayed(mServiceBroadcastingRunnable, 1000);

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListener,
                mConnectionInfoListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //mDeviceName = WifiDirectBroadcastReceiver.mDevice.deviceName;

    }

    /* from stack overflow
    * https://stackoverflow.com/questions/26300889/wifi-p2p-service-discovery-works-intermittently
    * */
    private Runnable mServiceBroadcastingRunnable = new Runnable() {
        @Override
        public void run() {
//            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                }
//
//                @Override
//                public void onFailure(int error) {
//                }
//            });
            lookForPeers();
        }
    };

    /*http://stackoverflow.com/questions/26643989/android-how-to-get-current-device-wifi-direct-name
    * get device name of this device*/

//    public static String getHostName(String defValue) {
//        try {
//            Method getString = Build.class.getDeclaredMethod("getString", String.class);
//            getString.setAccessible(true);
//            return getString.invoke(null, "net.hostname").toString();
//        } catch (Exception ex) {
//            return defValue;
//        }
//    }

    private void startServer(WifiP2pInfo wifiP2pInfo) {
        new OFileServerAsyncTask().execute();
    }

    private void sendFile(WifiP2pInfo wifiP2pInfo) {
        InetAddress[] addresses = new InetAddress[1];
        addresses[0] = wifiP2pInfo.groupOwnerAddress;
        new OFileClientAsyncTask().execute(addresses);
    }

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

    /**
     * When user clicks to request a file, tries to make connections with neighbors
     *
     * @param view
     */
    public void requestByFilename(View view) {
        String filename = mEdit.getText().toString();

        // if file exists locally, terminate request
        if (containsFile(filename)) {
            Context context = getApplicationContext();
            CharSequence text = "Requested file exists on this device";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
            // TODO move to next request
            return;
        }
        String deviceName = WifiDirectBroadcastReceiver.mDevice.deviceName;
        packet = new Packet(filename, TIME_TO_LIVE, deviceName, PacketType.REQUEST);
        packet.addToPath(deviceName);

        setContentView(R.layout.activity_propagate_request);

        lookForPeers();
    }

    /***
     * containsFile - check whether file is locally stored
     * @param filename
     * @return
     */
    public boolean containsFile(String filename) {
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        return db.containsFile(filename);
    }

    public void lookForPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

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

    /**
     * Initial callback method for peer list listener.
     * Once the state of the peer list listener changes,
     * this method is called
     *
     * @param deviceList
     */
    public void connectToFirstDevice(WifiP2pDeviceList deviceList) {

        // get first device
        WifiP2pDevice firstDevice = null;

        for (WifiP2pDevice device : deviceList.getDeviceList()) {
            System.out.println("to connect device: " + this.toConnectDevice);
            if (device.deviceName.equals(this.toConnectDevice)) {
                firstDevice = device;
                break;
            }
        }

        if (firstDevice == null) {
            Log.d("connectToFirstDevice", "device is not found");
            return;
        }

        // connect to device
        if (firstDevice != null) {

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = firstDevice.deviceAddress;
            config.groupOwnerIntent = 15;

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

    /**
     * disconnect from stack overflow
     * http://stackoverflow.com/questions/18679481/wifi-direct-end-connection-to-peer-on-android
     */
    protected void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null
                            && group.isGroupOwner()) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d("disconnect", "removeGroup onSuccess -");

                                // TODO remove this
                                Context context = getApplicationContext();
                                CharSequence text = "Disconnected";
                                int duration = Toast.LENGTH_SHORT;
                                Toast.makeText(context, text, duration).show();
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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * client side of connection
     */
    // receive request packet and parse it
    // if contains the file, send back acknowledgement
    // if not, repeat process
    // TODO sender of file (request)
    // turn request packet into a file
    // send request to each other device

    class OFileClientAsyncTask extends AsyncTask<InetAddress, Void, String> {

        private Context context;
        private TextView statusText;
        private CharSequence progress = "not connected";

        @Override
        protected String doInBackground(InetAddress[] params) {

            /* busy wait to get server hopefully running */
            for (int i = 0; i < 1000; i++) {

            }

            System.out.println("client started");

            Context context = getApplicationContext();
            InetAddress groupOwnerAddress = params[0];
            int port = 8888;
            int len;
            Socket socket = new Socket();
            byte[] buf = new byte[1024];
            OutputStream outputStream = null;
            InputStream inputStream = null;
            InputStream fileInputStream = null;
            PrintWriter out = null;

            try {
                /** Create a client socket with the host, port and timeout information
                 *
                 */
                socket.bind(null);

                /*changed timeout to 1000ms so connection has time to happen */
                socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 5500);

                if (Debug.isDebuggerConnected())
                    Debug.waitForDebugger();

                progress = "connected";
                /* get read and write ends of stream socket */
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(inputStream)
                );

                String packetTypeString = in.readLine();
                Log.v("client", packetTypeString);
                PacketType packetType = PacketType.fromInt(Integer.parseInt(packetTypeString));

                Packet pkt = null;
                String srcDevice;
                String filename;
                int ttl;
                String path;
                int pathPosition;

                System.out.println("got packet");
                switch (packetType) {
                    case REQUEST:
                        srcDevice = in.readLine();
                        filename = in.readLine();
                        ttl = Integer.parseInt(in.readLine());
                        path = in.readLine();
                        pathPosition = Integer.parseInt(in.readLine());
                        pkt = new Packet(filename, ttl, srcDevice, packetType, path, pathPosition);

                        System.out.println("request packet for: " + filename + " from: " + srcDevice);
                        progress = "received packet";
                        // if request has been seen before, ignore it, otherwise record it
                        final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
//                        if (db.requestSeen(filename, srcDevice)) {
//                            break;
//                        } else {
//                            db.addFilterRequest(filename, srcDevice);
//                        }
                        RequestFileActivity.this.packet = pkt;
                        RequestFileActivity.this.packet.addToPath(RequestFileActivity.this.mDeviceName);

                        if (containsFile(filename)) {
                            System.out.println("to connect device: " +
                                    RequestFileActivity.this.toConnectDevice + "\nthis device: " +
                                    RequestFileActivity.this.mDeviceName + "\nsrc: " + srcDevice);
                            System.out.println("found file: " +
                                    filename);
                            sendAck(srcDevice);
                            db.addResponse(filename, srcDevice);

                        } else {
                            // TODO uncomment
                            progress = "to broadcast packet";
                            broadcastRequest();
                        }

                        final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "hey.txt");
                        f.createNewFile();
                        break;
                    case ACK:
                        srcDevice = in.readLine();
                        filename = in.readLine();
                        ttl = Integer.parseInt(in.readLine());
                        path = in.readLine();
                        pathPosition = Integer.parseInt(in.readLine());
                        pkt = new Packet(filename, ttl, srcDevice, packetType, path, pathPosition);

                        System.out.println("ack packet for: " + filename + " from: " + srcDevice);
                        progress = "received packet";

                        pkt.decrPathPosition();
                        RequestFileActivity.this.toConnectDevice = pkt.getNodeAtPathPosition();
                        System.out.println("ack sending to " + RequestFileActivity.this.toConnectDevice);
                        RequestFileActivity.this.packet = pkt;

                        break;
                    case SEND:
                        break;
                    default:
                        break;
                }

                String file;
                if (pkt != null) {
                    file = pkt.getFilename();
                } else {
                    file = "whatup.txt";
                }
                final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        file);

                f.createNewFile();

            } catch (FileNotFoundException e) {
                Log.e("request file", e.getMessage());
            } catch (IOException e) {
                Log.e("request file", e.getMessage());
            } catch (NumberFormatException e) {
                Log.e("request file ", e.getMessage());
            } finally {
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
//                disconnect();

            }

            return null;
        }

        void broadcastRequest() {
            Context context = getApplicationContext();
            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
            List<String> peers = db.getTrustedPeers();
            RequestFileActivity.this.toConnectDevice = "Pia";
            System.out.println("BROADCAST: changed peer to connect to to be Pia");
            mBroadcastHandler
                    .postDelayed(mServiceBroadcastingRunnable, 1000);
            //lookForPeers();
            // TODO broadcast to friends (not sender)
            // here just broadcasting to one friend
        }

        /**
         * send ACK backwards through network
         *
         * @param src device REQ came from, and ACK should go to
         */
        void sendAck(String src) {
            RequestFileActivity.this.packet.changeToACK();
            RequestFileActivity.this.toConnectDevice = src;
            System.out.println("ACK: sending ack from " + RequestFileActivity.this.mDeviceName +
                    " to: " + src);
            String file = "ackSent";

            final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    file);

            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        protected void onProgressUpdate() {
            Context context = getApplicationContext();
            CharSequence text = "Sending...";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }

        @Override
        protected void onPostExecute(String results) {
            disconnect();
            lookForPeers();
//            if(RequestFileActivity.this.packet != null) {
//              switch (RequestFileActivity.this.packet.getPacketType()) {
//                  case REQUEST:
//                      broadcastRequest();
//                      break;
//                  default:
//                      break;
//              }
//            }
//            else {


//                Context context = getApplicationContext();
//                int duration = Toast.LENGTH_SHORT;
//                Toast.makeText(context, progress, duration).show();
//                Intent intent = new Intent(context, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intent);
            }
//        }

    }

    /**
     * server side of connection -
     * sender of various packets
     */

    class OFileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        @Override
        protected String doInBackground(Void... params) {
            if(RequestFileActivity.this.packet == null) {
                Log.d("ServerAsyncTask", "no packet is supplied");
            }
            System.out.println("server started");



            /** Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */

            PrintWriter out = null;
            ServerSocket serverSocket = null;
            Socket client = null;
            InputStream inputStream = null;

            try {

                serverSocket = new ServerSocket(8888);

                client = serverSocket.accept();

                if (Debug.isDebuggerConnected())
                    Debug.waitForDebugger();

                inputStream = client.getInputStream();
                out = new PrintWriter(client.getOutputStream(), true);

                if (packet == null) {
                    CharSequence text = "Request Error";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                    throw new NoPacketException();
                }


                switch (packet.getPacketType()) {
                    case REQUEST:
                        /**
                         * Write request packet data
                         * */
                        int translatedPacketType = PacketType.toInt(packet.getPacketType());
                        //out.println(translatedPacketType + "\n");
                        out.println("1");
                        out.println(packet.getSrc());
                        out.println(packet.getFilename());
                        out.println(packet.getTimeToLive() + "");
                        out.println(packet.pathToString());
                        out.println(packet.pathPosition);
                        break;
                    default:
                        break;
                }

                /**
                 *  If this code is reached, a client has connected and transferred data
                 */

//                InputStream inputStream = client.getInputStream();
//                String filename = inputStream.toString();
//
////                text = "recieved " + filename;
////                Toast.makeText(context, text, duration).show();
//
//                System.out.println("Recieved " + filename + "!");
//
//                serverSocket.close();
//
//                boolean returned = returnFile(filename);
                final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "done.txt");

                f.createNewFile();

                /*ONLY FOR DEMO PURPOSES (ALSO ASSUMING ONLY SEND TO ONE PEER) */
                RequestFileActivity.this.packet = null;

            } catch (NoPacketException e) {
                Log.e("Request File", e.getMessage());
            } catch (IOException e) {
                Log.e("Request File", e.getMessage());
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
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
                disconnect();
            }
            return null;
        }

        private void copyFile(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }
        }

        /**
         * If this phone has the requested file, send it back
         *
         * @param filename requested file
         * @return true if had file and sent it back, false otherwise
         */
        private boolean returnFile(String filename) {
            Context context = getApplicationContext();
            final MySQLLiteHelper db = MySQLLiteHelper.getHelper(context);
            List<MantaFile> files = db.getFilesWithName(filename);
            MantaFile toSend = null;
            if (files.isEmpty()) {
                return false;
            } else {
                // arbitrarily send first file in list of files with this name
                // assume there shouldn't be more than one file with a given name
                toSend = files.get(0);
            }
            // TODO send toSend back
            return true;
        }

        protected void onProgressUpdate() {
            Context context = getApplicationContext();
            CharSequence text = "Sending...";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }

        @Override
        protected void onPostExecute(String results) {
            disconnect();
            Context context = getApplicationContext();
            CharSequence text = "Sent";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        }

    }

}
