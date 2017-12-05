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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Philip on 10/26/2017.
 */
//Chip types:   RED : 0 regular, 1 bomb, 2 defuse, 3 wood
//              BLUE: 4         5          6        7
class Chip
{
    protected Bitmap im;
    private boolean isRed;
    private int type;
    boolean isDoneMoving = false;
    Chip(Bitmap _im, boolean _isRed, int _type)
    {
        im = _im;
        isRed = _isRed;
        type = _type;
    }
    public boolean Red()
    {
        return isRed;
    }
    public int getType() { return type; }
    public void Draw(int x, int y, Canvas c)
    {
        c.drawBitmap(im, x, y, null);
    }
}
class HoverChip{
    public Bitmap im;
    public boolean active;
    public int x;
    public int y;
    HoverChip(Bitmap _im, int _x, int _y){
        im = _im;
        x = _x;
        y = _y;
        active = false;
    }
    public void Draw(Canvas c, Paint p) {
        if (active)
            c.drawBitmap(im, x, y, p);
    }
}
class DragChip extends Chip
{
    int originalX;
    int originalY;
    private int left;
    private int top;
    private int width;
    private int height;
    private boolean isActive;
    public int coolDown;
    public static Paint coolDownPaint = new Paint();

    DragChip(Bitmap _im, boolean _isRed, int _type, int x, int y, int w, int h)
    {
        super(_im,_isRed,_type);
        setPosition(x,y);
        width = w;
        height = h;
        isActive = true;
        coolDown = 0;
        coolDownPaint.setColor(Color.RED);
        coolDownPaint.setTextSize(95f);
    }
    public boolean isInside(int x, int y)
    {
        if (isActive && coolDown == 0 && x >= left && x <= left+width && y >= top && y <= top+height)
            return true;
        return false;
    }
    public void tick(){coolDown--; if (coolDown < 0) {coolDown = 0;}}
    public void setActive(boolean state)
    {
        isActive = state;
    }
    public boolean getActive()
    {
        return isActive;
    }
    public void setPosition(int x, int y)
    {
        originalX = left = x;
        originalY = top = y;
    }
    public void movePosition(int x, int y)
    {
        left = x;
        top = y;
    }
    public void resetPosition()
    {
        left = originalX;
        top = originalY;
    }
    public void Draw(Canvas c)
    {
        if (isActive) {
            super.Draw(left, top, c);
            if (coolDown > 0)
            {
                c.drawRect(left,top,Connect4View.chipSize + left,Connect4View.chipSize + top,Connect4View.gameOverPaint);
                c.drawText(Integer.toString(coolDown),left+ (int)(Connect4View.chipSize*0.25),top + (int)(Connect4View.chipSize*0.75),coolDownPaint);
            }
        }

    }
}
class Animation {
    Bitmap[] ims;
    int frameCount;
    boolean loop;
    int frameDelay;
    boolean finished;
    int ticks;
    int currFrame;
    int x,y;
    Animation(Bitmap[] _ims, boolean _loop, int _delay, int _x, int _y){
        ims = _ims;
        frameCount = ims.length;
        loop = _loop;
        frameDelay = _delay;
        finished = false;
        ticks = 0;
        currFrame = 0;
        x = _x;
        y = _y;
    }
    void Tick(){
        if ((ticks%frameDelay) == 0) {
            currFrame++;
            if (currFrame >= frameCount) {
                if (loop)
                    currFrame = 0;
                else
                    finished = true;
            }
        }
        ticks++;
    }
    void Draw(Canvas c){
        if (!finished)
         c.drawBitmap(ims[currFrame], x, y, null);
    }
}

public class Connect4View extends SurfaceView implements Runnable {
    //boolean variable to track if the game is playing or not
    volatile boolean playing;

    //Have we setup the static vars?
    protected static boolean setup = false;

    //the game thread
    protected Thread gameThread = null;

    //A list of all events this game uses
    protected static String[] events = {"Chip_Placed", "Blue_Chip_Placed",
            "Red_Chip_Placed", "Blue_Wins", "Red_Wins", "Tie_Game", "Touch_Down", "Touch_Move", "Touch_Up"};

