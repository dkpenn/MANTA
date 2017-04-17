package com.mymanet.manta;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

public class TrustedPeersActivity extends AppCompatActivity {

    EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_peers);
        mEdit = (EditText) findViewById(R.id.peer_name);
        refreshListView();
    }

    private void refreshListView() {
        final MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);

        List<String> files = db.getTrustedPeers();
        final ArrayAdapter<String> imageNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
        ListView listView = (ListView) findViewById(R.id.trusted_peers_list);
        listView.setAdapter(imageNamesAdapter);
    }

    public void addPeer(View view) {
        String peer = mEdit.getText().toString();
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        db.addPeer(peer);
        refreshListView();

    }

    public void deletePeers(View view) {
        MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);
        db.deleteAllTrustedPeers();
        refreshListView();
    }
}