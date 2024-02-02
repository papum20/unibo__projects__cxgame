import re
import os
import matplotlib.pyplot as plt

def process_file(filename):
	player_data = {}
	current_player = None
	turn_count = 0
	max_turns = 0
	game_started = False

	with open(filename, 'r') as file:
		lines = file.readlines()

		# Iterate through lines, handling multiple games, blank lines, and "Score" ending
		i = 0
		while i < len(lines):
			if lines[i].strip() == "-_-" and lines[i + 1].strip() == "START GAME" and lines[i + 2].strip() == "-_-":
				if game_started:
					# New game found, reset data for the previous game
					player_data = {}
					current_player = None
					turn_count = 0
				game_started = True  # Set game_started to True
				max_turns = extract_max_turns(filename)

			# Skip blank lines until finding a player name
			if game_started:
				while i < len(lines) and not lines[i].strip():
					i += 1
				if i < len(lines) and lines[i].strip().startswith("PnDb"):
					current_player = extract_player_name(lines[i])

			# Check for "---", "#!/bin/bash", or end of data
			if game_started:
				if lines[i].strip() == "---":
					turn_count += 1
					current_player = None
				elif "#!/bin/bash" in lines[i]:  # Check for any mention of "#!/bin/bash"
					game_started = False  # End game processing
				else:
					extract_data(lines[i], player_data, current_player, turn_count, max_turns)

			i += 1

	return player_data


def extract_max_turns(filename):
    max_pattern = r"_(\d+)\.txt$"  # Regex to extract number from filename

    match = re.search(max_pattern, filename)
    if match:
        number = int(match.group(1))
        max_turns_dict = {
            995: (9, 9, 5),
            18207: (18, 20, 7),
            385812: (38, 58, 12),
            704711: (74, 47, 11),
            10010030: (100, 100, 30)
        }
        return max_turns_dict[number][0] * max_turns_dict[number][1]

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

def extract_data(line, player_data, player, turn_count, max_turns):
	for label in ["loops_n", "n_loops", "depth_rel_max", "dag_n", "proved_n", "created_n"]:
		match = re.search(rf"\b{label}\s*[=:]\s*(\d+)\b", line)
		if match:
			value = int(match.group(1))
			percentage = turn_count / max_turns
			add_value(player_data, player, label, percentage, value)

def add_value(player_data, player, label, percentage, value):
	label_name = label if label != "n_loops" else "loops_n"
	if player not in player_data:
		player_data[player] = {}
	if label_name not in player_data[player]:
		player_data[player][label_name] = []
	player_data[player][label_name].append((percentage, value))

def create_charts(data):
	for label_name in ["loops_n", "depth_rel_max", "dag_n", "proved_n", "created_n"]:
		chart_data = {}
		for player, player_data in data.items():
			for label_data, avgs in player_data.items():
				if label_name == label_data:
					chart_data[player] = avgs

		print(f"{label_name}\n{chart_data}")

		plt.figure(figsize=(10, 6))
		for player, avgs in chart_data.items():
			if label_name == "depth_rel_max":
				plt.plot(avgs.keys(), avgs.values(), label=player)
			elif player != "pndb": #pndb has corrupted values
				plt.semilogy(avgs.keys(), avgs.values(), label=player)
		plt.xlabel("percentage of game completed")
		plt.ylabel(label_name.replace("_", " ").title())
		plt.title(f"Average {label_name} Values Across Games")
		plt.legend()
	plt.show()

def main():
	data = {}
	for root, _, filenames in os.walk("."):
		for filename in filenames:
			if filename.endswith(".txt"):
				#print(os.path.join(root, filename))
				file_data = process_file(os.path.join(root, filename))
				#print(file_data)
				merge_data(data, file_data)

	avg_data = get_avg_data(data)
	print(avg_data)

	print("CREATE_CHARTS")
	create_charts(avg_data)

# put data in data
def merge_data(data, new_data):
	# format: {player: {label: [(percentage:int, [values]), ...], ...}, ...}
	for player, player_data in new_data.items():
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
	
def get_avg_data(data):
	# format: {player: {label: [percentage:int, avg], ...}, ...}
	avg_data = {}
	for player, player_data in data.items():
		if player not in avg_data:
			avg_data[player] = {}
		for label, datas in player_data.items():
			if label not in avg_data[player]:
				avg_data[player][label] = {}
				for percentage in range(0, 101): #, 2):
					avg_data[player][label][percentage] = [0]
			for percentage, values in datas.items():
				if len(values) > 0:
					avg_data[player][label][percentage] = sum(values) / len(values)

	return avg_data


if __name__ == "__main__":
	main()