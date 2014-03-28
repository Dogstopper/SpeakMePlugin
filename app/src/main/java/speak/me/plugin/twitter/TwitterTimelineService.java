package speak.me.plugin.twitter;

import android.content.SharedPreferences;

import speak.me.plugin.SpeakMePlugin;

/**
 * Created by stephen on 3/27/14.
 */
public class TwitterTimelineService extends SpeakMePlugin {

    private TwitterHandler handler;

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
        if (checkLoggedIn()) {
            invokeTTSReader("Reading your feed:");
            int tweetNum = 0;
            boolean quit = false;
            while (!quit) {
                invokeTTSReader(convertTweetToText(handler.getFeedTweet(tweetNum)));
                String[] results = queryUser(null,"",false,false);
                if (results != null) {
                    for (String res : results) {
                        if (res.toLowerCase().contains("stop") ||
                                res.toLowerCase().contains("quit")) {
                            quit = true;
                            invokeTTSReader("Quitting.");
                            break;
                        } else if (res.toLowerCase().contains("next")) {
                            quit = false;
                            continue;
                        }
                        else if (res.toLowerCase().contains("re tweet") ||
                                res.toLowerCase().contains("retweet")) {
                            if (handler.retweet(tweetNum)) {
                                invokeTTSReader("Retweet successful");
                            }

                        }
                        else if (res.toLowerCase().contains("reply")) {

                        }
                    }
                }
                tweetNum++;
            }
        }
    }

    private String convertTweetToText(String text) {
        text = text.replaceAll("http://[a-zA-z0-9./]*", ". hyperlink.");
        text = text.replaceAll("#", " hashtag ");
        text = text.replaceAll("@", " at ");
        return text;
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
            invokeTTSReader("There was an error validating your credentials. Please login" +
                    "by saying, 'Twitter settings'");
            return false;
        }
        return true;
    }

}
