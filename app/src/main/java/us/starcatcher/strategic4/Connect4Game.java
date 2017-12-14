package us.starcatcher.strategic4;

import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Connect4Game extends AppCompatActivity {
    private Connect4View conn4View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        conn4View = new Connect4View(this, true);

        setContentView(conn4View);
    }

    //pausing the game when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        conn4View.pause();
    }

    //running the game when activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        conn4View.resume();
    }
}
