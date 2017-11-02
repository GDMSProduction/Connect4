package project.connect4;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainMenu extends AppCompatActivity implements View.OnClickListener {

    private Button startCon4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main_menu);
        startCon4 = (Button)findViewById(R.id.btn_start_game);
        startCon4.setOnClickListener(this);

        Button btn_reset= (Button) findViewById(R.id.btn_reset_game);
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connect4View.newGame();
            }
        });
    }

    @Override
    public void onClick(View v)
    {
        startActivity(new Intent(this, Connect4Game.class));
    }
}
