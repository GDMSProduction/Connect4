package project.connect4;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;

import com.android.volley.toolbox.Volley;

/**
 * Created by User on 11/7/2017.
 */

public class Strategic4View extends Connect4View implements Runnable {
    private static String[] events = {"Chip_Placed", "Blue_Chip_Placed",
            "Red_Chip_Placed", "Blue_Wins", "Red_Wins", "Tie_Game", "Touch_Down",
            "Touch_Move", "Touch_Up", "Bomb_Placed", "Bomb_Explode", "Defuse_Placed","Wood_Placed"};

    private static Bitmap chipBomb;
    private static Bitmap chipWood;
    private static Bitmap chipDefuse;

    protected static MediaPlayer bomb_explode_sound;

    //First index is TEAM, second index is chip options
    protected static DragChip[][] team_Drags;

    private static void onChip_Placed()
    {
        switch (eventChipTarget.getType())
        {
            case 5:
            case 1://bomb
                EventSystem.triggerEvent("Bomb_Placed");
                break;
            case 2://defuse
            case 6:
                EventSystem.triggerEvent("Defuse_Placed");
                break;
            case 3://wood
            case 7:
                EventSystem.triggerEvent("Wood_Placed");
                break;
        }

        if (eventChipTarget.Red())
            EventSystem.triggerEvent("Red_Chip_Placed");
        else
            EventSystem.triggerEvent("Blue_Chip_Placed");

        //Play a sound effect
        mp.start();

        //Change turns after a chip was played
        if (placedFromInput) {
            int team = redsTurn?1:0;
            for (int i = 0; i < team_Drags[team].length; ++i) {
                team_Drags[team][i].tick();
            }
            redsTurn = !redsTurn;
            if (useOnline)
                netGameState = (netIsRed==redsTurn?1:2);
        }

        //Clear the temp drag chip
        dragged = null;
    }
    private static void onRed_Chip_Placed()
    {
        //dragged is the chip that was placed now
        //check type or something


        //Add some stats
        if (placedFromInput) {
            for (int j = 0; j < team_Drags[1].length; ++j) {
                team_Drags[1][j].setActive(true);
            }
            for (int j = 0; j < team_Drags[0].length; ++j) {
                team_Drags[0][j].setActive(false);
            }
        }
    }
    private static void onBlue_Chip_Placed()
    {
        //Add some stats
        if (placedFromInput) {
            for (int j = 0; j < team_Drags[0].length; ++j) {
                team_Drags[0][j].setActive(true);
            }
            for (int j = 0; j < team_Drags[1].length; ++j) {
                team_Drags[1][j].setActive(false);
            }
        }
    }

    //Touch events, use tmpx and tmpy for locations
    private static void onTouch_Down()
    {
        if (!useOnline || redsTurn == netIsRed) {
            for (int i = 0; i < team_Drags.length; ++i) {
                for (int j = 0; j < team_Drags[i].length; ++j) {
                    if (team_Drags[i][j].isInside((int) tmpX, (int) tmpY)) {
                        dragged = team_Drags[i][j];
                    }
                }
            }
        }
    }
    private static void onTouch_Move()
    {
        if (dragged != null && dragged.getActive())
        {
            dragged.movePosition((int)(tmpX-(chipSize/2)),(int)(tmpY-(chipSize/2)));
            MapGrid<Chip>.Coord tmp = mapGrid.getCoordOfTouch((int)tmpX,(int)tmpY);

            hoverChip.active = false;
            if (tmp.x >= 0 && tmp.x < 7) {
                //Show a temporary chip
                MapGrid<Chip>.Node target = mapGrid.getNodeCoord(tmp.x,0);
                while(target.down != null && target.down.data == null)
                {
                    target = target.down;
                    tmp.y++;
                }

                MapGrid<Chip>.Coord tmp2 = mapGrid.getCoordOfLoc(tmp.x,tmp.y);
                hoverChip.active = true;
                hoverChip.x = tmp2.x;
                hoverChip.y = tmp2.y;
                hoverChip.im = dragged.im;
            }
            doInvalidate = true;
        }
    }
    private static void onTouch_Up()
    {
        if (dragged != null && dragged.getActive())
        {
            MapGrid<Chip>.Coord tmp = mapGrid.getCoordOfTouch((int)tmpX,(int)tmpY);
            dragged.resetPosition();
            if (tmp.x >= 0 && tmp.x < 7)
            {

                if (addChip(dragged.Red(),tmp.x,dragged.im,dragged.getType())) {
                    placedFromInput = true;
                    dragged.setActive(false);
                    if (useOnline) {
                        online_KeepAlive();
                        online_SendEvent(1, tmp.x, dragged.getType());
                    }
                }
            }

            dragged = null;
            doInvalidate = true;
            hoverChip.active = false;
        }
    }
    private static void onBomb_Placed()
    {
        if (eventChipTarget.Red())
            team_Drags[0][1].coolDown = 2;
        else
            team_Drags[1][1].coolDown = 2;

        EventSystem.triggerEvent("Bomb_Explode");
    }
    private static void onBomb_Explode()
    {
        eventNodeTarget.data = null;
        if (eventNodeTarget.right != null) {
            explode_Wood(eventNodeTarget.right);
            eventNodeTarget.right.data = null;
        }
        if (eventNodeTarget.left != null) {
            explode_Wood(eventNodeTarget.left);
            eventNodeTarget.left.data = null;
        }
        bomb_explode_sound.start();
        startFalling = true;
    }
    private static void onDefuse_Placed()
    {
    }
    private static void onWood_Placed()
    {
    }
    private static void explode_Wood(MapGrid<Chip>.Node target)
    {
        //This is a wood tile
        if (target != null && target.data != null && target.data.getType()%4 == 3)
        {
            target.data = null;
            explode_Wood(target.up);
            explode_Wood(target.right);
            explode_Wood(target.down);
            explode_Wood(target.left);
        }
    }

