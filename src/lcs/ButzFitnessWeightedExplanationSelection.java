package lcs;

import base.Action;
import base.Explanation;
import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButzFitnessWeightedExplanationSelection implements ExplanationSelectionStrategy {
    private MersenneTwisterFast random;

    public ButzFitnessWeightedExplanationSelection(MersenneTwisterFast random) {
        this.random = random;
    }

    @Override
    public List<Explanation> acceptableExplanations(ExplanationRuleSet matchSet, boolean debug) {
        Map<Explanation, Double> actionToNum = new HashMap<>();
        Map<Explanation, Double> actionToDenom = new HashMap<>();
        Explanation[] explanations = Explanation.values();
        for (Explanation explanation : explanations) {
            for (ExplanationRule rule : matchSet.getRules()) {
                if (rule.norm.consequent == explanation) {
                    double num = actionToNum.getOrDefault(explanation, 0.0);
                    num += rule.fitness * rule.rewardPrediction;
                    actionToNum.put(explanation, num);
                    double denom = actionToDenom.getOrDefault(explanation, 0.0);
                    denom += rule.fitness;
                    actionToDenom.put(explanation, denom);
                }
            }
        }
        List<Explanation> possibleExplanations = new ArrayList<>();
        double curr = Double.NEGATIVE_INFINITY;
        for (Explanation explanation : explanations) {
//            double vote = actionToVote.getOrDefault(action, Double.NEGATIVE_INFINITY);
            Double num = actionToNum.get(explanation);
            if (num == null) continue;
            double denom = actionToDenom.getOrDefault(explanation, 1.0);
            double vote = (denom == 0) ? num : (num / denom);
            if (vote > curr) {
            	possibleExplanations.clear();;
            	possibleExplanations.add(explanation);
                curr = vote;
            } else if (vote == curr) {
            	possibleExplanations.add(explanation);
            }
        }
//        if (debug && possibleActions.contains(Action.IGNORE)) {
//            Debugger.debug(possibleActions, "possibleActions");
//            matchSet.print();
//        }
        return possibleExplanations;
    }
    
	@Override
	public Explanation selectExplanation(ExplanationRuleSet matchSet, boolean debug) {
		List<Explanation> possibleExplanations = acceptableExplanations(matchSet, debug);
        return possibleExplanations.get(random.nextInt(possibleExplanations.size()));
//        return possibleExplanations.get(random.nextInt(possibleExplanations.size()));
	}

}
