package com.example.memory_game;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class MainActivity2 extends AppCompatActivity {

    int N_pairs;
    int prev_i = -1;
    int pair_counter = 0;
    int numberSelected = 0;
    boolean evaluating = false;
    ArrayList<String> sel_pics, picseq;
    ArrayList<Integer> matchedIndex;
    CountDownTimer2 timer;
    private MediaPlayer correct;
    private MediaPlayer wrong;
    private MediaPlayer victory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        matchedIndex = new ArrayList<>();
        Intent intent = getIntent();
        sel_pics = intent.getStringArrayListExtra("sel_pics");
        N_pairs = sel_pics.size();
        playMemoryGame(sel_pics);
        correct = MediaPlayer.create(this,R.raw.correct);
        wrong = MediaPlayer.create(this,R.raw.wrong);
        victory = MediaPlayer.create(this,R.raw.victory);
    }

    private void correct(){
        correct.reset();
        correct=MediaPlayer.create(MainActivity2.this, R.raw.correct);
        correct.start();
    }

    private void wrong(){
        wrong.reset();
        wrong=MediaPlayer.create(MainActivity2.this, R.raw.wrong);
        wrong.start();
    }
    private void victory(){
        victory.reset();
        victory=MediaPlayer.create(MainActivity2.this, R.raw.victory);
        victory.start();
    }
    protected void playMemoryGame(ArrayList<String> sel_pics) {
        picseq = new ArrayList<>();

        for (int i = 0; i < N_pairs*2; i++)
            picseq.add(sel_pics.get(i % N_pairs));

        Collections.shuffle(picseq);

        long totalSeconds = 359999;     // 99*60*60 + 59*60 + 59 = 359,999. max N seconds.
        long intervalSeconds = 1;
        TextView timer_box = findViewById(R.id.timer);
        timer = new CountDownTimer2(totalSeconds * 1000, intervalSeconds * 1000, timer_box);
        timer.start();

        updateMatchCounter(pair_counter, N_pairs);

        for (int i = 0; i < picseq.size(); i++) {
            ImageView imgView = findViewById( getResources().getIdentifier("imageView" + i,"id", getPackageName()));
            imgView.setOnClickListener(view -> {
                int min_ImageView_id = (int) findViewById(R.id.imageView0).getUniqueDrawingId();
                int curr_i = (int) view.getUniqueDrawingId() - min_ImageView_id;
                if (evaluating == true || matchedIndex.contains(curr_i)){
                    return;
                }
                if (curr_i != prev_i){
                    reviewImage(curr_i);
                }
            });
        };
    }

    public void reviewImage(int curr_i){
        Bitmap bitmap = BitmapFactory.decodeFile(picseq.get(curr_i));
        if (bitmap != null) {
            int id = getResources().getIdentifier("imageView" + curr_i,
                    "id", getPackageName());
            ImageView curr_imgView = findViewById(id);
            int[] arr = {bitmap.getWidth(), bitmap.getHeight()};
            int dim = Arrays.stream(arr).filter((int x)->x != 0).min().getAsInt();
            Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, dim, dim);
            curr_imgView.setImageBitmap(resized);
            numberSelected += 1;
            if (numberSelected % 2 == 0){
                checkMatching(curr_i, curr_imgView);
            }else{
                prev_i = curr_i;
            }
        }
    }

    public void checkMatching(int curr_i, ImageView curr_imgView){
        new Thread(new Runnable(){
            @Override
            public void run() {
                evaluating = true;
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (picseq.get(curr_i) == picseq.get(prev_i)){
                    pair_counter+=1;
                    updateMatchCounter(pair_counter, N_pairs);
                    matchedIndex.add(curr_i);
                    matchedIndex.add(prev_i);
                    if(pair_counter==sel_pics.size()){

                        victory();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timer.stop();
                                Intent result = new Intent(MainActivity2.this, ResultActivity.class);
                                result.putExtra("time", timer.getCountUpTime());
                                finish();
                                startActivity(result);
                            }
                        });
                    }else{

                        correct();
                    }
                }else{
                    ImageView prev_imgView = findViewById(getResources().getIdentifier("imageView" + prev_i,
                            "id", getPackageName()));
                    wrong();
                    numberSelected -= 2;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            curr_imgView.setImageResource(MainActivity2.this.getResources().getIdentifier("question","drawable",MainActivity2.this.getPackageName()));
                            prev_imgView.setImageResource(MainActivity2.this.getResources().getIdentifier("question","drawable",MainActivity2.this.getPackageName()));
                        }
                    });
                }
                prev_i = curr_i;
                evaluating = false;
            }
        }).start();
    }

    protected void updateMatchCounter(int pair_counter, int N_pairs) {
        TextView matchCounter = findViewById(R.id.matchCounter);

        matchCounter.setText(pair_counter + " / " + N_pairs + " matches");
    }
}
