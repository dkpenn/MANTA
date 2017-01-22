package com.mymanet.manta;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void enterWifiP2PActivity(View view) {
        Intent intent = new Intent(this, WifiP2PActivity.class);
        startActivity(intent);
    }

    public void myFiles(View view) {
        Intent intent = new Intent(this, MyFilesActivity.class);
        startActivity(intent);
    }

    public void requestFile(View view) {
        Intent intent = new Intent(this, RequestFileActivity.class);
        startActivity(intent);
    }
}
