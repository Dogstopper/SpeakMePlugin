package speak.me.plugin.twitter;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.List;


import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import speak.me.plugin.twitter.BooleanCallback;

public class TwitterHandler {
    public static final String CONSUMER_KEY = "BHi8Bu5wU0lu6zp7FUsqA";
    public static final String CONSUMER_SECRET = "ydMyJFs1lFAXPLljodPmRUub6zZn7e6ZS5GpsuOXA";

    public String m_consumerKey;
    public String m_consumerSecret;
    public String m_accessToken;
    public String m_accessSecret;
    public RequestToken m_requestToken;
    Twitter m_twitter;

    TwitterHandler() {
        m_consumerKey = CONSUMER_KEY;
        m_consumerSecret = CONSUMER_SECRET;
        m_accessToken = "";
        m_accessSecret = "";

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(m_consumerKey)
                .setOAuthConsumerSecret(m_consumerSecret);

        TwitterFactory tf = new TwitterFactory(cb.build());
        m_twitter  = tf.getInstance();
    }

    TwitterHandler(String accessToken, String accessSecret) {
        m_consumerKey = CONSUMER_KEY;
        m_consumerSecret = CONSUMER_SECRET;

        authenticate(accessToken, accessSecret);
    }

    public void authenticate(String token, String secret) {
        m_accessToken = token;
        m_accessSecret = secret;

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(m_consumerKey)
                .setOAuthConsumerSecret(m_consumerSecret)
                .setOAuthAccessToken(m_accessToken)
                .setOAuthAccessTokenSecret(m_accessSecret);

        TwitterFactory tf = new TwitterFactory(cb.build());
        m_twitter  = tf.getInstance();
    }

    public void authenticate(AccessToken at) {
        authenticate(at.getToken(), at.getTokenSecret());
    }

    public String getAuthorizationUrl() {
        String url = "";
        try {
            // get request token.
            // this will throw IllegalStateException if access token is already available
            m_requestToken = m_twitter.getOAuthRequestToken();
            url = m_requestToken.getAuthorizationURL();
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }

        return url;
    }

    //Returns null if user did not authorize app or entered incorrect pin
    public AccessToken getAccessTokenUsingPin(String pin) {
        AccessToken accessToken = null;

        try {
            if (pin.length() > 0) {
                accessToken = m_twitter.getOAuthAccessToken(m_requestToken, pin);
            }
        } catch (TwitterException te) {
            if (401 == te.getStatusCode()) {
                Log.d("TwitterHandler", "Unable to get the access token.");
            } else {
                //ERROR
            }

            return null;
        }

        return accessToken;
    }

    //The callback is called when the tweet is complete. It passes true on success, false otherwise
    public void tweet(String text, final long inReplyTo, final BooleanCallback callback) {
        class TweetTask extends AsyncTask<String, Void, Boolean> {

            private Exception exception;

            protected Boolean doInBackground(String... tweet) {
                try {
                    StatusUpdate st = new StatusUpdate(tweet[0]);
                    if (inReplyTo != -1) {
                        st.inReplyToStatusId(inReplyTo);
                    }
                    twitter4j.Status status = m_twitter.updateStatus(st);
                    callback.run(true);
                } catch (TwitterException e) {
                    Log.e("LazyTweeter", "ERROR: " + e);
                    callback.run(false);
                }
                return true;
            }
        }

        new TweetTask().execute(text);
    }

    private ResponseList<Status> cache = null;
    public String getFeedTweet(int tweetNum) {
        final int cacheSize = 120;
        Log.d("TWITTER", "Getting tweet #:" +tweetNum);
        int page = tweetNum/cacheSize+1;
        int tweet = tweetNum%cacheSize;

        if (cache == null) {
            try {
                cache = m_twitter.getHomeTimeline(new Paging(page,cacheSize));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

        if (tweetNum >= cache.size()) {
            long id = cache.get(cache.size()-1).getId();
            try {
                cache = m_twitter.getHomeTimeline(new Paging(   id));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

        Status s = cache.get(tweet);
        if (s.isRetweet()) {
            return s.getUser().getName() + "retweeted " + s.getText().replaceAll("RT", "");
        }
        else {
            return s.getUser().getName() + " said " + s.getText();
        }
    }

    public long getReplyId(int tweetNum) {
        if (cache != null) {
            if (tweetNum < cache.size()) {
                long id = cache.get(tweetNum).getUser().getId();
                return id;
            }
        }
        return -1;
    }

    public boolean retweet(int tweetNum) {
        if (cache != null) {
            if (tweetNum < cache.size()) {
                try {
                    m_twitter.retweetStatus(cache.get(tweetNum).getId());
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public boolean reply(int tweetNum, String text) {
        if (cache != null) {
            if (tweetNum < cache.size()) {
                try {
                    Status replyTo = cache.get(tweetNum);
                    String screenName = replyTo.getUser().getScreenName();

                    StatusUpdate st = new StatusUpdate(screenName + ": " + text);
                    st.inReplyToStatusId(replyTo.getId());
                    m_twitter.updateStatus(st);
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    //The callback is run and passed the result when verification is complete
    public void verifyCredentials(final BooleanCallback callback) {
        class TweetTask extends AsyncTask<Void, Void, Boolean> {

            private Exception exception;

            protected Boolean doInBackground(Void... tweet) {
                try {
                    m_twitter.verifyCredentials(); //This throws an exception when verification fails
                    callback.run(true);
                } catch(TwitterException e) {
                    if(e.getErrorCode() != 215) //215 is the error code for the access token is invalid
                        Log.e("LazyTweeter", "ERROR: " + e);

                    callback.run(false);
                }
                return true;
            }
        }

        new TweetTask().execute();

    }

    public String getUserFromScreenName(String screenName) throws TwitterException {
//        Twitter twit = new TwitterFactory().getInstance();
        User u = m_twitter.showUser(screenName);
        return u.getName();
    }

    public String getUsernameFromID(long id) throws  TwitterException{
        String username = m_twitter.showUser(id).getScreenName();
        return username;
    }
}