    protected Paint fontPaint;
    protected Paint alphaPaint;
    protected static Paint gameOverPaint;
    protected static Canvas theCanvas;
    protected SurfaceHolder surfaceHolder;

    //Images
    protected static Bitmap background;
    protected static Bitmap gameField;
    protected static Bitmap chipRed;
    protected static Bitmap chipBlue;
    protected static Bitmap chipYellow;
    public static int chipSize = 150;

    protected static Bitmap blueWins_image;
    protected static Bitmap redWins_image;

    //Temporary yellow chip location
    protected static float tmpX = -100;
    protected static float tmpY = -100;

    //Booleans for game states
    protected static boolean redsTurn;
    protected static boolean gameOver;
    protected static boolean redWins;
    protected static boolean blueWins;
    protected static boolean placedFromInput;

    //The sound for chip placement
    protected static MediaPlayer mp;

    //The main grid which holds the chips
    protected static MapGrid<Chip> mapGrid;

    //The chip being dragged
    protected static DragChip dragged = null;

    //Draggable chips
    protected static DragChip[] drags;

    //Chip that events are used with
    protected static Chip eventChipTarget;
    protected static MapGrid<Chip>.Node eventNodeTarget;

    protected static HoverChip hoverChip;

    protected static List<Animation> anims;

    protected static boolean useOnline = false;
    protected static int netID = 0;

    private static void onChip_Placed()
    {
        if (eventChipTarget.Red())
            EventSystem.triggerEvent("Red_Chip_Placed");
        else
            EventSystem.triggerEvent("Blue_Chip_Placed");

        //Play a sound effect
        mp.start();



        //Change turns after a chip was played
        if (placedFromInput) {
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
            drags[1].setActive(true);
            drags[0].setActive(false);
        }
    }
    private static void onBlue_Chip_Placed()
    {
        //Add some stats

        if (placedFromInput) {
            drags[0].setActive(true);
            drags[1].setActive(false);
        }

    }
    protected static void onRed_Wins()
    {
        redWins = true;
        gameOver = true;

        //Add some stats
    }
    protected static void onBlue_Wins()
    {
        blueWins = true;
        gameOver = true;

        //Add some stats
    }
    protected static void onTie_Game()
    {
        gameOver = true;
        //Add some stats
    }

