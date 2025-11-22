package radio;

import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class RadioController {

    private List<RadioStation> stations;
    private int currentStationIndex = 0;
    private MusicPlayer player;
    private boolean isOn = false;
    private RadioUpdateListener uiListener;

    public interface RadioUpdateListener {
        void onTrackChanged(String title);
        void onStationChanged(String stationName);
    }

    public RadioController() {
        this.stations = new ArrayList<>();
        this.player = new MusicPlayer();
        initializeStations();
    }

    private void initializeStations() {
        // --- FIX: Attempt to load from config file first ---
        File configFile = new File("radio.json");
        if (configFile.exists()) {
            // Simple parser logic: Line 1 = Name, Line 2...N = URLs, Empty line = Next Station
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                RadioStation current = null;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("[")) {
                        // New Station: [Station Name]
                        if (current != null && current.hasTracks()) stations.add(current);
                        String name = line.substring(1, line.length() - 1);
                        current = new RadioStation(name);
                    } else if (current != null && (line.startsWith("http") || line.startsWith("file:"))) {
                        current.addTrack(line);
                    }
                }
                if (current != null && current.hasTracks()) stations.add(current);

                if (!stations.isEmpty()) {
                    System.out.println("Loaded radio stations from radio.json");
                    return; // Successfully loaded custom config
                }
            } catch (Exception e) {
                System.err.println("Failed to load radio.json, falling back to defaults.");
            }
        }

        // --- FALLBACK: Default Hardcoded Stations ---
        RadioStation gnr = new RadioStation("Galaxy News Radio");
        gnr.addTrack("https://archive.org/download/Fallout_3_galaxy_news_radio-2008/Allan%20Gray%20-%20Swing%20Doors.mp3");
        gnr.addTrack("https://archive.org/download/Fallout_3_galaxy_news_radio-2008/Billie%20Holiday%20-%20Crazy%20He%20Calls%20Me.mp3");
        // ... (Other tracks omitted for brevity)

        RadioStation rnv = new RadioStation("Radio New Vegas");
        rnv.addTrack("https://archive.org/download/Fallout_New_Vegas_Radio_New_Vegas/Ain't%20That%20a%20Kick%20in%20the%20Head.mp3");
        rnv.addTrack("https://archive.org/download/Fallout_New_Vegas_Radio_New_Vegas/Big%20Iron.mp3");

        RadioStation classical = new RadioStation("Classical Radio");
        // Placeholder for classical music

        if (gnr.hasTracks()) stations.add(gnr);
        if (rnv.hasTracks()) stations.add(rnv);
        if (classical.hasTracks()) stations.add(classical);
    }

    public void togglePower() {
        if (isOn) {
            turnOff();
        } else {
            isOn = true;
            playNext();
        }
    }

    public float getCurrentLevel() {
        return player.getCurrentAmplitude();
    }

    public void turnOff() {
        isOn = false;
        player.stop();
        if (uiListener != null) uiListener.onTrackChanged("OFFLINE");
    }

    public void nextStation() {
        if (stations.isEmpty()) return;
        currentStationIndex = (currentStationIndex + 1) % stations.size();
        playNext();
        if (uiListener != null) uiListener.onStationChanged(getCurrentStationName());
    }

    private void playNext() {
        if (!isOn || stations.isEmpty()) return;

        RadioStation station = stations.get(currentStationIndex);
        String trackUrl = station.getNextTrackUrl();

        if (trackUrl != null) {
            String displayName = "Unknown Track";
            try {
                String filename = trackUrl.substring(trackUrl.lastIndexOf('/') + 1);
                displayName = java.net.URLDecoder.decode(filename, "UTF-8");
                displayName = displayName.replace(".mp3", "").replace(".ogg", "").replace(".wav", "");
            } catch (Exception e) { }

            if (uiListener != null) uiListener.onTrackChanged(displayName);
            player.play(trackUrl, this::playNext);
        }
    }

    public void setVolume(float vol) {
        player.setVolume(vol);
    }

    public void setListener(RadioUpdateListener listener) {
        this.uiListener = listener;
    }

    public String getCurrentStationName() {
        if (stations.isEmpty()) return "No Signal";
        return stations.get(currentStationIndex).getName();
    }
}