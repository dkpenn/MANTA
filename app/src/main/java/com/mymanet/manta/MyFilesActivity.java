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

        String[] fileNames = fileList();
        boolean fileNamesFound = false;
        for(String name : fileNames)
        {
            if(name.equals("file_names.txt"))
            {
                fileNamesFound = true;
            }
        }

        List<String> files = new ArrayList<String>();
        if(fileNamesFound) {
            FileInputStream fis = null;
            BufferedReader br = null;
            try{
                fis = openFileInput("file_names.txt");
                br = new BufferedReader(new InputStreamReader(fis));
                String line;
                while ((line = br.readLine()) != null)
                {
                    files.add(line);
                }
            }
            catch(FileNotFoundException e) {
                e.printStackTrace();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            finally{
                /*http://stackoverflow.com/questions/8981589/close-file-in-finally-block-doesnt-work*/
                if(br != null)
                {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // This is unrecoverable. Just report it and move on
                        e.printStackTrace();
                    }
                }

                if(fis != null)
                {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // This is unrecoverable. Just report it and move on
                        e.printStackTrace();
                    }
                }
            }
        }

        final ArrayAdapter<String> imageNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
        ListView listView = (ListView) findViewById(R.id.uploaded_files);
        listView.setAdapter(imageNamesAdapter);


        /*TODO: Add Shared Preference to note that file is created*/
    }

    public void uploadFiles(View view) {
        Intent intent = new Intent(this, UploadFileActivity.class);
        startActivity(intent);
    }
}
