package com.example.MemoryGame;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ThemedSpinnerAdapter;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DownloadService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        String action = intent.getAction();
        if (action.compareToIgnoreCase("download") == 0)
        {
            String where = intent.getStringExtra("where");
            String filename = intent.getStringExtra("filename");
            String downloadThread_name_return = intent.getStringExtra("downloadThread_name");
            String webpage_url_return = intent.getStringExtra("webpage_url");

            downloadToSave(where, filename);
            Intent intent1 = new Intent();
            intent1.setAction("download_ok");
            intent1.putExtra("filename", filename);
            intent1.putExtra("downloadThread_name_return", downloadThread_name_return);
            intent1.putExtra("webpage_url_return", webpage_url_return);
            sendBroadcast(intent1);

        }

        // don't restart this task if killed by Android system
        return START_NOT_STICKY;
    }

    public boolean downloadToSave(String where, String filename) {
        File file = new File(filename);
        try {
            URL url = new URL(where);
            URLConnection conn = url.openConnection();

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = in.read(buf)) != -1)
                out.write(buf, 0, bytesRead);

            out.close();
            in.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}