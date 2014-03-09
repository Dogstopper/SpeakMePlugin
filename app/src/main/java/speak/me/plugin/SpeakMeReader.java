package speak.me.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class SpeakMeReader extends Service {
	private TextToSpeech tts;
    private IBinder mBinder = new TTSBinder();

    private boolean speaking =  false;
    private  String utteranceID = "utterance_id_speech";

    @Override
    public void onCreate() {
        super.onCreate();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if (tts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
                        tts.setLanguage(Locale.US);

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onStart(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    Log.d("TTS", "Starting");
                    speaking = true;
                }
            }

            @Override
            public void onDone(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    Log.d("TTS", "Done");
                    speaking = false;
                }
            }

            @Override
            public void onError(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    speaking = false;
                }
                Log.e("TTS", "onError()");
            }
        });
    }

    public class TTSBinder extends Binder {
        public void setupTTS() {
            //SpeakMeReader.this.setupTTS();
        }

        public void invokeTTS(String text) {
            if (text != null && text.trim().length() != 0) {
                textToSpeech(text);
            }
        }

        public boolean isDoneSpeaking() {
            return speaking == false;
        }
    }

    public void textToSpeech(String text) {
        this.utteranceID = text.replace(" ", "_");
        speaking = true;

        HashMap<String, String> voiceMap = new HashMap();
        voiceMap.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS,"true");
        voiceMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, this.utteranceID);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, voiceMap);
        Log.d("TTS", "finished Queueing");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

}
