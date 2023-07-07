package simulation;

import base.Action;
import base.Context;
import base.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ExplanationStats {
    public abstract boolean accept(Agent agent, Action calleeAction, Context neighborContext);

    public static class StatisticsUtil {
        public int noOfAccepts;
        public int noOfRejects;
        public int wearAccepts;
        public int notWearAccepts;
        public int wearRejects;
        public int notWearRejects;

        public void StatisticsUtil() {
            this.noOfAccepts = 0;
            this.noOfRejects = 0;
            this.wearAccepts = 0;
            this.notWearAccepts = 0;
            this.wearRejects = 0;
            this.notWearRejects = 0;
        }

        public void addAccept(Action action) {
            this.noOfAccepts++;
            if (action == Action.WEAR) wearAccepts++;
            else notWearAccepts++;
        }

        public void addReject(Action action) {
            this.noOfRejects++;
            if (action == Action.WEAR) wearRejects++;
            else notWearRejects++;
        }

        @Override
        public String toString() {
            return String.format("[total: %d, accepts: %d, rejects: %d, resolution: %f, wearAccepts: %d, " +
                            "notWearAccepts: %d, " +
                            "wearRejects: %d, notWearRejects: %d]",
                    noOfAccepts + noOfRejects, noOfAccepts, noOfRejects,
                    ((double)noOfAccepts) / (noOfAccepts + noOfRejects) * 100, wearAccepts,
                    notWearAccepts, wearRejects, notWearRejects);
        }
    }

    public static class Statistics {
        public StatisticsUtil globalStats;
        public Map<Location, StatisticsUtil> regionalStats;

        public Statistics() {
            this.globalStats = new StatisticsUtil();
            this.regionalStats = new LinkedHashMap<>();
            for (Location location : Location.values()) {
                this.regionalStats.put(location, new StatisticsUtil());
            }
        }

        public void addAccept(Action action) {
            this.globalStats.addAccept(action);
        }

        public void addAccept(Action action, Location location) {
            this.globalStats.addAccept(action);
            this.regionalStats.get(location).addAccept(action);
        }

        public void addReject(Action action) {
            this.globalStats.addReject(action);
        }

        public void addReject(Action action, Location location) {
            this.globalStats.addReject(action);
            this.regionalStats.get(location).addReject(action);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("===========================\n");
            builder.append(this.globalStats.toString()).append("\n");
            for (Location location : Location.values()) {
                builder.append(String.format("%s stats: %s\n", location.name(),
                        this.regionalStats.get(location).toString()));
            }
            builder.append("===========================\n");
            return builder.toString();
        }
    }
}
