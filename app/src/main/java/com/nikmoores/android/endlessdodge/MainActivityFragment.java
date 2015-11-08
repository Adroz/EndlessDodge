package com.nikmoores.android.endlessdodge;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.easing.Glider;
import com.daimajia.easing.Skill;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import java.util.List;
import java.util.Random;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

import static com.nikmoores.android.endlessdodge.Utilities.screenHeight;
import static com.nikmoores.android.endlessdodge.Utilities.screenWidth;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements View.OnClickListener,
        SensorEventListener, GameView.ThreadListener {

    final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    public static final int CURRENT_SCORE = 0;
    public static final int WORLD_SCORE = 1;
    public static final int SOCIAL_SCORE = 2;
    public static final int USER_SCORE = 3;

    /*
     *Animation constants
     */
    final static int DEATH_ANIMATION_SPEED = 1000;
    final static int RESTART_ANIMATION_SPEED = 1000;
    final static int START_ANIMATION_SPEED = 400;
    final static int START_ANIMATION_LONG_DELAY = 400;
    final static int START_ANIMATION_SHORT_DELAY = 0;

    final static int NO_FADE = 0;
    final static int FADE_IN = 1;
    final static int FADE_OUT = 2;

    private static List<int[]> colourArray;
    private static ColourSet colourSet;

    // Screen and View dimensioning
    private static int maxBackgroundSize;
    //    private static int screenX;
//    private static int screenY;
    public static int[] fabStartLocation = new int[2];
    public static int[] fabEndLocation = new int[]{0, 0};
    private static int fabRadius;
    private static int statusBarOffset;  // Only > 0 when API is <21 (should be tested and confirmed).

    private static boolean animationFlag = false;


    private FloatingActionButton mFab;
    private View mTempToolbar;
    private LinearLayout mAltBackground;
    private View mToolbarView;

    private TextView mWorldScore;
    private TextView mSocialScore;
    private TextView mUserScore;
    private TextView mCurrentScore;

    //    private TextView xValue;
//    private TextView yValue;
//    private TextView zValue;
    private SensorManager sensorManager = null;
    private int mXValue;

    /**
     * A handle to the thread that's actually running the animation.
     */
    private GameLoop mGameLoop;

    /**
     * A handle to the View in which the game is running.
     */
    private GameView mGameView;

    public MainActivityFragment() {

    }

    public interface Listener {
        void updateAccomplishments(int score);
    }

    Listener mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.game_fragment, container, false);

        Utilities.setScreenDimensions(getContext());
        initViews(rootView, savedInstanceState);
        initDimensions();
        initColours();

        // TODO: Try to get this working in the future. (FAB animates in on screen rotate).
//        animateFabIn(START_ANIMATION_LONG_DELAY);

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);

        return rootView;
    }

    private void initViews(View view, Bundle savedInstanceState) {
        int statusBarHeight = Utilities.getStatusBarHeight(getContext());
        int actionBarHeight = Utilities.getActionBarHeight(getContext());

        // Set toolbar (ActionBar/AppBar)
        Toolbar mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.app_name));
        mToolbar.setPadding(0, statusBarHeight, 0, 0);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        // Temporary views used to hide SurfaceView canvas and toolbar.
        mTempToolbar = view.findViewById(R.id.temp_toolbar);
        mTempToolbar.setPadding(0, statusBarHeight, 0, 0);
        mTempToolbar.setMinimumHeight(actionBarHeight + statusBarHeight);

        mToolbarView = view.findViewById(R.id.toolbar_bg);
        mToolbarView.setMinimumHeight(actionBarHeight + statusBarHeight);

        mAltBackground = (LinearLayout) view.findViewById(R.id.alt_background);

        // Initialise scores.
        mWorldScore = (TextView) view.findViewById(R.id.world_score);
        mSocialScore = (TextView) view.findViewById(R.id.social_score);
        mUserScore = (TextView) view.findViewById(R.id.user_score);
        mCurrentScore = (TextView) view.findViewById(R.id.current_score);
        mCurrentScore.setText("0");

        // GameView, extends SurfaceView to provide game animations and logic.
        mGameView = (GameView) view.findViewById(R.id.game_view);

        mGameView.setListener(this);
        mGameLoop = mGameView.getThread();

        //TODO: For testing, delete me.
