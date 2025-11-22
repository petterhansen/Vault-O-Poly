package radio;

import java.util.*;

public class RadioStation {
    private String stationName;
    // Note: String, NOT File
    private List<String> trackUrls = new ArrayList<>();
    private Queue<String> playQueue = new LinkedList<>();
    private Random random = new Random();

    public RadioStation(String name) {
        this.stationName = name;
    }

    public void addTrack(String url) {
        if (url != null && !url.isEmpty()) trackUrls.add(url);
    }

    public String getNextTrackUrl() {
        if (playQueue.isEmpty()) reshuffle();
        return playQueue.poll();
    }

    private void reshuffle() {
        if (trackUrls.isEmpty()) return;
        List<String> shuffled = new ArrayList<>(trackUrls);
        Collections.shuffle(shuffled, random);
        playQueue.addAll(shuffled);
    }

    public String getName() { return stationName; }
    public boolean hasTracks() { return !trackUrls.isEmpty(); }
}