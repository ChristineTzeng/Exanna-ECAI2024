import csv
import pandas as pd
import sys
import numpy as np

results_path = 'results/'

payoff_scheme = sys.argv[2]

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sim_tokens = sim_string.split(',')
simulations = [int(x) for x in sim_tokens]
no_of_simulations = len(simulations)

emergent_norms = False

def get_payoff_scheme_token(payoff_scheme):
    if payoff_scheme == '2':
        return '_updated_base'
    else:
        return ''

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

for sim in simulations:
    fil = get_file_name(sim, ratio, payoff_scheme, 0, False)
    with open(fil, 'a', newline = '') as csv_file:
        writer = csv.writer(csv_file, delimiter = ',')
        writer.writerow([
            'Step','Interactions','Payoff_Per_Interaction','Satisfaction_Per_Interaction','Avg_Payoff_in_Window',
            'Avg_Satisfaction_in_Window','Expected_Actor_Payoff_for_health_agents','Expected_Actor_Payoff_for_freedom_agents',
            'Avg_Expected_Actor_Payoff_for_health_agents_in_Window','Avg_Expected_Actor_Payoff_for_freedom_agents_in_Window',
            'Expected_Observer_Payoff_for_health_agents','Expected_Observer_Payoff_for_freedom_agents',
            'Avg_Expected_Observer_Payoff_for_health_agents_in_Window','Avg_Expected_Observer_Payoff_for_freedom_agents_in_Window',
            'Satisfaction_for_health_agents', 'Satisfaction_for_freedom_agents', 'Avg_privacy_loss', 'Avg_privacy_loss_in_Window'
            ])

trial_string = sys.argv[4]
trials = [x for x in range(1, int(trial_string) + 1)]

for sim in simulations:
    data = []
    for trial in trials:
        data_row = []
        fil = get_file_name(sim, ratio, payoff_scheme, trial, emergent_norms)
        print(f'Reading file = %s' % fil)
        with open(fil) as csv_file:
            next(csv_file)
            csv_reader = csv.reader(csv_file, delimiter = ',')
            for row in csv_reader:
                row = [float(x) if idx > 1 else int(x) for idx, x in enumerate(row)]
                data_row.append(row)
        data.append(data_row)

    out_data = np.average(data, axis = (0))
    fil = get_file_name(sim, ratio, payoff_scheme, 0, False)
    with open(fil, 'a', newline = '') as csv_file:
        writer = csv.writer(csv_file, delimiter = ',')
        for row in out_data:
            row = [x if idx > 0 else int(x) for idx, x in enumerate(row)]
            writer.writerow(row)

