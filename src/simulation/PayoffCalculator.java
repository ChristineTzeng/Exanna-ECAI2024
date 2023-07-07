package simulation;

import base.Action;
import base.Context;

public abstract class PayoffCalculator {

    //public abstract Payoffs calculate(Context context, base.Action calleeAction, boolean neighborAccept);

//    public abstract double calculateNeighborPayoff(Context context, Action calleeAction, boolean neighborAccept);
    public abstract double calculateObserverPayoff(Context context, Action actorAction, boolean accept);
    public abstract double calculateActorPayoff(Context context, Action actorAction);
    public abstract double calculateActorPayoff(Context context, Action actorAction, double[] dist);


    public static class Payoffs {
        public double actorPayoff;
        public double observerPayoff;
    }
}
