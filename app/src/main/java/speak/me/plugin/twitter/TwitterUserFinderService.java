package speak.me.plugin.twitter;

import android.content.SharedPreferences;

import speak.me.plugin.SpeakMePlugin;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by stephen on 4/12/14.
 */
public class TwitterUserFinderService extends SpeakMePlugin {
    TwitterHandler handler;

    @Override
    public void onCreate() {
        super.onCreate();

        // Check to see if the user is logged in
        SharedPreferences prefs = getSharedPreferences(AuthorizeActivity.PREFS_FILE,MODE_MULTI_PROCESS);
        String token = prefs.getString("token", "");
        String tokenSecret = prefs.getString("tokenSecret", "");
        handler = new TwitterHandler(token, tokenSecret);
    }

    @Override
    public void performAction(String text) {
//        04-12 13:31:17.557: D/TWITTER(11967): screenName = TanyaKlich:

        String name = "capable_monkey";
        if (checkLoggedIn()) {
            speak("Finding user named " + name);
            try {
                speak("User is named: " + handler.getUserFromScreenName(name));
            } catch (TwitterException e) {
                speak("ERROR: " + e.getMessage());
            }
        }
    }

    private boolean checkLoggedIn() {
        class ResultCallback implements BooleanCallback {
            public boolean invoked = false;
            public boolean result = true;

            @Override
            public void run(boolean bool) {
                invoked = true;
                if (!bool) {
                    result = false;
                }
            }
        };

        // Check that we're logged in.
        ResultCallback cb = new ResultCallback();
        handler.verifyCredentials(cb);
        while (!cb.invoked) {
            synchronized (this){
                try {
                    wait(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!cb.result) {
            speak("There was an error validating your credentials. Please login" +
                    "by saying, 'Twitter settings'");
            return false;
        }
        return true;
    }
}
