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

public class MyGames extends AppCompatActivity {

    public static void pressButton(int num){
        int i = 9;
        num++;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_games);

        findViewById(R.id.btn_FindGame).setOnClickListener(v -> {
            //Find Game menu here
            startActivity(new Intent(this, FindGames.class));
        });

        //The layout which is scrolled
        LinearLayout scrolly = (LinearLayout)findViewById(R.id.scroll_Layout);

        LayoutInflater inflater = LayoutInflater.from(this);

        //Ask the server for our games.
        Networking.GetMyGames(response -> {
            JSONArray games = null;
            try {
                games = response.getJSONArray("games");
                for (int i = 0; i < games.length(); i++) {
                    //Get data from the game and make a button layout

                    JSONObject game = games.getJSONObject(i);
                    ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.mygame_list_item, scrolly, false);
                    //Text object, username
                    ((TextView)layout.getChildAt(0)).setText(game.getString("OtherName") );
                    //The text object
                    boolean turn = game.getBoolean("isRedTurn");
                    ((TextView)layout.getChildAt(1)).setText(turn ? "Your Turn" : "Waiting" );

                    final int id = game.getInt("ID");

                    //The Button object
                    ((Button)layout.getChildAt(2)).setText("View #" + id);
                    layout.getChildAt(2).setOnClickListener(v -> {
                        pressButton(id);
                    });
                    scrolly.addView(layout, 0);
                }
            } catch (JSONException e) {
            }
        });
    }
}
