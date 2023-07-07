package simulation;

/**
 *
 * @author Hui
 */
public class Feedback {
    public Interaction interaction;
    public Agent giver;
    public double payoff;//instead of boolean feedback
    
    public Feedback(Interaction interaction, Agent giver, double payoff){
        this.interaction = interaction;
        this.giver = giver;
        this.payoff = payoff;
    }
}
