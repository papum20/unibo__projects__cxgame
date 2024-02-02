import re
import os
import matplotlib.pyplot as plt



CONFIGS = {
	995: (9, 9, 5),
	18207: (18, 20, 7),
	385812: (38, 58, 12),
	704711: (70, 47, 11),
	10010030: (100, 100, 30)
}
CONFIGS_MAX_TURNS = {
	'81': (9, 9, 5),
	'360': (18, 20, 7),
	'2204': (38, 58, 12),
	'3290': (70, 47, 11),
	'10000': (100, 100, 30)
}



def process_file(filename):
	player_data = {}
	current_player = None
	turn_count = -1
	max_turns = 0
	game_started = False

	with open(filename, 'r') as file:
		lines = file.readlines()

		# Iterate through lines, handling multiple games, blank lines, and "Score" ending
		i = 0
		while i < len(lines):

			# Skip blank lines 
			while i < len(lines) and not lines[i].strip():
				i += 1
			if i == len(lines):
				break

			if lines[i].strip() == "-_-" and lines[i + 1].strip() == "START GAME" and lines[i + 2].strip() == "-_-":
				if game_started:
					# New game found, reset data for the previous game
					player_data = {}
					current_player = None
					turn_count = -1
				game_started = True  # Set game_started to True
				max_turns = extract_max_turns(filename)

			# finding a player name
			if game_started:
				if i < len(lines) and lines[i].strip().startswith("PnDb"):
					current_player = f'{extract_player_name(lines[i])}_{str(max_turns)}'

			# Check for "---", "#!/bin/bash", or end of data
			if game_started:
				if lines[i].strip() == "---":
					turn_count += 1
					current_player = None
				elif "#!/bin/bash" in lines[i]:  # Check for any mention of "#!/bin/bash"
					game_started = False  # End game processing
				else:
					extract_data(lines[i], player_data, current_player, turn_count, max_turns, filename)

			i += 1

	return player_data


def extract_max_turns(filename):
    max_pattern = r"_(\d+)\.txt$"  # Regex to extract number from filename

    match = re.search(max_pattern, filename)
    if match:
        number = int(match.group(1))
        return CONFIGS[number][0] * CONFIGS[number][1]

    # No error handling here, assuming all files are in expected format
    return 0


def extract_player_name(line):
	player_names = {
		"PnDb dnull": "dnull",
		"PnDb delta": "delta",
		"PnDb": "pndb",
		"PnDb2": "pndb"  # Merge with "pndb"
	}
	for name in player_names:
		if name in line:
			return player_names[name]
	return None

def extract_data(line, player_data, player, turn_count, max_turns, filename):
	for label in ["loops_n", "n_loops", "depth_rel_max", "dag_n", "proved_n", "created_n"]:
		match = re.search(rf"\b{label}\s*[=:]\s*(\d+)\b", line)
		if match:

			# skip corrupted data
			print(label, filename)
			if label == "depth_rel_max" and not '2/' in filename:
				continue
			
			value = int(match.group(1))
			#percentage = turn_count / (70*74)
			percentage = turn_count / max_turns
			add_value(player_data, player, label, percentage, value)

def add_value(player_data, player, label, percentage, value):
	label_name = label if label != "n_loops" else "loops_n"
	if player not in player_data:
		player_data[player] = {}
	if label_name not in player_data[player]:
		player_data[player][label_name] = []
	player_data[player][label_name].append((percentage, value))

def create_charts_by_label(data):
	for label_name in ["loops_n", "depth_rel_max", "dag_n", "proved_n", "created_n"]:

		# only depth and loops
		if label_name != "depth_rel_max" and label_name != "loops_n":
			continue

		chart_data = {}
		for player, player_data in data.items():
			for label_data, avgs in player_data.items():
				if label_name == label_data:
					chart_data[player] = avgs

		print(f"{label_name}\n{chart_data}")

		plt.figure(figsize=(10, 6))
		for player, avgs in chart_data.items():

			# skip configs
			if str(100*100) in player or str(70*47) in player:
				continue
			# skip pndb for loops
			if label_name == "loops_n" and "pndb" in player:
				continue
			
			plt.plot(avgs.keys(), avgs.values(), label=f"{ player.split('_')[0] }_{'x'.join([str(c) for c in CONFIGS_MAX_TURNS[player.split('_')[1]] ]) }" )

		plt.xlabel("percentage of game completed turn/max_turns")
		plt.ylabel(label_name)
		plt.title(f"Average {label_name} Values Across Games")
		plt.legend()
	plt.show()

