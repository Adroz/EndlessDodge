package com.nikmoores.android.materialmove;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.util.Random;

/**
 * Created by Nik on 24/09/2015.
 */
public class WallPair {

    private static final int MAX_HEIGHT = 200;
    private static final int MIN_HEIGHT = 150;


    private int height;
    private int gap;
    private int xOffset;
    private int yOffset;

    private static final int BMP_ROWS = 4;
    private static final int BMP_COLUMNS = 3;
    private int x = 0;
    private int y = 0;
    private int xSpeed = 5;
    private GameView gameView;
    private Bitmap bmp;

    public WallPair() {
        this.height = new Random().nextInt(MAX_HEIGHT - MIN_HEIGHT +  1) + MIN_HEIGHT;


        Log.d("WallPair", "height = " + height);
    }

    public void restart(){
        x = 0;
        y = 0;
        xSpeed = 5;
    }

    private void update() {
//        if ((x > gameView.getWidth() - width - xSpeed || x + xSpeed < 0)) {
//            xSpeed = -xSpeed;
//        }
//        x = x + xSpeed;
//        y = y + FALLING_SPEED;
//        currentFrame = ++currentFrame % BMP_COLUMNS;
    }

    public void draw(Canvas canvas, int y) {
//        update();
//        int srcX = currentFrame * width;
//        int srcY = 1 * height;
//        Rect src = new Rect(srcX, srcY, srcX + width, srcY + height);
//        Rect dst = new Rect(mX, mY, mX + width, mY + height);
//        canvas.drawBitmap(bmp, x, y, null);

        canvas.drawRect(mScratchRect, mLinePaint);
    }

    public RectF getWall(int wall){
        return
    }

}
