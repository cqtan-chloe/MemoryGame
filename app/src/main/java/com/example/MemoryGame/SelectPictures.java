package com.example.MemoryGame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SelectPictures extends AppCompatActivity implements View.OnClickListener {

    int max_pics = 20;
    int max_sel = 6;
    Thread downloadThread;
    private boolean running = false;
    int pos, nsel;
    ArrayList<String> sel_pics, filenames;

    HandlerThread MainUI_ht;
    Handler sel_picsHandler;
    protected int PIC_SELECTED = 1;

    private long downloadThread_id;
    private String downloadThread_name;

    public void setInitialValue(){
        pos = -1;
        nsel = 0;
        sel_pics = new ArrayList<>();
        filenames = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setInitialValue();
        findViewById(R.id.fetch).setOnClickListener(this);

        findViewById(R.id.instruction).setVisibility(View.VISIBLE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("download_ok");
        filter.addAction("pic_selected");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onClick(View v) {
        if (running) {    // if the download process is already running
            stopService(new Intent(SelectPictures.this, DownloadService.class));
            downloadThread.interrupt();
            running = false;
        }

        EditText URLInput = findViewById(R.id.webpage_url);
        String webpage_url = URLInput.getText().toString();

        if (!webpage_url.equals("")) {    //prevent empty strings
            running = true;
            setInitialValue();
            startDownloading(webpage_url);
        }
    }

    public void startDownloading(String webpage_url){
        downloadThread = new Thread(new Runnable() {

            long downloadThread_id = Thread.currentThread().getId(); // internal copy of thread id

            @Override
            public void run() {
                ArrayList<String> urls = getUrls(webpage_url, max_pics);
                if (urls != null & urls.size() > 0) {   //invalid website

                    File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    ArrayList<String> filenames = makeFileNames(dir, urls);

                    for (int i = 0; i < filenames.size(); i++) {
                        Intent intent = new Intent(SelectPictures.this, DownloadService.class);
                        intent.setAction("download");
                        intent.putExtra("filename", filenames.get(i));
                        intent.putExtra("where", urls.get(i));
                        //intent.putExtra("downloadThread_id", Long.toString(downloadThread_id));
                        intent.putExtra("downloadThread_name", downloadThread_name);
                        startService(intent);

                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        downloadThread.start();

        downloadThread_name = downloadThread.getName(); // get another copy of thread id
    }

    protected ArrayList<String> getUrls(String webpage_url, int max_pics) {
        Document document = null;

        try {
            document = Jsoup.connect(webpage_url).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Elements es = document.select("img[src~=https.*jpg]");

        List<String> urls = new ArrayList<>();

        for (Element e : es)
            urls.add(e.attr("src"));

        if (urls.size() > max_pics)
            urls = urls.subList(0, max_pics);

        ArrayList<String> out = new ArrayList<String>(urls);

        return out;
    }

    protected ArrayList<String> makeFileNames(File dir, List<String> urls) {
        ArrayList<String> out = new ArrayList<>();

        for (int i = 0; i < urls.size(); i++) {
            out.add(new File(dir + "/" + new File(urls.get(i)).getName()).toString());
            System.out.println(out.get(i));
        }


        return out;
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            
            String action = intent.getAction();
            //long downloadThread_id_return = Long.valueOf(intent.getStringExtra("downloadThread_id_return"));
            String downloadThread_name_return = intent.getStringExtra("downloadThread_name_return");
            //System.out.println(downloadThread_id_return + ", " + downloadThread_id);
            //System.out.println(downloadThread_id_return == downloadThread_id);
            System.out.println(downloadThread_name_return + ", " + downloadThread_name);
            System.out.println(downloadThread_name_return.equals(downloadThread_name));
            if (action.equals("download_ok") & downloadThread_name_return.equals(downloadThread_name)) {
                pos++;
                String filename = intent.getStringExtra("filename");
                System.out.println(filename);
                imageToImageView(filename, pos);
                updateProgressBar(pos, max_pics);
                filenames.add(filename);
                if (pos == max_pics-1){
                    stopService(new Intent(SelectPictures.this, DownloadService.class));
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    findViewById(R.id.barText).setVisibility(View.GONE);
                    findViewById(R.id.instruction).setVisibility(View.VISIBLE);
                    running = false;    // computation done

                    waitForSelectedPics();
                }
            }
        }
    };


    protected void imageToImageView(String filename, int pos) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);

        if (bitmap != null) {
            int id = getResources().getIdentifier("imageView" + pos, "id", getPackageName());
            ImageView imgView = findViewById(id);
            imgView.setVisibility(View.VISIBLE);

            int[] arr = {bitmap.getWidth(), bitmap.getHeight(), imgView.getWidth(), imgView.getHeight()};
            int dim = Arrays.stream(arr).filter(x -> x > 0).min().getAsInt();
            Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, dim, dim);
            imgView.setImageBitmap(resized);

            imgView.setOnClickListener(view -> {
                long min_ImageView_id = findViewById(R.id.imageView0).getUniqueDrawingId();
                int index = (int) (view.getUniqueDrawingId() - min_ImageView_id);

                Message clicked = new Message();
                clicked.what = PIC_SELECTED;
                clicked.obj = index;

                sel_picsHandler.sendMessage(clicked);
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

    protected void waitForSelectedPics() {
        MainUI_ht = new HandlerThread("MainUI_ht");
        MainUI_ht.start();

        sel_picsHandler = new Handler(MainUI_ht.getLooper()) {
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == PIC_SELECTED & pos == max_pics - 1) { // selection of pictures start only after all pics are donwloaded
                    int index = (int) msg.obj;
                    int id = getResources().getIdentifier("imageView" + index, "id", getPackageName());
                    ImageView imgView = findViewById(id);

                    if (!sel_pics.contains(filenames.get(index))) {
                        sel_pics.add(filenames.get(index));
                        nsel++;
                        imgView.setImageAlpha(150);

                    } else {
                        sel_pics.remove(filenames.get(index));
                        nsel--;
                        imgView.setImageAlpha(255);     //change back to original ImageAlpha
                    }

                    if (nsel == max_sel)
                        play_game(sel_pics);
                }
            }
        };
    }

    protected void play_game(ArrayList<String> sel_pics) {
        Intent play_game = new Intent(this, PlayGame.class);
        play_game.putStringArrayListExtra("sel_pics", sel_pics);
        finish();
        startActivity(play_game);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setInitialValue();
        unregisterReceiver(receiver);
    }

}