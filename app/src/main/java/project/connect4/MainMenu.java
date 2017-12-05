package project.connect4;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.android.volley.toolbox.Volley;


public class MainMenu extends AppCompatActivity {

    private Button startCon4;
    private Button startStrat4;
    private Switch tgle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main_menu);
        startCon4 = (Button)findViewById(R.id.btn_start_con4);

        startCon4.setOnClickListener(this::onClickCon4);

        startStrat4 = (Button)findViewById(R.id.btn_start_strat4);
        startStrat4.setOnClickListener(this::onClickStrat4);

        tgle = (Switch) findViewById(R.id.swch_Online);
        tgle.setOnClickListener(v -> {
            Connect4View.useOnline = tgle.isChecked();
            Connect4View.setup = false;
        });

        findViewById(R.id.btn_Help).setOnClickListener(v -> {
            startActivity(new Intent(this, HelpMenu.class));
        });

        Networking.init(this);
    }

    @Override
    protected void onStop() {
        Networking.exit();
        super.onStop();
    }

    boolean con4_running = false;
    boolean strat4_running = false;
    public void onClickCon4(View v)
    {
        if (strat4_running) {
            strat4_running = false;
            Connect4View.setup = false;
        }
        con4_running = true;
        startActivity(new Intent(this, Connect4Game.class));
    }

    public void onClickStrat4(View v)
    {
        if (con4_running) {
            con4_running = false;
            Connect4View.setup = false;
        }
        strat4_running = true;
        startActivity(new Intent(this, Strategic4Game.class));
    }
}
