package speak.me.plugin.twitter;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
            speak("There was an error validating your credentials. Please login" +
                    "by saying, 'Twitter login' or 'Twitter settings'");
            return;
        }

        boolean containsYes = false;
        String outputText = twitterify(text);
        do {
            // If we are logged in, then we should tweet!

            speak("You said:");
            speak(outputText);
            String[] poss = queryUser("Do you want to post? Yes to post, no to re-record, quit to stop..",
                    "I did not understand. Please repeat Yes, No, or quit", true, false);


            for (String s : poss) {
                if (s.equalsIgnoreCase("yes")) {
                    containsYes = true;
                }
                if (s.equalsIgnoreCase("quit")) {
                    speak("Cancelling");
                    return;
                }
            }
            if (!containsYes) {
                String[] tweets = queryUser("Please repeat your tweet.",
                        "I'm sorry, there seems to be a problem recognizing your voice",
                        true, false);
                Log.d("Tweeter", "Possibilities: " + Arrays.toString(tweets));
                if (tweets.length > 0) {
                    outputText = twitterify(tweets[0]);
                } else {
                    outputText = "";
                }
            }
        } while(!containsYes);


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
            speak("Tweet success!");
        } else {
            speak("Tweet failed.");
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
            final String[] hashtagStrings = new String[]{"hashtag", "has tag", "hash tag"};
            List<Integer> discoveries = new LinkedList<Integer>();
            for (String poss: hashtagStrings) {
                discoveries.add(0,text.toLowerCase().indexOf(poss,nextIndex));
            }
            int hashtagIndex = findNonNegativeMin(discoveries);

            discoveries.clear();
            discoveries = new LinkedList<Integer>();
            for (String poss: hashtagStrings) {
                discoveries.add(0, text.toLowerCase().indexOf(poss, hashtagIndex+1));
            }
            nextIndex = findNonNegativeMin(discoveries);

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

    private int findNonNegativeMin(List<Integer> list)
    {
        Log.d("HASHTAGIFY", Arrays.toString(list.toArray()));
        int min = Integer.MAX_VALUE;
        for (int item : list) {
            if (item < min && item >= 0) {
                min = item;
            }
        }
        if (min == Integer.MAX_VALUE)
            min = -1;
        return min;
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
