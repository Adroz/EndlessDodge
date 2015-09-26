package com.nikmoores.android.materialmove;

import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;

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

    final static int DEATH_SPEED = 1000;
    final static int RESTART_SPEED = 1000;
    final static int START_SPEED = 400;


    private boolean running = false;

    // Screen and View dimensioning
    private static int maxBackgroundSize;
    private static int screenX;
    private static int screenY;
    private static int[] fabStartLocation = new int[2];
    private static int[] fabEndLocation = new int[]{0, 0};
    private static int fabRadius;
    private static int statusBarOffset;  // Only > 0 when API < 21 (should be tested and confirmed).

    private static boolean animationFlag = false;

    private FloatingActionButton mFab;
    private static GameView gameView;
    private View mTempToolbar;
    private View mTempBackground;
    private View mToolbarView;
    private Toolbar mToolbar;

    public MainActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.game_fragment, container, false);

        initViews(rootView);

        initDimensions();

        initColours();

        return rootView;
    }

    private void initViews(View view) {
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
        gameView = (GameView) view.findViewById(R.id.game_view);

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
                fabEndLocation[0] = (screenX - mFab.getMeasuredWidth()) / 2;
                fabEndLocation[1] = screenY - (screenY - mFab.getMeasuredHeight()) / 4;
                fabRadius = mFab.getHeight() / 2;
            }
        });

        mTempBackground.post(new Runnable() {
            @Override
            public void run() {
                // Get the final radius for the clipping circle
                maxBackgroundSize = Math.max(mTempBackground.getWidth(), mTempBackground.getHeight());
                statusBarOffset = screenY - mTempBackground.getMeasuredHeight();
                fabEndLocation[1] -= statusBarOffset; // Doesn't matter what view finishes first.
            }
        });
    }

    private void initColours() {
        // Initialise colourArray before attempting to use any colours.
        colourArray = Utilities.getMultiTypedArray(getContext(), "colour");

        // Set current colour pairing.
        colourSet = new ColourSet();

        // Set View colours
        gameView.setColour(colourSet.primaryColour);
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

        animateFab(running);

        gameView.start();


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

    }

    private void onGameStop() {
        Log.v(LOG_TAG, "onGameStop, game reset.");

        gameView.stop();

        running = false;

        // Generate new colours as the FAB will get a new colour.
        colourSet.setGameColours();

        // Animate the reveal.
        animateReveal();

        // Change colour.
        mFab.setBackgroundTintList(ColorStateList.valueOf(colourSet.secondaryColour));

        // Unlock screen rotation.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void animateFab(boolean starting) {
        if (starting) {
            // Animate to game start position.
            TranslateAnimation animation = new TranslateAnimation(
                    0, fabEndLocation[0] - fabStartLocation[0],
                    0, fabEndLocation[1] - fabStartLocation[1]);
            animation.setDuration(START_SPEED);
            animation.setFillAfter(true);
            mFab.startAnimation(animation);
        } else {
            // Animate in.
            int height = mFab.getMeasuredHeight();
            AnimatorSet set = new AnimatorSet();
            // TODO: Better calculate the entry point for the animation.
            set.playTogether(Glider.glide(
                    Skill.ElasticEaseOut,
                    RESTART_SPEED,
                    ObjectAnimator.ofFloat(mFab, "translationY", height + mFab.getHeight() / 2, 0)));
            set.setDuration(RESTART_SPEED);
            set.start();
            mFab.setVisibility(View.VISIBLE);
        }
    }

    private void animateReveal() {
        // Start the animations for background and toolbar.
        animationFlag = false;  // Reset animation flag.
        animateBackgroundReveal(mTempBackground).start();
        animateBackgroundReveal(mTempToolbar).start();

        mFab.setVisibility(View.INVISIBLE);
        mFab.clearAnimation();
    }

    private SupportAnimator animateBackgroundReveal(final View view) {
        // Create animator for view. Start radius is the FAB size.
        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                view,                           // The view to animate
                fabEndLocation[0] + fabRadius,  // Centre of reveal X
                fabEndLocation[1] + fabRadius,  // Centre of reveal Y
                mFab.getMeasuredHeight() / 2,   // Start radius
                maxBackgroundSize);             // End radius
        animator.setDuration(DEATH_SPEED);      // Duration
        animator.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd() {
                if (!animationFlag) animateFab(running);
                animationFlag = !animationFlag;
                gameView.setColour(colourSet.previousSecondaryColour);
                setToolbarColour(colourSet.previousSecondaryColourDark);
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

    private void setToolbarColour(int colour) {
        mToolbarView.setBackgroundColor(colour);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getActivity().setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, colour));
        }
    }

    public class ColourSet {

        private final static int MAIN_COLOUR_LOCATION = 3;
        private final static int DARK_COLOUR_LOCATION = 7;

        public TypedArray primarySet;
        public TypedArray secondarySet;

        public int primaryColour;
        public int primaryColourDark;
        public int secondaryColour;
        public int secondaryColourDark;
        public int previousSecondaryColour;
        public int previousSecondaryColourDark;

        public int activeColourSetIndex;


        public ColourSet() {
            secondarySet = colourArray.get(getRandomColourSet());
            setGameColours();
            previousSecondaryColour = getSecondaryColour(MAIN_COLOUR_LOCATION);
            previousSecondaryColourDark = getSecondaryColour(DARK_COLOUR_LOCATION);
        }

        public void setGameColours() {
            previousSecondaryColour = getSecondaryColour(MAIN_COLOUR_LOCATION);
            previousSecondaryColourDark = getSecondaryColour(DARK_COLOUR_LOCATION);

            activeColourSetIndex = getRandomColourSet();

            // TODO: Add checks for colour clashes.
            primarySet = colourArray.get(activeColourSetIndex);
            secondarySet = colourArray.get(getRandomColourSet());

            primaryColour = getPrimaryColour(MAIN_COLOUR_LOCATION);
            primaryColourDark = getPrimaryColour(DARK_COLOUR_LOCATION);

            secondaryColour = getSecondaryColour(MAIN_COLOUR_LOCATION);
            secondaryColourDark = getSecondaryColour(DARK_COLOUR_LOCATION);

//            previousMajorColour = minorColour;
//            previousMajorColourDark = ContextCompat.getColor(getContext(), colourIdGrid[1][minorColourIndex]);
//
//            majorColourIndex = new Random().nextInt(colours.length);
//            minorColourIndex = new Random().nextInt(colours.length);

//            majorColour = ContextCompat.getColor(getContext(), colourIdGrid[0][majorColourIndex]);
//            majorColourDark = ContextCompat.getColor(getContext(), colourIdGrid[1][majorColourIndex]);
//            minorColour = ContextCompat.getColor(getContext(), colourIdGrid[0][minorColourIndex]);
        }

        public int getRandomColourSet() {
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
