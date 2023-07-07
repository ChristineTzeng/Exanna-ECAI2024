package simulation;

import java.util.HashMap;
import java.util.Map;

import base.Relationship;

public class RelationshipStats {
    private Map<Relationship, Integer> stats;
    private int total;

    public RelationshipStats() {
        this.stats = new HashMap<>();
        this.total = 0;
    }

    public void add(Relationship relationship, int count) {
        int value = stats.getOrDefault(relationship, 0);
        stats.put(relationship, value + count);
        total += count;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("======== Relationship Stats ============").append("\n");
        for (Relationship relationship : Relationship.values()) {
            int stat = this.stats.getOrDefault(relationship, 0);
            builder.append(String.format("%s:== [total: %d, stat: %d, ratio (stat/total): %f]", relationship.name(),
                                                                                                        total, stat,
                                                                                            ((double)stat) / total))
                  .append("\n");
        }
        builder.append("========== Relationship Stats end ==============").append("\n");
        return builder.toString();
    }
}
