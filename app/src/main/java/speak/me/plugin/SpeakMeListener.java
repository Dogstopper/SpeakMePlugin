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
 * Retrieves speech from the user and provides different methods for handling errors that arise
 * from it. Note that all Speech-To-Text operations must occur on the Main Application thread, and
 * therefore, we must discover it.
 */
public class SpeakMeListener {

    private int silenceTimout;
    private Service parent = null;
    private Handler mainThread = null;

    /**
     * Sets the Service's parent and discovers the application main thread, which is necessary for
     * performing Speech-To-Text operations.
     *
     * @param parent
     */
    public SpeakMeListener(Service parent) {
        this.parent = parent;
        mainThread = new Handler(parent.getApplication().getMainLooper());
    }


    // Now implement the speech recognition stuff
    private SpeechRecognizer speechService;

    /**
     * Returns a String[] of possible user speech recommendations. It runs on the main thread and
     * uses a callback mechanism to allow this method to block during each operation. This means
     * that it will not return until the item in the background has been completed.
     * @return
     * @throws InvalidSpeechException
     */
    public String[] queryUser() throws InvalidSpeechException {
        // Set the callback to not be completed and then get user feedback by poting
        // onto the main thread.
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

        // Now that this operation has been posted, wait until the operation is complete.
        // To determine this, the completed variable in the callback is checked.
        while (!callback.completed) {
            synchronized (this) {
                try {
                    wait(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Now, we also have to cancel and destroy the service on the main thread. Neither
        // cancel nor destroy works by itself, so both must be called in order to not have a
        // memory leak upon leaving the plugin.
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                speechService.cancel();
                speechService.destroy();
            }
        });

        // If an error occurred, then the error flag in the callback will be set. Simply
        // throw an Exception and let the caller deal with it.
        if (callback.error) {
            throw new InvalidSpeechException();
        }

        // The results of the operation are located in the callback results.
        return callback.results;
    }

    /**
     * Helper method for queryUser(), this method is invoked in the main thread and is responsible
     * for starting the speech service that is used to query the user.
     */
    private void getInput() {
        // First initialize the listener
        speechService = SpeechRecognizer.createSpeechRecognizer(parent);
        speechService.setRecognitionListener(new SpeakMeRecognizer());

        // Set up the intent for 5 results in Dictation mode and no extra prompt.
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimout);
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, silenceTimout);
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimout);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "speak.me");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        speechService.startListening(intent);

    }

    private SynchronizedCallback callback = new SynchronizedCallback();

    /**
     * Contains the volatile variables needed to send dat across threads. This is used to block
     * methods and retrieve data from the main thread back to this background thread.
     */
    private class SynchronizedCallback {
        public volatile boolean completed = false;
        public volatile boolean error = false;
        public volatile String[] results;
    }

    /**
     * Sets the callback variables appropriately onError() and onResult(). This class is used in
     * on the main thread to detect and respond to speech.
     */
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

            // If there is an error, set the completed and boolean flags
            callback.completed = true;
            callback.error = true;
        }

        @Override
        public void onResults(Bundle bundle) {
            Log.d("SPEAKME LISTENER", "In onResults");

            // Retrieves the list of possible results.
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            // If there are results available, set the completed, error, and results
            // of the callback.
            callback.completed = true;
            callback.error = false;
            callback.results = results.toArray(new String[]{});
        }

        @Override
        public void onPartialResults(Bundle bundle) {}

        @Override
        public void onEvent(int i, Bundle bundle) {}
    }

    /**
     * Ensure that the parent and mainThread references are destroyed, so that the garbage collector
     * can remove them if necessary. This prevents memory leaks.
     */
    public void destroy() {
        parent = null;
        mainThread = null;
    }
}
