package com.nikmoores.android.endlessdodge;

import android.content.Context;
import android.content.Intent;
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
import com.google.android.gms.games.Player;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

import static com.nikmoores.android.endlessdodge.MainActivityFragment.CURRENT_SCORE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.Listener;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.USER_SCORE;
import static com.nikmoores.android.endlessdodge.MainActivityFragment.WORLD_SCORE;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Listener {

    final String LOG_TAG = MainActivity.class.getSimpleName();

    MainActivityFragment mMainActivityFragment;

    private Menu menu;

    private float mLocalWorldBestScore;
    private float mLocalBestScore;
    private float mLocalCurrentScore;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    // request codes we use when invoking an external activity
    private static final int RC_RESOLVE = 5000;
    private static final int RC_UNUSED = 5001;
    private static final int RC_SIGN_IN = 9001;

    private static final int REQUEST_LEADERBOARD = 4000;

    // achievements and scores we're pending to push to the cloud
    // (waiting for the user to sign in, for instance)
    Outbox mOutbox = new Outbox();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
                .build();

        // Create fragment and listen
        mMainActivityFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        mMainActivityFragment.setListener(this);

        // load outbox from file
        mOutbox.loadLocal(this);
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
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.menu_sign_out:
                mSignInClicked = false;
                mGoogleApiClient.disconnect();
                break;
            case R.id.menu_leaderboard:
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(
                                mGoogleApiClient, getString(R.string.leaderboard_leaderboard)),
                        REQUEST_LEADERBOARD);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
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
//        mMainMenuFragment.setShowSignInButton(false);

        // Show "you are signed in" message on win screen, with no sign in button.
//        mWinFragment.setShowSignInButton(false);

        // Set the greeting appropriately on main menu
        Player p = Games.Players.getCurrentPlayer(mGoogleApiClient);
        String displayName;
        if (p == null) {
            Log.w(LOG_TAG, "mGamesClient.getCurrentPlayer() is NULL!");
            displayName = "You";
        } else {
            displayName = p.getDisplayName();
        }
        // TODO: Use the name in the fragment XML file.
//        mMainMenuFragment.setGreeting("Hello, " + displayName);
        // TODO: Set Leaderboard scores.


        // if we have accomplishments to push, push them
        if (!mOutbox.isEmpty()) {
            pushAccomplishments();
//            Toast.makeText(this, getString(R.string.your_progress_will_be_uploaded),
//                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended(): attempting to connect");
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
    public void updateLeaderboards(int score) {
        mOutbox.mScore = score;
        pushAccomplishments();
    }

    void pushAccomplishments() {
        if (!isSignedIn()) {
            // can't push to the cloud, so save locally
            mOutbox.saveLocal(this);
            return;
        }
//        if (mOutbox.mPrimeAchievement) {
//            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_prime));
//            mOutbox.mPrimeAchievement = false;
//        }
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
//        if (mOutbox.mBoredSteps > 0) {
//            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_really_bored),
//                    mOutbox.mBoredSteps);
//            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_bored),
//                    mOutbox.mBoredSteps);
//        }
        if (mOutbox.mScore >= 0) {
            // Submit current score
            Games.Leaderboards.submitScore(
                    mGoogleApiClient,
                    getString(R.string.leaderboard_leaderboard),
                    mOutbox.mScore);

            mMainActivityFragment.updateScoreViews(CURRENT_SCORE, mOutbox.mScore);
            if (mOutbox.mScore > mLocalBestScore) {
                mLocalBestScore = mOutbox.mScore;
                mMainActivityFragment.updateScoreViews(USER_SCORE, mLocalBestScore);
            }
            if (mOutbox.mScore > mLocalWorldBestScore) {
                mLocalWorldBestScore = mOutbox.mScore;
                mMainActivityFragment.updateScoreViews(USER_SCORE, mLocalWorldBestScore);
            }

            // Update user best score.
            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(mGoogleApiClient,
                    getString(R.string.leaderboard_leaderboard),
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC)
                    .setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

                        @Override
                        public void onResult(Leaderboards.LoadPlayerScoreResult arg0) {
                            LeaderboardScore c = arg0.getScore();
                            if (c.getRawScore() > mLocalBestScore) {
                                mLocalBestScore = c.getRawScore();
                                mMainActivityFragment.updateScoreViews(
                                        USER_SCORE,
                                        mLocalBestScore);
                            }
                        }

                    });
            // Update world's best.
            Games.Leaderboards.loadTopScores(mGoogleApiClient,
                    getString(R.string.leaderboard_leaderboard),
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC,
                    1).setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {
                @Override
                public void onResult(Leaderboards.LoadScoresResult loadScoresResult) {
                    float topScore = loadScoresResult.getScores().get(0).getRawScore();
                    if (topScore > mLocalWorldBestScore) {
                        mLocalWorldBestScore = topScore;
                        mMainActivityFragment.updateScoreViews(
                                WORLD_SCORE,
                                mLocalWorldBestScore);
                    }
                }
            });
            mOutbox.mScore = -1;
        }
        mOutbox.saveLocal(this);
    }

    class Outbox {
        boolean mPrimeAchievement = false;
        boolean mHumbleAchievement = false;
        boolean mLeetAchievement = false;
        boolean mArrogantAchievement = false;
        int mBoredSteps = 0;
        int mScore = -1;

        boolean isEmpty() {
            return !mPrimeAchievement && !mHumbleAchievement && !mLeetAchievement &&
                    !mArrogantAchievement && mBoredSteps == 0 && mScore < 0;
        }

        public void saveLocal(Context ctx) {
            /* TODO: This is left as an exercise. To make it more difficult to cheat,
             * this data should be stored in an encrypted file! And remember not to
             * expose your encryption key (obfuscate it by building it from bits and
             * pieces and/or XORing with another string, for instance). */
        }

        public void loadLocal(Context ctx) {
            mLocalWorldBestScore = 0;
            mLocalBestScore = 0;
            mLocalCurrentScore = 0;
        }
    }
}
