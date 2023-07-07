package lcs;

import base.Context;

public interface MutationStrategy {
    Rule mutate(Rule rule, Context situation);
    
    ExplanationRule mutate(ExplanationRule rule, Context situation);
}
