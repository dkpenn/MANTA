package com.mymanet.manta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by PiaKochar on 1/13/17.
 */


public class PropagateRequestActivity extends AppCompatActivity {

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
                connectToAllPeers(wifiP2pDeviceList);
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
//                    sendFile(wifiP2pInfo);
                }
                // if group owner, then receive file like server
                else {
                    CharSequence text2 = "group owner\n";
                    Toast.makeText(context, text2, duration).show();
//                    startServer(wifiP2pInfo);
                }
            }
        };

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel,this, mPeerListListener, mConnectionInfoListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        lookForPeers();

        // TODO is any of this necessary?
        setContentView(R.layout.activity_propagate_request);
//        setContentView(R.layout.activity_wifi_p2_p);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

    }

    public void connectToAllPeers(WifiP2pDeviceList deviceList) {

        for (WifiP2pDevice device : deviceList.getDeviceList()) {

            // connect to device
            if (device != null) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

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
    }

    public void lookForPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
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
}
