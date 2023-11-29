
## PnSearch

### generateAllChildren

/* Heuristic: implicit threat.
	* Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
	* if the current player was to make a "null move".
	* In fact, the opponent could apply such winning sequence, if the current player was to 
	* make a move outside it, thus not changing his plans.
	* 
	* Actually, no moves are removed, but these results are only used to sort them.
	* 
	* Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
	* i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
	* the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
	* not ignored, since they could block the win too, and also to simplify the calculations by approximation.
	* 
	* note: related_cols should already contain only available, not full, columns.
	*/

// DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), Auxiliary.opponent(player), Operators.MAX_TIER);

/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
	* (i.e., for columns, the sum of the scores in the whole column).
	*/