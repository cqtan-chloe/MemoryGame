package com.example.MemoryGame;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlayGame extends AppCompatActivity {

    int N_pairs;
    int prev_i = -1;
    int pair_counter = 0;
    boolean evaluating = false;
    ArrayList<String> sel_pics;
    ArrayList<String> picseq = new ArrayList<>();
    ArrayList<Integer> matchedIndex = new ArrayList<>();
    CountDownTimer2 timer;

    MediaPlayer correct;
    MediaPlayer wrong;
    MediaPlayer victory;

    Drawable default_image;
    int min_ImageView_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        default_image = getDrawable(R.drawable.question);
        min_ImageView_id = (int) findViewById(R.id.imageView0).getUniqueDrawingId();
        correct = MediaPlayer.create(this,R.raw.correct);
        wrong = MediaPlayer.create(this,R.raw.wrong);
        victory = MediaPlayer.create(this,R.raw.victory);


        sel_pics = getIntent().getStringArrayListExtra("sel_pics");
        N_pairs = sel_pics.size();

        playMemoryGame(sel_pics);
    }

    protected void playMemoryGame(ArrayList<String> sel_pics) {
        for (int i = 0; i < N_pairs * 2; i++)
            picseq.add(sel_pics.get(i % N_pairs));

        Collections.shuffle(picseq);

        long totalSeconds = 359999;     // 99*60*60 + 59*60 + 59 = 359,999. max N seconds.
        long intervalSeconds = 1;
        TextView timer_box = findViewById(R.id.timer);
        timer = new CountDownTimer2(totalSeconds * 1000, intervalSeconds * 1000, timer_box);
        timer.start();

        updateMatchCounter(pair_counter, N_pairs);

        for (int i = 0; i < picseq.size(); i++)
            imageToImageView(picseq.get(i), i);
    }

    protected void imageToImageView(String filename, int pos) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);

        if (bitmap != null) {
            int id = getResources().getIdentifier("imageView" + pos, "id", getPackageName());
            ImageView imgView = findViewById(id);
            imgView.setImageBitmap(bitmap);
            imgView.setForeground(default_image);

            imgView.setOnClickListener(view -> {
                int curr_i = (int) (view.getUniqueDrawingId() - min_ImageView_id);

                if (evaluating == true || matchedIndex.contains(curr_i))
                    return;

                if (curr_i != prev_i)
                    revealImage(curr_i);
            });
        }
    }

    public void revealImage(int curr_i) {
        int id = getResources().getIdentifier("imageView" + curr_i, "id", getPackageName());
        ImageView curr_imgView = findViewById(id);
        curr_imgView.setForeground(null);

        if (prev_i == -1)   // no unmatched pictures revealed
            prev_i = curr_i;
        else
            new Thread(new Runnable() {
                @Override
                public void run() {
                    checkMatching(curr_i);
                }
            }).start();
    }

    public void checkMatching(int curr_i) {
        evaluating = true;

        if (picseq.get(curr_i) == picseq.get(prev_i)){
            correct.start();
            pair_counter+=1;
            updateMatchCounter(pair_counter, N_pairs);
            matchedIndex.add(curr_i);
            matchedIndex.add(prev_i);

            pause();

            if(pair_counter==sel_pics.size()) {
                victory.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        endGame();
                    }
                });
            }
        }else{
            wrong.start();
            ImageView prev_imgView = findViewById(getResources().getIdentifier("imageView" + prev_i, "id", getPackageName()));
            ImageView curr_imgView = findViewById(getResources().getIdentifier("imageView" + curr_i, "id", getPackageName()));

            pause();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    curr_imgView.setForeground(default_image);
                    prev_imgView.setForeground(default_image);
                }
            });
        }

        prev_i = -1;    // reset
        evaluating = false;
    }

    protected void pause() {
        try {
            TimeUnit.MILLISECONDS.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void endGame() {
        timer.stop();
        Intent result = new Intent(PlayGame.this, SelectPictures.class);
        result.putExtra("time", timer.getCountUpTime());
        finish();
        startActivity(result);
    }

    protected void updateMatchCounter(int pair_counter, int N_pairs) {
        TextView matchCounter = findViewById(R.id.matchCounter);

        matchCounter.setText(pair_counter + " / " + N_pairs + " matches");
    }
}