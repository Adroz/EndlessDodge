package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * The thread that draws the animation
     */
    private GameLoop gameLoop;

    private Context mContext;

    ThreadListener mListener = null;

    public interface ThreadListener {
        void updateGameLoop();
    }


    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // Register to listen to SurfaceView changes.
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        mContext = context;

        // Create game loop thread. Actually started in SurfaceCreated().
        gameLoop = new GameLoop(holder, context);
        // To ensure key events received.
        setFocusable(true);
    }

    public void setListener(ThreadListener l) {
        mListener = l;
    }


    public GameLoop getThread() {
        return gameLoop;
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. (Switching applications, incoming call, etc).
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) gameLoop.pause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (gameLoop.getState() != Thread.State.NEW) {
            gameLoop = new GameLoop(holder, mContext);
            mListener.updateGameLoop();
        }
        gameLoop.setRunning(true);
        gameLoop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        gameLoop.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Tell thread to shutdown and wait, or it might interact with the Surface and result
        // in something breaking.
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
}