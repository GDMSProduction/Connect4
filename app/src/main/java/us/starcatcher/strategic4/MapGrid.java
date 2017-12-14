package us.starcatcher.strategic4;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;


import static java.lang.Math.floor;

/**
 * Created by Philip on 10/31/2017.
 */

public class MapGrid<T> {

    public class Node{
        T data;
        int x=0,y=0;
        Node up= null, down= null, left= null, right= null;
    }
    @FunctionalInterface
    public interface DrawInterface<t>{
        void test(t c, int x, int y);
    }

    public class Coord{
        int x;
        int y;
    }
    private Bitmap backgroundV;
    protected Bitmap background;
    private RectF destBack;
    private RectF destAdj;
    private Rect Adjustment;
    private float prevHeight = 0;

    private Node map;
    int x;
    int y;
    float gridSizeX;
    float gridSizeY;


    public MapGrid(int _x, int _y, Bitmap _im, Rect adj)
    {
        x = _x;
        y = _y;
        GenerateMap(x,y);

        Adjustment = adj;
        background = _im;
    }


    //Calls draw on every cell in the NODE MESH
    void recurseDraw(Node n, int _x, int _y, DrawInterface<T> test)
    {
        if (n == null)
            return;

        if (n.data != null)
        {
            int tmpx = (int)(destBack.left + destAdj.left + gridSizeX*_x);
            int tmpy = (int)(destBack.top + destAdj.top + gridSizeY*_y);
            test.test(n.data,tmpx,tmpy);
        }
        recurseDraw(n.right,_x+1, _y, test);
        if (n.left == null)
            recurseDraw(n.down, _x ,_y+1, test);

    }
    void changeSize(int x, int y){
        float height = (float)y;
        if (prevHeight != height) {
            float align = height*1.16f;
            float offset = (x-align)/2;
            destBack = new RectF(offset, 0.0f, align + offset, height);
            float scaleX = align / background.getWidth();
            float scaleY = height / background.getHeight();
            destAdj = new RectF(Adjustment.left * scaleX, Adjustment.top * scaleY, Adjustment.right * scaleX, Adjustment.bottom * scaleY);

            gridSizeX = ((destBack.right - destAdj.right) - (destBack.left + destAdj.left))/(this.x);
            gridSizeY = ((destBack.bottom - destAdj.bottom) - (destBack.top + destAdj.top))/(this.y);

            prevHeight = height;
            backgroundV = Bitmap.createScaledBitmap(background,(int)(align),y,false);
        }
    }
    //Starts the draw on the mapGrid
    void DrawMap(DrawInterface<T> test)
    {
        recurseDraw(map,0,0, test);
    }
    public void Draw(Canvas c, View v, DrawInterface<T> test)
    {
        DrawMap(test);

        //c.drawBitmap(background,null,destBack, null);
        c.drawBitmap(backgroundV,destBack.left,0,null);
    }

    public Coord getCoordOfLoc(int _x, int _y){
        Coord tmp = new Coord();
        tmp.x = (int)(destBack.left + destAdj.left + gridSizeX*_x);
        tmp.y = (int)(destBack.top + destAdj.top + gridSizeY*_y);
        return tmp;
    }

    public Coord getCoordOfTouch(int x, int y)
    {
        Coord tmp = new Coord();
        tmp.x = (int)floor((x - destBack.left)/((destBack.right- destBack.left)/7));
        tmp.y = 0;
        return tmp;
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

    private void GenerateMap(int width, int height)
    {
        if (width < 2 || height < 2)
            return;
        int x=0,y=0;
        Node h = null;
        Node over = null;
        for (int i = 0; i < height; ++i)
        {
            if (h != null)
            {
                //Column 0 is linked up/down
                h.down = new Node();
                h.down.y = y;
                h.down.up = h;
                over = h.right;
                h = h.down;
            }
            else
            {
                //The very first node
                h = new Node();
                h.y = y;
                map = h;
            }
            h.data = null;
            Node w = h;
            x = 1;
            for (int j = 0; j < width - 1; ++j)
            {
                //Rows are linked left/right
                w.right = new Node();
                w.right.x = x;
                w.right.y = y;
                w.right.left = w;
                w = w.right;
                w.data = null;
                //Build the connections Up/Down for the rest of the width
                if (over != null)
                {
                    over.down = w;
                    w.up = over;
                    over = over.right;
                }
                x++;
            }
            y++;
        }
    }

}