//        mGameView.setTextView((TextView) view.findViewById(R.id.test_text));
//        xValue = (TextView) view.findViewById(R.id.x_value);
//        yValue = (TextView) view.findViewById(R.id.y_value);
//        zValue = (TextView) view.findViewById(R.id.z_value);

        if (savedInstanceState == null) {
            // Game just started, therefore set up a new game.
            mGameLoop.setState(GameLoop.STATE_END);
            Log.w(LOG_TAG, "sIS is null");
        } else {
            // Game is being restored, therefore resume the previous game.
            mGameLoop.restoreState(savedInstanceState);
            Log.w(LOG_TAG, "sIS is nonnull");
        }

        // FloatingActionButton starts game on click, and is disabled until game ends.
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(this);
    }

    public void updateGameLoop() {
        mGameLoop = mGameView.getThread();
        mGameLoop.setState(GameLoop.STATE_READY);
        mGameLoop.setColour(colourSet.primarySet);
        mGameLoop.setFabData(fabEndLocation[0], fabEndLocation[1], fabRadius);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mStateChangedReceiver, new IntentFilter(Utilities.INTENT_FILTER));
        // Register this class as a listener for the accelerometer sensor
        sensorManager
                .registerListener(this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "onPause called.");
        // When Fragment is paused, pause game.
        mGameLoop.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume called.");
        super.onResume();
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(mStateChangedReceiver);
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mGameLoop.saveState(outState);
        Log.w(LOG_TAG, "sIS called");
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    private void initDimensions() {
        // Code segment runs after FAB View is drawn to screen (otherwise some values are zero).
        mFab.post(new Runnable() {
            @Override
            public void run() {
                // Get FAB start and end locations, and radius
                mFab.getLocationOnScreen(fabStartLocation);
                fabRadius = mFab.getHeight() / 2;
                fabEndLocation[0] = (screenWidth - mFab.getHeight()) / 2;
                fabEndLocation[1] = screenHeight - (screenWidth - mFab.getHeight()) / 4;
                mGameLoop.setFabData(fabEndLocation[0], fabEndLocation[1], fabRadius);
                Utilities.setFabMeasurements(fabEndLocation[0], fabEndLocation[1], fabRadius);
            }
        });

        mAltBackground.post(new Runnable() {
            @Override
            public void run() {
                // Get the final radius for the clipping circle
                maxBackgroundSize = Math.max(mAltBackground.getWidth(), mAltBackground.getHeight());
                statusBarOffset = screenHeight - mAltBackground.getHeight();
                fabEndLocation[1] -= statusBarOffset; // Doesn't matter what view finishes first.
            }
        });
    }

    private void initColours() {
        // Initialise colourArray before attempting to use any colours.
        colourArray = Utilities.get2dResourceArray(getContext(), "colour");

        // A means to save colour configuration when screen is rotated between games.
        if (colourSet == null) {
            // Set new colour pairing.
            colourSet = new ColourSet();
        }

        // Set View colours
        mGameLoop.setColour(colourSet.primarySet);
        setToolbarColour(colourSet.primaryColourDark);
        mAltBackground.setBackgroundColor(colourSet.primaryColour);
        mTempToolbar.setBackgroundColor(colourSet.primaryColourDark);
        mFab.setBackgroundTintList(ColorStateList.valueOf(colourSet.secondaryColour));
    }

    private void onGameStart() {
        Log.v(LOG_TAG, "onGameStart, game started.");

        // Randomise initial acceleration direction.
        int[] val = new int[]{-1, 1};
        mXValue = val[new Random().nextInt(val.length)];
        mGameLoop.setDirection(mXValue);

        // Disable FAB while the game is running. // TODO: Still need to clean this up, button is not in centre.
        mFab.setOnClickListener(null);

        // TODO: Fix this so I can support API 16. (1/2)
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // TODO: Possibly add colour change animation - make FAB start as a 500 colour (darker).
        // TODO: Create a curved path for the animation and make the movement non-linear.
        // Animate to game start position.
        TranslateAnimation animation = new TranslateAnimation(
                0, fabEndLocation[0] - fabStartLocation[0],
                0, fabEndLocation[1] - fabStartLocation[1]);
        animation.setDuration(START_ANIMATION_SPEED);
        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Tell game to start.
                mGameLoop.doStart();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mFab.startAnimation(animation);

        animateBackgroundFade(mAltBackground, colourSet.secondaryColour);

        animateBackgroundFade(mTempToolbar, colourSet.secondaryColourDark);
    }

    private void onGamePause() {
        mGameLoop.setState(GameLoop.STATE_PAUSE);
    }

    public void onGameStop() {
        Log.v(LOG_TAG, "onGameStop, game reset.");

        int finalScore = mGameLoop.getCurrentScore();

        // TODO: Fix Me
        mGameLoop.setState(GameLoop.STATE_END);

        // Set score.
        mListener.updateAccomplishments(finalScore);

        // Generate new colours as the FAB will get a new colour.
        colourSet.setGameColours();

        // Start the reveal animations for background and toolbar.
        animationFlag = false;  // Reset animation flag.
        animateBackgroundReveal(mAltBackground).start();
        animateBackgroundReveal(mTempToolbar).start();

        // Modify FAB: make invisible, reset the animation, and change colour.
        mFab.setVisibility(View.INVISIBLE);
        mFab.clearAnimation();
        mFab.setBackgroundTintList(ColorStateList.valueOf(colourSet.secondaryColour));

        mFab.setOnClickListener(this);
    }

    public void updateScoreViews(int view, float score, int fadeType) {
//        Log.d(LOG_TAG, "Updating score (" + view + "): " + score);
        TextView textView = mCurrentScore;
        if (view == WORLD_SCORE) {
            textView = mWorldScore;
        } else if (view == SOCIAL_SCORE) {
            textView = mSocialScore;
        } else if (view == USER_SCORE) {
            textView = mUserScore;
        }
        textView.setText(String.format("%d", (int) score));
        if (fadeType == FADE_IN) {
            textView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        } else if (fadeType == FADE_OUT) {
            textView.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        }

    }

    private void animateBackgroundFade(final View view, final int colour) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(START_ANIMATION_SPEED);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
                view.setBackgroundColor(colour);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(alphaAnimation);
