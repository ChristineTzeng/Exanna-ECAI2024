package base;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a norm
 */
public class Norm {
    private Predicate<Context> antecedent;
    public Action consequent;
    private Context conditions;

    public Norm(Context conditions, Action consequent) {
        this.conditions = conditions;
        this.consequent = consequent;
        this.antecedent = getAntecedent(this.conditions);
    }

    private static Predicate<Context> getAntecedent(Context conditions) {
        return context -> {
            if (conditions.actorHealth != null && conditions.actorHealth != context.actorHealth)  return false;
            if (conditions.preference != null && conditions.preference != context.preference)  return false;
            if (conditions.observerAgentType != null && conditions.observerAgentType != context.observerAgentType)  return false;
            if (conditions.observerRelationship != null && conditions.observerRelationship != context.observerRelationship) return false;
            if (conditions.interactLocation != null && conditions.interactLocation != context.interactLocation) return false;
            return true;
        };
    }

    public boolean triggers(Context context) {
        return antecedent.test(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Norm norm = (Norm) o;
        return consequent == norm.consequent &&
                Objects.equals(conditions, norm.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consequent, conditions);
    }

    @Override
    public String toString() {
        return "Norm{" +
                "consequent=" + consequent +
                "; conditions=" + conditions +
                '}';
    }

    public Context getConditions() {
        return conditions;
    }

    public void setConditions(Context conditions) {
        this.conditions = conditions;
        this.antecedent = getAntecedent(this.conditions);
    }
    
    public Norm setConditionWithAttribute(String target) {
    	Norm copy = new Norm(this.conditions, this.consequent);
    	if ("health".equals(target)) {
    		copy.conditions.actorHealth = null;
    	} else if ("freedom".equals(target)) {
    		copy.conditions.preference = null;
    	}
    	return copy;
    }

    public boolean subsumes(Norm other) {
        return this.triggers(other.getConditions()) && this.consequent == other.consequent;
    }
}
