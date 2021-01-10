package com.example.memory_game;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

//    private ArrayList<Thread> threadList = new ArrayList<>();
    private Thread bkgthread2;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String action = intent.getAction();
        if (action.compareToIgnoreCase("download") == 0) {
            bkgthread2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> where = intent.getStringArrayListExtra("where");
                    ArrayList<String> filenames = intent.getStringArrayListExtra("filenames");
                    for (int i = 0; i < filenames.size(); i++) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                        if (Thread.interrupted()){
                            return;
                        }
                        downloadToSave(where.get(i), filenames.get(i));
                        Intent intent = new Intent();
                        intent.setAction("download_ok");
                        intent.putExtra("filename", filenames.get(i));
                        sendBroadcast(intent);
                    }
                }
            });
            bkgthread2.start();
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

        bkgthread2.interrupt();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DownloadService.this,
                        getString(R.string.service_ended),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}