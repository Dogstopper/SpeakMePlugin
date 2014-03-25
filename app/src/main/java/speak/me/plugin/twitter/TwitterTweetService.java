package speak.me.plugin.twitter;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import speak.me.plugin.SpeakMePlugin;
import speak.me.plugin.twitter.TwitterHandler;

public class TwitterTweetService extends SpeakMePlugin {
    private TwitterHandler handler;

    public TwitterTweetService() {

    }

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
                    "by saying, 'Twitter login' or 'Twitter settings'");
            return;
        }

        // If we are logged in, then we should tweet!
        String outputText = twitterify(text);

        cb = new ResultCallback();
        handler.tweet(outputText, cb);

        Log.d("TWITTER", "SPOKEN: " + text);
        Log.d("TWITTER", "TWEETED: " + outputText);
        // Determine Success
        while (!cb.invoked) {
            synchronized (this){
                try {
                    wait(200);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (cb.result) {
            invokeTTSReader("Tweet success!");
            invokeTTSReader("You posted:");
            invokeTTSReader(outputText);
        } else {
            invokeTTSReader("Tweet failed.");
        }
        Log.d("TWITTER", "DONE");

    }

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

    private String twitterify(String text) {
        text = text.replaceFirst("[T|t]witter", "");
        text = text.replaceFirst("[T|t]weet", "");
        text = text.trim();
        int nextIndex = 0;
        do {
            // Replace hashtags appropriately.
            int hashtagIndex = text.toLowerCase().indexOf("hashtag", nextIndex);
            int hashtagIndex2 = text.toLowerCase().indexOf("hash tag", nextIndex);
            if (hashtagIndex == -1) {
                hashtagIndex = hashtagIndex2;
            } else if (hashtagIndex2 == -1) {
                // LEave hashtagIndex the same
            } else {
                hashtagIndex = Math.min(hashtagIndex, hashtagIndex2);
            }

            nextIndex = text.toLowerCase().indexOf("hashtag", hashtagIndex+1);
            int nextIndex2 = text.toLowerCase().indexOf("hash tag", hashtagIndex+1);
            if (nextIndex == -1) {
                nextIndex = nextIndex2;
            } else if (nextIndex2 == -1) {
                // LEave hashtagIndex the same
            } else {
                nextIndex = Math.min(nextIndex, nextIndex2);
            }
            Log.d("TWITTER TWEET", "text: " + text);
            Log.d("TWITTER TWEET", "Hashtag index: " + hashtagIndex);
            Log.d("TWITTER TWEET", "NExtIndex:"  + nextIndex);
            if (hashtagIndex != -1 && nextIndex != -1) {
                String fixed = fixHashtags(text.substring(hashtagIndex, nextIndex));
                text = text.substring(0,hashtagIndex) + fixed +
                        text.substring(nextIndex).replaceAll("[H|h]ash[\\s]*tag","");
            }
        } while (nextIndex != -1);

        if (text.length() > 1) {
            String firstChar = text.charAt(0) + "";
            String rest = text.substring(1);
            String part = firstChar.toUpperCase() + rest;
            text = part;
        } else {
            text = text.toUpperCase();
        }

        return text;

    }

    private String fixHashtags(String hashtagString) {
        hashtagString = hashtagString.replaceAll("hashtag", "#");
        hashtagString = hashtagString.replaceAll("hash tag", "#");
        String[] parts = hashtagString.split(" ");
        StringBuffer sb = new StringBuffer();
        for (String s : parts) {
            if (s.length() > 1) {
                String firstChar = s.charAt(0) + "";
                String rest = s.substring(1);
                String part = firstChar.toUpperCase() + rest;
                sb.append(part);
            } else {
                sb.append(s.toUpperCase());
            }
        }
        return sb.toString();
    }
}