    public Strategic4View(Context context)
    {
        super(context,false);
        builder1 = new AlertDialog.Builder(context);
        //The first time this is made, setup statics
        if (!setup)
        {
            setupOnce(context);
            newGame();
        }
    }
    private void setupOnce(Context context)
    {
        setup = true;

        //The click sound
        mp = MediaPlayer.create(context,R.raw.chip_click);
        //The click sound
        bomb_explode_sound = MediaPlayer.create(context,R.raw.bombexplosion);

        //Image loading options
        BitmapFactory.Options options = new BitmapFactory.Options();
        //Don't scale up images
        options.inScaled = false;

        setupImages(context,options);

        //Alpha paint for gameover
        gameOverPaint = new Paint();
        gameOverPaint.setTextSize(360f);
        gameOverPaint.setColor(Color.argb(127,0,0,0));

        DragChip red_drag = new DragChip(chipRed,true, 0,25,25,150,150);
        DragChip red_bomb_drag = new DragChip(chipBomb,true, 1,25,200,150,150);
        DragChip red_wood_drag = new DragChip(chipWood,true, 3,25,375,150,150);

        DragChip blue_drag = new DragChip(chipBlue,false, 4, 25,25,150,150);
        DragChip blue_bomb_drag = new DragChip(chipBomb,false, 5,25,200,150,150);
        DragChip blue_wood_drag = new DragChip(chipWood,false, 7,25,375,150,150);

        team_Drags = new DragChip[][] {{red_drag,red_bomb_drag,red_wood_drag}, {blue_drag,blue_bomb_drag,blue_wood_drag}};
        blue_drag.setActive(false);

        setupEvents();
    }
    protected static void setupImages(Context context, BitmapFactory.Options options) {
        Connect4View.setupImages(context,options);
        chipBomb = BitmapFactory.decodeResource(context.getResources(),R.drawable.bombchip, options);
        chipWood = BitmapFactory.decodeResource(context.getResources(),R.drawable.woodchip, options);
    }
    public static void setupEvents() {

        //Clear any events from before
        EventSystem.clearEvents();
        //Add our event names
        for (int i = 0; i<events.length; ++i)
        {
            EventSystem.initEvent((events[i]));
        }
        //Add the event hooks
        EventSystem.addHook("Chip_Placed",Strategic4View::onChip_Placed);
        EventSystem.addHook("Blue_Chip_Placed",Strategic4View::onBlue_Chip_Placed);
        EventSystem.addHook("Red_Chip_Placed",Strategic4View::onRed_Chip_Placed);
        EventSystem.addHook("Red_Wins",Connect4View::onRed_Wins);
        EventSystem.addHook("Blue_Wins",Connect4View::onBlue_Wins);
        EventSystem.addHook("Tie_Game",Connect4View::onTie_Game);
        EventSystem.addHook("Touch_Down",Strategic4View::onTouch_Down);
        EventSystem.addHook("Touch_Move",Strategic4View::onTouch_Move);
        EventSystem.addHook("Touch_Up",Strategic4View::onTouch_Up);
        EventSystem.addHook("Bomb_Placed",Strategic4View::onBomb_Placed);
        EventSystem.addHook("Bomb_Explode",Strategic4View::onBomb_Explode);
        EventSystem.addHook("Defuse_Placed",Strategic4View::onDefuse_Placed);
        EventSystem.addHook("Wood_Placed",Strategic4View::onWood_Placed);
   }
    //Setup a new round, generates an empty grid
    public void newGame()
    {
        if (!setup)
            return;
        gameOver = false;
        redsTurn = true;
        redWins = false;
        blueWins = false;
        isFalling = false;
        startFalling = false;
        netID = 0;
        netGameState = -1;
        netMoveCount = 0;
        for (int j = 0; j < team_Drags[1].length; ++j)
        {
            team_Drags[0][j].setActive(true);
            team_Drags[0][j].coolDown = 0;
        }
        for (int j = 0; j < team_Drags[1].length; ++j)
        {
            team_Drags[1][j].setActive(false);
            team_Drags[1][j].coolDown = 0;
        }
        mapGrid = new MapGrid<Chip>(7,6, background, new Rect(7,7,5,5));
    }
    protected void drawGame(Canvas canvas) {

        //drawing a background color for canvas
        canvas.drawColor(Color.argb(255,25,180,25));

        hoverChip.Draw(canvas,alphaPaint);

        mapGrid.Draw(canvas, this, chipDraw);

        //canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,null);

        if (redsTurn) {
            //canvas.drawBitmap(chipRed, 25, 25, null);
            canvas.drawText("RED Turn",5,getHeight()-25,fontPaint);

        }
        else {
            //canvas.drawBitmap(chipBlue, getWidth() - 175, 25, null);
            canvas.drawText("BLUE Turn",getWidth() - 215,getHeight()-25,fontPaint);
        }
        for (int i = 0; i < team_Drags.length; ++i)
        {
            for (int j = 0; j < team_Drags[i].length; ++j)
            {
                team_Drags[i][j].Draw(canvas);
            }
        }

        if (gameOver)
        {
            if (redWins) {
                canvas.drawRect(0,0,getWidth(),getHeight(),gameOverPaint);
                canvas.drawBitmap(redWins_image,null,new Rect(50,50,getWidth()-50,getHeight()-50),null);
                //gameOverPaint.setColor(Color.argb(127,0,0,0));

            }
            else if (blueWins) {
                canvas.drawRect(0,0,getWidth(),getHeight(),gameOverPaint);
                canvas.drawBitmap(blueWins_image,null,new Rect(50,50,getWidth()-50,getHeight()-50),null);
                //gameOverPaint.setColor(Color.argb(127,0,0,0));

            }
            else {
                canvas.drawRect(0,0,getWidth(),getHeight(),gameOverPaint);
                gameOverPaint.setColor(Color.YELLOW);
                canvas.drawText("Tie Game!", 50, 615, gameOverPaint);
                gameOverPaint.setColor(Color.argb(127,0,0,0));
            }
        }
    }

