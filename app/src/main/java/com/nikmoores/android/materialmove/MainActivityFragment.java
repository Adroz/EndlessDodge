package com.nikmoores.android.materialmove;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.daimajia.easing.Glider;
import com.daimajia.easing.Skill;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import java.util.List;
import java.util.Random;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    private static List<TypedArray> colourArray;
    private static ColourSet colourSet;

    /*
     *Animation constants
     */
    final static int DEATH_ANIMATION_SPEED = 1000;
    final static int RESTART_ANIMATION_SPEED = 1000;
    final static int START_ANIMATION_SPEED = 400;
    final static int START_ANIMATION_LONG_DELAY = 400;
    final static int START_ANIMATION_SHORT_DELAY = 0;


    private boolean running = false;

    // Screen and View dimensioning
    private static int maxBackgroundSize;
    private static int screenX;
    private static int screenY;
    private static int[] fabStartLocation = new int[2];
    private static int[] fabEndLocation = new int[]{0, 0};
    private static int fabRadius;
    private static int statusBarOffset;  // Only > 0 when API is <21 (should be tested and confirmed).

    private static boolean animationFlag = false;


    private FloatingActionButton mFab;
    private View mTempToolbar;
    private View mTempBackground;
    private View mToolbarView;
    private Toolbar mToolbar;


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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.game_fragment, container, false);

        initViews(rootView, savedInstanceState);

        initDimensions();

        initColours();

        // Register BroadcastReceiver.
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mStateChangedReceiver,
                new IntentFilter(Utilities.INTENT_FILTER));

        // TODO: Try to get this working in the future. (FAB animates in on screen rotate).
