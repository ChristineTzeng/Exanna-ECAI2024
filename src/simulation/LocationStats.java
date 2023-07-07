package simulation;

import java.util.HashMap;
import java.util.Map;

import base.Location;

public class LocationStats {
    private Map<Location, Integer> stats;
    private int total;

    public LocationStats() {
        this.stats = new HashMap<>();
        this.total = 0;
    }

    public void add(Location location, int count) {
        int value = stats.getOrDefault(location, 0);
        stats.put(location, value + count);
        total += count;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("======== Location Stats ============").append("\n");
        for (Location location : Location.values()) {
            int stat = this.stats.getOrDefault(location, 0);
            builder.append(String.format("%s:== [total: %d, stat: %d, ratio (stat/total): %f]", location.name(),
                            total, stat,
                            ((double)stat) / total))
                    .append("\n");
        }
        builder.append("========== Location Stats end ==============").append("\n");
        return builder.toString();
    }
}
