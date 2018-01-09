package us.starcatcher.strategic4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class MatchSetup extends AppCompatActivity {

    protected static boolean useOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_setup);

        final TextView bombCDText = findViewById(R.id.text_BombCD);
        final SeekBar sk = findViewById(R.id.bar_BombCD);

        findViewById(R.id.btn_FinishMatchSetup).setOnClickListener(v -> {
            //Start a new game here, return to the main menu, which can load the waiting game.
            MainMenu.waitingForGame = true;
            //Set rules for the starting game
            MainMenu.bombCD = sk.getProgress() + 1;
            MainMenu.useBombs = ((CheckBox)findViewById(R.id.check_Bombs)).isChecked();
            MainMenu.useWood = ((CheckBox)findViewById(R.id.check_Wood)).isChecked();

            //TODO We need to tell the server that we created a new game with these rules.
            if (useOnline)
            {

            }

            startActivity(new Intent(this, MainMenu.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });



        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                bombCDText.setText("Bomb Cooldown: " + progress);
            }
        });
    }
}
