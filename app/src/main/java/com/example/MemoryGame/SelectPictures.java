package com.example.MemoryGame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SelectPictures extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    int max_pics = 20;
    private int search_session_id = 0;
    String webpage_url;
    Thread downloadThread;
    private boolean running = false;
    int pos;
    ArrayList<String> filenames;

    HandlerThread MainUI_ht;
    Handler sel_picsHandler;
    protected int PIC_SELECTED = 1;

    int nsel;
    ArrayList<Integer> max_sel;    // number of pictures to select
    int ncopies = 2;    // number of copies per picture (default)
    ArrayList<String> sel_pics;

    Button fetch;
    long min_ImageView_id;
    ProgressBar bar;
    TextView progressBarStatus;
    EditText URLInput;
    TextView instruction;
    Button go;

    protected void setInitialValue() {
        pos = -1;
        nsel = 0;
        filenames = new ArrayList<>();
        sel_pics = new ArrayList<>();
    }

    protected void getUIInfo() {
        fetch = findViewById(R.id.fetch);
        min_ImageView_id = findViewById(R.id.imageView0).getUniqueDrawingId();
        bar = findViewById(R.id.progressBar);
        progressBarStatus = findViewById(R.id.barText);
        URLInput = findViewById(R.id.webpage_url);
        instruction = findViewById(R.id.instruction);
        go = findViewById(R.id.go);
    }

    Integer[] NCOPIES  = {2, 3, 4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectpictures);

        setInitialValue();
        getUIInfo();
        fetch.setOnClickListener(this);
        go.setOnClickListener(this);

        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, NCOPIES);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("download_ok");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        ncopies = NCOPIES[position];

        int string_id = getResources().getIdentifier("instruction" + ncopies, "string", getPackageName());
        instruction.setText(string_id);

        max_sel = new ArrayList<>();    // reset or initialize

        switch (ncopies) {
            case 2:
                max_sel.add(6); max_sel.add(8); max_sel.add(10); max_sel.add(12);
                break;
            case 3:
                max_sel.add(4); max_sel.add(8);
                break;
            case 4:
                max_sel.add(3); max_sel.add(4); max_sel.add(5);
                break;
            default:
                max_sel.add(6);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.go)
            if (max_sel.contains(nsel))
                play_game(sel_pics);

        if (v.getId() == R.id.fetch) {
            if (running) {    // if the download process is already running
                stopService(new Intent(this, DownloadService.class));
                running = false;
            }

            webpage_url = URLInput.getText().toString();

            if (!webpage_url.equals("")) {    //prevent empty strings
                running = true;
                resetUI();
                setInitialValue();

                instruction.setVisibility(View.GONE);
                bar.setVisibility(View.VISIBLE);
                progressBarStatus.setVisibility(View.VISIBLE);

                search_session_id++;
                downloadThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StartDownloading(webpage_url, search_session_id);
                    }
                });

                downloadThread.start();
                waitForSelectedPics();
            }
        }
    }

    protected void resetUI() {
        for (int i = 0; i < max_pics; i++){
            ImageView imgView = findImageViewByName("imageView" + i);
            imgView.setVisibility(View.GONE);
        }
    }

    protected void StartDownloading(String webpage_url, int search_session_id){
        ArrayList<String> urls = getUrls(webpage_url, max_pics);
        if (urls != null & urls.size() > 0) {   //invalid website

            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            ArrayList<String> filenames = makeFileNames(dir, urls);

            for (int i = 0; i < filenames.size(); i++) {

                if (pos == max_pics - 1)
                    return;

                Intent intent = new Intent(this, DownloadService.class);
                intent.setAction("download");
                intent.putExtra("filename", filenames.get(i));
                intent.putExtra("where", urls.get(i));
                intent.putExtra("search_session_id", Integer.toString(search_session_id));
                startService(intent);

                pause(300);
            }
        }
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

        ArrayList<String> out = new ArrayList<>(urls);

        return out;
    }

    protected ArrayList<String> makeFileNames(File dir, List<String> urls) {
        ArrayList<String> out = new ArrayList<>();

        for (int i = 0; i < urls.size(); i++)
            out.add(new File(dir + "/" + new File(urls.get(i)).getName()).toString());

        return out;
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // seems like received intents are not evaluated in order.
            // immediately usher evaluation to be done on indiv bkgd threads
            new Thread(() -> EvaluateIntent(intent)).start();
        }
    };

    protected void EvaluateIntent(Intent intent) {
        String search_session_id_return = intent.getStringExtra("search_session_id_return");
        String filename = intent.getStringExtra("filename");

        boolean cond1 = Integer.parseInt(search_session_id_return) == search_session_id;
        boolean cond2 = pos < max_pics;
        boolean cond3 = !filenames.contains(filename);

        if (cond1 & cond2 & cond3)
            runOnUiThread(() -> UpdateUI(filename));
    }

    protected void UpdateUI(String filename) {
        pos++;
        imageToImageView(filename, pos);
        updateProgressBar(pos, max_pics);
        filenames.add(filename);

        if (pos == max_pics-1) {
            stopService(new Intent(this, DownloadService.class));
            bar.setVisibility(View.GONE);
            progressBarStatus.setVisibility(View.GONE);
            instruction.setVisibility(View.VISIBLE);
            running = false;    // computation done
        }
    }


    protected void imageToImageView(String filename, int pos) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);

        if (bitmap != null) {
            ImageView imgView = findImageViewByName("imageView" + pos);
            imgView.setVisibility(View.VISIBLE);
            imgView.setImageBitmap(bitmap);

            imgView.setOnClickListener(view -> {
                int index = (int) (view.getUniqueDrawingId() - min_ImageView_id);

                Message clicked = new Message();
                clicked.what = PIC_SELECTED;
                clicked.obj = index;

                sel_picsHandler.sendMessage(clicked);
            });
        }
    }

    protected void updateProgressBar(int pos, int max_pics) {
        bar.setProgress(0);
        bar.setMax(max_pics);
        bar.setProgress(pos + 1);
        progressBarStatus.setText("Downloading "+ (pos + 1 ) + " of " + max_pics + " images");
    }

    protected void waitForSelectedPics() {
        MainUI_ht = new HandlerThread("MainUI_ht");
        MainUI_ht.start();

        sel_picsHandler = new Handler(MainUI_ht.getLooper()) {
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == PIC_SELECTED & pos == max_pics - 1) { // selection of pictures start only after all pics are downloaded
                    int index = (int) msg.obj;
                    ImageView imgView = findImageViewByName("imageView" + index);

                    if (!sel_pics.contains(filenames.get(index))) {
                        sel_pics.add(filenames.get(index));
                        nsel++;
                        imgView.setImageAlpha(150);

                    } else {
                        sel_pics.remove(filenames.get(index));
                        nsel--;
                        imgView.setImageAlpha(255);     //change back to original ImageAlpha
                    }
                }
            }
        };
    }

    protected void play_game(ArrayList<String> sel_pics) {
        Intent play_game = new Intent(this, PlayGame.class);
        play_game.putStringArrayListExtra("sel_pics", sel_pics);
        play_game.putExtra("ncopies", ncopies);
        finish();

        startActivity(play_game);
    }

//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        super.onSaveInstanceState(savedInstanceState);
//        // Save UI state changes to the savedInstanceState.
//        // This bundle will be passed to onCreate if the process is
//        // killed and restarted.
//        savedInstanceState.putString("webpage_url", webpage_url);
//    }
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        // Restore UI state from the savedInstanceState.
//        // This bundle has also been passed to onCreate.
//        webpage_url = savedInstanceState.getString("webpage_url");
//        URLInput.setText(webpage_url);
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setInitialValue();
        unregisterReceiver(receiver);
    }

    /* library of custom methods */
    protected ImageView findImageViewByName(String name) {
        int id = getResources().getIdentifier(name, "id", getPackageName());
        ImageView imgView = findViewById(id);

        return imgView;
    }

    protected void pause(int n_milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(n_milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}