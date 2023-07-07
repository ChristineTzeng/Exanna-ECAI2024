package simulation;

import base.Action;
import base.Context;
import base.Health;
import base.Location;
import base.Preference;

import util.Debugger;

/**
 *
 * @author Hui
 */
public class RecordForLearning {
    
    public static String[] relationTypes = new String[]{"family", "colleague", "friend", "stranger"};
    
//    public int actorAgentType;
	public int observerAgentType;
    public int actorHealth;
    public int preference;
//    public int observerHealth;
    
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
//        this.actorAgentType = interaction.actor.agentType.getValue();
        this.actorHealth = interaction.actorHealth;
        this.preference = interaction.actor.preference;
        if (interaction.observer == null) {
        	this.existsObserver = false;
        	this.observerRelationship = -1;
//        	this.observerHealth = -1;
        	this.observerAgentType = -1;
        } else {
        	this.existsObserver = true;
//        	this.observerHealth = interaction.observerHealth;
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
//	   				 .actorAgentType(interaction.actor.agentType)
//	   				 .actorHealth(Health.get(interaction.actorHealth))
	   				 .actorHealth(Health.get(interaction.actor.health))
	   				 .preference(Preference.get(interaction.actor.preference))
	   				 .observerAgentType(interaction.observer.agentType)
//	   				 .observerHealth(Health.get(interaction.observerHealth))
	   				 .observerRelationship(interaction.getInteractionRelationship())
	   				 .build(), Action.fromID(interaction.action), interaction.actor.agentType.weights);
		//	        observerPayoff = agents.payoffCalculator.calculateObserverPayoff(
		//    		Context.builder().interactLocation(Location.get(interaction.location))
		//                             .observerRelationship(Agent.getInteractionRelationship(interaction))
		//                             .build(), Action.fromID(interaction.action));
		
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
//      				 .actorAgentType(interaction.actor.agentType)
//      				 .actorHealth(Health.get(interaction.actorHealth))
      				 .actorHealth(Health.get(interaction.actor.health))
      				 .preference(Preference.get(interaction.actor.preference))
      				 .observerRelationship(interaction.getInteractionRelationship())
      				 .build(), Action.fromID(interaction.action), interaction.actor.agentType.weights);
        }
//        Debugger.debug("actorPayoff", actorPayoff, "observerPayoff", observerPayoff);
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
    
//    public int getOrdinalFeedback(){
//        double payoff = this.getPayoff();
//        if (payoff>=1) return 2;
//        else if (payoff>=0.5)
//            return 1;
//        else if (payoff>=-0.5)
//            return 0;
//        else if (payoff>=-1)
//            return -1;
//        else
//            return -2;
//    }
    
    public String toCSVString(){
        return Agents.locations[location] + ","
//        		+ actorAgentType + ","
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
