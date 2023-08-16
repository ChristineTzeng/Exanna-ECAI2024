package simulation;

import base.Action;
import base.Context;
import base.Health;
import base.Location;
import base.Preference;

import util.Debugger;

public class RecordForLearning {
    
    public static String[] relationTypes = new String[]{"family", "colleague", "friend", "stranger"};
    
	public int observerAgentType;
    public int actorHealth;
    public int preference;
    
    public int location;
    public int observerRelationship;
    public int action; //0 for wear, 1 for not wear
    public boolean existsObserver;
    public boolean existsFamily;
    public boolean existsColleague;
    public boolean existsFriend;
    public double observerPayoff;
    public double actorPayoff;
//    public double averageNeighborPayoff;
    public Agent agent;
    
    public RecordForLearning(){
        location = 0;
        observerRelationship = 3;
        action = 0;
        existsObserver = false;
        existsFamily = false;
        existsColleague = false;
        existsFriend = false;
        actorPayoff = 0.0;
        observerPayoff = 0.0;
    }
    
    public RecordForLearning(Interaction interaction, Agents agents, Agent agent){
        this.agent = agent;
        this.location = (int)(interaction.location/Agents.numAgents);
        this.actorHealth = interaction.actorHealth;
        this.preference = interaction.actor.preference;
        if (interaction.observer == null) {
        	this.existsObserver = false;
        	this.observerRelationship = -1;
        	this.observerAgentType = -1;
        } else {
        	this.existsObserver = true;
        	this.observerAgentType = interaction.observer.agentType.getValue();
	        if (interaction.isFamily())
	            this.observerRelationship = 0;
	        else if (interaction.isColleague())
	            this.observerRelationship = 1;
	        else if (interaction.isFriend())
	            this.observerRelationship = 2;
	        else
	            this.observerRelationship = 3;
        }
        
        this.action = interaction.action;
        
        this.existsFamily = false;
        this.existsColleague = false;
        this.existsFriend = false;

        //callee payoff and caller payoff
        this.observerPayoff = 0.0;
        this.actorPayoff = 0.0;
        if (interaction.observer != null) {
	        actorPayoff = agents.payoffCalculator.calculateActorPayoff(
	        		Context.builder().interactLocation(Location.get(this.location))
	   				 .actorHealth(Health.get(interaction.actor.health))
	   				 .preference(Preference.get(interaction.actor.preference))
	   				 .observerAgentType(interaction.observer.agentType)
	   				 .observerRelationship(interaction.getInteractionRelationship())
	   				 .build(), Action.fromID(interaction.action), interaction.actor.agentType.weights);
		
		    Feedback feedback;
		    for(int i=0;i<interaction.feedbacks.size();i++){
		        feedback = (Feedback) interaction.feedbacks.get(i);
		        if (feedback.giver.familyCircle==interaction.actor.familyCircle)
		            this.existsFamily = true;
		        if (feedback.giver.colleagueCircle==interaction.actor.colleagueCircle)
		            this.existsColleague = true;
		        if (feedback.giver.friendCircle==interaction.actor.friendCircle)
		            this.existsFriend = true;
		        
		        observerPayoff+= feedback.payoff;
		    }
        } else {
        	actorPayoff = agents.payoffCalculator.calculateActorPayoff(
        			Context.builder().interactLocation(Location.get(this.location))
      				 .actorHealth(Health.get(interaction.actor.health))
      				 .preference(Preference.get(interaction.actor.preference))
      				 .observerRelationship(interaction.getInteractionRelationship())
      				 .build(), Action.fromID(interaction.action), interaction.actor.agentType.weights);
        }
    }
    
    public double getPayoff(){
    	if (existsObserver) {
    		return (actorPayoff + observerPayoff)/2.0;
    	}
        return actorPayoff;
    }
    
    public double getObserverPayoff(){
        return observerPayoff;
    }
    
    public String toCSVString(){
        return Agents.locations[location] + ","
        		+ actorHealth + ","
                + (existsObserver ? relationTypes[observerRelationship] : "None") + ","
                + existsFamily + ","
                + existsColleague + ","
                + existsFriend + ","
                + (action == 0 ? "Wear" : "Not wear") + ","
                + actorPayoff + ","
                + observerPayoff;
    }
    
}
