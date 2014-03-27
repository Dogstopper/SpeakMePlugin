package speak.me.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by stephen on 3/27/14.
 */
public class SpeakMeListener {

    // Set up the binding and the binder class
//    private IBinder mBinder = new SpeechBinder();
    private Service parent = null;
    private Handler mainThread = null;

    public SpeakMeListener(Service parent) {
        this.parent = parent;
        mainThread = new Handler(parent.getApplication().getMainLooper());
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//
//        return mBinder;
//    }
//
//    public class SpeechBinder extends Binder {
//        public void setupListener(Service parent) {
//            SpeakMeListener.this.parent = parent;
//        }
//
//       public String[] queryUser() throws InvalidSpeechException {
//           callback.completed = false;
//           parent.getMainLooper()
//           return getInput();
//       }
//    }

    // Now implement the speech recognition stuff
    private SpeechRecognizer speechService;

    public String[] queryUser() throws InvalidSpeechException {
        callback.completed = false;
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                getInput();
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
            }
        });

        while (!callback.completed) {
            synchronized (this) {
                try {
                    wait(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        mainThread.post(new Runnable() {
            @Override
            public void run() {
                speechService.cancel();
                speechService.destroy();
            }
        });

        if (callback.error) {
            throw new InvalidSpeechException();
        }
        return callback.results;
    }

    private void getInput() {
        // First initialize the listener
        speechService = SpeechRecognizer.createSpeechRecognizer(parent);
        speechService.setRecognitionListener(new SpeakMeRecognizer());

        // Setup the
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //Log.d("SPEAKME LISTENER", "Package:" + parent.getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "speak.me");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        speechService.startListening(intent);

    }

    private SynchronizedCallback callback = new SynchronizedCallback();
    private class SynchronizedCallback {
        public volatile boolean completed = false;
        public volatile boolean error = false;
        public volatile String[] results;
    }

    private class SpeakMeRecognizer implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle bundle) { Log.d("SPEAKME LISTENER", "In onReadyForSpeech"); }

        @Override
        public void onBeginningOfSpeech() { Log.d("SPEAKME LISTENER", "In onBeginningOfSpeech"); }

        @Override
        public void onRmsChanged(float v) {}

        @Override
        public void onBufferReceived(byte[] bytes) { }

        @Override
        public void onEndOfSpeech() { Log.d("SPEAKME LISTENER", "In onEndOfSpeech"); }

        @Override
        public void onError(int i) {
            Log.d("SPEAKME LISTENER", "In onOnError");
            callback.completed = true;
            callback.error = true;
        }

        @Override
        public void onResults(Bundle bundle) {
            Log.d("SPEAKME LISTENER", "In onResults");
            callback.completed = true;
            callback.error = false;
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            callback.results = results.toArray(new String[]{});
        }

        @Override
        public void onPartialResults(Bundle bundle) {}

        @Override
        public void onEvent(int i, Bundle bundle) {}
    }

    public void destroy() {
        parent = null;
        mainThread = null;
    }
}
