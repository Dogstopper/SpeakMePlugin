package speak.me.plugin;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * SpeakMePlugin will be the parent class of ALL plugins, and it will perform critical
 * binding tasks with the other services that would otherwise need to be handled by the
 * plugin developer. In addition, this should start/stop SpeakMe passive listening service
 * while it's running.
 *
 * Finally, this should provide helper methods for everything from calling the helper services
 * and spawn the main worker method.
 */
public abstract class SpeakMePlugin extends Service {

    private SpeakMeReader ttsProvider;
    private SpeakMeListener sttProvider;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Intent callerIntent;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Log.d(this.getClass().getCanonicalName(), "Called Before performAction()");
            // Send stop signal
            Intent stopIntent = new Intent();
            stopIntent.setAction("event-stop-broadcast");
            sendBroadcast(stopIntent);

            // Connect to the TTS and Speech to Text providers.
            ttsProvider = new SpeakMeReader(SpeakMePlugin.this);
            sttProvider = new SpeakMeListener(SpeakMePlugin.this);

            // Do all the plugin work Here! Calling performAction is like calling
            // the main function in any other app
            String text = msg.getData().getString("TEXT");
            performAction(text);

            sttProvider.destroy();

            // Send restart signal
            Intent startIntent = new Intent();
            startIntent.setAction("event-start-broadcast");
            sendBroadcast(startIntent);
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopService(callerIntent);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Deals with the startService(Intent) call and delegates to our abstract method,
     * performAction() in order to implement that specific plugin. This also uses the
     * intent to start and stop the passive speech recognition based on the extras that are
     * passed into it.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return START_NOT_STICKY
     */
	public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        this.callerIntent = intent;

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();

        Bundle b = new Bundle();
        b.putString("TEXT", intent.getExtras().getString("TEXT"));

        msg.setData(b);
        msg.arg1 = startId;

        mServiceHandler.sendMessage(msg);
        // If this dies in memory, then destroy it and its resources
		return START_NOT_STICKY;
	}

    /**
     * This is the main method and entry point for SpeakMe Plugins. Implement or delegate
     * plugin functionality from this method. This is executed in a background thread, so make
     * sure to use getMainLooper() to post on the main thread.
     * @param text The text sent from the core app with the text that was used to launch this
     *             plugin including the plugin name.
     */
    public abstract void performAction(String text);

    /**
     * Makes a call to the provided Text To Speech reader and blocks until it is completed speech.
     * Therefore, make sure you break up the desired text into smaller components and then maybe
     * listen for user feedback between to see if there is anything that you want to do.
     *
     * @param speech
     */
    protected void speak(String speech) {
        ttsProvider.invokeTTS(speech);
        while(!ttsProvider.isDoneSpeaking()) {
            try {
                synchronized (this) {
                    wait(200);
                }
            } catch (InterruptedException e) {
                Log.e(this.getClass().getCanonicalName(), e.getMessage());
            }
        }
    }

    /**
     * Returns a String[] of possibilities generated by the Speech to Text parser. This
     * overloaded method deals with error handling internally, where the error message is
     * passed in and the desired behavior for processing that error.
     * @param query The string to initially present the user for a prompt.
     * @param errorMessage The string to speak on an error happening.
     * @param repeatOnError Indicates whether to continue (true) or not (false) on error
     * @param repeatPromptOnError Indicates whether to re-speak the prompt on error.
     * @return String[] of possibilities in descending order
     */
    protected String[] queryUser(String query, String errorMessage, boolean repeatOnError,
                                 boolean repeatPromptOnError) {
        String[] returnArray = null;
        boolean repeated = false;
        do {
            try {
                // If we are on the first iteration OR have repeatPrompt turned on, respeak
                if (repeated && !repeatPromptOnError)
                    returnArray = queryUser();
                else
                    returnArray = queryUser(query);
            } catch (InvalidSpeechException e) {
                speak(errorMessage);
                repeated = true;
            }
        } while (repeatOnError && returnArray == null);
        return returnArray;
    }

    /**
     * Returns a String[] of possibilities returned by the Speech to Text parser. There is no
     * query, and you must handle errors yourself.
     * @return String[] of possibilities in descending order
     * @throws InvalidSpeechException
     */
    protected String[] queryUser() throws InvalidSpeechException {
        return queryUser(null);
    }

    /**
     * Returns a String[] of possibilities returned by the Speech to Text parser. There is a given
     * query, and you must handle errors yourself.
     * @param query The prompt for the question.
     * @return String[] of possibilities in descending order
     * @throws InvalidSpeechException
     */
    protected String[] queryUser(String query) throws InvalidSpeechException {
        if (query != null && !query.equals("")) {
            Log.d("PLUGIN", "outputting prompt.");
            speak(query);
            Log.d("PLUGIN", "prompt complete.");
        }

        return sttProvider.queryUser();
    }

	@Override
    public void onDestroy() {
		super.onDestroy();
	}

}