//        animateFabIn(START_ANIMATION_LONG_DELAY);

        return rootView;
    }

    private void initViews(View view, Bundle savedInstanceState) {
        int statusBarHeight = Utilities.getStatusBarHeight(getContext());
        int actionBarHeight = Utilities.getActionBarHeight(getContext());

        // Set toolbar (ActionBar/AppBar)
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.app_name));
        mToolbar.setPadding(0, statusBarHeight, 0, 0);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        // Temporary views used to hide SurfaceView canvas and toolbar.
        mTempToolbar = view.findViewById(R.id.temp_toolbar);
        mTempToolbar.setPadding(0, statusBarHeight, 0, 0);
        mTempToolbar.setMinimumHeight(actionBarHeight + statusBarHeight);

        mToolbarView = view.findViewById(R.id.toolbar_bg);
        mToolbarView.setMinimumHeight(actionBarHeight + statusBarHeight);

        mTempBackground = view.findViewById(R.id.temp_bg);

        // GameView, extends SurfaceView to provide game animations and logic.
        mGameView = (GameView) view.findViewById(R.id.game_view);

        mGameLoop = mGameView.getThread();

        //TODO: For testing, delete me.
        // give the LunarView a handle to the TextView used for messages
        mGameView.setTextView((TextView) view.findViewById(R.id.test_text));

        if (savedInstanceState == null) {
            // Game just started, therefore set up a new game.
            mGameLoop.setState(GameLoop.STATE_READY);
            Log.w(LOG_TAG, "sIS is null");
        } else {
            // Game is being restored, therefore resume the previous game.
            mGameLoop.restoreState(savedInstanceState);
            Log.w(LOG_TAG, "sIS is nonnull");
        }

        // FloatingActionButton starts game on click, and is disabled until game ends.
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!running) {
                    onGameStart();
//                    mFab.setOnClickListener(null);     //TODO: Uncomment
                } else {
                    onGameStop();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver((mStateChangedReceiver), new IntentFilter(Utilities.INTENT_FILTER));
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "onPause called.");
        // When Fragment is paused, pause game.
        mGameView.getThread().pause();
        super.onPause();
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(mStateChangedReceiver);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mGameLoop.saveState(outState);
        Log.w(LOG_TAG, "sIS called");
    }

    private void initDimensions() {
        // Get screen dimensions.
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenX = dm.widthPixels;
        screenY = dm.heightPixels;

        // Code segment runs after FAB View is drawn to screen (otherwise some values are zero).
        mFab.post(new Runnable() {
            @Override
            public void run() {
                // Get FAB start and end locations, and radius
                mFab.getLocationOnScreen(fabStartLocation);
                fabRadius = mFab.getHeight() / 2;
                fabEndLocation[0] = (screenX - mFab.getHeight()) / 2;
                fabEndLocation[1] = screenY - (screenY - mFab.getHeight()) / 4;
            }
        });

        mTempBackground.post(new Runnable() {
            @Override
            public void run() {
                // Get the final radius for the clipping circle
                maxBackgroundSize = Math.max(mTempBackground.getWidth(), mTempBackground.getHeight());
                statusBarOffset = screenY - mTempBackground.getHeight();
                fabEndLocation[1] -= statusBarOffset; // Doesn't matter what view finishes first.
            }
        });
    }

    private void initColours() {
        // Initialise colourArray before attempting to use any colours.
        colourArray = Utilities.getMultiTypedArray(getContext(), "colour");

        // A means to save colour configuration when screen is rotated between games.
        if (colourSet == null) {
            // Set new colour pairing.
            colourSet = new ColourSet();
        }

        // Set View colours
        mGameLoop.setColour(colourSet.primaryColour);
        setToolbarColour(colourSet.primaryColourDark);
        mTempBackground.setBackgroundColor(colourSet.primaryColour);
        mTempToolbar.setBackgroundColor(colourSet.primaryColourDark);
        mFab.setBackgroundTintList(ColorStateList.valueOf(colourSet.secondaryColour));
    }

    private void onGameStart() {
        Log.v(LOG_TAG, "onGameStart, game started.");
        running = true;

        // TODO: Fix this so I can support API 16.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // TODO: Possibly add colour change animation - make FAB start as a 500 colour (darker).
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
                // Set game state to start.
                mGameLoop.setState(GameLoop.STATE_RUNNING);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mFab.startAnimation(animation);

        mTempToolbar.setVisibility(View.INVISIBLE);
        mTempBackground.setVisibility(View.INVISIBLE);

        // TODO: Implement this.
//        Animation animFadeOut = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
//        animFadeOut.setDuration(500);
//        mTempToolbar.animate().alpha(0).setDuration(500).setListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                mTempToolbar.setVisibility(View.INVISIBLE);
//                super.onAnimationEnd(animation);
//            }
//        });

        mTempToolbar.setBackgroundColor(colourSet.secondaryColourDark);
        mTempBackground.setBackgroundColor(colourSet.secondaryColour);
    }

    private void onGamePause() {
        mGameLoop.setState(GameLoop.STATE_PAUSE);
    }

    public void onGameStop() {
        Log.v(LOG_TAG, "onGameStop, game reset.");

        // TODO: Fix Me
        mGameLoop.setState(GameLoop.STATE_END);
        running = false;

        // Generate new colours as the FAB will get a new colour.
        colourSet.setGameColours();

        // Start the reveal animations for background and toolbar.
        animationFlag = false;  // Reset animation flag.
        animateBackgroundReveal(mTempBackground).start();
        animateBackgroundReveal(mTempToolbar).start();

        // Modify FAB: make invisible, reset the animation, and change colour.
        mFab.setVisibility(View.INVISIBLE);
        mFab.clearAnimation();
        mFab.setBackgroundTintList(ColorStateList.valueOf(colourSet.secondaryColour));

        // Unlock screen rotation.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
                    mGameLoop.setColour(colourSet.primaryColour);
                    setToolbarColour(colourSet.primaryColourDark);
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
                    onGameStop();
                    break;
                default:
                    Log.v(LOG_TAG, "Unknown state received.");
            }
        }
    };

    public class ColourSet {

        private final static int MAIN_COLOUR_LOCATION = 3;
        private final static int DARK_COLOUR_LOCATION = 7;

        public TypedArray primarySet;
        public TypedArray secondarySet;

        public int secondaryColour;
        public int secondaryColourDark;
        public int primaryColour;
        public int primaryColourDark;

        public ColourSet() {
            secondarySet = colourArray.get(getRandomColourSet());
            setGameColours();
        }

        public void setGameColours() {
            primaryColour = getSecondaryColour(MAIN_COLOUR_LOCATION);
            primaryColourDark = getSecondaryColour(DARK_COLOUR_LOCATION);
            primarySet = secondarySet;

            // If primary and secondary colours are clashing, choose another random value.
            do {
                secondarySet = colourArray.get(getRandomColourSet());
            } while (!isGoodColourPair());

            secondaryColour = getSecondaryColour(MAIN_COLOUR_LOCATION);
            secondaryColourDark = getSecondaryColour(DARK_COLOUR_LOCATION);
        }

        private boolean isGoodColourPair() {
            if (secondarySet.getInt(MAIN_COLOUR_LOCATION, 0) == primaryColour) {
                return false;
            }
            // TODO: Add support for rejecting too-similar colours.
            return true;
        }

        private int getRandomColourSet() {
            return new Random().nextInt(colourArray.size());
        }

        public int getColour(TypedArray colour, int colourLevel) {
            return colour.getInt(colourLevel, 0);
        }

        public int getPrimaryColour(int colourLevel) {
            return getColour(primarySet, colourLevel);
        }

        public int getSecondaryColour(int colourLevel) {
            return getColour(secondarySet, colourLevel);
        }
    }
}