//        return alphaAnimation;
    }

    private SupportAnimator animateBackgroundReveal(final View view) {
        // Create animator for view. Start radius is the FAB size.
        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                view,                           // The view to animate
                fabEndLocation[0] + fabRadius,  // Centre of reveal X
                fabEndLocation[1] + fabRadius,  // Centre of reveal Y
                fabRadius,                      // Start radius
                maxBackgroundSize);             // End radius
        animator.setDuration(DEATH_ANIMATION_SPEED);      // Duration
        animator.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd() {
                if (!animationFlag) {
                    // If this animation has finished first, animate the FAB in from the bottom
                    // of the screen. Also reset the game to the READY state.
                    animateFabIn(START_ANIMATION_SHORT_DELAY);
                    mGameLoop.setState(GameLoop.STATE_READY);
                    mGameLoop.setColour(colourSet.primarySet);
                    setToolbarColour(colourSet.primaryColourDark);
                    // Unlock screen rotation. // TODO: Fix this so I can support API 16. (2/2)
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
                animationFlag = true;
            }

            @Override
            public void onAnimationCancel() {

            }

            @Override
            public void onAnimationRepeat() {

            }
        });
        return animator;
    }

    private void animateFabIn(long startDelay) {
        // If this animation has finished first, animate the FAB in from the bottom
        // of the screen.
        AnimatorSet set = new AnimatorSet();
        set.playTogether(Glider.glide(
                Skill.ElasticEaseOut,
                1,
                ObjectAnimator.ofFloat(mFab, "translationY", fabRadius * 3, 0)));
        set.setDuration(RESTART_ANIMATION_SPEED);
        set.setStartDelay(startDelay);
        set.start();
        mFab.setVisibility(View.VISIBLE);
    }

    private void setToolbarColour(int colour) {
        mToolbarView.setBackgroundColor(colour);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getActivity().setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, colour));
        }
    }

    private BroadcastReceiver mStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Utilities.STATE_KEY, GameLoop.STATE_PAUSE)) {
                case GameLoop.STATE_END:
                    // Stopping game from broadcast receiver.
                    onGameStop();
                    break;
                default:
                    Log.v(LOG_TAG, "Unknown state received.");
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:      // Handle FAB click listener.
//                if (!running) {
                onGameStart();
//                } else {
//                    onGameStop();
//                }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                xValue.setText(Float.toString(event.values[0]));
//                yValue.setText(Float.toString(event.values[1]));
//                zValue.setText(Float.toString(event.values[2]));

                // TODO: Add support for landscape mode.

                // IMPORTANT: mDirection is equal to negative direction. This helps with drawing
                // (ie: right is +ve, just as in the canvas).
                int xDirection = (-Math.ceil(event.values[0]) < 0) ? 1 : -1;
                if (mXValue != xDirection) {
                    mXValue = xDirection;
                    mGameLoop.setDirection(mXValue);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class ColourSet {
        public int[] primarySet;
        public int[] secondarySet;

        public int secondaryColour;
        public int secondaryColourDark;
        public int primaryColour;
        public int primaryColourDark;

        public ColourSet() {
            secondarySet = colourArray.get(getRandomColourSet());
            setGameColours();
        }

        public void setGameColours() {
            primaryColour = getSecondaryColour(Utilities.COLOUR_LOCATION_MAIN);
            primaryColourDark = getSecondaryColour(Utilities.COLOUR_LOCATION_DARK);
            primarySet = secondarySet;

            // If primary and secondary colours are clashing, choose another random value.
            do {
                secondarySet = colourArray.get(getRandomColourSet());
            } while (!isGoodColourPair());

            secondaryColour = getSecondaryColour(Utilities.COLOUR_LOCATION_MAIN);
            secondaryColourDark = getSecondaryColour(Utilities.COLOUR_LOCATION_DARK);
        }

        private boolean isGoodColourPair() {
            if (secondarySet[Utilities.COLOUR_LOCATION_MAIN] == primaryColour) {
                return false;
            }
            // TODO: Add support for rejecting too-similar colours.
            return true;
        }

        private int getRandomColourSet() {
            return new Random().nextInt(colourArray.size());
        }

        public int getColour(int[] colour, int colourLevel) {
            return colour[colourLevel];
        }

        public int getPrimaryColour(int colourLevel) {
            return getColour(primarySet, colourLevel);
        }

        public int getSecondaryColour(int colourLevel) {
            return getColour(secondarySet, colourLevel);
        }
    }
}