    public Bitmap getImageofChip(int type)
    {
        switch(type)
        {
            case 0:
                return chipRed;
            case 1:
                return chipBomb;
            case 2:
                return chipDefuse;
            case 3:
                return chipWood;
            case 4:
                return chipBlue;
            case 5:
                return chipBomb;
            case 6:
                return chipDefuse;
            case 7:
                return chipWood;
        }
        return null;
    }
    public boolean getTeamofChip(int type)
    {
        for (int i = 0; i < team_Drags.length; ++i) {
            for (int j = 0; j < team_Drags[i].length; ++j) {
                if (team_Drags[i][j].getType() == type)
                    return team_Drags[i][j].Red();
            }
        }
        return false;
    }

    @Override
    public void onSizeChanged(int x, int y, int w, int h) {
        super.onSizeChanged(x,y,w,h);
        if (x == 0)
            return;
        chipWood = Bitmap.createScaledBitmap(chipWood,chipSize,chipSize,false);
        chipBomb = Bitmap.createScaledBitmap(chipBomb,chipSize,chipSize,false);
        //Resize anything that may have needed it
        for (int j = 0; j < team_Drags[0].length; ++j)
        {
            team_Drags[0][j].im = getImageofChip(team_Drags[0][j].getType());
        }
        for (int j = 0; j < team_Drags[1].length; ++j)
        {
            team_Drags[1][j].setPosition(x-175,25 + j*175);
            team_Drags[1][j].im = getImageofChip(team_Drags[1][j].getType());
        }
    }
    //Network gameID, Strategic4 = 2
    public int getGameID(){return 2;}
}
