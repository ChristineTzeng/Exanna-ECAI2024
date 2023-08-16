package lcs;

import util.Debugger;
import util.Randomizer;

import java.util.ArrayList;
import java.util.List;

public class TournamentParentSelection implements ParentSelectionStrategy {
    private static final double SELECTION_RATIO = 0.3;
    private Randomizer randomizer;

    public TournamentParentSelection(Randomizer randomizer) {
        this.randomizer = randomizer;
    }

    @Override
    public Rule select(RuleSet ruleSet) {
        List<Rule> rules = ruleSet.getRules();
        int actionSetSize = rules.size();
        int tournamentSize = (int)(Math.ceil(SELECTION_RATIO * actionSetSize));
        List<Rule> tournament = randomizer.sample(rules, actionSetSize, tournamentSize);
        List<Rule> maximumRules = new ArrayList<>();
        double maxFitness = 0.0;
        for (Rule rule : tournament) {
            if (rule.fitness > maxFitness) {
                maximumRules.clear();
                maximumRules.add(rule);
                maxFitness = rule.fitness;
            } else if (rule.fitness == maxFitness) {
                maximumRules.add(rule);
            }
        }
        if (maximumRules.size() == 0) {
            Debugger.debug(maximumRules.size(), "maximumRules.size()");
        }
        return maximumRules.get(randomizer.getRandom(0, maximumRules.size()));
    }

	@Override
	public ExplanationRule select(ExplanationRuleSet ruleSet) {
		List<ExplanationRule> rules = ruleSet.getRules();
        int actionSetSize = rules.size();
        int tournamentSize = (int)(Math.ceil(SELECTION_RATIO * actionSetSize));
        List<ExplanationRule> tournament = randomizer.sample(rules, actionSetSize, tournamentSize);
        List<ExplanationRule> maximumRules = new ArrayList<>();
        double maxFitness = 0.0;
        for (ExplanationRule rule : tournament) {
            if (rule.fitness > maxFitness) {
                maximumRules.clear();
                maximumRules.add(rule);
                maxFitness = rule.fitness;
            } else if (rule.fitness == maxFitness) {
                maximumRules.add(rule);
            }
        }
        return maximumRules.get(randomizer.getRandom(0, maximumRules.size()));
	}
}
