package com.mymanet.manta;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by dk on 11/1/16.
 * From Wifi Peer-to-Peer Tutorial
 */

public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

    private Context context;
    private TextView statusText;


    @Override
    protected String doInBackground(Void ... params) {

        /** Create a server socket and wait for client connections. This
         * call blocks until a connection is accepted from a client
         */
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket client = serverSocket.accept();

            /**
             *  If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
//            final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                +  ".jpg");

            final File f = new File(Environment.getExternalStorageDirectory() + "/"
                    + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                    +  ".jpg");

            File dirs = new File(f.getParent());
            if(!dirs.exists())
            {
                dirs.mkdirs();
            }
            f.createNewFile();
            InputStream inputStream = client.getInputStream();
            copyFile(inputStream, new FileOutputStream(f));
            serverSocket.close();
            return f.getAbsolutePath();
        } catch (IOException e) {
            Log.e("Wifi P2P activity", e.getMessage());
        }
        return null;
    }

    private void copyFile(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1)
        {
            fileOutputStream.write(buffer, 0, read);
        }
    }

}
