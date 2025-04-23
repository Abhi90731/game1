package com.example.mygame1;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // int level = getIntent().getIntExtra("level", 1); // default to level 1
        int level = 2; // testing level 2 change it after done
        gameView = new GameView(this, level);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }
}

