import re

lines = [
	"dag_n = -1	proved_n = 358504	created_n = 6300729",
	"dag_n = -3	proved_n = 269274	created_n = 8176898",
	"dag_n = -1	proved_n = 358504	created_n = 6300730",
	"dag_n = -3	proved_n = 269274	created_n = 8176899",
	"dag_n = -1	proved_n : 358504	created_n = 6300731"
]

labels = ['loops_n', 'n_loops', 'depth_rel_max', 'dag_n', 'proved_n', 'created_n']

for label in labels:
	for l in lines:
		if label in l:
				print(l)
				for label in labels:
						match = re.search(rf"\b{label}\s*[=:]\s*(\d+)\b", l)
						if match:
							print(match.groups())

