package radio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.sound.sampled.*;

public class MusicPlayer {

    private SourceDataLine line; // Replaces Clip
    private float volume = 0.75f;
    private boolean isPlaying = false;
    private Runnable onTrackFinished;
    private Thread playbackThread;

    // Shared variable for the visualizer (0.0 to 1.0)
    private volatile float currentAmplitude = 0.0f;

    public void play(String urlString, Runnable onFinish) {
        stop(); // Kill previous
        this.onTrackFinished = onFinish;

        playbackThread = new Thread(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) return;

                // 1. Connection Setup
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                InputStream webStream = new BufferedInputStream(conn.getInputStream());
                AudioInputStream in = AudioSystem.getAudioInputStream(webStream);
                AudioFormat baseFormat = in.getFormat();

                // 2. Decode to PCM
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

                // 3. Open SourceDataLine (Streaming Line)
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decodedFormat);

                setVolume(volume); // Apply initial volume

                line.start();
                isPlaying = true;

                // 4. Playback Loop (The Magic Happens Here)
                byte[] buffer = new byte[4096];
                int n = 0;

                while (isPlaying && (n = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        line.stop();
                        break;
                    }

                    // A. Write to Audio Card
                    if (n > 0) {
                        line.write(buffer, 0, n);

                        // B. Calculate Amplitude for Visualizer
                        calculateAmplitude(buffer, n);
                    }
                }

                // 5. Cleanup / Finish
                line.drain();
                line.stop();
                line.close();
                isPlaying = false;
                currentAmplitude = 0.0f; // Reset visualizer

                // Trigger next song only if we finished naturally
                if (n == -1 && onTrackFinished != null) {
                    onTrackFinished.run();
                }

            } catch (Exception e) {
                // e.printStackTrace(); // Debug if needed
                // Skip on error
                if (onTrackFinished != null) onTrackFinished.run();
            }
        });
        playbackThread.start();
    }

    /**
     * Calculates the Root Mean Square (RMS) amplitude of the byte buffer.
     * This gives a smooth representation of "loudness".
     */
    private void calculateAmplitude(byte[] buffer, int bytesRead) {
        long sum = 0;
        // Process 16-bit samples (2 bytes per sample)
        for (int i = 0; i < bytesRead; i += 2) {
            // Combine low and high bytes
            int sample = (short)((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            sum += sample * sample; // Sum of squares
        }

        // Mean Square
        double mean = sum / (bytesRead / 2.0);

        // Root Mean Square (RMS)
        double rms = Math.sqrt(mean);

        // Normalize to 0.0 - 1.0 (Max 16-bit value is 32768)
        float normalized = (float) (rms / 32768.0);

        // Apply sensitivity boost so it looks better
        this.currentAmplitude = Math.min(1.0f, normalized * 2.5f);
    }

    public float getCurrentAmplitude() {
        return isPlaying ? currentAmplitude : 0.0f;
    }

    public synchronized void stop() {
        isPlaying = false; // Breaks the while loop
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
        if (line != null) {
            line.stop();
            line.close();
        }
        currentAmplitude = 0.0f;
        onTrackFinished = null;
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(vol, 1.0f));
        if (line != null && line.isOpen()) {
            try {
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);
            } catch (Exception e) { }
        }
    }

    public boolean isPlaying() { return isPlaying; }
}