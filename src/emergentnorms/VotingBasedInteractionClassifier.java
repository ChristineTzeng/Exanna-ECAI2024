package emergentnorms;

import ec.util.MersenneTwisterFast;
import sim.util.Bag;
import base.Action;
import simulation.Agent;
import simulation.Agents;
import simulation.Interaction;
import simulation.RecordForLearning;
import util.Debugger;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingBasedInteractionClassifier {
    private List<Agent> agents;
    private Agents agentsState;
    private MersenneTwisterFast random;

    public VotingBasedInteractionClassifier(Agents state, Bag agents) {
        this.agentsState = state;
        //this.state = state;
        //private SimState state;
        int numAgents = agents.size();
        this.agents = new ArrayList<>(numAgents);
        for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
            this.agents.add((Agent)agents.get(agentIndex));
        }
        this.random = state.random;
    }

    private Instances getDataset() {
        //Attribute list
        ArrayList<Attribute> alist = new ArrayList<Attribute>();
        Attribute aa;

        //location
        aa = new Attribute("location", new ArrayList<String>(
                Arrays.asList(Agents.locations)));
        alist.add(aa);

        //caller relation
        aa = new Attribute("caller_relation", new ArrayList<String>(
                Arrays.asList(RecordForLearning.relationTypes)));
        alist.add(aa);

        //urgency
        List<String> tf = new ArrayList<String>();//True of False attributes
        tf.add("true");tf.add("false");
        aa = new Attribute("urgency", tf);
        alist.add(aa);
        //exists_family
        aa = new Attribute("exists_family", tf);
        alist.add(aa);
        //exists_colleague
        aa = new Attribute("exists_colleague", tf);
        alist.add(aa);
        //exists_friend
        aa = new Attribute("exists_friend", tf);
        alist.add(aa);
        //answer or not
        aa = new Attribute("@Class@", tf);
        alist.add(aa);

        //payoff, numeric
        //aa = new Attribute("@Class@");
        //alist.add(aa);

        Instances dataset = new Instances("Sample calls", alist, 0);
        dataset.setClassIndex(dataset.numAttributes()-1);
        return dataset;
    }

    private Instances getLabeledInstances(List<Interaction> interactionList) {
        int numInteractions = interactionList.size();
        Instances instances = getDataset();
        Map<Integer, List<Integer>> agentIndexToActions = new HashMap<>();
        int numAgents = agents.size();
        int wearAction = 0;
        int notWearAction = 0;
        for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
            Agent agent = agents.get(agentIndex);
            List<Integer> actions = agent.getBulkAction(interactionList, agentsState);
            for (int action : actions) {
                if (action == 0) {
                	wearAction++;
                } else {
                	notWearAction++;
                }
            }
            agentIndexToActions.put(agentIndex, actions);
        }
        int wearCount = 0;
        int notWearCount = 0;
        int interactionWearCount = 0;
        int interactionNotWearCount = 0;
        double maxNotWearCountPercet = 0.0;
        double minNotWearCountPercentage = 1.0;
        for (int interactionIndex = 0; interactionIndex < numInteractions; interactionIndex++) {
        	Interaction interaction = interactionList.get(interactionIndex);
            if (interaction.action == 0) {
            	interactionWearCount++;
            } else {
            	interactionNotWearCount++;
            }
            int wearVote = 0;
            int notWearVote = 0;
            for (int agentIndex = 0; agentIndex < numAgents; agentIndex++) {
                int action = agentIndexToActions.get(agentIndex).get(interactionIndex);
                if (action == 0) {
                    wearVote++;
                } else {
                	notWearVote++;
                }
            }
            double notWearPercentage = ((double) notWearVote) / (wearVote + notWearVote);
            maxNotWearCountPercet = Math.max(maxNotWearCountPercet, notWearPercentage);
            minNotWearCountPercentage = Math.min(minNotWearCountPercentage, notWearPercentage);
            int popularAction = wearCount >= notWearCount ? 0 : 1;
            if (popularAction == 0) {
            	wearCount++;
            } else {
            	notWearCount++;
            }
            instances.add(getInstance(interaction, popularAction, instances));
        }
        Debugger.debug(wearCount, "wearCount", notWearCount, "notWearCount",
        		interactionWearCount, "interactionWearCount", interactionNotWearCount, "interactionNotWearCount",
        		maxNotWearCountPercet, "max not wear vote percentage",
        		minNotWearCountPercentage, "min not wear vote percentage",
                wearAction, "answer actions",
                notWearAction, "ignore actions");
        return instances;
    }

    public Instances getLabeledInstances(Bag interactions) {
        int numinteractions = interactions.size();
        List<Interaction> wearCases = new ArrayList<>();
        List<Interaction> notWearCases = new ArrayList<>();
        for (int interactionIndex = 0; interactionIndex < numinteractions; interactionIndex++) {
        	Interaction interaction = (Interaction) interactions.get(interactionIndex);
            if (interaction.action == Action.WEAR.getValue()) {
            	wearCases.add(interaction);
            } else {
            	notWearCases.add(interaction);
            }
        }
        Debugger.debug(wearCases.size(), "wearCasesCountInData", notWearCases.size(), "notWearCasesCountInData");
        List<Interaction> interactionList = new ArrayList<>(notWearCases);
        int wearNum = wearCases.size();
        //int takeNum = Math.min(ignoreNum, answeredCalls.size());
        int notWearNum = notWearCases.size();
        List<Interaction> wearSamples = sample(wearCases, wearNum);
        Debugger.debug(wearNum, "wearNum", wearSamples.size(), "wear sample size");
        interactionList.addAll(wearSamples);

        return getLabeledInstances(interactionList);
    }

    private <T> List<T> sample(List<T> list, int num) {
        int[] perm = getRandomPermutation(num);
        List<T> ret = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            ret.add(list.get(perm[i]));
        }
        return ret;
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

    private Instance getInstance(Interaction interaction, int action, Instances dataset) {
        double[] row = new double[dataset.numAttributes()];
        RecordForLearning rec = new RecordForLearning(interaction, agentsState, agents.get(0));
        
        row[0] = rec.location;
        //actorAgentType
//        row[1] = rec.actorAgentType;
        //actorHealth
        row[1] = rec.actorHealth;
        //preference
        row[2] = rec.preference;
        //observerAgentType
        row[3] = rec.observerAgentType;
//        //observerHealth
//        row[4] = rec.observerHealth;
      //relationship
        row[4] = rec.observerRelationship;
        //action
        row[5] = action;

        return new DenseInstance(1.0, row);
    }
}