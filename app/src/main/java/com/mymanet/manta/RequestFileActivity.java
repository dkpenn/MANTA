package com.mymanet.manta;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * allows user to request a file in the network
 */

// TODO: implement the following steps
    // when the button on the page is clicked, read the filename specified and
public class RequestFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_file);
    }

    /**
     * when user clicks to request a file, tries to make connections with neighbors
     * @param view
     */
    public void requestFile(View view) {
        // read requested filename from view


        // if within max # of hops
        // compare requested filename with own files
            // if has file,
        // make connections with neighbors
        // send request packet to each possible neighbor
        // decrement max # of hops
        // add this node to path to traverse
        Intent intent = new Intent(this, WifiP2PActivity.class);
        startActivity(intent);
    }

}