    //Touch events, use tmpx and tmpy for locations
    private static void onTouch_Down()
    {
        if (!useOnline || redsTurn == netIsRed) {
            for (int i = 0; i < drags.length; ++i) {
                if (drags[i].isInside((int) tmpX, (int) tmpY)) {
                    dragged = drags[i];
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
                if (addChip(dragged.Red(),tmp.x,dragged.Red()?chipRed:chipBlue,0)) {
                    placedFromInput = true;
                    dragged.setActive(false);
                    if (useOnline) {
                        online_KeepAlive();
                        online_SendEvent(1, tmp.x, dragged.Red() ? 0 : 4);
                    }
                }
            }
            dragged = null;
            doInvalidate = true;
            hoverChip.active = false;
        }
    }
    AlertDialog.Builder builder1;
    AlertDialog mainAlert;
    long alertTimer = 0;
    public void newAlert(String msg)
    {
        builder1.setMessage(msg);
        builder1.setCancelable(true);

        if (mainAlert != null && mainAlert.isShowing())
            mainAlert.cancel();

        mainAlert = builder1.create();
        mainAlert.show();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        theCanvas = canvas;
        //canvas.drawText(canvas.isHardwareAccelerated()?"YES":"NO",5,getHeight()-75,fontPaint);
        drawGame(canvas);
    }

    //Class constructor
    public Connect4View(Context context, boolean doSetup) {
        super(context);
        surfaceHolder = getHolder();
        fontPaint = new Paint();
        fontPaint.setTextSize(45f);
        alphaPaint = new Paint();
        alphaPaint.setAlpha(127);
        builder1 = new AlertDialog.Builder(context);
        this.setWillNotDraw(false);

        hoverChip = new HoverChip(null,0,0);
        //The first time this is made, setup statics
        if (!setup && doSetup)
        {
            setupOnce(context);
            newGame();
        }
    }
    public static Rect viewSize;
    @Override
    public void onSizeChanged(int x, int y, int w, int h) {
        super.onSizeChanged(x,y,w,h);
        if (x == 0)
            return;
        viewSize = new Rect(0,0,x,y);
        float scale = (y/1080.0f);
        chipSize = (int)(150*scale);
        chipRed = Bitmap.createScaledBitmap(chipRed,chipSize,chipSize,false);
        chipBlue = Bitmap.createScaledBitmap(chipBlue,chipSize,chipSize,false);
        chipYellow = Bitmap.createScaledBitmap(chipYellow,chipSize,chipSize,false);
        mapGrid.changeSize(x,y);

        //Resize anything that may have needed it
        if (drags != null) {
            drags[1].setPosition(x - 175, 25);
            drags[0].im = chipRed;
            drags[1].im = chipBlue;
        }
    }
    private void setupOnce(Context context)
    {
        setup = true;

        //The click sound
        mp = MediaPlayer.create(context,R.raw.chip_click);

        //Image loading options
        BitmapFactory.Options options = new BitmapFactory.Options();
        //Don't scale up images
        options.inScaled = false;

        setupImages(context,options);

        //Alpha paint for gameover
        gameOverPaint = new Paint();
        gameOverPaint.setTextSize(360f);
        gameOverPaint.setColor(Color.argb(127,0,0,0));

        anims = new ArrayList<>();

        DragChip red_drag = new DragChip(chipRed,true, 0,25,25,150,150);
        DragChip blue_drag = new DragChip(chipBlue,false, 0, 25,25,150,150);
        drags = new DragChip[] {red_drag, blue_drag};
        blue_drag.setActive(false);

        setupEvents();
    }
    protected static void setupImages(Context context, BitmapFactory.Options options)
    {
        //The main game background
        background = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4board, options);

        gameField = BitmapFactory.decodeResource(context.getResources(),R.drawable.field, options);

        //Load piece image, and split it into colors
        Bitmap sprite = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4chips, options);
        chipRed = Bitmap.createBitmap(sprite,0,0,150,150);
        chipBlue = Bitmap.createBitmap(sprite,150,0,150,150);
        chipYellow = Bitmap.createBitmap(sprite,0,150,150,150);


