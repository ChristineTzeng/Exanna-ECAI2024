package lcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import base.Explanation;
import ec.util.MersenneTwisterFast;

public class FitnessWeightedExplanationSelection implements ExplanationSelectionStrategy {
	private MersenneTwisterFast random;

    public FitnessWeightedExplanationSelection(MersenneTwisterFast random) {
        this.random = random;
    }

    @Override
    public List<Explanation> acceptableExplanations(ExplanationRuleSet matchSet, boolean debug) {
        Map<Explanation, Double> explanationToNum = new HashMap<>();
        Map<Explanation, Double> explanationToDenom = new HashMap<>();
        Explanation[] explanations = Explanation.values();
        for (Explanation explanation : explanations) {
            for (ExplanationRule rule : matchSet.getRules()) {
                if (rule.norm.consequent == explanation) {
                    double num = explanationToNum.getOrDefault(explanation, 0.0);
                    num += rule.fitness * rule.numerosity * rule.rewardPrediction;
                    explanationToNum.put(explanation, num);
                    double denom = explanationToDenom.getOrDefault(explanation, 0.0);
                    denom += rule.fitness * rule.numerosity;
                    explanationToDenom.put(explanation, denom);
                }
            }
        }
        List<Explanation> possibleExplanations = new ArrayList<>();
        double curr = Double.NEGATIVE_INFINITY;
        for (Explanation action : explanations) {
            Double num = explanationToNum.get(action);
            if (num == null) continue;
            double denom = explanationToDenom.getOrDefault(action, 1.0);
            double vote = (denom == 0) ? (Double.NEGATIVE_INFINITY) : (num / denom);
            if (vote > curr) {
            	possibleExplanations.clear();;
            	possibleExplanations.add(action);
                curr = vote;
            } else if (vote == curr) {
            	possibleExplanations.add(action);
            }
        }
        return possibleExplanations;
    }

    @Override
    public Explanation selectExplanation(ExplanationRuleSet matchSet, boolean debug) {
        List<Explanation> possibleExplanations = acceptableExplanations(matchSet, debug);
        return possibleExplanations.get(random.nextInt(possibleExplanations.size()));
    }
}
