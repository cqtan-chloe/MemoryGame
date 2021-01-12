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
import java.util.concurrent.TimeUnit;

public class PlayGame extends AppCompatActivity implements View.OnClickListener {

    int N_pairs;
    int prev_i = -1;
    int pair_counter = 0;
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
        default_image = getDrawable(R.drawable.question);
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
        setContentView(R.layout.activity_playgame);

        getUIInfo();
        setUIInfo();
        quit.setOnClickListener(this);

        sel_pics = getIntent().getStringArrayListExtra("sel_pics");
        N_pairs = sel_pics.size();

        playMemoryGame(sel_pics);
    }

    protected void playMemoryGame(ArrayList<String> sel_pics) {
        for (int i = 0; i < N_pairs * 2; i++)
            picseq.add(sel_pics.get(i % N_pairs));

        Collections.shuffle(picseq);

        timer = makeCountUpTimer(timer_box);
        timer.start();

        updateMatchCounter(pair_counter, N_pairs);

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

                if (curr_i != prev_i)
                    revealImage(curr_i);
            });
        }
    }

    public void revealImage(int curr_i) {
        ImageView curr_imgView = findImageViewByName("imageView" + curr_i);
        curr_imgView.setForeground(null);

        if (prev_i == -1)   // no unmatched pictures revealed
            prev_i = curr_i;
        else
            new Thread(() -> checkMatching(curr_i)).start();
    }

    public void checkMatching(int curr_i) {
        evaluating = true;

        if (picseq.get(curr_i).equals(picseq.get(prev_i))){
            correct.start();
            pair_counter+=1;
            updateMatchCounter(pair_counter, N_pairs);
            matchedIndex.add(curr_i);
            matchedIndex.add(prev_i);

            pause(1500);

            if(pair_counter==sel_pics.size()) {
                victory.start();
                runOnUiThread(this::endGame);
            }
        }else{
            ImageView prev_imgView = findImageViewByName("imageView" + prev_i);
            ImageView curr_imgView = findImageViewByName("imageView" + curr_i);

            pause(1500);

            runOnUiThread(() -> {
                curr_imgView.setForeground(default_image);
                prev_imgView.setForeground(default_image);
            });
        }

        prev_i = -1;    // reset
        evaluating = false;
    }

    protected void endGame() {
        timer.stop();
        Intent result = new Intent(this, ResultActivity.class);
        result.putExtra("time", timer.getCountUpTime());
        finish();
        startActivity(result);
    }

    protected void updateMatchCounter(int pair_counter, int n_pairs) {
        matchCounter.setText(pair_counter + " / " + n_pairs + " matches");
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