package us.starcatcher.strategic4;

import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class FindGames extends AppCompatActivity {

    public void pressButton(int num){
        Networking.JoinGame(num,response -> {
            MyGames.doRefresh = true;
            finish();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_games);

        findViewById(R.id.btn_CreateGame).setOnClickListener(v -> {
            //Create Game menu here
            MatchSetup.useOnline = true;
            startActivity(new Intent(this, MatchSetup.class));
        });

        //The layout which is scrolled
        LinearLayout scrolly = (LinearLayout)findViewById(R.id.scroll_Layout_FGames);

        LayoutInflater inflater = LayoutInflater.from(this);

        //Ask the server for our games.
        Networking.FindGames(response -> {
            JSONArray games = null;
            try {
                games = response.getJSONArray("games");
                for (int i = 0; i < games.length(); i++) {
                    //Get data from the game and make a button layout

                    JSONObject game = games.getJSONObject(i);
                    ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.findgame_list_item, scrolly, false);
                    //Text object, username
                    ((TextView)layout.getChildAt(0)).setText(game.getString("OtherName") );

                    final int id = game.getInt("ID");
                    //The Button object
                    ((Button)layout.getChildAt(2)).setText("Join #" + id);
                    layout.getChildAt(2).setOnClickListener(v -> {
                        pressButton(id);
                    });
                    JSONObject rules = game.getJSONObject("rules");
                    ((TextView)layout.getChildAt(3)).setText(rules.getBoolean("useBombs") ? "Bombs, " + (rules.getInt("bombCD")-1) + " cd" : "No Bombs");
                    ((TextView)layout.getChildAt(4)).setText(rules.getBoolean("useWood") ? "Wood" : "No Wood");
                    scrolly.addView(layout, 0);
                }
            } catch (JSONException e) {
            }
        });

    }
}
