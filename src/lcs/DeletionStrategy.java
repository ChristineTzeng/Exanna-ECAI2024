package lcs;

public interface DeletionStrategy {
    boolean prune(RuleSet ruleSet);
    
    boolean prune(ExplanationRuleSet ruleSet);
}
