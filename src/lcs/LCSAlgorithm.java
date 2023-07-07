package lcs;

import base.Action;
import base.Context;
import base.Explanation;
import base.ExplanationNorm;
import base.Norm;
import simulation.AgentType;

import java.util.List;
import java.util.Map;

public interface LCSAlgorithm {
    Action getDecision(Context context);
    List<Norm> explainDecision(Context context, Action action);
    void learn(Context context, Action action, double reward);
    List<Action> getAcceptableDecisions(Context context);
    List<Action> getAcceptableDecisions(Context context, boolean acceptingByDefault);
    List<Action> getAcceptableDecisions(List<Norm> arguments);
    List<Action> getAcceptableDecisions(List<Norm> arguments, boolean acceptingByDefault);
    List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext);
    List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext, boolean acceptingByDefault);
    void printStats();
    void voteNorms(Map<Norm, Double> voteMap);
    
    List<Norm> filterValues(List<Norm> rules, List<AgentType> types);
    
    Explanation getExplanation(Context context);
    List<ExplanationNorm> explainDecision(Context context, Explanation explanation);
    void learn(Context context, Explanation explanation, double reward);
    void voteExplanationNorms(Map<ExplanationNorm, Double> voteMap);
	int[] sharedInfo(List<Norm> lcsNewExplanation);
}