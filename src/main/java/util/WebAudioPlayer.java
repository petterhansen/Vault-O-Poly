package util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.*;

public class WebAudioPlayer {

    // Global SFX Volume (0.0 to 1.0). Default 75%
    private static float sfxVolume = 0.75f;

    public static void setVolume(float volume) {
        sfxVolume = Math.max(0.0f, Math.min(volume, 1.0f));
    }

    public static float getVolume() {
        return sfxVolume;
    }

    /**
     * Plays a sound from a URL (supports .wav and .ogg if libraries are present).
     */
    public static void play(String urlString) {
        if (urlString == null || urlString.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                InputStream bufferedIn = new BufferedInputStream(url.openStream());
                AudioInputStream in = AudioSystem.getAudioInputStream(bufferedIn);
                AudioFormat baseFormat = in.getFormat();

                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, in);

                Clip clip = AudioSystem.getClip();
                clip.open(decodedIn);

                // --- NEW: Apply Volume ---
                applyVolume(clip);
                // -------------------------

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();

            } catch (Exception e) {
                System.err.println("Error playing audio: " + urlString);
            }
        }).start();
    }

    /**
     * Helper to set the volume on a specific clip based on the global setting.
     */
    private static void applyVolume(Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            // Convert 0.0-1.0 scale to Decibels
            // Formula: 20 * log10(linear)
            // We clamp the minimum to -80dB to avoid -Infinity errors
            float db;
            if (sfxVolume < 0.01f) {
                db = -80.0f; // Effectively mute
            } else {
                db = (float) (20.0 * Math.log10(sfxVolume));
            }

            gainControl.setValue(db);
        }
    }
}