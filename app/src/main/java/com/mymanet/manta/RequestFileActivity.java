package com.mymanet.manta;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;

/**
 * allows user to request a file in the network
 */

public class RequestFileActivity extends AppCompatActivity {

    int TIME_TO_LIVE = 10;
    EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_file);
        mEdit = (EditText)findViewById(R.id.requested_file);
    }

    /**
     * when user clicks to request a file, tries to make connections with neighbors
     * @param view
     */
    public void requestFile(View view) {
        String filename = mEdit.getText().toString();

        // get request src id
        TelephonyManager tManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        String uid = tManager.getDeviceId();

        RequestPacket packet = new RequestPacket(filename, TIME_TO_LIVE, uid);
        setContentView(R.layout.activity_propagate_request);

        // TODO move stuff from PropagateRequestActivity here

        // check if current device contains files
        // TODO connect to all of the devices around
        // TODO send request to each other device
//    private void propagateRequest(RequestPacket packet) {
//        if (!packet.isTtlZero()) {
//            // compare requested filename with own files
//        }
//    }
        // compare requested filename with own files
        // if has file,
        // make connections with neighbors
        // send request packet to each possible neighbor
        // decrement max # of hops
        // add this node to path to traverse
//        Intent intent = new Intent(this, WifiP2PActivity.class);
//        startActivity(intent);
    }
}
