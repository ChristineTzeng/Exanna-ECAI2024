package simulation;//import sim.engine.*;
//import sim.util.*;

import java.util.*;

import base.Action;
import base.Context;
import base.Health;
import base.Location;
import base.Norm;
import base.Preference;
import ec.util.MersenneTwisterFast;
import lcs.ActionSelectionStrategy;
import lcs.AlwaysMatchingMutationStrategy;
import lcs.BaseLCS;
import lcs.ButzFitnessWeightedActionSelection;
import lcs.CoveringStrategy;
import lcs.DistinctActionsBasedCoveringStrategy;
import lcs.EpsilonGreedyExplorationStrategy;
import lcs.ExplorationStrategy;
import lcs.FitnessWeightedActionSelection;
import lcs.InitialPeriodExplorationStrategy;
import lcs.KovacsDeletionScheme;
import lcs.LCSAlgorithm;
import lcs.SingleRuleCoveringStrategy;
import lcs.TournamentParentSelection;
import lcs.UniformCrossoverStrategy;
import rulebasedrl.MatchingSet;
import rulebasedrl.NormEntry;
import rulebasedrl.NormProvider;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import util.Randomizer;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 *
 * @author Hui
 */
public class Agent implements Steppable {
    
    public int remainingSteps = 0;
    public double interactionRate = 0.5;
    public int location = -1;

    public AgentType agentType = AgentType.HEALTH;
    public int familyCircle = -1;
    public int colleagueCircle = -1;
    public int friendCircle = -1;
    
    public Bag myFamilies = new Bag();
    public Bag myColleagues = new Bag();
    public Bag myFriends = new Bag();
    public Bag myStrangers = new Bag();
    
    public int health = -1;
    public int preference = -1;
    public double[] values;
    
    //neighboring behavior to which this agent needs to give feedbacks
    public Bag neighboringBehaviors = new Bag();
    
    //history of records for classification
    //public Bag records = new Bag();
    
    //Weka dataset
    public Instances data;

    //No. of instances learnt in reinforcement learning
    public int rlDataInstancesCount;

    //current neighbors;
    public Bag currentNeighbors = new Bag();
    
    //lock to avoid being called twice in a step. 
    public boolean isPairedUp = false;
    public Interaction currentInteraction = null;
    
    public int id = -1;

    public List<NormEntry> internalNorms;
    public LCSAlgorithm actionModel;

    private static double LEARNING_RATE = 0.9;

    private NormProvider normProvider;

    public Agent(){
        location = -1;
        remainingSteps = 0;
        this.id = -1;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
    }

    public Agent(int id, AgentType type){
        this.agentType = type;
        location = -1;
        remainingSteps = 0;
        this.id = id;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
    }

    public Agent(int id){
        location = -1;
        remainingSteps = 0;
        this.id = id;
        initDataset();
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }
    
    public void setAgentValues(double[] values) {
        this.values = values;
    }

