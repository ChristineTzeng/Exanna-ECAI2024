package simulation;

import base.Action;
import base.Context;
import util.Debugger;

import java.io.BufferedReader;
import java.io.FileReader;

public class MultiEmergentNormBasePayoffCalculator extends PayoffCalculator {
    //pay off table, if neighbor thinks callee should answer
    private double[] payoff_a;
    //pay off table, if neighbor thinks callee should ignore
    //during first and second simulations, neighbor thinks callee should ignore
    //during a meeting or in a library (hard-coded in this way).
    private double[] payoff_i;

    public MultiEmergentNormBasePayoffCalculator() {
        payoff_a = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,1,-1,0,0,1,-1,0,0};
        payoff_i = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,-1,1,0,0,-1,1,0,0};
        try{
            BufferedReader reader = new BufferedReader(new FileReader("payoff_multi_emergent_updated.txt"));
            String line;
            String[] items;
            int i = 0;
            while((line=reader.readLine())!=null){
                if (i>=payoff_a.length) break;
                line = line.trim();
                if (line.length()<=0) continue;
                if (line.startsWith("#")) continue;
                items = line.split("\\s+");
                payoff_a[i] = Double.parseDouble(items[0]);
                try{
                    payoff_i[i] = Double.parseDouble(items[1]);
                }catch(Exception ee){
                    payoff_i[i] = payoff_a[i];
                }
                i++;
            }
            reader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        Debugger.debug(payoff_a, "payoff_a");
        Debugger.debug(payoff_i, "payoff_i");
    }

	@Override
	public double calculateObserverPayoff(Context context, Action actorAction, boolean accept) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double calculateActorPayoff(Context context, Action actorAction) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double calculateActorPayoff(Context context, Action actorAction, double[] dist) {
		// TODO Auto-generated method stub
		return 0;
	}
}
