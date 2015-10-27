package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * Created by Nik on 24/09/2015.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * The thread that actually draws the animation
     */
    private GameLoop gameLoop;


    // Testing TextView.
    private TextView mStatusText;


    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // Register to listen to SurfaceView changes.
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // Create game loop thread. Actually started in SurfaceCreated().
        gameLoop = new GameLoop(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                //noinspection ResourceType
//                mStatusText.setVisibility(m.getData().getInt("viz"));
//                mStatusText.setText(m.getData().getString("text"));
            }
        });
        // To ensure key events received.
        setFocusable(true);
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

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameLoop.setRunning(true);
        gameLoop.start();           // TODO: This can still cause an error apparently. Investigate.
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
//                Log.e(LOG_TAG, "Error destroying GameView: " + e.toString());
            }
        }
    }
}