package base;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import simulation.AgentType;

/**
 * Represents a context. Maybe a partially filled context view.
 */
public class Context {
	public AgentType observerAgentType;
    public Health actorHealth;
    public Preference preference;
    public Relationship observerRelationship;
    public Location interactLocation;
	
	public Health sharedHealth;
	public Preference sharedPreference;

    public static Context fromVector(List<Object> vector) {
        Context context = new Context();
        context.actorHealth = (Health) vector.get(0);
        context.preference = (Preference) vector.get(1);
        context.observerAgentType = (AgentType) vector.get(2);
        context.observerRelationship = (Relationship) vector.get(3);
        context.interactLocation = (Location) vector.get(4);
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Context other) {
        return new Builder()
                .actorHealth(other.actorHealth)
                .preference(other.preference)
                .observerAgentType(other.observerAgentType)
                .observerRelationship(other.observerRelationship)
                .interactLocation(other.interactLocation);
    }

    public static class Builder {
        public Health actorHealth;
        public Preference preference;
        public Location interactLocation;
		private Relationship observerRelationship = null;
		private AgentType observerAgentType = null;
		public Health sharedHealth;
		public Preference sharedPreference;

        public Builder actorHealth(Health actorHealth) {
            this.actorHealth = actorHealth;
            return this;
        }
        
        public Builder sharedHealth(Health sharedHealth) {
            this.sharedHealth = sharedHealth;
            return this;
        }

        public Builder preference(Preference preference) {
            this.preference = preference;
            return this;
        }
        
        public Builder sharedPreference(Preference sharedPreference) {
            this.sharedPreference = sharedPreference;
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

        public Context build() {
            Context context = new Context();
            context.actorHealth = this.actorHealth;
            context.sharedHealth = this.sharedHealth;
            context.preference = this.preference;
            context.sharedPreference = this.sharedPreference;
            context.observerAgentType = this.observerAgentType;
//            context.observerHealth = this.observerHealth;
            context.observerRelationship = this.observerRelationship;
            context.interactLocation = this.interactLocation;
            return context;
        }
    }

    //Only fills missing values
    public Context merge(Context other) {
        Context context = Context.builder(this).build();
        if (context.actorHealth == null) context.actorHealth = other.actorHealth;
        if (context.preference == null) context.preference = other.preference;
        if (context.observerAgentType == null) context.observerAgentType = other.observerAgentType;
        if (context.observerRelationship == null) context.observerRelationship = other.observerRelationship;
        if (context.interactLocation == null) context.interactLocation = other.interactLocation;
        return context;
    }

    public void mergeFrom(Context other) {
        if (this.actorHealth == null) this.actorHealth = other.actorHealth;
        if (this.preference == null) this.preference = other.preference;
        if (this.observerAgentType == null) this.observerAgentType = other.observerAgentType;
        if (this.observerRelationship == null) this.observerRelationship = other.observerRelationship;
        if (this.interactLocation == null) this.interactLocation = other.interactLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Context context = (Context) o;
        return actorHealth == context.actorHealth &&
        		preference == context.preference &&
        		observerAgentType == context.observerAgentType &&
        		observerRelationship == context.observerRelationship &&
                interactLocation == context.interactLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorHealth, preference, observerAgentType, observerRelationship, interactLocation);
    }

    @Override
    public String toString() {
		return "Context{" +
                (actorHealth == null ? "" : ("actorHealth=" + actorHealth)) +
                (preference == null ? "" : ("; preference=" + preference)) +
                (observerAgentType == null ? "" : "; OberverAgentType" + observerAgentType.toString()) +
                (observerRelationship == null ? "" : ("; observerRelationship=" + observerRelationship)) +
                (interactLocation == null ? "" : ("; interactLocation=" + interactLocation)) +
                '}';
    }

    public List<Object> getVector() {
        List<Object> vector = new ArrayList<>();
        vector.add(actorHealth);
        vector.add(preference);
        vector.add(observerAgentType);
        vector.add(observerRelationship);
        vector.add(interactLocation);
        return vector;
    }

    public double getInformationCost() {
        double cost = 0;
        if (actorHealth != null) cost++;
        if (preference != null) cost++;
        return cost / 2;
    }
}
