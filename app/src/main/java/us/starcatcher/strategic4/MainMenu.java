package us.starcatcher.strategic4;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;


public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main_menu);

        //The local play buttons
        findViewById(R.id.btn_start_conn4).setOnClickListener(v -> {
            if (Connect4View.useOnline) {
                Connect4View.useOnline = false;
                Connect4View.setup = false;
            }
            startConn4(v);
        });
        findViewById(R.id.btn_start_strat4).setOnClickListener(v -> {
            if (Connect4View.useOnline) {
                Connect4View.useOnline = false;
                Connect4View.setup = false;
            }
            startStrat4(v);
        });

        //The online play buttons
        findViewById(R.id.btn_start_con4_online).setOnClickListener(v -> {
            if (!Connect4View.useOnline) {
                Connect4View.useOnline = true;
                Connect4View.setup = false;
            }
            startConn4(v);
        });
        findViewById(R.id.btn_start_strat4_online).setOnClickListener(v -> {
            if (!Connect4View.useOnline) {
                Connect4View.useOnline = true;
                Connect4View.setup = false;
            }
            startStrat4(v);
        });

        //Help button
        findViewById(R.id.btn_Help).setOnClickListener(v -> {
            startActivity(new Intent(this, HelpMenu.class));
        });

        //Help button
        findViewById(R.id.btn_MyGames).setOnClickListener(v -> {
            startActivity(new Intent(this, MyGames.class));
        });

        //Start networking
        Networking.init(this);
    }

    @Override
    protected void onStop() {
        Networking.exit();
        super.onStop();
    }

    boolean con4_running = false;
    boolean strat4_running = false;
    public void startConn4(View v)
    {
        if (strat4_running) {
            strat4_running = false;
            Connect4View.setup = false;
        }
        con4_running = true;
        startActivity(new Intent(this, Connect4Game.class));
    }
    public void startStrat4(View v)
    {
        if (con4_running) {
            con4_running = false;
            Connect4View.setup = false;
        }
        strat4_running = true;
        startActivity(new Intent(this, Strategic4Game.class));
    }
}
