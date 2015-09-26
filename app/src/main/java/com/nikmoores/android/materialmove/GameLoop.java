package com.nikmoores.android.materialmove;

import android.graphics.Canvas;
import android.util.Log;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameLoop extends Thread {

    final String LOG_TAG = MainActivity.class.getSimpleName();


    static final long FPS = 60;
    private GameView view;
    private boolean running = false;

    public GameLoop(GameView view) {
        this.view = view;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        Log.v(LOG_TAG, "GameLoop run() Called");
        long ticksPS = 1000 / FPS;
        long startTime;
        long sleepTime;
        while (running) {
//            Log.v(LOG_TAG, "GameLoop loop: " + running);
            Canvas c = null;
            startTime = System.currentTimeMillis();
            try {
                c = view.getHolder().lockCanvas();
                synchronized (view.getHolder()) {
                    view.render(c);
                }
            } finally {
                if (c != null) {
                    view.getHolder().unlockCanvasAndPost(c);
                }
            }
            sleepTime = ticksPS-(System.currentTimeMillis() - startTime);
            try {
                if (sleepTime > 0)
                    sleep(sleepTime);
                else
                    sleep(10);
            } catch (Exception e) {}
        }

    }
}
