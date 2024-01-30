# TESTS

## Results
Some tests results.  

(pndb2 is final pndb printing before and after visit).  
(pndbtime is final pndb with 9900ms time).  

FORMAT:
*	M	N	X	: WONP1	WONP2	DRAW	|	ERROR1	ERROR2

### pndb	-	pndb2
*	9	9	5	:	2	1	0	|	0	0
*	18	20	7	:	2	0	1	|	0	0
### pndb2	-	alpha
*	9	9	5	:	2	0	0	|	0	1
*	18	20	7	:	3	0	0	|	0	1
	*	alpha lost quickly, is just broken
### pndb2	-	betha
*	9	9	5	:	3	0	0	|	0	0
### pndb2	-	dnull
*	9	9	5	:	6	1	0	|	0	0
*	18	20	7	:	4	2	1	|	0	0
	*	dnull
		*	n_loops: starts 40-50.000
### pndb2	-	xcono
*	9	9	5	:	4	3	0	|	0	0
*	18	20	7	:	6	1	0	|	0	0
### delta	-	dnull
*	9	9	5	:	3	4	0	|	0	0
	*	both
		*	tag tree: 50ms
		*	remove unmarked: 50-200ms
		*	gc: 50-150ms
		*	all: ascending to top after few moves, then descending
		*	mem: occupied about 800MB max, always released 300MB after gc
		*	proved_n: dnull starts with +100%, suddenly to 50-33%, at the end only +10%
	*	delta
		*	n_loops: 75.000 descending
		*	dag_n: 500.000 descending
		*	proved_n: usually 240.000, sometimes 500.000 at the end
		*	created_n: 8.2M
	*	dnull
		*	n_loops: 99.000 descending
		*	dag_n: 350.000 descending
		*	proved_n: usually 300.000, sometimes 550.000 at the end
		*	created_n: 6.2M
*	18	20	7	:	1	4	2	|	0	0
	*	both
		*	tag tree: <50ms, only once 100ms (more at start)
		*	remove unmarked: 500ms fast to 200ms slowly to 100-0ms
		*	gc: 50ms
		*	all: ascending to top after few moves, then descending
		*	mem: =
		*	loops_n: 75.000 vs. 50.000 soon descend to almost stable 55 vs 40, slowly to 0
		*	dag_n: +40% linearly to <10% (700.000 vs 550.000)
		*	proved_n: dnull x2 (72.000 vs 36.000)
	*	delta
		*	n_loops: 75.000 descending
		*	created_n: 67M
	*	dnull
		*	n_loops: 99.000 descending
		*	created_n: 50M


