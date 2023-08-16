package simulation;

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
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
    }

    public Agent(int id, AgentType type){
        this.agentType = type;
        location = -1;
        remainingSteps = 0;
        this.id = id;
        this.internalNorms = new ArrayList<>();
        this.rlDataInstancesCount = 0;
    }

    public Agent(int id){
        location = -1;
        remainingSteps = 0;
        this.id = id;
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

        List<String> tf = new ArrayList<String>();
        tf.add("true");tf.add("false");
        //wear or not
        aa = new Attribute("wear", tf);
        alist.add(aa);
        //payoff, numeric
        aa = new Attribute("@Class@");
        alist.add(aa);

        Instances dataset = new Instances("Interaction Record", alist, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
        return dataset;
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
        }
        
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
                for(int i = 0; i < this.currentNeighbors.size(); i++){
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
