package base;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import simulation.AgentType;

/**
 * Represents a context. Maybe a partially filled context view.
 */
public class ExplanationContext {
	public AgentType observerAgentType;
    public Health actorHealth;
    public Preference preference;
    public Action action;
    public Relationship observerRelationship;
    public Location interactLocation;

    public static ExplanationContext fromVector(List<Object> vector) {
    	ExplanationContext context = new ExplanationContext();
        context.actorHealth = (Health) vector.get(0);
        context.preference = (Preference) vector.get(1);
        context.observerAgentType = (AgentType) vector.get(2);
        context.observerRelationship = (Relationship) vector.get(3);
        context.interactLocation = (Location) vector.get(4);
        context.action = (Action) vector.get(5);
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ExplanationContext other) {
        return new Builder()
                .actorHealth(other.actorHealth)
                .preference(other.preference)
                .observerAgentType(other.observerAgentType)
                .observerRelationship(other.observerRelationship)
                .interactLocation(other.interactLocation)
                .action(other.action);
    }

    public static class Builder {
        public Health actorHealth;
        public Preference preference;
        public Location interactLocation;
		private Relationship observerRelationship = null;
		private AgentType observerAgentType = null;
		private Action action = null;

        public Builder actorHealth(Health actorHealth) {
            this.actorHealth = actorHealth;
            return this;
        }

        public Builder preference(Preference preference) {
            this.preference = preference;
            return this;
        }
        
        public Builder observerAgentType(AgentType observerAgentType) {
            this.observerAgentType = observerAgentType;
            return this;
        }

        public Builder observerRelationship(Relationship observerRelationship) {
            this.observerRelationship = observerRelationship;
            return this;
        }

        public Builder interactLocation(Location interactLocation) {
            this.interactLocation = interactLocation;
            return this;
        }
        
        public Builder action(Action action) {
            this.action = action;
            return this;
        }
        
        public ExplanationContext context(Context from) {
        	ExplanationContext context = new ExplanationContext();
            context.actorHealth = from.actorHealth;
            context.preference = from.preference;
            context.observerAgentType = from.observerAgentType;
            context.observerRelationship = from.observerRelationship;
            context.interactLocation = from.interactLocation;
            return context;
        }

        public ExplanationContext build() {
        	ExplanationContext context = new ExplanationContext();
            context.actorHealth = this.actorHealth;
            context.preference = this.preference;
            context.observerAgentType = this.observerAgentType;
            context.observerRelationship = this.observerRelationship;
            context.interactLocation = this.interactLocation;
            context.action = this.action;
            return context;
        }
    }

    //Only fills missing values
    public ExplanationContext merge(ExplanationContext other) {
    	ExplanationContext context = ExplanationContext.builder(this).build();
        if (context.actorHealth == null) context.actorHealth = other.actorHealth;
        if (context.preference == null) context.preference = other.preference;
        if (context.observerAgentType == null) context.observerAgentType = other.observerAgentType;
        if (context.observerRelationship == null) context.observerRelationship = other.observerRelationship;
        if (context.interactLocation == null) context.interactLocation = other.interactLocation;
        if (context.action == null) context.action = other.action;
        return context;
    }

    public void mergeFrom(ExplanationContext other) {
        if (this.actorHealth == null) this.actorHealth = other.actorHealth;
        if (this.preference == null) this.preference = other.preference;
        if (this.observerAgentType == null) this.observerAgentType = other.observerAgentType;
        if (this.observerRelationship == null) this.observerRelationship = other.observerRelationship;
        if (this.interactLocation == null) this.interactLocation = other.interactLocation;
        if (this.action == null) this.action = other.action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExplanationContext context = (ExplanationContext) o;
        return actorHealth == context.actorHealth &&
        		preference == context.preference &&
        		observerAgentType == context.observerAgentType &&
        		observerRelationship == context.observerRelationship &&
                interactLocation == context.interactLocation &&
                action == context.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorHealth, preference, observerAgentType, observerRelationship, interactLocation, action);
    }

    @Override
    public String toString() {
		return "Context{" +
                (actorHealth == null ? "" : ("actorHealth=" + actorHealth)) +
                (preference == null ? "" : ("; preference=" + preference)) +
                (observerAgentType == null ? "" : "; OberverAgentType" + observerAgentType.toString()) +
                (observerRelationship == null ? "" : ("; observerRelationship=" + observerRelationship)) +
                (interactLocation == null ? "" : ("; interactLocation=" + interactLocation)) +
                (action == null ? "" : ("; action=" + action)) +
                '}';
    }

    public List<Object> getVector() {
        List<Object> vector = new ArrayList<>();
        vector.add(actorHealth);
        vector.add(preference);
        vector.add(observerAgentType);
        vector.add(observerRelationship);
        vector.add(interactLocation);
        vector.add(action);
        return vector;
    }
}
