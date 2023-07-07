package lcs;

import base.Context;
import base.Explanation;
import base.ExplanationNorm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExplanationRuleSet {
    private Map<ExplanationNorm, ExplanationRule> ruleMap;
    private Runnable addCallback;

    public ExplanationRuleSet(Runnable addCallback) {
        this.ruleMap = new LinkedHashMap<>();
        this.addCallback = addCallback;
    }

    public ExplanationRuleSet() {
        this.ruleMap = new LinkedHashMap<>();
    }

    public ExplanationRuleSet(List<ExplanationRule> rules, Runnable addCallback) {
        this.addCallback = addCallback;
        this.ruleMap = new LinkedHashMap<>();
        for (ExplanationRule rule : rules) {
            this.ruleMap.put(rule.norm, rule);
        }
    }

    public ExplanationRuleSet(List<ExplanationRule> rules) {
        this.ruleMap = new LinkedHashMap<>();
        for (ExplanationRule rule : rules) {
            this.ruleMap.put(rule.norm, rule);
        }
    }

    public ExplanationRuleSet getMatchSet(Context context) {
        List<ExplanationRule> matchingRules = new ArrayList<>();
        for (Map.Entry<ExplanationNorm, ExplanationRule> entry : ruleMap.entrySet()) {
            if (entry.getKey().triggers(context)) {
                matchingRules.add(entry.getValue());
            }
        }
        return new ExplanationRuleSet(matchingRules);
    }

    public ExplanationRuleSet getExplanationSet(Explanation explanation) {
        List<ExplanationRule> explanationRules = new ArrayList<>();
        for (Map.Entry<ExplanationNorm, ExplanationRule> entry : ruleMap.entrySet()) {
            if (entry.getKey().consequent == explanation) {
            	explanationRules.add(entry.getValue());
            }
        }
        return new ExplanationRuleSet(explanationRules);
    }

    public void addRule(ExplanationRule rule) {
    	ExplanationRule existingRule = ruleMap.get(rule.norm);
        if (existingRule == null) {
            ruleMap.put(rule.norm, rule);
        } else {
            existingRule.numerosity += rule.numerosity;
        }
        if (addCallback != null) {
            addCallback.run();
        }
    }

    public void addAll(List<ExplanationRule> rules) {
        for (ExplanationRule rule : rules) {
            addRule(rule);
        }
    }

    public boolean removeRule(ExplanationRule rule) {
        return removeRule(rule, 1);
    }

    public boolean removeRule(ExplanationRule rule, int count) {
    	ExplanationRule existingRule = ruleMap.get(rule.norm);
        if (existingRule == null) {
            return false;
        }
        existingRule.numerosity -= count;
        if (existingRule.numerosity <= 0) {
            existingRule.numerosity = 0;
            ruleMap.remove(existingRule.norm);
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return ruleMap.isEmpty();
    }

    public List<ExplanationRule> getRules() {
        return new ArrayList<>(this.ruleMap.values());
    }

    public double getAvgExperienceSinceRD() {
        List<ExplanationRule> rules = getRules();
        int totalExperience = 0;
        int totalNumerosity = 0;
        for (ExplanationRule rule : rules) {
            totalExperience += rule.experienceSinceRD * rule.numerosity;
            totalNumerosity += rule.numerosity;
        }
        return totalNumerosity == 0 ? 0.0 : (((double)totalExperience) / totalNumerosity);
    }

    public boolean contains(ExplanationRule child) {
        return ruleMap.containsKey(child.norm);
    }

    public void resetExperienceSinceRD() {
        for (Map.Entry<ExplanationNorm, ExplanationRule> entry : ruleMap.entrySet()) {
            entry.getValue().resetExperienceSinceRD();
        }
    }

    public void print() {
        System.out.printf("Population with %d unique rules\n", ruleMap.size());
        List<ExplanationRule> rules = getRules();
        rules.sort(Comparator.comparing(ExplanationRule::getRewardPrediction).reversed());
        for (ExplanationRule rule : rules) {
            System.out.println(rule.toString(false));
        }
    }

    //Union Find is the best solution for this. But using nested loops for now.
    public void subsumeAll() {
        List<ExplanationRule> rules = getRules();
        int ruleSize = rules.size();
        for (int i = 0; i < ruleSize; i++) {
        	ExplanationRule iRule = rules.get(i);
            if (!this.ruleMap.containsKey(iRule.getNorm())) continue;
            for (int j = 0; j < ruleSize; j++) {
                if (i == j) continue;
                ExplanationRule jRule = rules.get(j);
                if (!this.ruleMap.containsKey(jRule.getNorm())) continue;
                if (iRule.subsumes(jRule)) this.ruleMap.remove(jRule.getNorm());
            }
        }
    }
}

