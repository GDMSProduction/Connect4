package project.connect4;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainMenu extends AppCompatActivity {

    private Button startCon4;
    private Button startStrat4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main_menu);
        startCon4 = (Button)findViewById(R.id.btn_start_con4);

        startCon4.setOnClickListener(this::onClickCon4);

        startStrat4 = (Button)findViewById(R.id.btn_start_strat4);
        startStrat4.setOnClickListener(this::onClickStrat4);

        Button btn_reset= (Button) findViewById(R.id.btn_reset_game);
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect4View.setup = false;
            }
        });
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
