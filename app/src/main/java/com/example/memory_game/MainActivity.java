package com.example.memory_game;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    int max_pics = 20;
    int max_sel = 6;
    Thread bkgdThread;
    private boolean running = false;
    int pos, nsel;
    ArrayList<String> sel_pics, filenames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setInitialValue();
        findViewById(R.id.fetch).setOnClickListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("download_ok");
        filter.addAction("pic_selected");
        registerReceiver(receiver, filter);
    }

    protected String[] getUrls(String webpage_url, int max_pics) {
        Document document = null;

        try {
            document = Jsoup.connect(webpage_url).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (document == null){
            return new String[]{""};
        }
        Elements es = document.select("img[src~=https.*jpg]");

        List<String> urls = new ArrayList<String>();

        for (Element e : es)
            urls.add(e.attr("src"));

        if (urls.size() > max_pics)
            urls = urls.subList(0, max_pics);

        String[] out = urls.toArray(new String[urls.size()]);

        return out;
    }

    protected String[] makeFileNames(File dir, String[] urls) {
        String[] out = new String[urls.length];

        for (int i = 0; i < urls.length; i++)
            out[i] = new File(dir + "/" + new File(urls[i]).getName()).toString();

        return out;
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("download_ok")) {
                pos++;
                String filename = intent.getStringExtra("filename");
                imageToImageView(filename, pos);
                updateProgressBar(pos, max_pics);
                filenames.add(filename);
                if (pos == max_pics-1){
                    stopService(new Intent(MainActivity.this, DownloadService.class));
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    findViewById(R.id.barText).setVisibility(View.GONE);
                    findViewById(R.id.instruction).setVisibility(View.VISIBLE);
                    running = false;    // computation done
                }
            }
            if (action.equals("pic_selected")) {
                if(pos == max_pics -1) {
                    int index = Integer.parseInt(intent.getStringExtra("index"));
                    int id = getResources().getIdentifier("imageView" + index,"id", getPackageName());
                    ImageView imgView = findViewById(id);
                    if (!sel_pics.contains(filenames.get(index))){
                        sel_pics.add(filenames.get(index));
                        nsel++;
                        imgView.setImageAlpha(150);
                        if (nsel == max_sel) {
                            stopService(new Intent(MainActivity.this, DownloadService.class));
                            play_game(sel_pics);
                        }
                    }else{
                        sel_pics.remove(filenames.get(index));
                        nsel--;
                        //change back to original ImageAlpha
                        imgView.setImageAlpha(255);
                    }
                }
            }
        }
    };

    protected void play_game(ArrayList<String> sel_pics) {
        Intent play_game = new Intent(this, MainActivity2.class); // go to part 2
        play_game.putStringArrayListExtra("sel_pics", sel_pics);
        finish();
        startActivity(play_game);
    }

    @Override
    public void onClick(View v) {
        EditText URLInput = findViewById(R.id.webpage_url);
        String webpage_url = URLInput.getText().toString();
        if (!webpage_url.equals("")){   //prevent empty strings
            running = !running;     // change from false to true
            findViewById(R.id.instruction).setVisibility(View.GONE);
            setInitialValue();
            if (! running) {    // change from true to false (stop running)
                bkgdThread.interrupt();
                stopService(new Intent(MainActivity.this, DownloadService.class));
                setInitialValue();
                new CountDownTimer(300, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        for (int i = 0; i < 20; i++){
                            ImageView img = findViewById(getResources().getIdentifier("imageView" + i,"id", getPackageName()));
                            img.setVisibility(View.GONE);
                        }
                        startDownloading(webpage_url);
                        running = true;
                    }
                }.start();
            }else{
                for (int i = 0; i < 20; i++){
                    ImageView img = findViewById(getResources().getIdentifier("imageView" + i,"id", getPackageName()));
                    img.setVisibility(View.GONE);
                }
                startDownloading(webpage_url);
            }
        }
    }

    public void startDownloading(String webpage_url){
        bkgdThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] urls = getUrls(webpage_url, max_pics);
                if (urls == null || urls[0].equals("")){
                    return;//invalid website
                }
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                String[] filenames = makeFileNames(dir, urls);
                if (Thread.interrupted()){
                    return;// downloading aborted
                }
                startDownloadService(urls, filenames);
            }
        });
        bkgdThread.start();
    }

    protected void startDownloadService(String[] urls, String[] filenames) {
//        for (int i = 0; i < filenames.length; i++) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction("download");

        intent.putStringArrayListExtra("filenames", new ArrayList(Arrays.asList(filenames)));
        intent.putStringArrayListExtra("where", new ArrayList(Arrays.asList(urls)));
//        intent.putExtra("where", urls[i]);
        startService(intent);
    }
//    }

    protected void imageToImageView(String filename, int pos) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);

        if (bitmap != null) {
            int id = getResources().getIdentifier("imageView" + pos,"id", getPackageName());
            ImageView imgView = findViewById(id);
            imgView.setVisibility(View.VISIBLE);
            int[] arr = {bitmap.getWidth(), bitmap.getHeight()};
            int dim = Arrays.stream(arr).filter((int x)->x != 0).min().getAsInt();
            Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, dim, dim);
            imgView.setImageBitmap(resized);

            imgView.setOnClickListener(view -> {
                long min_ImageView_id = findViewById(R.id.imageView0).getUniqueDrawingId();
                long index = view.getUniqueDrawingId() - min_ImageView_id;


                Intent intent = new Intent();
                intent.setAction("pic_selected");
                intent.putExtra("index", String.valueOf(index));
                sendBroadcast(intent);
            });
        }
    }

    protected void updateProgressBar(int pos, int max_pics) {
        ProgressBar bar = findViewById(R.id.progressBar);
        TextView progressBarStatus = findViewById(R.id.barText);
        bar.setVisibility(View.VISIBLE);
        bar.setProgress(0);
        bar.setMax(max_pics);
        bar.setProgress(pos + 1);
        progressBarStatus.setText("Downloading "+ (pos + 1 ) + " of " + bar.getMax()+ " images");
        progressBarStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setInitialValue();
        unregisterReceiver(receiver);
    }

    public void setInitialValue(){
        pos = -1;
        nsel = 0;
        sel_pics = new ArrayList<>();
        filenames = new ArrayList<>();
    }
}