def create_charts_by_max_turns(data):
	for max_turns, config in CONFIGS_MAX_TURNS.items():
		chart_data = {}
		for player, player_data in data.items():
			if max_turns in player:
				chart_data[player.split('_')[0]] = player_data	# rm _max_turns

		print(f"{max_turns}\n{chart_data}")

		plt.figure(figsize=(10, 6))
		i = 1
		for player, player_data in chart_data.items():
			plt.subplot(1, 3, i)
			labels = [f'{player}-proved_n', f'{player}-created_n/10']
			plt.stackplot(
				[i for i in range(101)],
				player_data['proved_n'].values(),
				list(map( lambda x: x/10, player_data['created_n'].values() )),
				labels=labels
			)
			#if label_name == "depth_rel_max":
			#	plt.plot(avgs.keys(), avgs.values(), label=player)
			#elif player != "pndb": #pndb has corrupted values
			#plt.plot(avgs.keys(), avgs.values(), label=player)
			plt.semilogy([i for i in range(101)], player_data['dag_n'].values(), label=f'{player}-dag_n')
			plt.legend()
			i += 1
		plt.xlabel("percentage of game completed turn/max_turns")
		plt.title(f"Nodes for {config[0]}x{config[1]}x{config[2]}")
	plt.show()


def main():
	data = {}
	data_half = {}
	for root, _, filenames in os.walk("."):
		for filename in filenames:
			if filename.endswith(".txt"):
				#print(os.path.join(root, filename))
				file_data = process_file(os.path.join(root, filename))
				print(os.path.join(root, filename))
				print(file_data)
				merge_data(data, file_data)
				merge_data_half(data_half, file_data)

	avg_data = get_avg_data(data)
	avg_data_half = get_avg_data_half(data_half)
	print(avg_data)

	print("CREATE_CHARTS")
	create_charts_by_label(avg_data_half)
	create_charts_by_max_turns(avg_data)

# put data in data
def merge_data(data, new_data):
	# format: {player: {label: [(percentage:int, [values]), ...], ...}, ...}
	for player, player_data in new_data.items():

		# select configurations
		#if not str(70*47) in player and not str(9*9) in player:
		#	continue
		
		if player not in data:
			data[player] = {}
		for label, values in player_data.items():
			if label not in data[player]:
				data[player][label] = {}
				for percentage in range(0, 101): #, 2):
					data[player][label][percentage] = []
			for percentage, value in values:
				data[player][label][min(100, int(round(percentage*100)))].append(value)
				#data[player][label][min(100, int(round(percentage*50)) * 2)].append(value)
				# every 2, so no problems with who starts playing first
	
# put data halved in data
def merge_data_half(data, new_data):
	# format: {player: {label: [(percentage:int, [values]), ...], ...}, ...}
	for player, player_data in new_data.items():

		# select configurations
		#if not str(70*47) in player and not str(9*9) in player:
		#	continue
		
		if player not in data:
			data[player] = {}
		for label, values in player_data.items():
			if label not in data[player]:
				data[player][label] = {}
				for percentage in range(0, 101, 2): #, 2):
					data[player][label][percentage] = []
			for percentage, value in values:
				data[player][label][min(100, int(round(percentage*50)) * 2 )].append(value)
				#data[player][label][min(100, int(round(percentage*50)) * 2)].append(value)
				# every 2, so no problems with who starts playing first
	
def get_avg_data(data):
	# format: {player: {label: [percentage:int, avg], ...}, ...}
	avg_data = {}
	for player, player_data in data.items():
		if player not in avg_data:
			avg_data[player] = {}
		for label, datas in player_data.items():
			if label not in avg_data[player]:
				avg_data[player][label] = {}
				for percentage in range(0, 101):
					avg_data[player][label][percentage] = 0.
			for percentage, values in datas.items():
				if len(values) > 0:
					avg_data[player][label][percentage] = sum(values) / len(values)

	return avg_data

def get_avg_data_half(data):
	# format: {player: {label: [percentage:int, avg], ...}, ...}
	avg_data = {}
	for player, player_data in data.items():
		if player not in avg_data:
			avg_data[player] = {}
		for label, datas in player_data.items():
			if label not in avg_data[player]:
				avg_data[player][label] = {}
				for percentage in range(0, 101, 2):
					avg_data[player][label][percentage] = 0.
			for percentage, values in datas.items():
				if len(values) > 0:
					avg_data[player][label][percentage] = sum(values) / len(values)

	return avg_data


if __name__ == "__main__":
	main()