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

//    private boolean ttsBound;
//    private boolean sttBound;

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

            // Bind to the services
//            Intent ttsBindIntent = new Intent(SpeakMePlugin.this, SpeakMeReader.class);
//            bindService(ttsBindIntent, mTTSConnection, Context.BIND_AUTO_CREATE);
//
//            Intent sttBindIntent = new Intent(SpeakMePlugin.this, SpeakMeListener.class);
//            bindService(sttBindIntent, mSTTConnection, Context.BIND_AUTO_CREATE);
//            synchronized (this) {
//                while (!ttsBound || !sttBound) {
//                    try {
//                        wait(20);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
            ttsProvider = new SpeakMeReader(SpeakMePlugin.this);
            sttProvider = new SpeakMeListener(SpeakMePlugin.this);

            // Do all the plugin work
            String text = msg.getData().getString("TEXT");
            performAction(text);

            sttProvider.destroy();
//            unbindService(mTTSConnection);
//            unbindService(mSTTConnection);

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
     * plugin functionality from this method.
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
    protected void invokeTTSReader(String speech) {
//        Log.d("TTS", "bound==" + ttsBound);
//        if (ttsBound) {
//            ttsProvider.setupTTS();
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
//        }


    }

    protected String[] queryUser(String query, String errorMessage, boolean repeatOnError,
                                 boolean repeatPromptOnError) {
        String[] returnArray = null;
        boolean repeated = false;
        do {
            try {
                if (repeated && !repeatPromptOnError)
                    returnArray = queryUser();
                else
                    returnArray = queryUser(query);
            } catch (InvalidSpeechException e) {
                invokeTTSReader(errorMessage);
                repeated = true;
            }
        } while (repeatOnError && returnArray == null);
        return returnArray;
    }

    protected String[] queryUser() throws InvalidSpeechException {
        return queryUser(null);
    }

    protected String[] queryUser(String query) throws InvalidSpeechException {
        if (query != null && !query.equals("")) {
            Log.d("PLUGIN", "outputting prompt.");
            invokeTTSReader(query);
            Log.d("PLUGIN", "prompt complete.");
        }
//        if (sttBound) {
//            sttProvider.setupListener(this);
        return sttProvider.queryUser();
//
//        return new String[]{};
    }

	@Override
    public void onDestroy() {
		super.onDestroy();
	}

    /** Defines callbacks for service binding, passed to bindService() */
//    private ServiceConnection mTTSConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            Log.d("PLUGIN", "BINDING SUCCESS");
//            ttsProvider = (SpeakMeReader.TTSBinder) service;
//            ttsBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            Log.d("PLUGIN", "BINDING SUCCESS");
//            ttsBound = false;
//        }
//    };
//
//    private ServiceConnection mSTTConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            Log.d("PLUGIN", "BINDING SUCCESS");
//            sttProvider = (SpeakMeListener.SpeechBinder) service;
//            sttBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            Log.d("PLUGIN", "BINDING SUCCESS");
//            sttBound = false;
//        }
//    };

}
