package lcs;

public interface CrossoverStrategy {
    Rule[] crossover(Rule firstParent, Rule secondParent);
    
    ExplanationRule[] crossover(ExplanationRule firstParent, ExplanationRule secondParent);
}
