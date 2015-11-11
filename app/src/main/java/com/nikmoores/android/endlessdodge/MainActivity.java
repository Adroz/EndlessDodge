package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

import static com.nikmoores.android.endlessdodge.MainActivityFragment.CURRENT_SCORE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.FADE_IN;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.FADE_OUT;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.Listener;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.NO_FADE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.SOCIAL_SCORE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.USER_SCORE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.WORLD_SCORE;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Listener {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    MainActivityFragment mMainActivityFragment;

    private Menu menu;

    public static final String WORLD_BEST = "WB";
    public static final String SOCIAL_BEST = "WB";
    public static final String PERSONAL_BEST = "PB";
    public static final String LAST_SCORE = "last_score";
    public static final String TOTAL_DISTANCE = "total_distance";

    SharedPreferences preferences;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    // request codes we use when invoking an external activity
    private static final int RC_SIGN_IN = 9001;

    private static final int REQUEST_LEADERBOARD = 4000;
    private static final int REQUEST_ACHIEVEMENTS = 5000;

    // achievements and scores we're pending to push to the cloud
    // (waiting for the user to sign in, for instance)
    Outbox mOutbox = new Outbox();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getPreferences(Context.MODE_PRIVATE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
                .build();

        // FOR TESTING ONLY - Reset mOutbox
//        mOutbox.reset();
//        mOutbox.saveLocal();

        // Create fragment and listen
        mMainActivityFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        mMainActivityFragment.setListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        hideMenuItem(R.id.menu_sign_out);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.menu_sign_in:
                // TODO: Fix InputEventReciever issue here.
                mSignInClicked = true;
                mGoogleApiClient.connect();
//                mHandler.postDelayed(new setSigninState(mGoogleApiClient, true), 100);
                break;
            case R.id.menu_sign_out:
                mSignInClicked = false;
                mGoogleApiClient.disconnect();
//                mHandler.postDelayed(new setSigninState(mGoogleApiClient, false), 100);
                break;
            case R.id.menu_leaderboard:
                if (isSignedIn()) {
                    startActivityForResult(Games.Leaderboards.getLeaderboardIntent(
                                    mGoogleApiClient, getString(R.string.leaderboard_leaderboard)),
                            REQUEST_LEADERBOARD);
                }
                break;
            case R.id.menu_achievements:
                if (isSignedIn()) {
                    startActivityForResult(Games.Achievements.getAchievementsIntent(
                            mGoogleApiClient), REQUEST_ACHIEVEMENTS);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // load outbox from file
        mOutbox.loadLocal();
        updateAccomplishments(mOutbox.mScore);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOutbox.saveLocal();
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    private void setSignedInMode() {
        if (isSignedIn()) {
            showMenuItem(R.id.menu_sign_in);
            hideMenuItem(R.id.menu_sign_out);
            hideMenuItem(R.id.menu_leaderboard);
            hideMenuItem(R.id.menu_achievements);
            mMainActivityFragment.updateScoreViews(SOCIAL_SCORE, mOutbox.mSocialBest, FADE_IN);
            mMainActivityFragment.updateScoreViews(WORLD_SCORE, mOutbox.mWorldBest, FADE_IN);
        } else {
            hideMenuItem(R.id.menu_sign_in);
            showMenuItem(R.id.menu_sign_out);
            showMenuItem(R.id.menu_leaderboard);
            showMenuItem(R.id.menu_achievements);
            mMainActivityFragment.updateScoreViews(SOCIAL_SCORE, mOutbox.mSocialBest, FADE_OUT);
            mMainActivityFragment.updateScoreViews(WORLD_SCORE, mOutbox.mWorldBest, FADE_OUT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            Log.d(LOG_TAG, "onActivityResult: Attempting sign-in");
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "onActivityResult: RESULT_OK");
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected(): connected to Google APIs");
        // Show sign-out button on main menu
        hideMenuItem(R.id.menu_sign_in);
        showMenuItem(R.id.menu_sign_out);
        showMenuItem(R.id.menu_leaderboard);
        showMenuItem(R.id.menu_achievements);

        // if we have accomplishments to push, push them
        if (!mOutbox.isEmpty()) {
            pushAchievements();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended(): attempting to connect");
        setSignedInMode();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            Log.d(LOG_TAG, "onConnectionFailed(): already resolving");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(
                    this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error));
        }

        // Sign-in failed, so show sign-in button on main menu
        showMenuItem(R.id.menu_sign_in);
        hideMenuItem(R.id.menu_sign_out);
    }

    private void hideMenuItem(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showMenuItem(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    @Override
    public void updateAccomplishments(final int score) {
        mOutbox.mScore = score;
        mOutbox.mTotalDistance += score;
        mMainActivityFragment.updateScoreViews(CURRENT_SCORE, (float) mOutbox.mScore, NO_FADE);

        pushAchievements();
        pushLeaderboards();

        if (!isSignedIn()) {
            // can't push to the cloud, so save locally
            mOutbox.saveLocal();
            return;
        }

        // Pull user best score.
        Games.Leaderboards.loadCurrentPlayerLeaderboardScore(mGoogleApiClient,
                getString(R.string.leaderboard_leaderboard),
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC)
                .setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

                    @Override
                    public void onResult(Leaderboards.LoadPlayerScoreResult arg0) {
                        if (arg0 == null) return;
                        float score = arg0.getScore().getRawScore();
                        if (score > mOutbox.mBest) {
                            mOutbox.mBest = score;
                            mMainActivityFragment.updateScoreViews(USER_SCORE, mOutbox.mBest, NO_FADE);
                            mOutbox.saveLocal();
                        }
                    }
                });

        // Pull world best
        pullLeaderboards(LeaderboardVariant.COLLECTION_PUBLIC, mOutbox.mWorldBest, WORLD_SCORE);
        // Pull social best
        pullLeaderboards(LeaderboardVariant.COLLECTION_SOCIAL, mOutbox.mSocialBest, SOCIAL_SCORE);

        mOutbox.saveLocal();
    }

    private void pullLeaderboards(int collection, final float currentBestScore, final int view) {
        Games.Leaderboards.loadTopScores(mGoogleApiClient,
                getString(R.string.leaderboard_leaderboard),
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                collection,
                1).setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {
            @Override
            public void onResult(Leaderboards.LoadScoresResult loadScoresResult) {
                if (loadScoresResult == null) return;
                float topScore = loadScoresResult.getScores().get(0).getRawScore();
                if (topScore > currentBestScore) {
                    if (view == SOCIAL_SCORE) {
                        mOutbox.mSocialBest = topScore;
                    } else {
                        mOutbox.mWorldBest = topScore;
                    }
                    mMainActivityFragment.updateScoreViews(view, topScore, NO_FADE);
                    mOutbox.saveLocal();
                }
                loadScoresResult.release();
            }
        });
    }

    void pushAchievements() {
        if (mOutbox.mScore == 0) mOutbox.mCaughtNappingAchievement = true;
//        if (mOutbox.mScore == 0) mOutbox.mCaughtNappingAchievement = true;
//        if (mOutbox.mScore == 0) mOutbox.mCaughtNappingAchievement = true;

        if (!isSignedIn()) return;

        if (mOutbox.mCaughtNappingAchievement) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_caught_napping));
            mOutbox.mCaughtNappingAchievement = false;
        }
