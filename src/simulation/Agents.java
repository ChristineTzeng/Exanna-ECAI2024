package simulation;

import base.Action;
import base.Context;
import base.Health;
import base.Location;
import base.Norm;
import base.Preference;
import base.Relationship;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import util.Debugger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Agents extends SimState {

    //Which simulation are we doing?
    //4: share all
	//5: share rules
	//6: exanna
    public static int simulationNumber = 5;

    public static int trial = 101;

    //Main parameters
    public static int jobs = 1; // runs of simulations: 10
    public static int numSteps = 30000; //30000
    //There is an assumption in global location id calculator that numAgents >= numHomes, numOffices, numParties
    public static int numAgents = 200;//1000; 200
    public static int numHomes = 5;//20; 5
    public static int numOffices = 5;//20; 5
    public static int numParties = 5;//20; 5
    public static int[] agentSocietyRatios = new int[]{1, 1};
    
    //Location names
    public static String[] locations = {"home", "park", "grocery_store", "hospital"};

    //location weights are multipliers for probabilities and durations.
    public static int[] locationWeights = {1,1,1,1,1};
    
    //Agent will start to learn (instead of using fixed norms) 
    //after the learning period.
    public static int learningPeriod = 50;

    public static boolean MEASURE_TIME = false;

    private static final double NORM_EMERGENCE_THRESHOLD = 0.9;

    //Average the output in a window (#steps)
    public static int windowSize = 200;
    public static int windowSize2 = 200;

    public long lastExceptionCaseStep = 0;

    public LinkedHashMap<Long, Double> window = new LinkedHashMap<Long, Double>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
            return this.size() > windowSize;
        }
    };
    
    public LinkedHashMap<Long, Integer> numInteractions = new LinkedHashMap<Long, Integer>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest){
            return this.size() > windowSize;
        }
    };
    
    public LinkedHashMap<Long, Double> window2 = new LinkedHashMap<Long, Double>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
            return this.size() > windowSize2;
        }
    };

    public ExplanationStats.Statistics explanationStatistics;
    public RelationshipStats interactionRelationshipStats;
    public LocationStats interactionLocationStats;
    public ExplanationStats.Statistics interactionBasedExplanationStatistics;
    public AgentTypeStats.Statistics agentTypeStats;

    public AgentPayoffWindow actorPayoffWindow = new AgentPayoffWindow();
    public AgentPayoffWindow observerPayoffWindow = new AgentPayoffWindow();
    public WindowForMetric windowForDecideTimeMetric = new WindowForMetric();
    public WindowForMetric windowForFeedbackTimeMetric = new WindowForMetric();
    public WindowForMetric windowForInformationCostMetric = new WindowForMetric();
    public WindowForMetric windowForPayoffMetric = new WindowForMetric();
    public WindowForMetric windowForSatisfactionMetric = new WindowForMetric();

    private static LinkedHashMap<Long, Double> getWindowMapDouble() {
        return new LinkedHashMap<Long, Double>(){
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Double> eldest){
                return this.size() > windowSize;
            }
        };
    }

    private static LinkedHashMap<Long, Integer> getWindowMapInteger() {
        return new LinkedHashMap<Long, Integer>(){
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest){
                return this.size() > windowSize;
            }
        };
    }

    //cost of giving feedbacks
    public static double costFeedback = 0.5; //not used for now
    
    //cost of giving feedbacks and giving explanation;
    public static double costExplanation = 0.5;

    public PayoffCalculator payoffCalculator;
    
    //reference to all agents in the simulation
    public Bag allAgents = new Bag();
    
    //keep all calls happening in current step
    public Bag interactionsInThisStep = new Bag();

    public Bag allInteractions = new Bag();
    
    public double overallSatisfaction = 0.0;
    //total payoff in a step
    public double payoff = 0.0;

    public Map<AgentType, Double> agentTypeActorPayoffInWindow = new HashMap<>();
    public Map<AgentType, Double> agentTypeObserverPayoffInWindow = new HashMap<>();
    
    public Map<AgentType, Integer> satisfactionByAgentType = new HashMap<>();

    //Write results to file
    public BufferedWriter out = null;
    public BufferedWriter normsDataWriter = null;
    private static final String RESULTS_PATH = "results/";

    public Agents(long seed){
        super(seed);
    }

    /**
     * Generates a random permutation of numbers from 1 to n-1, both inclusive
     */
    private int[] getRandomPermutation(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }
        for (int i = n; i > 1; i--) {
            swap(arr, i-1, random.nextInt(i));
        }
        return arr;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    private int[] getPopulationsBasedOnRatio(int[] ratio, int total) {
        int len = ratio.length;
        int ratioSum = 0;
        for (int i = 0; i < len; i++) {
            ratioSum += ratio[i];
        }
        int[] populations = new int[len];
        int rem = total;
        double common = ((double) total) / ratioSum;
        for (int i = 0; i < len; i++) {
            if (i < (len - 1)) {
                populations[i] = (int) (common * ratio[i]);
                rem -= populations[i];
            } else {
                populations[i] = rem;
            }
        }
        return populations;
    }

    public static boolean isLCS() {
    	return simulationNumber >= 3;
    }

    public static boolean isListenExplanation() {
        return (simulationNumber != 7) && (simulationNumber >= 4);
    }

    public static boolean isLCSNormExplanationPlusOwnContext() { // isLCSNormExplanation
        return simulationNumber >= 5 && simulationNumber != 7;
    }

    public static boolean isLCSNormExplanationPlusValue() { //isLCSNormExplanationPlusOwnContext
    	return simulationNumber >= 6 && simulationNumber != 7;
    }

    //executed before steppings. 
    public void start(){
        super.start();
        explanationStatistics = new ExplanationStats.Statistics();
        interactionBasedExplanationStatistics = new ExplanationStats.Statistics();
        interactionRelationshipStats = new RelationshipStats();
        interactionLocationStats = new LocationStats();
        agentTypeStats = new AgentTypeStats.Statistics();

        //read in the payoff table.
        payoffCalculator = new UpdatedBasePayoffCalculator();

        try{
            out = new BufferedWriter(new FileWriter(RESULTS_PATH + getResultsFilePath(".csv")));
            List<String> fields = Arrays.stream(new String[]{
                    "Step",
                    "Interactions",
                    "Payoff_Per_Interaction",
                    "Satisfaction_Per_Interaction",
                    "Avg_Payoff_in_Window",
                    "Avg_Satisfaction_in_Window",
            }).collect(Collectors.toList());
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Expected_Actor_Payoff_for_%s_agents", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Avg_Expected_Actor_Payoff_for_%s_agents_in_Window", type.name().toLowerCase()));
            }
            
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Expected_Observer_Payoff_for_%s_agents", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Avg_Expected_Observer_Payoff_for_%s_agents_in_Window", type.name().toLowerCase()));
            }
            for (AgentType type: AgentType.values()) {
                fields.add(String.format("Satisfaction_for_%s_agents", type.name().toLowerCase()));
            }
            
            if (MEASURE_TIME) {
                fields.add("Avg time to decide");
                fields.add("Avg time for feedbacks");
                fields.add("Avg time to decide in Window");
                fields.add("Avg time for feedbacks in Window");
            }
            fields.add("Avg_privacy_loss");
            fields.add("Avg_privacy_loss_in_Window");
            out.write(String.join(",", fields) + "\r\n");
        }catch(Exception e){
            try{
                out.close();
            }catch(Exception e2){
            }
            out = null;
        }
        
        //reset everything
        window.clear();
        window2.clear();
        windowForPayoffMetric.clear();;
        windowForSatisfactionMetric.clear();
        windowForDecideTimeMetric.clear();
        windowForInformationCostMetric.clear();
        windowForFeedbackTimeMetric.clear();
        allAgents = new Bag();
        interactionsInThisStep = new Bag();
        allInteractions = new Bag();
        overallSatisfaction = 0.0;
        payoff = 0.0;
        
        //initialize all agents
        for(int i=0; i<numAgents; i++){
            Agent agent = new Agent(i);
            
            //define networks
            agent.familyCircle = (int)(i/numHomes);
            agent.colleagueCircle = i % numOffices;
            //friend circle is random. To be updated.
            agent.friendCircle = (int)(random.nextDouble()*numParties);
            
            //define health states
            agent.health = random.nextInt((Health.values()).length);
            agent.preference = random.nextInt((Preference.values()).length);
            
            if (isLCS()) {
                agent.initLCS(this);
            }
            allAgents.add(agent);
        }

        //Assign agent types as per the defined ratio
        int[] agentIdPermutation = getRandomPermutation(numAgents);
        int[] agentPopulationsBasedOnRatio = getPopulationsBasedOnRatio(agentSocietyRatios, numAgents);
        AgentType[] agentTypes = AgentType.values();
        System.out.println("Populations for each type of agent:");
        for (int typeId = 0; typeId < agentTypes.length; typeId++) {
            System.out.printf("%s: %d\n", agentTypes[typeId].name(), agentPopulationsBasedOnRatio[typeId]);
        }
        int currentAgentTypeId = 0;
        int[] counts = new int[3];
        int currentSet = 0;
        for (int i = 0; i < numAgents; i++) {
            int id = agentIdPermutation[i];
            Agent agent = (Agent) allAgents.get(id);
            while (currentSet >= agentPopulationsBasedOnRatio[currentAgentTypeId]) {
                currentAgentTypeId++;
                currentSet = 0;
            }
            agent.setAgentType(agentTypes[currentAgentTypeId]);
            agent.setAgentValues(agentTypes[currentAgentTypeId].weights);
            counts[currentAgentTypeId]++;
            currentSet++;
        }
        Debugger.debug(counts, "actual counts");

        //Each agent keeps lists of their family, colleagues and friends
        Agent temp;
        int i = 0;
        for(i=0; i<numAgents; i++){
            Agent agent = (Agent)allAgents.get(i);
            for(int j=0; j<allAgents.size();j++){
                if (i==j) continue;
                temp = (Agent)allAgents.get(j);
                
                //keep references to members in my circles. 
                if (agent.familyCircle==temp.familyCircle)
                    agent.myFamilies.add(temp);
                else if (agent.colleagueCircle==temp.colleagueCircle)
                    agent.myColleagues.add(temp);
                else if (agent.friendCircle==temp.friendCircle)
                    agent.myFriends.add(temp);
                else
                    agent.myStrangers.add(temp);
            }
            schedule.scheduleRepeating(agent, 0, 1.0);
        }
        
        //give feedback after all interactions are made
        schedule.scheduleRepeating(new Steppable(){
            public void step(SimState state){
                Agents agents = (Agents)state;
                Agent temp;
                for(int i=0;i<agents.allAgents.size();i++){
                    temp = (Agent)agents.allAgents.get(i);
                    temp.giveFeedbacks(state);
                }
            }
        }, 1, 1.0);
        
        //after each step, output information and 
        //reset isPairedUp and currentInteraction of each agent.
        schedule.scheduleRepeating(new Steppable(){
            public void step(SimState state){
                Agents agents = (Agents)state;
                Agent temp;
                
                RecordForLearning record;
                for(int i=0;i<agents.allAgents.size();i++){
                    temp = (Agent) agents.allAgents.get(i);
                    if (temp.isPairedUp){
                        
                        //each agent keeps a record of their interaction for
                        //future classification
                        record = new RecordForLearning(temp.currentInteraction, agents, temp);

                        
                        if (isLCS()) {
                            temp.addRecordLCS(record, temp.currentInteraction);
                        } 
                        
                        temp.isPairedUp = false;
                        temp.currentInteraction = null;
                    }
                }
                
                // Cohesion:  
                // Calculate the overall satisfaction in this step.
                // Satisfaction: +1 if as same as expected, -1 if not
                // Neighbors: based on feedbacks. 
                // Say there are at least one neighbor. For each neighbor, 
                // if positive feedback, positive sanction, otherwise, negative sanction.
                // The number for sanction is based on the relationship
                
                // need to rewrite for satisfaction
                overallSatisfaction = 0.0;
                int peopleinvolved = 0;
                Interaction interaction;
                Feedback feedback;

                AgentPayoffs actorPayoffs = new AgentPayoffs();
                AgentPayoffs observerPayoffs = new AgentPayoffs();
                
                Satisfactions satisfactionByAgentType = new Satisfactions();

                //Calculate payoff
                payoff = 0.0;
                int multiplier = 1;
                double totalTimeToDecide = 0;
                double totalTimeForFeedbacks = 0;
                double totalInformationCost = 0.0;
                for(int i=0; i < agents.interactionsInThisStep.size(); i++){
                	interaction = (Interaction) agents.interactionsInThisStep.get(i);
                    if (MEASURE_TIME) {
                        totalTimeToDecide += interaction.timeToDecide;
                        totalTimeForFeedbacks += interaction.timeToGiveFeedbacks;
                    }
                    totalInformationCost += interaction.informationCost;
                    
                    double actorPayoff = 0;
                    double actorPayoffCounterpart = 0;
                    int counterpartAction = (interaction.action == 0) ? 1 : 0;
                    
                    if (interaction.observer != null) {
                    	Context interactionContext = Context.builder().interactLocation(Location.get(interaction.location / Agents.numAgents))
        	   				 .actorHealth(Health.get(interaction.actor.health))
        	   				 .preference(Preference.get(interaction.actor.preference))
        	   				 .observerAgentType(interaction.observer.agentType)
        	   				 .observerRelationship(interaction.getInteractionRelationship())
        	   				 .build();
            	        actorPayoff = agents.payoffCalculator.calculateActorPayoff(
            	        		interactionContext, Action.fromID(interaction.action), interaction.actor.agentType.weights);
            	        actorPayoffCounterpart = agents.payoffCalculator.calculateActorPayoff(
            	        		interactionContext, Action.fromID(counterpartAction), interaction.actor.agentType.weights);
                    } else {
                    	Context interactionContext = Context.builder().interactLocation(Location.get(interaction.location / Agents.numAgents))
              				 .actorHealth(Health.get(interaction.actor.health))
              				 .preference(Preference.get(interaction.actor.preference))
              				 .observerRelationship(interaction.getInteractionRelationship())
              				 .build();
                    	actorPayoff = agents.payoffCalculator.calculateActorPayoff(
                    			interactionContext, Action.fromID(interaction.action), interaction.actor.agentType.weights);
                    	actorPayoffCounterpart = agents.payoffCalculator.calculateActorPayoff(
            	        		interactionContext, Action.fromID(counterpartAction), interaction.actor.agentType.weights);
                    }
                    
                    // calculate goal satisfaction
                    // if the payoff of the selected action is smaller than other other, the agent deviates from its goal
                    if (actorPayoff < actorPayoffCounterpart){
                    	overallSatisfaction -= 1.0;
                    	satisfactionByAgentType.add(interaction.actor, false);
                    }
                    else{
                    	overallSatisfaction += 1.0;
                    	satisfactionByAgentType.add(interaction.actor, true);
                    }
                    
                    payoff += actorPayoff;
                    actorPayoffs.add(interaction.actor, actorPayoff);

                    //UPDATE: Use the actual payoffs the neighbors gave
                    multiplier = interaction.feedbacks.size();
                    double n = 0.0;
                    for(int j = 0; j < interaction.feedbacks.size(); j++){
                        Feedback observerFeedback = (Feedback) interaction.feedbacks.get(j);
                        observerPayoffs.add(observerFeedback.giver, observerFeedback.payoff / multiplier);
                        n += observerFeedback.payoff;
                    }
                    if (multiplier > 0)
                    	n /= multiplier;
                    //n is the average neighbor payoff
                    if (interaction.observer != null) {
	                    if (n >= 0) {
	                    	interactionBasedExplanationStatistics.addAccept(Action.fromID(interaction.action),
	                                Location.get(interaction.location / Agents.numAgents));
	                    	agentTypeStats.addAccept(Action.fromID(interaction.action),
	                                interaction.actor.agentType, interaction.observer.agentType);
	                    } else {
	                    	interactionBasedExplanationStatistics.addReject(Action.fromID(interaction.action),
	                                Location.get(interaction.location / Agents.numAgents));
	                    	agentTypeStats.addReject(Action.fromID(interaction.action),
	                                interaction.actor.agentType, interaction.observer.agentType);
	                    }
                    }
                    payoff += n;
                }
                
                // average goal satisfaction
                if (agents.interactionsInThisStep.size() > 0)
                	overallSatisfaction /= (double) agents.interactionsInThisStep.size();
                
                //UPDATE: average payoff per interaction
                if (agents.interactionsInThisStep.size()>0)
                	payoff /= (double) agents.interactionsInThisStep.size();
                

                if (MEASURE_TIME && agents.interactionsInThisStep.size() > 0) {
                    totalTimeToDecide /= agents.interactionsInThisStep.size();
                    totalTimeForFeedbacks /= agents.interactionsInThisStep.size();
                    windowForDecideTimeMetric.add(state.schedule.getSteps(), totalTimeToDecide);
                    windowForFeedbackTimeMetric.add(state.schedule.getSteps(), totalTimeForFeedbacks);
                }
                if (agents.interactionsInThisStep.size() > 0) {
                	totalInformationCost /= agents.interactionsInThisStep.size();
                }
                windowForInformationCostMetric.add(state.schedule.getSteps(), totalInformationCost);

                if (out!=null){
                    try{
                        String[] row = new String[]{
                                Long.toString(state.schedule.getSteps()),
                                Integer.toString(agents.interactionsInThisStep.size()),
                                Double.toString(payoff),
                                Double.toString(overallSatisfaction)
                        };
                        out.write(String.join(",", row));
                    }catch(Exception e){
                        try{out.close();}catch(Exception e2){}
                        out = null;
                    }
                }

                actorPayoffWindow.add(state.schedule.getSteps(), actorPayoffs);
                observerPayoffWindow.add(state.schedule.getSteps(), observerPayoffs);

                //Output the average payoff over a window 
                window.put(state.schedule.getSteps(), payoff);
                windowForPayoffMetric.add(state.schedule.getSteps(), payoff);
                windowForSatisfactionMetric.add(state.schedule.getSteps(), overallSatisfaction);
                numInteractions.put(state.schedule.getSteps(), agents.interactionsInThisStep.size());
                
                //Use mean of window, considering #interactions in each step
                payoff = 0.0;
                int interactionCount = 0;
                int totalInteractionCount = 0;
                for(Map.Entry<Long, Double> entry : window.entrySet()){
                	interactionCount = numInteractions.get(entry.getKey());
                    payoff += entry.getValue() * interactionCount;
                    totalInteractionCount += interactionCount;
                }
                if (totalInteractionCount > 0)
                	payoff /= totalInteractionCount;
                
                //Use average over window2 as output
                window2.put(state.schedule.getSteps(), overallSatisfaction);
                //Use mean of window, considering #calls in each step
                overallSatisfaction = 0.0;
                for(Map.Entry<Long, Double> entry : window2.entrySet()){
                	interactionCount = numInteractions.get(entry.getKey());
                	overallSatisfaction+=entry.getValue() * interactionCount;
                }
                if (totalInteractionCount > 0)
                	overallSatisfaction /= totalInteractionCount;
                
                if (out!=null){
                    try{
                     	// Avg Payoff in Window, Avg Satisfaction in Window
                        out.write("," + payoff + "," + overallSatisfaction);
                        List<String> agentPayoffTokens = new ArrayList<>();
                        //  Expected Payoff for {} agents
                        for (AgentType type : AgentType.values()) {
                            agentPayoffTokens.add(Double.toString(actorPayoffs.getAvgPayoff(type)));
                        }
                        // Avg Expected Payoff for {} agents in Window
                        agentTypeActorPayoffInWindow.clear();
                        for (AgentType type : AgentType.values()) {
                            double actorPayoffInWindow = actorPayoffWindow.getAvgPayoff(type);
                            agentTypeActorPayoffInWindow.put(type, actorPayoffInWindow);
                            agentPayoffTokens.add(Double.toString(actorPayoffInWindow));
                        }
                        
                        for (AgentType type : AgentType.values()) {
                            agentPayoffTokens.add(Double.toString(observerPayoffs.getAvgPayoff(type)));
                        }
                        agentTypeObserverPayoffInWindow.clear();
                        for (AgentType type : AgentType.values()) {
                            double agentObserverPayoffInWindow = observerPayoffWindow.getAvgPayoff(type);
                            agentTypeObserverPayoffInWindow.put(type, agentObserverPayoffInWindow);
                            agentPayoffTokens.add(Double.toString(agentObserverPayoffInWindow));
                        }
                        
                        for (AgentType type : AgentType.values()) {
                            agentPayoffTokens.add(Double.toString(satisfactionByAgentType.getAvgSatisfaction(type)));
                        }
                        
                        out.write("," + String.join(",", agentPayoffTokens));
                        if (MEASURE_TIME) {
                            out.write("," + totalTimeToDecide + "," + totalTimeForFeedbacks);
                            out.write("," + windowForDecideTimeMetric.getAvgMetric() + "," +
                                    windowForFeedbackTimeMetric.getAvgMetric());
                        }
                        out.write("," + totalInformationCost);
                        out.write("," + windowForInformationCostMetric.getAvgMetric());
                        out.write("\r\n");
                    }catch(Exception e){
                        try{out.close();}catch(Exception e2){}
                        out = null;
                    }
                }
                
                interactionsInThisStep = new Bag();
            }
        }, 2, 1.0);
        System.out.println("Simulation started.");
    }

    private String getResultsFilePath(String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append("Results_Sim");
        builder.append(simulationNumber);
        for (int rat: agentSocietyRatios) {
            builder.append("_").append(rat);
        }
        if (payoffCalculator instanceof UpdatedBasePayoffCalculator) {
            builder.append("_updated_base");
        }

        if (trial != 0) {
            builder.append("_trial").append(trial);
        }
        if (extension != null) {
            builder.append(extension);
        }
        return builder.toString();
    }

    private String getResultsFilePath() {
        return getResultsFilePath(null);
    }

    private String getEmergentNormsPath() {
        String filePath = getResultsFilePath();
        return filePath + "_emergent_norms.txt";
    }

    private String getDetailedNormsDataPath() {
        String filePath = getResultsFilePath();
        return filePath + "_norms_data.csv";
    }

    //Get all agents currently in a location
    public Bag getNeighbors(int location){
        Bag neighbors = new Bag();
        Agent temp;
        for(int i=0;i<allAgents.size();i++){
            temp = (Agent)allAgents.get(i);
            if (temp.location==location)
                neighbors.add(temp);
        }
        return neighbors;
    }

    private List<NormDataRow> getNormsData() {
        Map<Norm, Double> normVoteMap = new LinkedHashMap<>();
        for (int i = 0; i < numAgents; i++) {
            Agent agent = (Agent) allAgents.get(i);
            agent.contributeNorms(normVoteMap);
        }
        List<NormDataRow> normsData = new ArrayList<>();
        for (Map.Entry<Norm, Double> entry : normVoteMap.entrySet()) {
            Norm norm = entry.getKey();
            double support = getSupport(norm);
            NormDataRow dataRow = new NormDataRow(norm, support);
            if (support > NORM_EMERGENCE_THRESHOLD) {
                dataRow.isEmerged = true;
            }
            normsData.add(dataRow);
        }

        int dataSize = normsData.size();

        for (int i = 0; i < dataSize; i++) {
            NormDataRow dataRowI = normsData.get(i);
            for (int j = 0; j < dataSize; j++) {
                if (i == j) continue;
                NormDataRow dataRowJ = normsData.get(j);
                if (dataRowJ.isEmerged && dataRowJ.norm.subsumes(dataRowI.norm)) {
                    dataRowI.isSubsumedByEmerged = true;
                }
            }
        }

        return normsData;
    }

    private List<ElectionResult> getEmergentNormsLCS(List<NormDataRow> normsData) {
        List<ElectionResult> popularNorms = new ArrayList<>();
        List<ElectionResult> allNorms = new ArrayList<>();
        List<ElectionResult> emergedNorms = new ArrayList<>();

        for (NormDataRow dataRow : normsData) {
            allNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
            if (dataRow.isEmerged) {
                popularNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
                if (!dataRow.isSubsumedByEmerged) {
                    emergedNorms.add(new ElectionResult(dataRow.norm, dataRow.acceptanceRate));
                }
            }
        }

        allNorms.sort(Comparator.comparing(ElectionResult::getPopularityRatio).reversed());

        System.out.println();
        System.out.println("== All popular rules ===" + popularNorms.size());
        for (ElectionResult electionResult : popularNorms) {
        	if (electionResult.popularityRatio > 0.7) {
        		System.out.printf("[%s, popularity: %f]\n", electionResult.norm, electionResult.popularityRatio);
        	}

        }

        return emergedNorms.stream()
                .sorted(Comparator.comparing(ElectionResult::getPopularityRatio).reversed())
                .collect(Collectors.toList());
    }

    private static class NormDataRow {
        public Norm norm;
        public double acceptanceRate;
        public boolean isEmerged;

        //Only relevant if isEmerged is true
        public boolean isSubsumedByEmerged;

        public NormDataRow(Norm norm, double acceptanceRate) {
            this.norm = norm;
            this.acceptanceRate = acceptanceRate;
            this.isEmerged = false;
            this.isSubsumedByEmerged = false;
        }

        public String toCSV() {
            return String.join(",", norm.toString(), Double.toString(acceptanceRate),
                    Boolean.toString(isEmerged), Boolean.toString(isSubsumedByEmerged));
        }
    }

    private static class ElectionResult {
        public Norm norm;
        public double popularityRatio;

        public ElectionResult(Norm norm, double popularityRatio) {
            this.norm = norm;
            this.popularityRatio = popularityRatio;
        }

        public double getPopularityRatio() {
            return popularityRatio;
        }
    }

    private double getSupport(Norm norm) {
        int votes = 0;
        for (int i = 0; i < numAgents; i++) {
            Agent agent = (Agent) allAgents.get(i);
            if (agent.actionModel.getAcceptableDecisions(Collections.singletonList(norm), false)
                    .contains(norm.consequent)) {
                votes++;
            }
        }
        return ((double)votes) / numAgents;
    }

    private String getNormsDataHeader() {
        List<String> fields = Arrays.stream(new String[]{
                "norm",
                "acceptance_rate",
                "is_emerged",
                "is_subsumed_by_emerged"
        }).collect(Collectors.toList());
        return String.join(",", fields);
    }

    public void finish(){
        super.finish();
        try{out.close();}catch(Exception e){}
        out = null;
        if (isLCS()) {
            Agent agent = (Agent) allAgents.get(0);
            agent.actionModel.printStats();
        }
        try {
            out = new BufferedWriter(new FileWriter(RESULTS_PATH + getEmergentNormsPath()));
            normsDataWriter = new BufferedWriter(new FileWriter(RESULTS_PATH + getDetailedNormsDataPath()));
            normsDataWriter.write(getNormsDataHeader() + "\n");
            out.write(explanationStatistics.toString() + "\n");
            System.out.println(explanationStatistics.toString());
            out.write("========== Interaction Based explanation stats ===============\n");
            System.out.println("========== Interaction Based explanation stats ===============");
            out.write(interactionBasedExplanationStatistics.toString() + "\n");
            System.out.println(interactionBasedExplanationStatistics.toString());
            out.write("========== Interaction Based explanation stats ends===============\n");
            System.out.println("========== Interaction Based explanation stats ends===============");
            out.write("== Location Stats==\n");
            System.out.println("== Location Stats==");
            out.write(interactionLocationStats.toString() + "\n");
            System.out.println(interactionLocationStats.toString());
            out.write(interactionRelationshipStats.toString() + "\n");
            System.out.println(interactionRelationshipStats.toString());
            
            
            out.write("========== Agent types stats ===============\n");
            System.out.println("========== Interaction Based explanation stats ===============");
            out.write(agentTypeStats.toString() + "\n");
            System.out.println(agentTypeStats.toString());
            out.write("========== Agent types stats ends===============\n");
            
            System.out.printf("Last exception case step: %d\n", lastExceptionCaseStep);
            if (MEASURE_TIME) {
                System.out.printf("Avg time to decide : %f\n", windowForDecideTimeMetric.getAvgMetric());
                System.out.printf("Avg time for feedbacks: %f\n", windowForFeedbackTimeMetric.getAvgMetric());
            }
            System.out.printf("Avg_privacy_loss: %f\n", windowForInformationCostMetric.getAvgMetric());
            out.write(String.format("Avg_privacy_loss: %f\n", windowForInformationCostMetric.getAvgMetric()));
            System.out.printf("Avg_payoff: %f\n", windowForPayoffMetric.getAvgMetric());
            out.write(String.format("Avg_payoff: %f\n", windowForPayoffMetric.getAvgMetric()));
            System.out.printf("Avg_goal_satisfaction: %f\n", windowForSatisfactionMetric.getAvgMetric());
            out.write(String.format("Avg_goal_satisfaction: %f\n", windowForSatisfactionMetric.getAvgMetric()));
            if (isLCS()) {
                List<NormDataRow> normsData = getNormsData();
                for (NormDataRow normDataRow : normsData) {
                    normsDataWriter.write(normDataRow.toCSV() + "\n");
                }
                System.out.println("========== Emergent Norms ===============");
                List<ElectionResult> electionResults = getEmergentNormsLCS(normsData);
                System.out.println();
                System.out.println("=== Final Emergent Norms ===");
                out.write("=== Final Emergent Norms ===\n");
                for (ElectionResult result : electionResults) {
                    System.out.printf("[%s, popularity: %f]\n", result.norm, result.popularityRatio);
                    out.write(String.format("[%s, popularity: %f]\n", result.norm, result.popularityRatio));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{out.close();}catch(Exception e){}
        try {
            normsDataWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Simulation ends");
    }
    
    public static void main(String[] args){
    	
    	SimState state = new Agents(System.currentTimeMillis());
    	for(int job = 0; job < jobs; job++)
    	{
	    	state.setJob(job);
	    	state.start();
    	do
    		if (!state.schedule.step(state)) break;
    	while(state.schedule.getSteps() < numSteps);
    	state.finish();
    	}
	
        System.exit(0);
    }

    private static class WindowForMetric {
        private LinkedHashMap<Long, Double> metricMap;

        public WindowForMetric() {
            this.metricMap = getWindowMapDouble();
        }

        public void add(long stepId, double metric) {
            metricMap.put(stepId, metric);
        }

        public double getAvgMetric() {
            int total = 0;
            double metric = 0.0;
            for (Map.Entry<Long, Double> entry : metricMap.entrySet()) {
                metric += entry.getValue();
                total++;
            }
            if (total > 0) {
                return metric / total;
            } else {
                return 0.0;
            }
        }

        public void clear() {
            this.metricMap.clear();
        }
    }

    private static class AgentPayoffWindow {
        private Map<AgentType, LinkedHashMap<Long, Double>> payoffWindow;
        private Map<AgentType, LinkedHashMap<Long, Integer>> countWindow;

        public AgentPayoffWindow() {
            this.payoffWindow = new HashMap<>();
            for (AgentType type : AgentType.values()) {
                payoffWindow.put(type, getWindowMapDouble());
            }
            this.countWindow = new HashMap<>();
            for (AgentType type : AgentType.values()) {
                countWindow.put(type, getWindowMapInteger());
            }
        }

        public void add(long stepId, AgentPayoffs agentPayoffs) {
            for (AgentType type : AgentType.values()) {
                double payoff = agentPayoffs.getAvgPayoff(type);
                int count = agentPayoffs.getCount(type);
                LinkedHashMap<Long, Double> typePayoffWindow = payoffWindow.get(type);
                typePayoffWindow.put(stepId, payoff);
                LinkedHashMap<Long, Integer> typeCountWindow = countWindow.get(type);
                typeCountWindow.put(stepId, count);
                payoffWindow.put(type, typePayoffWindow);
                countWindow.put(type, typeCountWindow);
            }
        }

        public double getAvgPayoff(AgentType type) {
            LinkedHashMap<Long, Double> typePayoffWindow = payoffWindow.get(type);
            LinkedHashMap<Long, Integer> typeCountWindow = countWindow.get(type);
            int total = 0;
            double payoffs = 0.0;
            for (Map.Entry<Long, Double> entry : typePayoffWindow.entrySet()) {
                int count = typeCountWindow.get(entry.getKey());
                payoffs += entry.getValue() * count;
                total += count;
            }
            
            if (total > 0) {
                return payoffs / total;
            } else {
                return 0.0;
            }
        }
    }

    private static class AgentPayoffs {
        private Map<AgentType, Double> payoffs;
        private Map<AgentType, Integer> counts;

        public AgentPayoffs() {
            this.payoffs = new HashMap<>();
            this.counts = new HashMap<>();
        }

        public void add(Agent agent, double payoff) {
            this.payoffs.put(agent.agentType, payoffs.getOrDefault(agent.agentType, 0.0) + payoff);
            this.counts.put(agent.agentType, counts.getOrDefault(agent.agentType, 0) + 1);
        }

        /**
         * Total payoff for people of this type divided by the number of instances where payoff is calculated
         */
        public double getAvgPayoff(AgentType agentType) {
            int count = counts.getOrDefault(agentType, 0);
            if (count == 0) {
                return 0.0;
            } else {
                return payoffs.getOrDefault(agentType, 0.0) / count;
            }
        }

        public int getCount(AgentType type) {
            return counts.getOrDefault(type, 0);
        }
    }
    
    private static class Satisfactions {
        private Map<AgentType, Double> satisfactions;
        private Map<AgentType, Integer> counts;

        public Satisfactions() {
            this.satisfactions = new HashMap<>();
            this.counts = new HashMap<>();
        }

        public void add(Agent agent, boolean satisfy) {
            
            this.counts.put(agent.agentType, counts.getOrDefault(agent.agentType, 0) + 1);
            if (satisfy) {
            	this.satisfactions.put(agent.agentType, satisfactions.getOrDefault(agent.agentType, 0.0) + 1);
            } else {
            	this.satisfactions.put(agent.agentType, satisfactions.getOrDefault(agent.agentType, 0.0) - 1);
            }
        }

        /**
         * Total payoff for people of this type divided by the number of instances where payoff is calculated
         */
        public double getAvgSatisfaction(AgentType agentType) {
            int count = counts.getOrDefault(agentType, 0);
            if (count == 0) {
                return 0.0;
            } else {
                return satisfactions.getOrDefault(agentType, 0.0) / count;
            }
        }

        public int getCount(AgentType type) {
            return counts.getOrDefault(type, 0);
        }
    }
}