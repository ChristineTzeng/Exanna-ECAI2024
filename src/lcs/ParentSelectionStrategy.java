package lcs;

public interface ParentSelectionStrategy {
    Rule select(RuleSet ruleSet);
    ExplanationRule select(ExplanationRuleSet ruleSet);
}
