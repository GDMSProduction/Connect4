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

    //the game thread
    private Thread gameThread = null;

    private static String[] events = {"Chip_Placed", "Blue_Chip_Placed", "Red_Chip_Placed", "Blue_Wins", "Red_Wins", "Tie_Game"};
    private static boolean setup = false;

    private Paint paint;
    private Paint fontPaint;
    private Paint gameOverPaint;
    private static Canvas canvas;
    private SurfaceHolder surfaceHolder;

    private static Bitmap chipRed;
    private static Bitmap chipBlue;
    private static Bitmap chipYellow;

    private static Bitmap blueWins_image;
    private static Bitmap redWins_image;


    private float tmpX = -100;
    private float tmpY = -100;

    private static boolean redsTurn = true;
    private static boolean gameOver = false;
    private static boolean redWins = false;
    private static boolean blueWins = false;
    private static MediaPlayer mp;
    MapGrid<Chip> mapGrid;

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
        paint = new Paint();
        fontPaint = new Paint();
        fontPaint.setTextSize(45f);
        gameOverPaint = new Paint();
        gameOverPaint.setTextSize(360f);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        Bitmap background = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4board, options);

        mapGrid = new MapGrid<Chip>(7,6, background, new Rect(14,14,12,12));

        if (!setup)
        {
            mp = MediaPlayer.create(context,R.raw.chip_click);

            Bitmap sprite = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4chips, options);
            chipRed = Bitmap.createBitmap(sprite,0,0,150,150);
            chipBlue = Bitmap.createBitmap(sprite,150,0,150,150);
            chipYellow = Bitmap.createBitmap(sprite,0,150,150,150);

            blueWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerblue, options);
            redWins_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.winnerred, options);

            setupOnce();
        }

    }
    private static void setupOnce()
    {
        setup = true;
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
    private void newGame()
    {
        gameOver = false;
        redsTurn = true;
        redWins = false;
        blueWins = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                MapGrid<Chip>.Coord tmp = mapGrid.getCoordOfTouch((int)x,(int)y);
                if (tmp.x >= 0 && tmp.x < 7)
                {
                    addChip(redsTurn,tmp.x);
                }
            case MotionEvent.ACTION_MOVE:
                tmpX = x;
                tmpY = y;
                break;
        }

        return true;
    }
    public boolean addChip(boolean red, int column)
    {
        if (gameOver)
            return false;
        MapGrid<Chip>.Node target = mapGrid.getNodeCoord(0,0);

        for (int i = 0; i < column; ++i)
        {
            target = target.right;
        }

        if (target.data != null)
            return false;

        while (true)
        {
            if (target.down == null || target.down.data != null)
                break;
            target = target.down;
        }

        target.data = new Chip(red?chipRed:chipBlue, red);


        if (winCheck(target))
        {
            if (redsTurn)
                EventSystem.triggerEvent("Red_Wins");
            else
                EventSystem.triggerEvent("Blue_Wins");
        }
        else if (tieCheck())
        {
            EventSystem.triggerEvent("Tie_Game");
        }

        EventSystem.triggerEvent("Chip_Placed");
        if (red)
            EventSystem.triggerEvent("Red_Chip_Placed");
        else
            EventSystem.triggerEvent("Blue_Chip_Placed");


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

    private void update() {

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

            canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,paint);

            if (redsTurn) {
                canvas.drawBitmap(chipRed, 25, 25, paint);
                canvas.drawText("RED Turn",5,215,fontPaint);

            }
            else {
                canvas.drawBitmap(chipBlue, getWidth() - 175, 25, paint);
                canvas.drawText("BLUE Turn",getWidth() - 225,215,fontPaint);
            }

            if (gameOver)
            {
                if (redWins) {
                    //gameOverPaint.setColor(Color.RED);
                    //canvas.drawText("Red Wins!", 50, 615, gameOverPaint);
                    canvas.drawBitmap(redWins_image,null,new Rect(50,50,getWidth()-50,getHeight()-50),paint);
                }
                else if (blueWins) {
                    //gameOverPaint.setColor(Color.BLUE);
                    //canvas.drawText("Blue Wins!", 50, 615, gameOverPaint);
                    canvas.drawBitmap(blueWins_image,null,new Rect(50,50,getWidth()-50,getHeight()-50),paint);

                }
                else {
                    gameOverPaint.setColor(Color.YELLOW);
                    canvas.drawText("Tie Game!", 50, 615, gameOverPaint);
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
    boolean tieCheck()
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
    boolean winCheck(MapGrid<Chip>.Node check)
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
