package com.mymanet.manta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class UploadFileActivity extends AppCompatActivity {

    public void onBackButtonClick(View button) {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* First check if any files already exist in saved */
        /* TODO: Add action to filter out files that already exist*/

        File imageDir = Environment.getExternalStoragePublicDirectory("/" +
                "DCIM/Camera");
        Boolean isDir = imageDir.isDirectory();
        //Get list of images
        String [] images = imageDir.list();
        //Display in listview
        // https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
        final ArrayAdapter<String> imageNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, images);
        ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(imageNamesAdapter);

        final MySQLLiteHelper db = MySQLLiteHelper.getHelper(this);

        //Set Action for when file is selected
        AdapterView.OnItemClickListener childClickedHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String imageName = imageNamesAdapter.getItem(position);

                db.addFile(new MantaFile(imageName));
            }
        };
        listView.setOnItemClickListener(childClickedHandler);

    }

}

