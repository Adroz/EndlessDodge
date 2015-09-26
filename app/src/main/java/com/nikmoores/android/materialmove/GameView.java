package com.nikmoores.android.materialmove;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameView extends SurfaceView {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    private Bitmap bmp;
    private SurfaceHolder holder;
    private GameLoop gameLoop;
    private Obstacle obstacle;

    private boolean isReady = false;


    private int x = 1;
    private int xSpeed = 15;

    private int mColour = Color.BLACK;


    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        gameLoop = new GameLoop(this);
        holder = getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                boolean retry = true;
                gameLoop.setRunning(false);
                while (retry) {
                    try {
                        gameLoop.join();
                        retry = false;
                    } catch (InterruptedException e) {
                        Log.e(LOG_TAG, "Error destroying GameView: " + e.toString());
                    }
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                isReady = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }
        });
        bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        obstacle = new Obstacle(this, bmp);
    }

    public void setColour(int colour) {
//        Log.v(LOG_TAG, "setColour: " + colour);
        mColour = colour;
    }

    public void start() {
        if (isReady) {
            obstacle.restart();
            gameLoop = new GameLoop(this);
            gameLoop.setRunning(true);
            gameLoop.start();
        }
    }

    public void stop() {
        gameLoop.setRunning(false);
//        gameLoop.interrupt();
    }

    protected void render(Canvas canvas) {
//        Log.v(LOG_TAG, "render called.");
        if (canvas != null) {
            canvas.drawColor(mColour);
            obstacle.draw(canvas);
        }
    }
}