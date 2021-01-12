package com.example.MemoryGame;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ThemedSpinnerAdapter;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class DownloadService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        String action = intent.getAction();
        if (action.compareToIgnoreCase("download") == 0)
        {
            // new thread needs to be created from within service
            // to run code in service in threads other than main thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String where = intent.getStringExtra("where");
                    String filename = intent.getStringExtra("filename");
                    String search_session_id_return = intent.getStringExtra("search_session_id");

//                    System.out.println(filename);
//                    System.out.println("this Thread ID: " + Thread.currentThread().getId());
//                    System.out.println("this Thread Name: " + Thread.currentThread().getName());

                    if (downloadToSave(where, filename)) {
                        resizeImage(filename);
                        Intent intent1 = new Intent();
                        intent1.setAction("download_ok");
                        intent1.putExtra("filename", filename);
                        intent1.putExtra("search_session_id_return", search_session_id_return);
                        sendBroadcast(intent1);
                    }
                }
            }).start();
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

    public void resizeImage(String filename) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);
        if (bitmap != null) {
            int[] arr = {bitmap.getWidth(), bitmap.getHeight()};
            int dim = Arrays.stream(arr).filter((int x)->x != 0).min().getAsInt();
            Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, dim, dim);

            try {
                File file = new File(filename);
                FileOutputStream out = new FileOutputStream(file);

                resized.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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