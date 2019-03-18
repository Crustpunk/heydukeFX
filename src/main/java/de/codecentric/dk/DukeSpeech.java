package de.codecentric.dk;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;
import marytts.modules.synthesis.Voice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;
import java.io.IOException;
import java.util.List;

public class DukeSpeech {

    TextToSpeech textToSpeech = new TextToSpeech();

    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    private String result;

    Thread speechThread;
    Thread resourcesThread;

    private LiveSpeechRecognizer speechRecognizer;

    private volatile boolean recognizerStopped = true;

    public DukeSpeech() {

        logger.info("Loading..\n");

        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");

        configuration.setGrammarPath("resource:/grammars");
        configuration.setGrammarName("grammar");
        configuration.setUseGrammar(true);

        try {
            speechRecognizer = new LiveSpeechRecognizer(configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Voice.getAvailableVoices().stream().forEach(voice -> logger.info("Voice: " + voice));
        textToSpeech.setVoice("cmu-slt-hsmm");

        speechRecognizer.startRecognition(true);
        startResourcesThread();
    }


    public void startSpeechThread() {

        if (speechThread != null && speechThread.isAlive())
            return;

        speechThread = new Thread(() -> {
            recognizerStopped = false;

            try {
                while (!recognizerStopped) {
                    SpeechResult speechResult = speechRecognizer.getResult();
                    if (speechResult != null) {
                        result = speechResult.getHypothesis();
                        makeDecision(result, speechResult.getWords());
                    }
                }
            } catch (Exception e) {
                logger.warn("",e);
                recognizerStopped = true;
            }
        });
        speechThread.start();
    }

    public void stopSpeechThread() {
        if (speechThread != null && speechThread.isAlive()) {
            recognizerStopped = true;
        }
    }

    public void startResourcesThread() {
        if (resourcesThread != null && resourcesThread.isAlive()) {
            return;
        }

        resourcesThread = new Thread(() -> {
            try {
                while (true) {
                    if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {

                    } else {
                        logger.info("Microphone is not available.\n");
                    }
                    Thread.sleep(350);
                }
            } catch (InterruptedException e) {
                logger.warn("",e);
                resourcesThread.interrupt();
            }
        });
    }

    private void makeDecision(String speech, List<WordResult> words) {
        if (speech.contentEquals("how are you")) {
            textToSpeech.speak("Fine Thanks", false, true);
        }
    }
}
