package lcs;

import base.Explanation;

import java.util.List;

public interface ExplanationSelectionStrategy {
	List<Explanation> acceptableExplanations(ExplanationRuleSet matchSet, boolean debug);
	Explanation selectExplanation(ExplanationRuleSet matchSet, boolean debug);
}