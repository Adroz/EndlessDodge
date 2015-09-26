package com.nikmoores.android.materialmove;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Created by Nik on 24/09/2015.
 */
public class Obstacle {
    private static final int FALLING_SPEED = 15;

    private static final int BMP_ROWS = 4;
    private static final int BMP_COLUMNS = 3;
    private int x = 0;
    private int y = 0;
    private int xSpeed = 5;
    private GameView gameView;
    private Bitmap bmp;
    private int currentFrame = 0;
    private int width;
    private int height;

    public Obstacle(GameView gameView, Bitmap bmp) {
        this.gameView = gameView;
        this.bmp = bmp;
        this.width = bmp.getWidth() / BMP_COLUMNS;
        this.height = bmp.getHeight() / BMP_ROWS;
    }

    public void restart(){
        x = 0;
        y = 0;
        xSpeed = 5;
    }

    private void update() {
        if ((x > gameView.getWidth() - width - xSpeed || x + xSpeed < 0)) {
            xSpeed = -xSpeed;
        }
        x = x + xSpeed;
        y = y + FALLING_SPEED;
//        currentFrame = ++currentFrame % BMP_COLUMNS;
    }

    public void draw(Canvas canvas) {
        update();
//        int srcX = currentFrame * width;
//        int srcY = 1 * height;
//        Rect src = new Rect(srcX, srcY, srcX + width, srcY + height);
//        Rect dst = new Rect(x, y, x + width, y + height);
        canvas.drawBitmap(bmp, x, y, null);
    }

}
