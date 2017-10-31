package project.connect4;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.floor;

/**
 * Created by User on 10/26/2017.
 */
class Chip
{
    private Bitmap im;
    private boolean isRed;
    private static Paint paint;
    Chip(Bitmap _im, boolean _isReal, boolean _isRed)
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

    private Paint paint;
    private Paint fontPaint;
    private Paint gameOverPaint;
    private static Canvas canvas;
    private SurfaceHolder surfaceHolder;

    private Bitmap chipRed;
    private Bitmap chipBlue;
    private Bitmap chipYellow;


    private float tmpX = -100;
    private float tmpY = -100;

    private boolean redsTurn = true;
    private boolean gameOver = false;
    private boolean redWins = false;
    private boolean blueWins = false;

    Map<Chip> map;

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
        Bitmap sprite = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4chips, options);
        chipRed = Bitmap.createBitmap(sprite,0,0,150,150);
        chipBlue = Bitmap.createBitmap(sprite,150,0,150,150);
        chipYellow = Bitmap.createBitmap(sprite,0,150,150,150);

        map = new Map<Chip>(7,6, background, new Rect(14,14,12,12));
    }
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                Map<Chip>.Coord tmp = map.getCoordOfTouch((int)x,(int)y);
                if (tmp.x >= 0 && tmp.x < 7)
                {
                    if (addChip(redsTurn,tmp.x))
                        redsTurn = !redsTurn;
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
        Map<Chip>.Node target = map.getNodeCoord(0,0);

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

        target.data = new Chip(red?chipRed:chipBlue,true, red);

        if (winCheck(target))
        {
            gameOver = true;
            if (redsTurn)
                redWins = true;
            else
                blueWins = true;
        }
        else if (tieCheck())
        {
            gameOver = true;
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

    private void update() {

    }

    public static void DrawChip(Chip c, Map<Chip>.Coord loc)
    {
        c.Draw(loc.x,loc.y,canvas);
    }
    private Map.DrawInterface<Chip> chipDraw = Connect4View::DrawChip;

    private int count = 0;
    private void draw() {
        //Need a valid surface to draw
        if (surfaceHolder.getSurface().isValid()) {
            //locking the canvas
            canvas = surfaceHolder.lockCanvas();

            //drawing a background color for canvas
            int col = Color.HSVToColor(new float[]{count++%360,1,1});
            canvas.drawColor(col);

            map.Draw(canvas, this, chipDraw);


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
                    gameOverPaint.setColor(Color.RED);
                    canvas.drawText("Red Wins!", 50, 615, gameOverPaint);
                }
                else if (blueWins) {
                    gameOverPaint.setColor(Color.BLUE);
                    canvas.drawText("Blue Wins!", 50, 615, gameOverPaint);
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
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    boolean tieCheck()
    {
        Map<Chip>.Node tmp = map.getNodeCoord(0,0);
        while (tmp != null) {
            if (tmp.data == null)
                break;
            if (tmp.right == null)
                return true;
            tmp = tmp.right;
        }
        return false;
    }
    boolean winCheck(Map<Chip>.Node check)
    {
        Map<Chip>.Node tmp = check;
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
