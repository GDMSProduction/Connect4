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
class Chip
{
    private Bitmap im;
    private boolean isRed;
    private static Paint paint;
    Chip(Bitmap _im, boolean _isRed)
    {
        im = _im;
        isRed = _isRed;
        paint = new Paint();
    }
    public boolean Red()
    {
        return isRed;
    }
    public void Draw(int x, int y, Canvas c)
    {
        c.drawBitmap(im, x, y, paint);
    }
}

public class Connect4View extends SurfaceView implements Runnable {
    //boolean variable to track if the game is playing or not
    volatile boolean playing;

    //Have we setup the static vars?
    private static boolean setup = false;

    //the game thread
    private Thread gameThread = null;

    //A list of all events this game uses
    private static String[] events = {"Chip_Placed", "Blue_Chip_Placed", "Red_Chip_Placed", "Blue_Wins", "Red_Wins", "Tie_Game"};

    private Paint fontPaint;
    private static Paint gameOverPaint;
    private static Canvas canvas;
    private SurfaceHolder surfaceHolder;

    //Images
    private static Bitmap background;
    private static Bitmap chipRed;
    private static Bitmap chipBlue;
    private static Bitmap chipYellow;

    private static Bitmap blueWins_image;
    private static Bitmap redWins_image;

    //Temporary yellow chip location
    private float tmpX = -100;
    private float tmpY = -100;

    //Booleans for game states
    private static boolean redsTurn;
    private static boolean gameOver;
    private static boolean redWins;
    private static boolean blueWins;

    //The sound for chip placement
    private static MediaPlayer mp;

    //The main grid which holds the chips
    private static MapGrid<Chip> mapGrid;

    private static void onChip_Placed()
    {
        //Play a sound effect
        mp.start();

        //Change turns after a chip was played
        redsTurn = !redsTurn;
    }
    private static void onRed_Chip_Placed()
    {
        //Add some stats
    }
    private static void onBlue_Chip_Placed()
    {
        //Add some stats
    }
    private static void onRed_Wins()
    {
        redWins = true;
        gameOver = true;

        //Add some stats
    }
    private static void onBlue_Wins()
    {
        blueWins = true;
        gameOver = true;

        //Add some stats
    }
    private static void onTie_Game()
    {
        gameOver = true;
        //Add some stats
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
    private static void setupOnce(Context context)
    {
        setup = true;

        //Image loading options
        BitmapFactory.Options options = new BitmapFactory.Options();
        //Don't scale up images
        options.inScaled = false;

        //The main game background
        background = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4board, options);

        //The click sound
        mp = MediaPlayer.create(context,R.raw.chip_click);

        //Load piece image, and split it into colors
        Bitmap sprite = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4chips, options);
        chipRed = Bitmap.createBitmap(sprite,0,0,150,150);
        chipBlue = Bitmap.createBitmap(sprite,150,0,150,150);
        chipYellow = Bitmap.createBitmap(sprite,0,150,150,150);

        //End game images
        blueWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerblue, options);
        redWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerred, options);

        //Alpha paint for gameover
        gameOverPaint = new Paint();
        gameOverPaint.setTextSize(360f);
        gameOverPaint.setColor(Color.argb(127,0,0,0));

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
    }
    //Setup a new round, generates an empty grid
    private void newGame()
    {
        gameOver = false;
        redsTurn = true;
        redWins = false;
        blueWins = false;
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
                MapGrid<Chip>.Coord tmp = mapGrid.getCoordOfTouch((int)x,(int)y);
                if (tmp.x >= 0 && tmp.x < 7)
                {
                    addChip(redsTurn,tmp.x);
                }
                //No break to update the yellow chip location
                //We can get continued movement here
            case MotionEvent.ACTION_MOVE:
                tmpX = x;
                tmpY = y;
                break;
        }

        return true;
    }
    public boolean addChip(boolean red, int column)
    {
        if (gameOver || isFalling)
            return false;
        MapGrid<Chip>.Node target = mapGrid.getNodeCoord(column,0);

        if (target.data != null)
            return false;

        target.data = new Chip(red?chipRed:chipBlue, red);

        if (target.down.data != null)
        {
            EventSystem.triggerEvent("Chip_Placed");
            if (target.data.Red())
                EventSystem.triggerEvent("Red_Chip_Placed");
            else
                EventSystem.triggerEvent("Blue_Chip_Placed");

            //If we can't fall, it could be a tie
            if (tieCheck())
            {
                EventSystem.triggerEvent("Tie_Game");
            }
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
                        EventSystem.triggerEvent("Chip_Placed");
                        if (check.down.data.Red())
                            EventSystem.triggerEvent("Red_Chip_Placed");
                        else
                            EventSystem.triggerEvent("Blue_Chip_Placed");
                    }
                }
                check = check.up;
            }
            width = width.right;
        }

    }
    //If something fell last frame
    private static boolean isFalling = false;
    //If we should start falling next frame
    private static boolean startFalling = false;
    //A previous falling state to see when we are done
    private static boolean wasFalling = false;
    private void update() {
        wasFalling = isFalling;

        if (isFalling && !startFalling)
            fallDownTiles();

        //Everything is done moving, check wins
        if (wasFalling && !isFalling)
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
    private MapGrid.DrawInterface<Chip> chipDraw = Connect4View::DrawChip;

    private void draw() {
        //Need a valid surface to draw
        if (surfaceHolder.getSurface().isValid()) {
            //locking the canvas
            canvas = surfaceHolder.lockCanvas();

            //drawing a background color for canvas
            canvas.drawColor(Color.argb(255,25,180,25));

            mapGrid.Draw(canvas, this, chipDraw);

            canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,null);

            if (redsTurn) {
                canvas.drawBitmap(chipRed, 25, 25, null);
                canvas.drawText("RED Turn",5,215,fontPaint);

            }
            else {
                canvas.drawBitmap(chipBlue, getWidth() - 175, 25, null);
                canvas.drawText("BLUE Turn",getWidth() - 225,215,fontPaint);
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

    private void sleep() {
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
    static boolean winCheck(MapGrid<Chip>.Node check)
    {
        MapGrid<Chip>.Node tmp = check;
        int count = 0;
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
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
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
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
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
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
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
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
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.left;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.right;
        }
        if (count >= 4)
            return true;

        tmp = check;
        count = 0;
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
                count++;
            else
                break;
            tmp = tmp.up;
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data != null && tmp.data.Red() == check.data.Red())
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
