package simulation;//import sim.util.*;

import base.Norm;
import base.Relationship;
import rulebasedrl.MatchingSet;
import sim.util.Bag;
import simulation.ExplanationStats;

import java.util.List;
import java.util.Random;

/**
 *
 * @author Hui
 */
public class Interaction {
    public Agent actor;
    public Agent observer;
    
    public int action; //0 for wear, 1 for not wear.
    public int explanation; //0 for none, 1 for health, 2 for preference, 3 for all
    public int location; //keep location, since agents move around
    public int actorHealth; //keep health, since observations may be wrong
    public int observerHealth; //keep health, since observations may be wrong
    
    public Bag feedbacks;
    
    public long step; //keep step number
    public ExplanationStats explanationStats;
    public ExplanationStats.Statistics explanationStatistics;
    public List<Norm> lcsNewExplanation;
    public MatchingSet matchingSet;
    public long timeToDecide;
    public long timeToGiveFeedbacks;
    public double informationCost;

    public Interaction(Agent actor, int location, long step){
        this.actor = actor;
        this.observer = null;
        this.action = -1;
        this.explanation = 0;
        this.feedbacks = new Bag();
        this.location = actor.location;
        this.step = step;
        this.timeToGiveFeedbacks = 0;
        this.observerHealth = -1;
        
//        // observed health state for others
//        if (this.actor.health != 0)
////        	this.actorHealth = new Random().nextBoolean() ? 1 : 2;
//        	this.actorHealth = 2;
////        else
//////        	this.actorHealth = 0;
////        	this.actorHealth = new Random().nextBoolean() ? 0 : 2;
    }
    
    public Interaction(Agent actor, Agent observer, int location, long step){
        this.actor = actor;
        this.observer = observer;
        this.action = -1;
        this.feedbacks = new Bag();
        this.location = location;
        this.step = step;
        this.timeToGiveFeedbacks = 0;
        
//        // observed health state for others
//        if (this.actor.health != 0)
////        	this.actorHealth = new Random().nextBoolean() ? 1 : 2;
//        	this.actorHealth = 2;
////        else
//////        	this.actorHealth = 0;
////        	this.actorHealth = new Random().nextBoolean() ? 0 : 2;
//        
//        // observed health state for actor
//        if (this.observer.health != 0)
////        	this.observerHealth = new Random().nextBoolean() ? 1 : 2;
//        	this.observerHealth = 2;
////        else
//////        	this.observerHealth = 0;
////        	this.observerHealth = new Random().nextBoolean() ? 0 : 2;
    }
    
    public void switchRoles() {
    	Agent temp = this.observer;
    	int tempHealth = this.observerHealth;
    	this.observer = this.actor;
    	this.actor = temp;
    	this.observerHealth = this.actorHealth;
    	this.actorHealth = tempHealth;
    }
    
    public boolean hasNoObserver() {
    	return observer == null;
    }
    
    public boolean isFamily() {
        return observer != null && actor.familyCircle == observer.familyCircle;
    }
    
    public boolean isColleague(){
        return observer != null && actor.colleagueCircle == observer.colleagueCircle;
    }
    
    public boolean isFriend(){
        return observer != null && actor.friendCircle == observer.friendCircle;
    }
    
    public boolean isStranger(){
    	if (observer != null) {
    		if (isFamily() || isColleague() || isFriend())
                return false;
            else
                return true;
    	} else return false;
    	
    }
    
    public Relationship getInteractionRelationship() {
        if (this.isFamily()) return Relationship.FAMILY;
        if (this.isFriend()) return Relationship.FRIEND;
        if (this.isColleague()) return Relationship.COLLEAGUE;
        if (this.isStranger()) return Relationship.STRANGER;
        return null;
    }
}
