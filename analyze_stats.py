import sys
import pandas as pd
# from scipy.stats import ttest_rel
from scipy.stats import ttest_ind
import pingouin as pt
import numpy as np
import csv

results_path = 'results/'

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sim_tokens = sim_string.split(',')
simulations = [int(x) for x in sim_tokens]

labels = ['Share-all', 'Share-rules', 'Exanna']
values = [4, 5, 6]

emergent_norms = False

def get_payoff_scheme_token(payoff_scheme):
    if payoff_scheme == '2':
        return '_updated_base'
    elif payoff_scheme == '3':
        return '_multi_updated_base'
    else:
        return ''


trial_string = sys.argv[4]
trials = [x for x in range(1, int(trial_string) + 1)]

def get_file_name(sim, ratio, payoff_scheme, trial, emergent_norms = True):
    if (trial == 0):
        trial_token = ''
    else:
        trial_token = '_trial' + str(trial)
    if emergent_norms:
        emergency_norm_token = '_emergent_norms'
        ext = '.txt'
    else:
        emergency_norm_token = ''
        ext = '.csv'
    return results_path + 'Results_Sim' + str(sim) + '_' + '_'.join(ratio) \
            + get_payoff_scheme_token(payoff_scheme) + trial_token + emergency_norm_token + ext

def extract_stats(file_name, field_name):
    payoff = None
    coh = None
    payoff_done = False
    coh_done = False
    data = None
    with open(file_name, 'r') as f:
        for line in f:
            if line.startswith(field_name):
            	row = line.split(':')
            	data = float(row[1])
            elif (line.startswith('[total') and (field_name in line)):
            	attributes = line.split(',')
            	

            	for attr in attributes:
            		attr = attr.strip()
            		if attr.startswith(field_name):
            			row = attr.split(':')
            			data = float(row[1])
            			break
                        
    return data

payoff_scheme = sys.argv[2]

sim_data = []

# field_names = ['Avg_payoff', 'Avg_goal_satisfaction', 'Avg_privacy_loss', 'resolution']
# field_names = ['Satisfaction_for_health_agents', 'Satisfaction_for_freedom_agents']
field_names = ['Satisfaction_Per_Interaction']

row = []
data_row = []

for sim in simulations:
    for trial in trials:
        
    
        fil = get_file_name(sim, ratio, payoff_scheme, trial, emergent_norms)
        print(f'Reading file = %s' % fil)
        for attr_name in field_names:
        	data = extract_stats(fil, attr_name)
        	data = np.array(data).T
        	row.append(data)
        # row.append(labels[values.index(sim)])
        # print(labels[values.index(sim)])
        row.append(sim)
        data_row.append(row)
        row = []
    # print(data_row)
    sim_data.extend(data_row)
    

    sim_data_df = pd.DataFrame(sim_data, columns = field_names + ['source'])
    
    print('Mean', sim_data_df.mean())
    print('std', sim_data_df.std())

    sim_data_df['source'] = [labels[values.index(sim)] for i in range(10)]

    sim_data_df.to_csv('results/Summary.csv', index=False)


    data_row = []