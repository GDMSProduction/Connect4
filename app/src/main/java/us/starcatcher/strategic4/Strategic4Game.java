package us.starcatcher.strategic4;

import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Strategic4Game extends AppCompatActivity {
    private Strategic4View strat4View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        strat4View = new Strategic4View(this);

        setContentView(strat4View);
    }
    //pausing the game when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        strat4View.pause();
    }

    //running the game when activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        strat4View.resume();
    }
}
