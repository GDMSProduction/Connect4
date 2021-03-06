package us.starcatcher.strategic4;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainMenu extends AppCompatActivity {

    private Button btn_Resume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main_menu);

        //The local play setup
        findViewById(R.id.btn_Setup_Local).setOnClickListener(v -> {
            MatchSetup.useOnline = false;
            Connect4View.useOnline = false;
            startActivity(new Intent(this, MatchSetup.class));
        });
        //The resume local game button
        btn_Resume = findViewById(R.id.btn_Resume);
        btn_Resume.setOnClickListener(v -> {
            startStrat4();
        });

        //The online play buttons
        findViewById(R.id.btn_start_con4_online).setOnClickListener(v -> {
            if (!Connect4View.useOnline || Networking.slowGame) {
                Connect4View.setup = false;
                Connect4View.netID = 0;
                Connect4View.useOnline = true;
            }
            Networking.slowGame = false;
            startConn4(v);
        });
        findViewById(R.id.btn_start_strat4_online).setOnClickListener(v -> {
            if (!Connect4View.useOnline || Networking.slowGame) {
                Connect4View.setup = false;
                Connect4View.netID = 0;
                Connect4View.useOnline = true;
            }
            Networking.slowGame = false;
            startStrat4();
        });

        //Help button
        findViewById(R.id.btn_Help).setOnClickListener(v -> {
            startActivity(new Intent(this, HelpMenu.class));
        });

        //MyGames button
        findViewById(R.id.btn_MyGames).setOnClickListener(v -> {
            startActivity(new Intent(this, MyGames.class));
        });

        //Start networking
        Networking.init(this);
    }

    protected static boolean waitingForGame = false;
    protected static boolean useBombs = true;
    protected static boolean useWood = true;
    protected static int bombCD = 2;
    protected static int netGameID = -1;

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForGame)
        {
            waitingForGame = false;
            strat4_running = false;
            Connect4View.setup = false;
            Connect4View.netID = netGameID;
            if (netGameID > 0)
                Connect4View.useOnline = true;
            else
                Connect4View.useOnline = false;

            //Set rules for the waiting game
            Strategic4View.bomb_Cooldown = bombCD;
            Strategic4View.useBombs = useBombs;
            Strategic4View.useWood = useWood;
            startStrat4();
        }

        //Enable the Resume button
        if (strat4_running && !Strategic4View.useOnline && !Connect4View.gameOver)
            btn_Resume.setEnabled(true);
        else
            btn_Resume.setEnabled(false);
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
    public void startStrat4()
    {
        if (con4_running) {
            con4_running = false;
            Connect4View.setup = false;
        }
        strat4_running = true;
        startActivity(new Intent(this, Strategic4Game.class));
    }
}
