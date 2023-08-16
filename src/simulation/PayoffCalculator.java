package simulation;

import base.Action;
import base.Context;

public abstract class PayoffCalculator {

    public abstract double calculateObserverPayoff(Context context, Action actorAction, boolean accept);
    public abstract double calculateActorPayoff(Context context, Action actorAction);
    public abstract double calculateActorPayoff(Context context, Action actorAction, double[] dist);


    public static class Payoffs {
        public double actorPayoff;
        public double observerPayoff;
    }
}