    public static Instances getEmptyDataset() {
        //Attribute list
        ArrayList<Attribute> alist = new ArrayList<Attribute>();
        Attribute aa;
        
        //location
        aa = new Attribute("location", new ArrayList<String>(
                Arrays.asList(Agents.locations)));
        alist.add(aa);

        aa = new Attribute("relation", new ArrayList<String>(
                Arrays.asList(RecordForLearning.relationTypes)));
        alist.add(aa);

        //urgency
        List<String> tf = new ArrayList<String>();//True of False attributes
        tf.add("true");tf.add("false");
        //answer or not
        aa = new Attribute("wear", tf);
        alist.add(aa);
        //payoff, numeric
        aa = new Attribute("@Class@");
        alist.add(aa);

        Instances dataset = new Instances("Interaction Record", alist, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
        return dataset;
    }

    //Initialize the Weka dataset
    public void initDataset(){
        this.data = getEmptyDataset();
    }

    public void addRecordLCS(RecordForLearning rec, Interaction interaction) {
        double reward = rec.getPayoff();
        Context fullContext = null;
        if (interaction.observer != null)
        	fullContext = Context.builder()
    		.actorHealth(Health.get(interaction.actor.health))
    		.preference(Preference.get(interaction.actor.preference))
    		.observerAgentType(interaction.observer.agentType)
//    		.observerHealth(Health.get(interaction.observerHealth))
            .interactLocation(Location.get(interaction.location / Agents.numAgents))
            .observerRelationship(interaction.getInteractionRelationship())
            .build();
        else
	        fullContext = Context.builder()
	        		.actorHealth(Health.get(interaction.actor.health))
	        		.preference(Preference.get(interaction.actor.preference))
	                .interactLocation(Location.get(interaction.location / Agents.numAgents))
	                .observerRelationship(null)
	                .build();
        Action actorAction = Action.fromID(interaction.action);
        actionModel.learn(fullContext, actorAction, reward);
        this.rlDataInstancesCount++;
    }

//    private double getUpdatedWeight(double oldWeight, double reward) {
//        return oldWeight + LEARNING_RATE * (reward - oldWeight);
//    }

    //Add a record to Weka dataset
    public void addRecord(RecordForLearning rec){
        double[] one = new double[data.numAttributes()];
//        location', 'actorAgentType', 'actorHealth', 'preference', 'observerAgentType', 'observerHealth', 'relationship', 'action', 'payoff'
//        location', 'actorAgentType', 'actorHealth', 'preference', 'observerAgentType', 'relationship', 'action', 'payoff'
        //location
        one[0] = rec.location;
        //actorAgentType
//        one[1] = rec.actorAgentType;
        //actorHealth
        one[1] = rec.actorHealth;
        //preference
        one[2] = rec.preference;
        //observerAgentType
        one[3] = rec.observerAgentType;
//        //observerHealth
//        one[4] = rec.observerHealth;
      //relationship
        one[4] = rec.observerRelationship;
        //answer or not?
        one[5] = rec.action;
        //payoff
        one[6] = rec.getPayoff();
        
        data.add(new DenseInstance(1.0, one));
    }
    
    public void step(SimState state){
        
        Agents agents = (Agents)state;
        double x = 0.0;
        
        //Enter a random place
        if (true) {
            int sum = 0;
            int i;
            for(i=0;i<agents.locationWeights.length; i++){
                sum += agents.locationWeights[i];
            }
            x = agents.random.nextDouble();
            int y = 0;
            for(i = 0;i<agents.locationWeights.length;i++){
                y += agents.locationWeights[i];
                if (x <= (double) y / (double) sum)
                    break;
            }
            if (i >= agents.locationWeights.length)
                i = agents.locationWeights.length;
            
            //location = loction type id * number of agents + location id
            //e.g., meeting #1 with 1000 agents = 1*1000+1=1001
            //75% probability, agent enters own home/office/party
            //25% probability, agent enters another random home/meeting/party
            x = agents.random.nextDouble();
            switch(i){
                case 0: //home
                    x = x * agents.numHomes * 4;
                    if (x >= agents.numHomes)
                        location = i * agents.numAgents + familyCircle;
                    else
                        location = i * agents.numAgents + (int) x;
                    break;
                case 1: //office
                    x = x * agents.numOffices * 4;
                    if ( x>= agents.numOffices)
                        location = i * agents.numAgents + colleagueCircle;
                    else
                        location = i * agents.numAgents + (int) x;
                    break;
                case 2: //party
                    x = x * agents.numParties * 4;
                    if (x >= agents.numParties)
                        location = i * agents.numAgents + friendCircle;
                    else
                        location = i * agents.numAgents + (int) x;
                    break;
                default:
                    location = i * agents.numAgents;
            }
//            remainingSteps = (int)(agents.random.nextGaussian() * 30 + 60.5);
//            if (remainingSteps > 90) remainingSteps = 90;
//            if (remainingSteps < 30) remainingSteps = 30;
//            remainingSteps *= agents.locationWeights[i];
        }
//        }else{
//            remainingSteps --;
//        }
        
        //Once every agent enters a place
        
        if (this.isPairedUp) {
        	Interaction interaction = this.currentInteraction;
        	this.makeDecision(interaction, state);
            agents.interactionsInThisStep.add(interaction);
            agents.allInteractions.add(interaction);
            
            this.currentInteraction = interaction;
            
            //update with interaction that decision was made
            interaction.observer.neighboringBehaviors.add(interaction);
            
        } else {
        	//Randomly interact with someone at the same location
            x = agents.random.nextDouble();
            if (x <= interactionRate){
            	//Add this interaction to its observer
                this.currentNeighbors = agents.getNeighbors(this.location);
                Agent neighbor = null;
                Agent temp = null;
              //make sure that each agent is only paired up once in each step
                for(int i=0;i<this.currentNeighbors.size();i++){
                	temp = (Agent) this.currentNeighbors.get(i);
                    if (!temp.isPairedUp && temp.id != this.id) {
                    	neighbor = temp;
                    	break;
                    }
                    else
                    	neighbor = null;
                }
                
                if (neighbor != null){
                    x = agents.random.nextDouble();
                    Interaction interaction = new Interaction(this, neighbor, this.location, state.schedule.getSteps());

                    this.makeDecision(interaction, state);
                    this.currentInteraction = interaction;
                    agents.interactionsInThisStep.add(interaction);
                    agents.allInteractions.add(interaction);
                    
                    neighbor.neighboringBehaviors.add(interaction);
                    neighbor.isPairedUp = true;
                    
                    interaction = new Interaction(neighbor, this, this.location, state.schedule.getSteps());
                    neighbor.currentInteraction = interaction;
                    
                } else {
                	Interaction interaction = new Interaction(this, this.location, state.schedule.getSteps());
                	this.currentInteraction = interaction;
                }
            } else {
            	Interaction interaction = new Interaction(this, this.location, state.schedule.getSteps());
            	this.currentInteraction = interaction;
            	
            }
            this.isPairedUp = true;
        }
        
    }
    
    public void makeDecision(Interaction interaction, SimState state){
        long startTime = 0;
        if (Agents.MEASURE_TIME) {
            startTime = System.nanoTime();
        }
        Agents agents = (Agents)state;

        interaction.explanationStatistics = agents.explanationStatistics;
        agents.interactionRelationshipStats.add(interaction.getInteractionRelationship(), 1);
        agents.interactionLocationStats.add(Location.get(interaction.location / Agents.numAgents), 1);

        //A better way to make a decision, with adaptive learning, 
        //is using Weka's classification methods.
        //Learning starts after a learning period
        if ((Agents.simulationNumber <= 2) && (this.data.numInstances()>Agents.learningPeriod)){
            int temp = getAction(interaction, state);
            if (temp >= 0)
            	interaction.action = temp;
        }

        Context interactionContext = null;
        
        if (interaction.observer != null)
        	interactionContext = Context.builder()
    		.actorHealth(Health.get(interaction.actor.health))
    		.preference(Preference.get(interaction.actor.preference))
    		.observerAgentType(interaction.observer.agentType)
//    		.observerHealth(Health.get(interaction.observerHealth))
            .interactLocation(Location.get(interaction.location / Agents.numAgents))
            .observerRelationship(interaction.getInteractionRelationship())
            .build();
        else
        	interactionContext = Context.builder()
	        		.actorHealth(Health.get(interaction.actor.health))
	        		.preference(Preference.get(interaction.actor.preference))
	                .interactLocation(Location.get(interaction.location / Agents.numAgents))
	                .observerRelationship(null)
	                .build();

        if (Agents.isLCS()) {
            Action action = actionModel.getDecision(interactionContext);
            interaction.action = action.value;
            
            if (interaction.observer != null) {
            	if (Agents.isLCSNormExplanationPlusValue()) {
            		interaction.lcsNewExplanation = actionModel.explainDecision(interactionContext, action);
            		List<AgentType> types = new ArrayList<>();
            		types.add(interaction.actor.agentType);types.add(interaction.observer.agentType);
            		interaction.lcsNewExplanation = actionModel.filterValues(interaction.lcsNewExplanation, types);
                    interaction.informationCost = getInformationScoreFromLCSExplanation(interaction.lcsNewExplanation);
                } else if (Agents.isLCSNormExplanationPlusOwnContext()) {
                    interaction.lcsNewExplanation = actionModel.explainDecision(interactionContext, action);
                    interaction.informationCost = getInformationScoreFromLCSExplanation(interaction.lcsNewExplanation);
                // default: listen to full context
                } else if (Agents.isListenExplanation()) {
                	interaction.informationCost = 1.0; 
                }
            }
        }

        if (Agents.MEASURE_TIME) {
        	interaction.timeToDecide = System.nanoTime() - startTime;
        }
    }

    private double getInformationScoreFromLCSExplanation(List<Norm> lcsNewExplanation) {
        Context context = Context.builder().build();
        for (Norm norm : lcsNewExplanation) {
            context.mergeFrom(norm.getConditions());
        }
        return context.getInformationCost();
    }
    
    public void giveFeedbacks(SimState state) {
        Agents agents = (Agents)state;
        Bag todo = new Bag(neighboringBehaviors);
        neighboringBehaviors = new Bag();
        if (todo.size() <= 0)
            return;
        Interaction interaction;
        Feedback temp;
        
        //boolean feedback = true;
        //UPDATE: Now we use payoff instead of boolean feedback;
        double payoff = 0.0;
        
        for(int i=0; i < todo.size(); i++){
        	interaction = (Interaction)todo.get(i);
            long startTime = 0;
            if (Agents.MEASURE_TIME) {
                startTime = System.nanoTime();
            }

            int l = (int)(interaction.location/Agents.numAgents);
            Location interactLocation = Location.get(l);
            Action actorAction = Action.fromID(interaction.action);
            //decide a feedback based on interaction info
            boolean accept = false;

            //In the 2nd simulation,
            //neighbor hears the complete explanation
            if ((Agents.simulationNumber == 2) && (this.data.numInstances() > Agents.learningPeriod)){
                //get the action that the neighbor would take
                int action = getAction(interaction, state);
                accept = action == interaction.action;
            }

            Context fullContext = null;
            if (interaction.observer != null)
            	fullContext = Context.builder()
//        		.actorHealth(Health.get(interaction.observer.health))
        		.actorHealth(Health.get(interaction.actor.health))
//        		.preference(Preference.get(interaction.observer.preference))
        		.preference(Preference.get(interaction.actor.preference))
        		.observerAgentType(interaction.observer.agentType)
//        		.observerAgentType(interaction.actor.agentType)
//        		.observerHealth(Health.get(interaction.actorHealth))
//        		.observerHealth(Health.get(interaction.observerHealth))
        		.sharedHealth(Health.get(interaction.actor.health))
        		.sharedPreference(Preference.get(interaction.actor.preference))
                .interactLocation(Location.get(interaction.location / Agents.numAgents))
                .observerRelationship(interaction.getInteractionRelationship())
                .build();
            else
    	        fullContext = Context.builder()
    	        		.actorHealth(Health.get(interaction.actor.health))
//    	        		.actorHealth(Health.get(interaction.observer.health))
    	        		.preference(Preference.get(interaction.actor.preference))
//    	        		.preference(Preference.get(interaction.observer.preference))
    	                .interactLocation(Location.get(interaction.location / Agents.numAgents))
    	                .observerRelationship(null)
    	                .build();

            if (Agents.isListenExplanation() && this.rlDataInstancesCount > Agents.learningPeriod) {
                List<Action> validActions;
                if (Agents.isLCSNormExplanationPlusValue()) {
                	int[] sharedInfo = actionModel.sharedInfo(interaction.lcsNewExplanation);
                    Context ownContext = Context.builder()
                            .actorHealth(Health.get(interaction.observer.health))
                    		.preference(Preference.get(interaction.observer.preference))
                    		.observerAgentType(interaction.actor.agentType)
//                    		.observerHealth(Health.get(interaction.actorHealth))
                            .interactLocation(Location.get(interaction.location / Agents.numAgents))
                            .observerRelationship(interaction.getInteractionRelationship())
                            .build();
                    if (sharedInfo[0] == 1) {
                    	ownContext.sharedHealth = Health.get(interaction.actor.health);
                    	ownContext.actorHealth = Health.get(interaction.actor.health);
                    }
                    if (sharedInfo[1] == 1) {
                    	ownContext.sharedPreference = Preference.get(interaction.actor.preference);
                    	ownContext.preference = Preference.get(interaction.actor.preference);
                    }
                    validActions = actionModel.getAcceptableDecisions(interaction.lcsNewExplanation, ownContext);
                } else if (Agents.isLCSNormExplanationPlusOwnContext()) {
                	int[] sharedInfo = actionModel.sharedInfo(interaction.lcsNewExplanation);
                	Context ownContext = Context.builder()
                            .actorHealth(Health.get(interaction.observer.health))
                    		.preference(Preference.get(interaction.observer.preference))
                    		.observerAgentType(interaction.actor.agentType)
//                    		.observerHealth(Health.get(interaction.actorHealth))
                            .interactLocation(Location.get(interaction.location / Agents.numAgents))
                            .observerRelationship(interaction.getInteractionRelationship())
                            .build();
                	if (sharedInfo[0] == 1) {
                		ownContext.sharedHealth = Health.get(interaction.actor.health);
                		ownContext.actorHealth = Health.get(interaction.actor.health);
                	}
                    if (sharedInfo[1] == 1) {
                    	ownContext.sharedPreference = Preference.get(interaction.actor.preference);
                    	ownContext.preference = Preference.get(interaction.actor.preference);
                    }
                    validActions = actionModel.getAcceptableDecisions(interaction.lcsNewExplanation, ownContext);
                } else {
                    validActions = actionModel.getAcceptableDecisions(fullContext);
                }
                accept = validActions.contains(actorAction);
            }

            if (accept) {
            	interaction.explanationStatistics.addAccept(actorAction, interactLocation);
            } else {
            	interaction.explanationStatistics.addReject(actorAction, interactLocation);
            }
            payoff = agents.payoffCalculator.calculateObserverPayoff(
                    Context.builder().observerRelationship(interaction.getInteractionRelationship()).build(), actorAction, accept);

            //temp = new Feedback(call, this, feedback);
            temp = new Feedback(interaction, this, payoff);
            interaction.feedbacks.add(temp);
            if (Agents.MEASURE_TIME) {
            	interaction.timeToGiveFeedbacks += System.nanoTime() - startTime;
            }
        }
    }

    public String getLocationString(){
        return Agents.locations[location/Agents.numAgents]
                +" #"+(location%Agents.numAgents);
    }

    public List<Integer> getBulkAction(List<Interaction> interactions, SimState state) {
        Agents agents = (Agents)state;
        int action = -1;
        Classifier cls = new LinearRegression();
        List<Integer> actions = new ArrayList<>();
        try {
            cls.buildClassifier(data);
            for (Interaction interaction : interactions) {
                RecordForLearning rec = new RecordForLearning(interaction, agents, this);
                double[] one = new double[data.numAttributes()];
               //location
                one[0] = rec.location;
                //actorAgentType
//                one[1] = rec.actorAgentType;
                //actorHealth
                one[1] = rec.actorHealth;
                //preference
                one[2] = rec.preference;
                //observerAgentType
                one[3] = rec.observerAgentType;
//                //observerHealth
//                one[4] = rec.observerHealth;
              //relationship
                one[4] = rec.observerRelationship;
                //What if I not wear?
                one[5] = 1;
                //payoff
                one[6] = 0;//0 for now

                try {
                    //What if I not wear?
                    one[5] = 1;
                    double a1 = cls.classifyInstance(new DenseInstance(1.0, one));
                    //What if I wear?
                    one[5] = 0;
                    double a2 = cls.classifyInstance(new DenseInstance(1.0, one));

                    //choose the action with higher predicted overall payoff
                    if (a1 > a2)
                        action = 1;
                    else
                        action = 0;
                    actions.add(action);
                } catch (Exception e) {
                    //do nothing
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actions;
    }

    //Decided, based on history, which action is better
    public int getAction(Interaction interaction, SimState state){
        Agents agents = (Agents)state;
        int action = -1;
        Classifier cls = new LinearRegression();
        RecordForLearning rec = new RecordForLearning(interaction, agents, this);
        double[] one = new double[data.numAttributes()];
      //location
        one[0] = rec.location;
        //actorAgentType
//        one[1] = rec.actorAgentType;
        //actorHealth
        one[1] = rec.actorHealth;
        //preference
        one[2] = rec.preference;
        //observerAgentType
        one[3] = rec.observerAgentType;
//        //observerHealth
//        one[4] = rec.observerHealth;
      //relationship
        one[4] = rec.observerRelationship;
        //What if I not wear?
        one[5] = 1;
        //payoff
        one[6] = 0;//0 for now

        try{
            cls.buildClassifier(data);
            //What if I not wear?
            one[5] = 1;
            double a1 = cls.classifyInstance(new DenseInstance(1.0, one));
            //What if I wear?
            one[5] = 0;
            double a2 = cls.classifyInstance(new DenseInstance(1.0, one));

            //choose the action with higher predicted overall payoff
            if (a1 > a2)
                action = 1;
            else
                action = 0;
        }
        catch(Exception e){
            //do nothing
        }
        return action;
    }

//    private static class DecisionResponse {
//        public Action action;
//        public Explanation explanation;
//        public MatchingSet matchingSet;
//
//        public DecisionResponse(Action action, Explanation explanation, MatchingSet matchingSet) {
//            this.action = action;
//            this.explanation = explanation;
//            this.matchingSet = matchingSet;
//        }
//    }

    public MatchingSet getMatchingSetForExplicitNorms(Context context) {
        MatchingSet matchingSet = new MatchingSet();
        for (NormEntry entry : this.internalNorms) {
            if (entry.norm.triggers(context)) {
                matchingSet.add(entry);
            }
        }
        return matchingSet;
    }

    public void initializeNorms() {
        this.internalNorms = normProvider.provide();
    }

    public void initLCS(SimState state) {
        //Add simulationNumber checks
        BaseLCS.Parameters parameters = new BaseLCS.Parameters();
        parameters.maxPopulation = 30;
        Randomizer randomizer = new Randomizer(state.random);
        this.actionModel = new BaseLCS(
                state.random,
//                new FitnessWeightedActionSelection(state.random),
                getActionSelectionStrategy(state.random),
                getCoveringStrategy(randomizer, parameters.dontCareProb),
                new TournamentParentSelection(randomizer),
                new UniformCrossoverStrategy(randomizer, parameters.crossoverBitSwapProbability),
                new AlwaysMatchingMutationStrategy(randomizer, parameters.mutationProb),
                new KovacsDeletionScheme(randomizer, parameters.experienceThresholdForDeletion,
                        parameters.maxPopulation, parameters.fitnessThreshold),
                getLCSExplorationStrategy(randomizer),
                parameters
        );
    }

    private ActionSelectionStrategy getActionSelectionStrategy(MersenneTwisterFast random) {
    	if (Agents.simulationNumber >= 4) {
            return new ButzFitnessWeightedActionSelection(random);
        } else {
            return new FitnessWeightedActionSelection(random);
        }
    }

    private CoveringStrategy getCoveringStrategy(Randomizer randomizer, double dontCareProb) {
        	if (Agents.simulationNumber >= 4) {
            return new DistinctActionsBasedCoveringStrategy(randomizer, dontCareProb);
        } else {
            return new SingleRuleCoveringStrategy(randomizer, dontCareProb);
        }
    }

    private ExplorationStrategy getLCSExplorationStrategy(Randomizer randomizer) {
        
        if (Agents.simulationNumber >= 3) {
            return new InitialPeriodExplorationStrategy(new EpsilonGreedyExplorationStrategy(randomizer),
                    100);
        }
        throw new RuntimeException();
    }

    public void contributeNorms(Map<Norm, Double> voteMap) {
    	actionModel.voteNorms(voteMap);
    }
}
