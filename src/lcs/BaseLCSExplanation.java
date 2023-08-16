package lcs;

import base.Action;
import base.Context;
import base.Explanation;
import base.ExplanationNorm;
import base.Norm;
import ec.util.MersenneTwisterFast;
import simulation.AgentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseLCSExplanation implements LCSAlgorithm {
    private ExplanationRuleSet population;
    private MersenneTwisterFast random;
    private ExplanationSelectionStrategy explanationSelectionStrategy;
    private CoveringStrategy coveringStrategy;
    private Parameters parameters;
    private ParentSelectionStrategy parentSelectionStrategy;
    private CrossoverStrategy crossoverStrategy;
    private MutationStrategy mutationStrategy;
    private DeletionStrategy deletionStrategy;
    private ExplorationStrategy explorationStrategy;

    public BaseLCSExplanation(MersenneTwisterFast random,
    		ExplanationSelectionStrategy explanationSelectionStrategy,
                   CoveringStrategy coveringStrategy,
                   ParentSelectionStrategy parentSelectionStrategy,
                   CrossoverStrategy crossoverStrategy,
                   MutationStrategy mutationStrategy,
                   DeletionStrategy deletionStrategy,
                   ExplorationStrategy explorationStrategy) {
        this.population = new ExplanationRuleSet(this::prune);
        this.parameters = new Parameters();
        this.random = random;
        this.explanationSelectionStrategy = explanationSelectionStrategy;
        this.coveringStrategy = coveringStrategy;
        this.parentSelectionStrategy = parentSelectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.deletionStrategy = deletionStrategy;
        this.explorationStrategy = explorationStrategy;
    }
    

    public BaseLCSExplanation(MersenneTwisterFast random,
    		ExplanationSelectionStrategy explanationSelectionStrategy, //
                   CoveringStrategy coveringStrategy, 
                   ParentSelectionStrategy parentSelectionStrategy,
                   CrossoverStrategy crossoverStrategy,
                   MutationStrategy mutationStrategy,
                   DeletionStrategy deletionStrategy,
                   ExplorationStrategy explorationStrategy,
                   Parameters parameters) { //
//        this.population = new ArrayList<>();
        this.population = new ExplanationRuleSet(this::prune);
        this.parameters = parameters;
        this.random = random;
        this.explanationSelectionStrategy = explanationSelectionStrategy;
        this.coveringStrategy = coveringStrategy;
        this.parentSelectionStrategy = parentSelectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.deletionStrategy = deletionStrategy;
        this.explorationStrategy = explorationStrategy;
    }

    @Override
    public Explanation getExplanation(Context context) {
        if (explorationStrategy.explore()) {
            return Explanation.get(random.nextInt(Explanation.values().length));
        } else {
        	ExplanationRuleSet matchSet = population.getMatchSet(context);
            if (coveringStrategy.initiateCovering(matchSet)) {
                List<ExplanationRule> coveringRules = coveringStrategy.getCoveringRules(context, matchSet);
                matchSet.addAll(coveringRules);
                this.population.addAll(coveringRules);
            }
            return explanationSelectionStrategy.selectExplanation(matchSet, false);
        }
    }

    @Override
    public void voteExplanationNorms(Map<ExplanationNorm, Double> voteMap) {
        List<ExplanationRule> rules = this.population.getRules();
        for (ExplanationRule rule : rules) {
            double existingVote = voteMap.getOrDefault(rule.getNorm(), 0.0);
            voteMap.put(rule.getNorm(), existingVote + rule.fitness * rule.rewardPrediction);
        }
    }

    @Override
    public List<ExplanationNorm> explainDecision(Context context, Explanation explanation) {
    	ExplanationRuleSet matchSet = population.getMatchSet(context);
    	ExplanationRuleSet actionSet = matchSet.getExplanationSet(explanation);
        return actionSet.getRules().stream().map(ExplanationRule::getNorm).collect(Collectors.toList());
    }

    @Override
    public void learn(Context context, Explanation action, double reward) {
    	ExplanationRuleSet matchSet = population.getMatchSet(context);
    	ExplanationRuleSet actionSet = matchSet.getExplanationSet(action);
        updateRuleParameters(actionSet, reward);
        if (parameters.doActionSetSubSumption) {
            actionSetSubSumption(actionSet);
        }
        geneticExploration(actionSet, context);
    }

    private void actionSetSubSumption(ExplanationRuleSet actionSet) {
        List<ExplanationRule> actionRules = actionSet.getRules();
        int selectedRuleBitCount = -1;
        List<ExplanationRule> selectedRules = new ArrayList<>();
        for (ExplanationRule rule : actionRules) {
            if (!(rule.experience > parameters.experienceThresholdForSubSumption
                    && rule.errorOfPrediction < parameters.accuracyThreshold)) {
                continue;
            }
            int bitCount = rule.getNumberOfKnownBits();
            if (bitCount > selectedRuleBitCount) {
                selectedRules.clear();
                selectedRules.add(rule);
                selectedRuleBitCount = bitCount;
            } else if (bitCount == selectedRuleBitCount) {
                selectedRules.add(rule);
            }
        }
        if (selectedRules.isEmpty()) return;
        ExplanationRule selectedRule = selectedRules.get(random.nextInt(selectedRules.size()));

        for (ExplanationRule rule : actionRules) {
            if (!rule.equals(selectedRule) && selectedRule.norm.triggers(rule.norm.getConditions())) {
                selectedRule.numerosity += rule.numerosity;
                this.population.removeRule(rule, rule.numerosity);
                actionSet.removeRule(rule, rule.numerosity);
            }
        }
    }

    private void geneticExploration(ExplanationRuleSet actionSet, Context situation) {
        if (actionSet.getAvgExperienceSinceRD() <= parameters.gaThreshold) {
            return;
        }
        actionSet.resetExperienceSinceRD();
        ExplanationRule firstParent = parentSelectionStrategy.select(actionSet);
        ExplanationRule secondParent = parentSelectionStrategy.select(actionSet);

        ExplanationRule firstChild, secondChild;

        if (random.nextBoolean(parameters.crossoverProbability)) {
        	ExplanationRule[] children = crossoverStrategy.crossover(firstParent, secondParent);
            firstChild = children[0];
            secondChild = children[1];
        } else {
            firstChild = firstParent;
            secondChild = secondParent;
        }
        firstChild = mutationStrategy.mutate(firstChild, situation);
        secondChild = mutationStrategy.mutate(secondChild, situation);

        ExplanationRule[] children = new ExplanationRule[]{firstChild, secondChild};
        ExplanationRule[] parents = new ExplanationRule[]{firstParent, secondParent};
        for (ExplanationRule child : children) {
            if (parameters.doGASubSumption) {
                boolean subsumed = false;
                for (ExplanationRule parent : parents) {
                    boolean shouldSubsume = parent.experience > parameters.experienceThresholdForSubSumption
                            && parent.errorOfPrediction < parameters.accuracyThreshold
                            && parent.getNorm().triggers(child.getNorm().getConditions());
                    if (shouldSubsume) {
                    	ExplanationRule copyParent = new ExplanationRule(parent);
                        copyParent.numerosity = 1;
                        this.population.addRule(copyParent);
                        subsumed = true;
                        break;
                    }
                }
                if (subsumed) continue;
            }

            if (!this.population.contains(child)) {
                child.fitness = (firstParent.fitness + secondParent.fitness) / 2.0;
                child.errorOfPrediction = (firstParent.errorOfPrediction + secondParent.errorOfPrediction) / 2.0;
                child.rewardPrediction = (firstParent.rewardPrediction + secondParent.rewardPrediction) / 2.0;
            }

            this.population.addRule(child);
        }
    }

    private void updateRuleParameters(ExplanationRuleSet actionSet, double reward) {
        List<Double> rawAccuracies = new ArrayList<>();
        double accuracySum = 0.0;
        List<ExplanationRule> actionRules = actionSet.getRules();
        int totalNumerosity = 0;
        for (ExplanationRule rule : actionRules) {
            totalNumerosity += rule.numerosity;
            rule.incrementExperience();
            rule.rewardPrediction = rule.rewardPrediction + parameters.betaLearningRate
                    * (reward - rule.rewardPrediction);
            rule.errorOfPrediction = rule.errorOfPrediction
                    + parameters.betaLearningRate * (Math.abs(reward - rule.rewardPrediction) - rule.errorOfPrediction);
            double accuracy = (rule.errorOfPrediction < parameters.accuracyThreshold)
                    ? 1.0
                    : (parameters.fitnessFalloffAlpha
                    * Math.pow(rule.errorOfPrediction / parameters.accuracyThreshold, -parameters.fitnessExponent));
            rawAccuracies.add(accuracy);
            accuracySum += accuracy * rule.numerosity;
        }
//        double accuracySum = rawAccuracies.stream().mapToDouble(i -> i).sum();
        int uniqueRuleSize = actionRules.size();
        for (int index = 0; index < uniqueRuleSize; index++) {
            double rawAccuracy = rawAccuracies.get(index);
            ExplanationRule rule = actionRules.get(index);
//            double normalizedAccuracy = rawAccuracy / accuracySum;
            double normalizedAccuracy = (rawAccuracy * rule.numerosity) / accuracySum;
            rule.fitness = rule.fitness + parameters.betaLearningRate * (normalizedAccuracy - rule.fitness);

            rule.actionSetSize += parameters.betaLearningRate * (totalNumerosity - rule.actionSetSize);
        }
    }

    public void prune() {
        deletionStrategy.prune(this.population);
    }

    @Override
    public void printStats() {
        this.population.print();
    }

    public static class Parameters {
        public double betaLearningRate;
        public int maxPopulation;
        public double dontCareProb;
        public double accuracyThreshold;
        public double fitnessExponent;
        public double fitnessFalloffAlpha;
        public int gaThreshold;
        public boolean doActionSetSubSumption;
        public int experienceThresholdForSubSumption;
        public double crossoverProbability;
        public double crossoverBitSwapProbability;
        public boolean doGASubSumption;
        public int experienceThresholdForDeletion;
        public double fitnessThreshold;
        public double mutationProb;

        public Parameters() {
            this.betaLearningRate = 0.1;
            this.maxPopulation = 1000;
            this.dontCareProb = 0.3;
            this.accuracyThreshold = 0.01;
            this.fitnessExponent = 5.0;
            this.fitnessFalloffAlpha = 0.1;
            this.gaThreshold = 25;
            this.doActionSetSubSumption = true;
            this.experienceThresholdForSubSumption = 20;
            this.crossoverProbability = 0.8;
            this.crossoverBitSwapProbability = 0.5;
            this.doGASubSumption = true;
            this.experienceThresholdForDeletion = 20;
            this.fitnessThreshold = 0.1;
            this.mutationProb = 0.4;
        }
    }

	@Override
	public Action getDecision(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Norm> explainDecision(Context context, Action action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void learn(Context context, Action action, double reward) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Action> getAcceptableDecisions(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> getAcceptableDecisions(Context context, boolean acceptingByDefault) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> getAcceptableDecisions(List<Norm> arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> getAcceptableDecisions(List<Norm> arguments, boolean acceptingByDefault) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> getAcceptableDecisions(List<Norm> arguments, Context ownContext, boolean acceptingByDefault) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void voteNorms(Map<Norm, Double> voteMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Norm> filterValues(List<Norm> rules, List<AgentType> types) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] sharedInfo(List<Norm> rules) {
		// TODO Auto-generated method stub
		return null;
	}
}
