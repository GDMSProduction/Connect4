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
    private boolean real;
    private static Paint paint;
    Chip(Bitmap _im, boolean _isReal, boolean _isRed)
    {
        im = _im;
        real = _isReal;
        isRed = _isRed;
        paint = new Paint();
    }
    public boolean isReal()
    {
        return real;
    }
    public boolean Red()
    {
        return isRed;
    }
    public void Draw(int x, int y, Canvas c, RectF dest)
    {
        if (real)
            c.drawBitmap(im,(24*x + 290 + x * 150),(24*y +  21 + y * 150), paint);
    }
}
class Node
{
    Chip data;
    Node up = null, down = null, left = null, right = null;
}

public class Connect4View extends SurfaceView implements Runnable {
    //boolean variable to track if the game is playing or not
    volatile boolean playing;

    //the game thread
    private Thread gameThread = null;

    private Paint paint;
    private Paint fontPaint;
    private Canvas canvas;
    private SurfaceHolder surfaceHolder;

    private Bitmap background;
    private Bitmap chipRed;
    private Bitmap chipBlue;
    private Bitmap chipYellow;

    private Rect backRect;
    private RectF destBack;

    private float tmpX = -100;
    private float tmpY = -100;
    private float prevHeight = 0;

    private boolean redsTurn = true;

    Node map;

    //Class constructor
    public Connect4View(Context context) {
        super(context);
        surfaceHolder = getHolder();
        paint = new Paint();
        fontPaint = new Paint();
        //fontPaint.setTextScaleX(6f);
        fontPaint.setTextSize(45f);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        background = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4board, options);
        Bitmap sprite = BitmapFactory.decodeResource(context.getResources(),R.drawable.connect4chips, options);
        chipRed = Bitmap.createBitmap(sprite,0,0,150,150);
        chipBlue = Bitmap.createBitmap(sprite,150,0,150,150);
        chipYellow = Bitmap.createBitmap(sprite,0,150,150,150);

        backRect = new Rect(0,0,background.getWidth(),background.getHeight());

        GenerateMap(7,6);
    }
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                int column = (int)floor((x - destBack.left)/((destBack.right- destBack.left)/7));
                if (column >= 0 && column < 7)
                {
                    if (addChip(redsTurn,column))
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
        Node target = map;
        for (int i = 0; i < column; ++i)
        {
            target = target.right;
        }

        if (target.data.isReal())
            return false;

        while (true)
        {
            if (target.down == null || target.down.data.isReal())
                break;
            target = target.down;
        }

        target.data = new Chip(red?chipRed:chipBlue,true, red);

       // if (winCheck(target))
        //{
        //    playing = false;
        //}
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
    private int count = 0;
    private void draw() {
        //Need a valid surface to draw
        if (surfaceHolder.getSurface().isValid()) {
            //locking the canvas
            canvas = surfaceHolder.lockCanvas();

            //drawing a background color for canvas
            int col = Color.HSVToColor(new float[]{count++%360,1,1});
            canvas.drawColor(col);

            DrawMap(canvas,destBack);

            float height = (float)getHeight();
            if (prevHeight != height) {
                float align = height*1.16f;
                float offset = (getWidth()-align)/2;
                destBack = new RectF(offset, 0.0f, align + offset, height);
                prevHeight = height;
            }
            canvas.drawBitmap(background,backRect,destBack, paint);


            canvas.drawBitmap(chipYellow,tmpX-75,tmpY-75,paint);

            if (redsTurn) {
                canvas.drawBitmap(chipRed, 25, 25, paint);
                canvas.drawText("RED Turn",5,215,fontPaint);

            }
            else {
                canvas.drawBitmap(chipBlue, getWidth() - 175, 25, paint);
                canvas.drawText("BLUE Turn",getWidth() - 225,215,fontPaint);

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
    boolean winCheck(Node check)
    {
        Node tmp = check;
        int count = 0;
        //Diagonal up-left
        while (tmp != null) {
            if (tmp.data.isReal() && tmp.data.Red() == check.data.Red())
                count++;
            if (tmp.left != null) {
                tmp = tmp.left.up;
            }
        }
        tmp = check;
        count--;
        while (tmp != null) {
            if (tmp.data.isReal() && tmp.data.Red() == check.data.Red())
                count++;
            if (tmp.right != null) {
                tmp = tmp.right.down;
            }
        }
        if (count >= 4)
            return true;


        return false;
    }
    //Calls draw on every cell in the NODE MESH
    void recurseDraw(Node n, Canvas c, RectF dest, int x, int y)
    {
        if (n == null)
            return;

        n.data.Draw(x,y,c,dest);
        recurseDraw(n.right, c, dest, x+1, y);
        if (n.left == null)
            recurseDraw(n.down,c , dest, x ,y+1);

    }
    //Starts the draw on the map
    void DrawMap(Canvas c, RectF dest)
    {
        recurseDraw(map,c,dest,0,0);
    }
    public Node getNodeCoord(int x, int y)
    {
        Node tmp = map;
        int i = 0;
        for (; i < x; ++i)
        {
            tmp = tmp.right;
        }
        for (i = 0; i < y; ++i)
        {
            tmp = tmp.down;
        }
        return tmp;
    }
    public void GenerateMap(int width, int height)
    {
        if (width < 2 || height < 2)
            return;

        Node h = null;
        Node over = null;
        for (int i = 0; i < height; ++i)
        {
            if (h != null)
            {
                //Column 0 is linked up/down
                h.down = new Node();
                h.down.up = h;
                over = h.right;
                h = h.down;
            }
            else
            {
                //The very first node
                h = new Node();
                map = h;
            }
            h.data = new Chip(null, false, false);
            Node w = h;
            for (int j = 0; j < width - 1; ++j)
            {
                //Rows are linked left/right
                w.right = new Node();
                w.right.left = w;
                w = w.right;
                w.data = new Chip(null, false, false);
                //Build the connections Up/Down for the rest of the width
                if (over != null)
                {
                    over.down = w;
                    w.up = over;
                    over = over.right;
                }
            }
        }
    }
}
