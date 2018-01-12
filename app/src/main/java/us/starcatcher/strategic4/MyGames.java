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

    public void pressButton(int num, boolean useB, int bombCD, boolean useW){
        //Networking.JoinGame(num,response -> {
            MainMenu.waitingForGame = true;
            MainMenu.netGameID = num;
            Networking.slowGame = true;

            //Set rules from that game too
            MainMenu.useBombs = useB;
            MainMenu.bombCD = bombCD;
            MainMenu.useWood = useW;


            finish();
        //});
    }
    private LinearLayout scrolly;
    LayoutInflater inflater;
    public void Refresh(){
        doRefresh = false;
        //Ask the server for our games.
        Networking.GetMyGames(response -> {
            JSONArray games = null;
            scrolly.removeViews(0,scrolly.getChildCount()-1);
            try {
                games = response.getJSONArray("games");
                for (int i = 0; i < games.length(); i++) {
                    //Get data from the game and make a button layout

                    JSONObject game = games.getJSONObject(i);
                    ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.mygame_list_item, scrolly, false);
                    //Text object, username
                    ((TextView)layout.getChildAt(0)).setText(game.getString("OtherName") );
                    //The text object
                    boolean turn = game.getBoolean("isRedTurn") == game.getBoolean("isRed");
                    ((TextView)layout.getChildAt(1)).setText(turn ? "Your Turn" : "Waiting" );

                    final int id = game.getInt("ID");

                    JSONObject rules = game.getJSONObject("rules");
                    final int bombCD = rules.getInt("bombCD");
                    final boolean useBombs = rules.getBoolean("useBombs");
                    final boolean useWood = rules.getBoolean("useWood");

                    //The Button object
                    ((Button)layout.getChildAt(2)).setText("View #" + id);
                    layout.getChildAt(2).setOnClickListener(v -> {
                        pressButton(id, useBombs, bombCD, useWood);
                    });
                    scrolly.addView(layout, 0);
                }
            } catch (JSONException e) {
            }
        });
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
        scrolly = (LinearLayout)findViewById(R.id.scroll_Layout);
        inflater = LayoutInflater.from(this);

        Refresh();
    }

    public static boolean doRefresh = false;
    @Override
    protected void onResume() {
        super.onResume();
        if (doRefresh) {
            Refresh();
        }
    }
}
