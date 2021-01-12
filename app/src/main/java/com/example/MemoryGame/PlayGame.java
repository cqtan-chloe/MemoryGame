package com.example.MemoryGame;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class PlayGame extends AppCompatActivity implements View.OnClickListener {

    int N_sets;
    int ncells;
    int ncopies;
    ArrayList<Integer> opened_indexes;
    int set_counter = 0;
    boolean evaluating = false;
    ArrayList<String> sel_pics;
    ArrayList<String> picseq = new ArrayList<>();
    ArrayList<Integer> matchedIndex = new ArrayList<>();
    CountDownTimer2 timer;

    Drawable default_image;
    int min_ImageView_id;
    TextView matchCounter;
    TextView timer_box;
    Button quit;

    protected void getUIInfo() {
        default_image = getDrawable(R.drawable.question2);
        min_ImageView_id = (int) findViewById(R.id.imageView0).getUniqueDrawingId();
        matchCounter = findViewById(R.id.matchCounter);
        timer_box = findViewById(R.id.timer);
        quit = findViewById(R.id.quit);
    }

    MediaPlayer correct;
    MediaPlayer victory;

    protected void setUIInfo() {
        correct = MediaPlayer.create(this,R.raw.correct);
        victory = MediaPlayer.create(this,R.raw.victory);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, SelectPictures.class);
        finish();
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sel_pics = getIntent().getStringArrayListExtra("sel_pics");
        N_sets = sel_pics.size();
        ncopies = getIntent().getIntExtra("ncopies", -1);
        ncells = N_sets * ncopies;

        int id = getResources().getIdentifier("activity_playgame" + ncells, "layout", getPackageName());
        setContentView(id);

        getUIInfo();
        setUIInfo();
        quit.setOnClickListener(this);
        opened_indexes = new ArrayList<>();

        playMemoryGame(sel_pics);
    }

    protected void playMemoryGame(ArrayList<String> sel_pics) {
        for (int i = 0; i < N_sets * ncopies; i++)
            picseq.add(sel_pics.get(i % N_sets));

        Collections.shuffle(picseq);

        timer = makeCountUpTimer(timer_box);
        timer.start();

        updateMatchCounter(set_counter, N_sets);

        for (int i = 0; i < picseq.size(); i++)
            imageToImageView(picseq.get(i), i);
    }

    protected void imageToImageView(String filename, int pos) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);

        if (bitmap != null) {
            ImageView imgView = findImageViewByName("imageView" + pos);
            imgView.setImageBitmap(bitmap);
            imgView.setForeground(default_image);

            imgView.setOnClickListener(view -> {
                int curr_i = (int) (view.getUniqueDrawingId() - min_ImageView_id);

                if (evaluating || matchedIndex.contains(curr_i))
                    return;

                if (!opened_indexes.contains(curr_i))
                    revealImage(curr_i);
            });
        }
    }

    public void revealImage(int curr_i) {
        ImageView curr_imgView = findImageViewByName("imageView" + curr_i);
        curr_imgView.setForeground(null);

        opened_indexes.add(curr_i);

        if (opened_indexes.size() == ncopies)   // has 2 or more unmatched revealed pictures
            new Thread(() -> checkMatching(opened_indexes)).start();
    }

    public void checkMatching(ArrayList<Integer> opened_indexes) {
        evaluating = true;

        ArrayList<String> tmp = new ArrayList<>();
        for (Integer index : opened_indexes)
            tmp.add(picseq.get(index));

        boolean allEqual = new HashSet<String>(tmp).size() == 1;

        if (allEqual){
            correct.start();
            set_counter+=1;
            updateMatchCounter(set_counter, N_sets);
            
            for (Integer index : opened_indexes)
                matchedIndex.add(index);

            pause(1500);

            if(set_counter==sel_pics.size()) {
                victory.start();
                runOnUiThread(this::endGame);
            }
        }else{
            pause(1500);

            for (Integer index : opened_indexes)
                runOnUiThread(() -> findImageViewByName("imageView" + index).setForeground(default_image));
        }

        opened_indexes.clear();    // reset
        evaluating = false;
    }

    protected void endGame() {
        timer.stop();
        Intent result = new Intent(this, ResultActivity.class);
        result.putExtra("time", timer.getCountUpTime());
        finish();
        startActivity(result);
    }

    protected void updateMatchCounter(int set_counter, int n_sets) {
        matchCounter.setText(set_counter + " / " + n_sets + " matches");
    }


    /* library of custom methods */
    protected ImageView findImageViewByName(String name) {
        int id = getResources().getIdentifier(name, "id", getPackageName());
        ImageView imgView = findViewById(id);

        return imgView;
    }

    protected CountDownTimer2 makeCountUpTimer(TextView timer_box) {
        long totalSeconds = 359999;     // 99*60*60 + 59*60 + 59 = 359,999. max N seconds.
        long intervalSeconds = 1;
        CountDownTimer2 out = new CountDownTimer2(totalSeconds * 1000, intervalSeconds * 1000, timer_box);

        return out;
    }

    protected void pause(int n_milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(n_milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}