//        if (mOutbox.mArrogantAchievement) {
//            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_arrogant));
//            mOutbox.mArrogantAchievement = false;
//        }
//        if (mOutbox.mHumbleAchievement) {
//            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_humble));
//            mOutbox.mHumbleAchievement = false;
//        }
//        if (mOutbox.mLeetAchievement) {
//            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_leet));
//            mOutbox.mLeetAchievement = false;
//        }
        if (mOutbox.mScore > 0) {
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_and_i_would_walk_500_miles),
                    mOutbox.mScore);
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_run_away_run_away_run_run_away),
                    mOutbox.mScore);
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_hes_going_the_distance),
                    mOutbox.mScore);
            Games.Achievements.increment(mGoogleApiClient,
                    getString(R.string.achievement_gonna_take_her_for_a_ride_on_a_big_jet_plane),
                    mOutbox.mScore);
        }
    }

    private void pushLeaderboards() {
        if (mOutbox.mScore > mOutbox.mBest) {
            mOutbox.mBest = mOutbox.mScore;
            mMainActivityFragment.updateScoreViews(USER_SCORE, mOutbox.mBest, NO_FADE);
        }
        if (mOutbox.mScore > mOutbox.mSocialBest) {
            mOutbox.mSocialBest = mOutbox.mScore;
            mMainActivityFragment.updateScoreViews(SOCIAL_SCORE, mOutbox.mSocialBest, NO_FADE);
        }
        if (mOutbox.mScore > mOutbox.mWorldBest) {
            mOutbox.mWorldBest = mOutbox.mScore;
            mMainActivityFragment.updateScoreViews(WORLD_SCORE, mOutbox.mWorldBest, NO_FADE);
        }

        if (!isSignedIn()) return;

        // Submit current score
        Games.Leaderboards.submitScore(
                mGoogleApiClient,
                getString(R.string.leaderboard_leaderboard),
                mOutbox.mScore);
    }