        //End game images
        blueWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerblue, options);
        redWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerred, options);
    }
    public static void setupEvents()
    {
        //Clear any events from before
        EventSystem.clearEvents();
        //Add our event names
        for (int i = 0; i<events.length; ++i)
        {
            EventSystem.initEvent((events[i]));
        }
        //Add the event hooks
        EventSystem.addHook("Chip_Placed",Connect4View::onChip_Placed);
        EventSystem.addHook("Blue_Chip_Placed",Connect4View::onBlue_Chip_Placed);
        EventSystem.addHook("Red_Chip_Placed",Connect4View::onRed_Chip_Placed);
        EventSystem.addHook("Red_Wins",Connect4View::onRed_Wins);
        EventSystem.addHook("Blue_Wins",Connect4View::onBlue_Wins);
        EventSystem.addHook("Tie_Game",Connect4View::onTie_Game);
        EventSystem.addHook("Touch_Down",Connect4View::onTouch_Down);
        EventSystem.addHook("Touch_Move",Connect4View::onTouch_Move);
        EventSystem.addHook("Touch_Up",Connect4View::onTouch_Up);
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
        drags[0].setActive(true);
        drags[1].setActive(false);
        mapGrid = new MapGrid<Chip>(7,6, background, new Rect(7,7,5,5));

    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction())
        {
            //Our main chip placement
            case MotionEvent.ACTION_DOWN:
                tmpX = x;
                tmpY = y;
                EventSystem.triggerEvent("Touch_Down");
                break;
            //We can get continued movement here
            case MotionEvent.ACTION_MOVE:
                tmpX = x;
                tmpY = y;
                EventSystem.triggerEvent("Touch_Move");
                break;
            //Up events
            case MotionEvent.ACTION_UP:

                tmpX = x;
                tmpY = y;
                EventSystem.triggerEvent("Touch_Up");

                break;
        }

        return true;
    }
    public static boolean addChip(boolean red, int column, Bitmap _im, int _type)
    {
        if (gameOver || isFalling)
            return false;
        MapGrid<Chip>.Node target = mapGrid.getNodeCoord(column,0);

        if (target.data != null)
            return false;

        target.data = new Chip(_im, red,_type);


        if (target.down.data != null) {
            target.data.isDoneMoving = true;
        }

        startFalling = true;
        isFalling = true;

        return true;
    }
    final static double TICK_RATE = 16.667;
    final static double TICKS_PER_SECOND = 1000/TICK_RATE;
    @Override
    public void run() {
        double lag = 0;
        long newTime = 0;
        long lastTime = System.nanoTime()/1000000;
        while (playing) {
            newTime = System.nanoTime()/1000000;
            lag +=  newTime - lastTime;
            lastTime = newTime;
            while (lag > TICK_RATE) {
                this.post(() -> update());
                lag -= TICK_RATE;
            }
            sleep();
        }
    }

    protected void sleep() {
        try {
            gameThread.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void fallDownTiles()
    {
        isFalling = false;
        //Go through every width
        MapGrid<Chip>.Node width = mapGrid.getNodeCoord(0,4);
        Chip fallCheck = null;
        MapGrid<Chip>.Node fallTarget = null;
        while (width != null)
        {
            //Go through every height
            MapGrid<Chip>.Node check = width;
            while (check != null)
            {
                fallCheck = check.data;
                fallTarget = check;
                //Can we move down?
                if (fallCheck != null)
                {
                    //Move it down here
                    if (check.down.data == null) {
                        check.down.data = check.data;
                        check.data = null;

                        isFalling = true;
                        check.down.data.isDoneMoving = false;
                        if (check.down.down == null || check.down.down.data != null) {
                            check.down.data.isDoneMoving = true;
                            fallCheck = check.down.data;
                            fallTarget = check.down;
                        }
                    }
                    if (fallCheck.isDoneMoving)
                    {
                        fallCheck.isDoneMoving = false;
                        eventChipTarget = fallCheck;
                        eventNodeTarget = fallTarget;
                        EventSystem.triggerEvent("Chip_Placed");
                        eventChipTarget = null;
                        eventNodeTarget = null;
                        placedFromInput = false;
                    }
                }

                check = check.up;
            }
            width = width.right;
        }

    }
    //If something fell last frame
    protected static boolean isFalling = false;
    //If we should start falling next frame
    protected static boolean startFalling = false;
    //A previous falling state to see when we are done
    protected static boolean wasFalling = false;
    private static long timeNow;

    //Game states for online -1 none, 0 waiting for player, 1 my turn, 2 other turn
    protected static int netGameState = -1;
    //Are we red or blue online
    protected static boolean netIsRed = true;

    protected static boolean doInvalidate = false;

    int dropTicks = 0;

    protected void update() {
        wasFalling = isFalling;
        long newTime = System.currentTimeMillis()/1000;
        if (isFalling && !startFalling) {
            dropTicks++;
            //Chip drop speed
            if(dropTicks >= 6) {
                fallDownTiles();
                doInvalidate = true;
                dropTicks = 0;
            }
        }

        //Everything is done moving
        if (wasFalling && !isFalling)
        {
            //Something can go here
            checkAllWins();
            doInvalidate = true;
        }
        //Delay the fall for one update
        if (startFalling)
        {
            startFalling = false;
            doInvalidate = true;
        }
        if (useOnline) {
            switch(netGameState) {
                case 0:
                    //Waiting for players...
                    if (newTime - timeNow > 3){
                        online_Waiting();
                        online_KeepAlive();
                        timeNow = newTime;
                    }
                    break;
                case 1:
                    if (newTime - timeNow > 8){
                        online_KeepAlive();
                        timeNow = newTime;
                    }
                    break;
                case 2:
                    //Process turns from the server every two seconds
                    if (newTime - timeNow > 2){
                        online_GetTurns();
                        timeNow = newTime;
                    }
                break;
        if (alertTimer > 0){
            alertTimer--;
            if (alertTimer <= 0 ) {
                mainAlert.cancel();
            }
        }

        for (int i = anims.size()-1; i >= 0 ; i--){
            if (anims.get(i).finished)
                anims.remove(i);
            else
                anims.get(i).Tick();
        }
        if (doInvalidate) {
            invalidate();
            doInvalidate = false;
        }
    }
    //Add chip without events
    public void addChipFast(boolean red, int column, Bitmap _im, int _type){

        MapGrid<Chip>.Node target = mapGrid.getNodeCoord(column,0);

        while(target.down != null && target.down.data == null)
        {
            target = target.down;
        }
        target.data = new Chip(_im, red,_type);
    }
    //Pull down all tiles without events
    public void fallDownFast(){
        //Go through every width
        MapGrid<Chip>.Node width = mapGrid.getNodeCoord(0,5);
        while (width != null)
        {
            //Go through every height
            MapGrid<Chip>.Node check = width;
            while (check.up != null && check.data == null)
            {
                //Is something over us
                MapGrid<Chip>.Node over = check.up;
                while (over != null) {
                    if (over.data != null) {
                        //Move it here
                        check.data = over.data;
                        over.data = null;
                        break;
                    }
                    over = over.up;
                }
                check = check.up;
            }
            width = width.right;
        }
    }

    //Go through a list of all moves and play them
    public void fastForward(JSONArray moves){
        try{
            for (int i = 0; i < moves.length(); i++) {
                JSONObject move = moves.getJSONObject(i);
                int event = move.getInt("Event");
                //Some events for now, 1=placeChip
                int data = move.getInt("Data");
                //Some data for now, location of placement
                int type = move.getInt("Type");
                //Some data for now, type of placement
                if (event == 1) {
                    addChipFast(getTeamofChip(type), data, getImageofChip(type), type);
                    fallDownFast();
                }
            }
        } catch (JSONException e) {
            newAlert("JSON error while connecting");
        }
        checkAllWins();
        doInvalidate = true;
    }

    public static void DrawChip(Chip c, int x, int y)
    {
        c.Draw(x,y,theCanvas);
    }
    protected MapGrid.DrawInterface<Chip> chipDraw = Connect4View::DrawChip;


    protected void drawGame(Canvas canvas) {

        //drawing a background color for canvas
        canvas.drawColor(Color.argb(255,25,180,25));
        //canvas.drawBitmap(gameField,null,viewSize, null);

        hoverChip.Draw(canvas,alphaPaint);

        mapGrid.Draw(canvas, this, chipDraw);

        for (int i = anims.size()-1; i >= 0 ; i--){
            anims.get(i).Draw(canvas);
        }

        //canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,null);
        //canvas.drawText(canvas.isHardwareAccelerated()?"YES":"NO",5,getHeight()-55,fontPaint);
        if (redsTurn) {
            //canvas.drawBitmap(chipRed, 25, 25, null);
            canvas.drawText("RED Turn",5,getHeight()-25,fontPaint);
        }
        else {
            //canvas.drawBitmap(chipBlue, getWidth() - 175, 25, null);
            canvas.drawText("BLUE Turn",getWidth() - 215,getHeight()-25,fontPaint);
        }
        for (int i =0;i < drags.length; ++i)
        {
            drags[i].Draw(canvas);
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

    public void pause() {
        //when the game is paused
        //setting the variable to false
        playing = false;
        Networking.stopRequests();
        try {
            //stopping the thread
            gameThread.join();
        } catch (InterruptedException e) {
        }
    }

    public void resume() {
        //when the game is resumed
        //starting the thread again
        if (gameOver)
            newGame();

        if (useOnline && netID == 0)
        {
            online_Connect();
        }
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    static boolean tieCheck()
    {
        MapGrid<Chip>.Node tmp = mapGrid.getNodeCoord(0,0);
        while (tmp != null) {
            if (tmp.data == null)
                break;
            if (tmp.right == null)
                return true;
            tmp = tmp.right;
        }
        return false;
    }
    static void checkAllWins()
    {
        //Check wins
        MapGrid<Chip>.Node tmp = mapGrid.getNodeCoord(0,0);
        boolean redWon = false;
        boolean blueWon = false;
        while (tmp != null)
        {
            MapGrid<Chip>.Node tmp2 = tmp;
            while (tmp2 != null) {
                if (winCheck(tmp2)) {
                    if (tmp2.data.Red())
                        redWon = true;
                    else
                        blueWon = true;
                }
                tmp2 = tmp2.down;
            }

            tmp = tmp.right;
        }
        if (redWon && blueWon)
        {
            EventSystem.triggerEvent("Tie_Game");
        }
        else if (redWon)
        {
            EventSystem.triggerEvent("Red_Wins");
        }
        else if (blueWon)
        {
            EventSystem.triggerEvent("Blue_Wins");
        }
        else if (!gameOver && tieCheck())
        {
            EventSystem.triggerEvent("Tie_Game");
        }
    }
    static boolean winCheck(MapGrid<Chip>.Node check)
    {
        if (check == null || check.data == null || check.data.getType()%4 != 0)
            return false;

        MapGrid<Chip>.Node tmp = check;
        int count = 0;
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            if (tmp.left != null) {
                tmp = tmp.left.up;
            }
            else
                break;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            if (tmp.right != null) {
                tmp = tmp.right.down;
            }
            else
                break;
        }
        if (count >= 4)
            return true;

        tmp = check;
        count = 0;
        //Diagonal up-right
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            if (tmp.right != null) {
                tmp = tmp.right.up;
            }
            else
                break;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            if (tmp.left != null) {
                tmp = tmp.left.down;
            }
            else
                break;
        }
        if (count >= 4)
            return true;

        tmp = check;
        count = 0;
        //Left-right
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.left;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.right;
        }
        if (count >= 4)
            return true;

        tmp = check;
        count = 0;
        //Up-down
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.up;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && (tmp.data.getType()%4) == 0 && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.down;
        }
        if (count >= 4)
            return true;


        return false;
    }

    public Bitmap getImageofChip(int type)
    {
        return drags[type/4].im;
    }
    public boolean getTeamofChip(int type)
    {
        return drags[type/4].Red();
    }
    
    //Network gameID, Connect4 = 1
    public int getGameID(){return 1;}
    protected static int netMoveCount = 0;

    public void online_Connect(){
        Networking.Connect(getGameID(), response -> {
            try {
                netID = response.getInt("ID");
                netIsRed = response.getBoolean("isRed");
                //Move to waiting state
                netGameState = 0;
                if (netIsRed) {
                    newAlert("Connected! Waiting...");
                }
            } catch (JSONException e) {
                newAlert("JSON error while connecting");
            }
        },error -> newAlert("Connection failed, " + error.getMessage()));
    }
    public void online_Waiting(){
        Networking.GetTurns(netID, response -> {
            try {
                if (response.getBoolean("started"))
                {
                    if (netIsRed)
                        netGameState = 1;
                    else
                        netGameState = 2;
                    newAlert("Ready, you are " + (netIsRed?"First":"Second"));
                    alertTimer = 80;
                }
            } catch (JSONException e) {
                newAlert("JSON error reading moves");
            }
        },error -> {
            newAlert("This game is over, " + error.getMessage());
            netGameState = -1;
            netID = 0;
        });
    }

    public void online_GetTurns(){
        Networking.GetTurns(netID, response -> {
            try {
                JSONArray moves = response.getJSONArray("moves");
                for (int i = netMoveCount; i < moves.length(); i++)
                {
                    JSONObject move = moves.getJSONObject(i);
                    int event = move.getInt("Event");
                    //Some events for now, 1=placeChip
                    int data = move.getInt("Data");
                    //Some data for now, location of placement
                    int type = move.getInt("Type");
                    //Some data for now, type of placement
                    if (event==1) {
                        placedFromInput = true;
                        addChip(getTeamofChip(type), data, getImageofChip(type), type);
                    }
                    doInvalidate = true;
                    netMoveCount++;
                }
            } catch (JSONException e) {

                newAlert("JSON error reading moves");
            }
        }, error -> {
            newAlert("This game is over, " + error.getMessage());
            netGameState = -1;
            netID = 0;
        });
    }
    public static void online_SendEvent(int _event, int _data, int _type){
        //Don't read our own events
        netMoveCount++;
        Networking.SendEvent(netID,_event,_data,_type, response -> {
        });
    }
    public static void online_KeepAlive(){
        Networking.KeepAlive(netID);
    }
}
