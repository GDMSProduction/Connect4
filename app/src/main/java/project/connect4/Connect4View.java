package project.connect4;

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
class DragChip extends Chip
{
    int originalX;
    int originalY;
    private int left;
    private int top;
    private int width;
    private int height;
    private boolean isActive;

    DragChip(Bitmap _im, boolean _isRed, int _type, int x, int y, int w, int h)
    {
        super(_im,_isRed,_type);
        setPosition(x,y);
        width = w;
        height = h;
        isActive = true;
    }
    public boolean isInside(int x, int y)
    {
        if (isActive && x >= left && x <= left+width && y >= top && y <= top+height)
            return true;
        return false;
    }
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
        if (isActive)
            super.Draw(left, top, c);
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
    protected static Paint gameOverPaint;
    protected static Canvas canvas;
    protected SurfaceHolder surfaceHolder;

    //Images
    protected static Bitmap background;
    protected static Bitmap chipRed;
    protected static Bitmap chipBlue;
    protected static Bitmap chipYellow;

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

    //The sound for chip placement
    protected static MediaPlayer mp;

    //The main grid which holds the chips
    protected static MapGrid<Chip> mapGrid;

    //The chip being dragged
    protected static DragChip dragged = null;

    //Draggable chips
    protected static DragChip[] drags;

    private static void onChip_Placed()
    {
        //Play a sound effect
        mp.start();

        checkAllWins();

        //Change turns after a chip was played
        redsTurn = !redsTurn;

        //Clear the temp drag chip
        dragged = null;
    }
    private static void onRed_Chip_Placed()
    {
        //dragged is the chip that was placed now
        //check type or something

        //Add some stats
        drags[1].setActive(true);
        drags[0].setActive(false);
    }
    private static void onBlue_Chip_Placed()
    {
        //Add some stats
        drags[0].setActive(true);
        drags[1].setActive(false);

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
        for (int i = 0; i < drags.length; ++i) {
            if (drags[i].isInside((int) tmpX, (int) tmpY)) {
                dragged = drags[i];
            }
        }
    }
    private static void onTouch_Move()
    {
        if (dragged != null && dragged.getActive())
        {
            dragged.movePosition((int)tmpX-75,(int)tmpY-75);
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
                addChip(dragged.Red(),tmp.x);
            }
            else
                dragged = null;
        }
    }
    //Class constructor
    public Connect4View(Context context) {
        super(context);
        surfaceHolder = getHolder();
        fontPaint = new Paint();
        fontPaint.setTextSize(45f);


        //The first time this is made, setup statics
        if (!setup)
        {
            setupOnce(context);
            newGame();
        }
    }
    public Connect4View(Context context, boolean doSetup) {
        super(context);
        surfaceHolder = getHolder();
        fontPaint = new Paint();
        fontPaint.setTextSize(45f);


        //The first time this is made, setup statics
        if (!setup && doSetup)
        {
            setupOnce(context);
            newGame();
        }
    }
    @Override
    public void onSizeChanged(int x, int y, int w, int h) {
        super.onSizeChanged(x,y,w,h);
        if (x == 0)
            return;
        //Resize anything that may have needed it
        if (drags != null)
            drags[1].setPosition(x-175,25);
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
    public static void newGame()
    {
        if (!setup)
            return;
        gameOver = false;
        redsTurn = true;
        redWins = false;
        blueWins = false;
        isFalling = false;
        startFalling = false;
        drags[0].setActive(true);
        drags[1].setActive(false);
        mapGrid = new MapGrid<Chip>(7,6, background, new Rect(14,14,12,12));
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
    public static boolean addChip(boolean red, int column)
    {
        if (gameOver || isFalling)
            return false;
        MapGrid<Chip>.Node target = mapGrid.getNodeCoord(column,0);

        if (target.data != null)
            return false;

        target.data = new Chip(red?chipRed:chipBlue, red,0);

        if (target.down.data != null)
        {

            if (target.data.Red())
                EventSystem.triggerEvent("Red_Chip_Placed");
            else
                EventSystem.triggerEvent("Blue_Chip_Placed");
            EventSystem.triggerEvent("Chip_Placed");
        }
        else
        {
            startFalling = true;
            isFalling = true;
        }

        return true;
    }

    @Override
    public void run() {
        while (playing) {
            update();

            draw();

            sleep();
        }
    }
    private static void fallDownTiles()
    {
        isFalling = false;
        //Go through every width
        MapGrid<Chip>.Node width = mapGrid.getNodeCoord(0,4);
        while (width != null)
        {
            //Go through every height
            MapGrid<Chip>.Node check = width;
            while (check != null)
            {
                //Can we move down?
                if (check.data != null && check.down.data == null)
                {
                    //Move it down here
                    check.down.data = check.data;
                    check.data = null;
                    isFalling = true;

                    if (check.down.down == null || check.down.down.data != null) {
                        if (check.down.data.Red())
                            EventSystem.triggerEvent("Red_Chip_Placed");
                        else
                            EventSystem.triggerEvent("Blue_Chip_Placed");
                        EventSystem.triggerEvent("Chip_Placed");
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
    protected void update() {
        wasFalling = isFalling;

        if (isFalling && !startFalling)
            fallDownTiles();

        //Everything is done moving
        if (wasFalling && !isFalling)
        {
            //Something can go here
        }
        //Delay the fall for one update
        if (startFalling)
        {
            startFalling = false;
        }
    }

    public static void DrawChip(Chip c, MapGrid<Chip>.Coord loc)
    {
        c.Draw(loc.x,loc.y,canvas);
    }
    protected MapGrid.DrawInterface<Chip> chipDraw = Connect4View::DrawChip;

    protected void draw() {
        //Need a valid surface to draw
        if (surfaceHolder.getSurface().isValid()) {
            //locking the canvas
            canvas = surfaceHolder.lockCanvas();

            //drawing a background color for canvas
            canvas.drawColor(Color.argb(255,25,180,25));

            mapGrid.Draw(canvas, this, chipDraw);

            //canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,null);

            if (redsTurn) {
                //canvas.drawBitmap(chipRed, 25, 25, null);
                canvas.drawText("RED Turn",5,215,fontPaint);

            }
            else {
                //canvas.drawBitmap(chipBlue, getWidth() - 175, 25, null);
                canvas.drawText("BLUE Turn",getWidth() - 225,215,fontPaint);
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

            //Unlocking the canvas
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    protected void sleep() {
        try {
            gameThread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        //when the game is paused
        //setting the variable to false
        playing = false;
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
        while (tmp != null)
        {
            MapGrid<Chip>.Node tmp2 = tmp;
            while (tmp2 != null) {
                if (winCheck(tmp2)) {
                    if (tmp2.data.Red())
                        EventSystem.triggerEvent("Red_Wins");
                    else
                        EventSystem.triggerEvent("Blue_Wins");
                    return;
                }
                tmp2 = tmp2.down;
            }

            tmp = tmp.right;
        }
        if (!gameOver && tieCheck())
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
}
