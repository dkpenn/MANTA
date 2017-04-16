package com.mymanet.manta;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void myFiles(View view) {
        Intent intent = new Intent(this, MyFilesActivity.class);
        startActivity(intent);
    }

    public void trustedPeers(View view) {
        Intent intent = new Intent(this, TrustedPeersActivity.class);
        startActivity(intent);
    }

}