//    private class setSigninState implements Runnable{
////        private final GoogleApiClient mGoogleApiClient;
//        private final boolean signIn;
//
//        public setSigninState(GoogleApiClient googleApiClient, boolean signIn){
////            mGoogleApiClient = googleApiClient;
//            this.signIn = signIn;
//        }
//
//        @Override
//        public void run() {
//            mSignInClicked = signIn;
//            if(signIn){
//                mGoogleApiClient.connect();
//            }else{
//                mGoogleApiClient.disconnect();
//            }
//        }
//    }

    class Outbox {
        boolean mCaughtNappingAchievement = false;
        boolean mHumbleAchievement = false;
        boolean mLeetAchievement = false;
        boolean mArrogantAchievement = false;
        int mTotalDistance = 0;
        int mScore = 0;
        float mWorldBest = 0;
        float mSocialBest = 0;
        float mBest = 0;

        void reset() {
            mCaughtNappingAchievement = false;
            mHumbleAchievement = false;
            mLeetAchievement = false;
            mArrogantAchievement = false;
            mTotalDistance = 0;
            mScore = 0;
            mWorldBest = 0;
            mSocialBest = 0;
            mBest = 0;
        }

        boolean isEmpty() {
            return !mCaughtNappingAchievement && !mHumbleAchievement && !mLeetAchievement &&
                    !mArrogantAchievement;
        }

        public void saveLocal() {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putFloat(WORLD_BEST, mWorldBest)
                    .putFloat(SOCIAL_BEST, mSocialBest)
                    .putFloat(PERSONAL_BEST, mBest)
                    .putInt(LAST_SCORE, mScore)
                    .putInt(TOTAL_DISTANCE, mTotalDistance)
                    .apply();
        }

        public void loadLocal() {
            mWorldBest = preferences.getFloat(WORLD_BEST, 0);
            mSocialBest = preferences.getFloat(SOCIAL_BEST, 0);
            mBest = preferences.getFloat(PERSONAL_BEST, 0);
            mScore = preferences.getInt(LAST_SCORE, 0);
            mTotalDistance = preferences.getInt(TOTAL_DISTANCE, 0);
            mMainActivityFragment.updateScoreViews(CURRENT_SCORE, mOutbox.mScore, NO_FADE);
            mMainActivityFragment.updateScoreViews(WORLD_SCORE, mOutbox.mWorldBest, NO_FADE);
            mMainActivityFragment.updateScoreViews(SOCIAL_SCORE, mOutbox.mSocialBest, NO_FADE);
            mMainActivityFragment.updateScoreViews(USER_SCORE, mOutbox.mBest, NO_FADE);
        }
    }
}
