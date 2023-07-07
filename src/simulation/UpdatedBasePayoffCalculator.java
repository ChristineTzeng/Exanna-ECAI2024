package simulation;

import base.Action;
import base.Context;
import base.Location;
import base.Preference;
import base.Relationship;
import base.Values;

import java.util.HashMap;

public class UpdatedBasePayoffCalculator extends PayoffCalculator {
	private double locationMultiplier = 0.5;
	private double relationshipMultiplier = 0;
	private double generalMultiplier = 1;
	private double sanctionMultiplier = 1;
    
    private HashMap<Location, double[]> locationPayoffs;
    private HashMap<Relationship, double[]> relationshipPayoffs;
//    private HashMap<Values, double[]> generalPayoffs;
    private HashMap<Values, double[]> generalPayoffs;
    private HashMap<Relationship, double[]> sanctionPayoffs;

    public UpdatedBasePayoffCalculator() {
    	locationPayoffs = new HashMap<Location, double[]>();
    	locationPayoffs.put(Location.HOME, new double[] {-0.5, 0.5});
    	locationPayoffs.put(Location.OFFICE, new double[] {0.5, -0.5});
    	locationPayoffs.put(Location.PARTY, new double[] {-0.5, 0.5});
    	locationPayoffs.put(Location.PARK, new double[] {-1.0, 1.0});
    	locationPayoffs.put(Location.HOSPITAL, new double[] {1.0, -1.0});
    	
    	relationshipPayoffs = new HashMap<Relationship, double[]>();
    	relationshipPayoffs.put(Relationship.FAMILY, new double[] {-1.0, 1.0});
    	relationshipPayoffs.put(Relationship.FRIEND, new double[] {-0.5, 0.5});
    	relationshipPayoffs.put(Relationship.COLLEAGUE, new double[] {0.5, -0.5});
    	relationshipPayoffs.put(Relationship.STRANGER, new double[] {1.0, -1.0});
    	
//    	generalPayoffs = new HashMap<Values, double[]>();
    	generalPayoffs = new HashMap<Values, double[]>();
    	// payoff table for approving/disapproving actions 
//    	generalPayoffs.put(Values.HEALTH, new double[] {0.5, 0.5, 1.0, -0.5, -0.5, -1});
//    	{healthy, allergy, infected} for wear, not wear for healthy, infected
//    	generalPayoffs.put(Values.HEALTH, new double[][] {{0.0, 0.0, 1.0, 0.0, 0.0, -1}, {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}});
//    	generalPayoffs.put(Values.HEALTH, new double[][] {{0.0, 0.0, 1.0, 0.0, 0.0, -1}, {0.0, 0.0, 0.0, 0.5, 0.5, 0.5}});
    	// updated healthy risk settings
    	generalPayoffs.put(Values.HEALTH, new double[] {0.0, 1.0, 0.0, 0.0, -1});
//    	generalPayoffs.put(Values.FREEDOM, new double[] {});
    	generalPayoffs.put(Values.FREEDOM, new double[] {});
    	
    	sanctionPayoffs = new HashMap<Relationship, double[]>();
    	sanctionPayoffs.put(Relationship.FAMILY, new double[] {1.0, -1.0});
//    	sanctionPayoffs.put(Relationship.FRIEND, new double[] {0.33, -0.67});
//    	sanctionPayoffs.put(Relationship.COLLEAGUE, new double[] {0.67, -1});
//    	sanctionPayoffs.put(Relationship.STRANGER, new double[] {0, -0.33});
    	sanctionPayoffs.put(Relationship.FRIEND, new double[] {0.75, -0.75});
    	sanctionPayoffs.put(Relationship.COLLEAGUE, new double[] {0.5, -5});
    	sanctionPayoffs.put(Relationship.STRANGER, new double[] {0.25, -0.25});
    }
    
    @Override
    public double calculateActorPayoff(Context context, Action actorAction) {
    	return 0;
    }
    
    @Override
    public double calculateActorPayoff(Context context, Action actorAction, double[] dist) {
    	double payoff = 0.0;
    	
    	payoff += locationPayoffs.get(context.interactLocation)[actorAction.value] * locationMultiplier;
    	
    	if (context.observerRelationship != null) {
    		payoff += relationshipPayoffs.get(context.observerRelationship)[actorAction.value] * relationshipMultiplier;
//    		payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[(int) (actorAction.value * 3 + context.observerHealth.getValue())] * generalMultiplier;
//    		if (context.actorHealth.value == 2) {
//    			payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[1][(int) (actorAction.value * 3 + context.observerHealth.getValue())] * generalMultiplier;
//    		} else {
//    			payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[0][(int) (actorAction.value * 3 + context.observerHealth.getValue())] * generalMultiplier;
//    		}
    	} else {
    		// if no other agent around, assume it's safe environment
//    		payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[(int) (actorAction.value * 3)] * generalMultiplier;
//    		payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[0][(int) (actorAction.value * 3)] * generalMultiplier;
    	}
    	
    	payoff += dist[0] * generalPayoffs.get(Values.HEALTH)[(int) (actorAction.value * 2  + context.actorHealth.getValue())] * generalMultiplier;
    	double[] prefPayoff = context.preference == Preference.WEAR ? new double[] {1.0, 1.0, -1.0, -1.0} : new double[] {-1.0, -1.0, 1.0, 1.0}; 
    	payoff += dist[1] * prefPayoff[actorAction.value * 2] * generalMultiplier;
    	
		return payoff;
    }

    @Override
    public double calculateObserverPayoff(Context context, Action actorAction, boolean accept) {
    	if (accept) {
    		return sanctionPayoffs.get(context.observerRelationship)[0] * sanctionMultiplier;
    	} else {
    		return sanctionPayoffs.get(context.observerRelationship)[1] * sanctionMultiplier;
    	}
    }
    
    public double calculateInfoCost(Context context) {
    	return 0;
    }
}
