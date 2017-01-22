package com.mymanet.manta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MyFilesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_files);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       refreshListView();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refreshListView();
    }

    public void uploadFiles(View view) {
        Intent intent = new Intent(this, UploadFileActivity.class);
        startActivity(intent);
    }

    public void deleteAll(View view) {
        MySQLLiteHelper db = new MySQLLiteHelper(this);
        db.deleteAllFiles();
        refreshListView();
    }

    private void refreshListView() {
        final MySQLLiteHelper db = new MySQLLiteHelper(this);

        List<String> files = db.getAllFileNames();
        final ArrayAdapter<String> imageNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
        ListView listView = (ListView) findViewById(R.id.uploaded_files);
        listView.setAdapter(imageNamesAdapter);
    }
}
