package simulation;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

import base.Action;
import base.Context;
import base.Location;
import simulation.ExplanationStats.StatisticsUtil;

public abstract class AgentTypeStats {
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
        public Map<AgentType, Map<AgentType, StatisticsUtil>> agentTypeStats;

        public Statistics() {
            this.globalStats = new StatisticsUtil();
            this.agentTypeStats = new LinkedHashMap<>();
            for (AgentType actorAgentType : AgentType.values()) {
            	this.agentTypeStats.put(actorAgentType, new LinkedHashMap<>());
	            for (AgentType observerAgentType : AgentType.values()) {
	        	  this.agentTypeStats.get(actorAgentType).put(observerAgentType, new StatisticsUtil());
	          	}
            }
            
        }

        public void addAccept(Action action) {
            this.globalStats.addAccept(action);
        }
        
        public void addAccept(Action action, AgentType actorAgentType, AgentType observerAgentType) {
            this.globalStats.addAccept(action);
            this.agentTypeStats.get(actorAgentType).get(observerAgentType).addAccept(action);
        }

        public void addReject(Action action) {
            this.globalStats.addReject(action);
        }

        public void addReject(Action action, AgentType actorAgentType, AgentType observerAgentType) {
            this.globalStats.addReject(action);
            this.agentTypeStats.get(actorAgentType).get(observerAgentType).addReject(action);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("===========================\n");
            builder.append(this.globalStats.toString()).append("\n");
            for (AgentType actorAgentType : AgentType.values()) {
            	for (AgentType observerAgentType : AgentType.values()) {
            		builder.append(String.format("%s vs %s stats: %s\n", actorAgentType.name(), observerAgentType.name(),
                            this.agentTypeStats.get(actorAgentType).get(observerAgentType).toString()));
            	}
            }
            builder.append("===========================\n");
            return builder.toString();
        }
    }
}
