# Exanna
 
Codebase for simulations where agents provide rationales with different strategies in a simulated environment.


# Generate simulation results in the paper
The simulation code is in Java.
Run the simulation with the Agents.java file under src/simulation.

We run 10 trials for each type of society.
Simulation parameter:
- simulationNumber
	4: share all society
	5: share decision rule society
	6: exanna society

- trial: set trial number

# Process the simulation results

The code is in python and run with command line.

avg_results.py - To generate the average numbers over multiple runs of simulations on files with details.
analyze_stats.py - To generate the average numbers over multiple runs of simulations on summary files.

Use the following commands to generate the results:
file_name.py 1,1 2 simulationNumber numTrials

To generate results for all types of settings in the paper, we set simulationNumber as 4,5,6 and numTrials as 10.

# Generate figures in the paper

The code is in python3 and run with jupyter notebook.

plot_figure.ipynb - To generate the graphs in the paper

# Required packgaes or libraries

For Java code: 
jar files under /lib folder

For python code:

scipy
numpy
pandas
seaborn