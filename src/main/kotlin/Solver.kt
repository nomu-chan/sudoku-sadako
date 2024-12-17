import jdk.internal.org.jline.reader.Candidate
import kotlin.collections.set

typealias TileList = MutableList<Tile>
typealias IntList2D = MutableList<MutableList<Int>>
typealias IntList = MutableList<Int>
typealias IntSet = MutableSet<Int>
typealias AnyList2D = MutableList<Any>

class Solver(var board : Board) {

    // initialize boxCoords for easier 3x3 box iteration.
    val boxCoords : IntList2D = mutableListOf()
    init {
        for (i in 0..2)
            for (j in 0..2)
                boxCoords.add(mutableListOf(i, j))
    }

    // Fill the whole grid with candidates
    fun candidateFill() {
        for (i in 0..8) {
            for (j in 0..8) {
                val candidates : IntList = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toMutableList()
                if (board.grid[i][j].tileNum == 0) board.grid[i][j].candidates = candidates
                eliminateCandidatesInTile(i, j)
            }
        }
    }

    // L1: Checks the whole board for tiles with single candidates and fills them up
    fun checkSingleCandidates() : Int {
        var changes = 0;
        for (i in 0..8) {
            for (j in 0..8) {
                if (board.grid[i][j].tileNum == 0 && board.grid[i][j].candidates.count() == 1) {
                    val num = board.grid[i][j].candidates.first()
                    board.grid[i][j].candidates.remove(num)
                    fillTile(num, i, j)
                    changes++
                }
            }
        }
        return changes
    }

    /*
         L2 : Checks the whole board for isolated candidates - these are candidates that only
         have one of itself in either a row, column or 3x3 box. Used in L2.

         @return = number of changes after checking and filling isolated candidates
     */
    fun checkIsolatedCandidates() : Int {
        var changes = 0;
        // row check
        for (i in 0..8) {
            val tileList : TileList = mutableListOf()
            for (j in 0..8) tileList.add(board.grid[i][j])

            // generate candidate count mapping
            val candidateMap : MutableMap<Int, Int> = generateCandidateCountMapping(tileList)

            // fill tiles that have isolated candidate
            changes += fillIsolatedCandidates(candidateMap, tileList)
        }

        // column check
        for (j in 0..8) {
            val tileList : TileList = mutableListOf()
            for (i in 0..8) tileList.add(board.grid[i][j])

            val candidateMap : MutableMap<Int, Int> = generateCandidateCountMapping(tileList)

            changes += fillIsolatedCandidates(candidateMap, tileList)
        }

        // box check
        for (boxCoord in boxCoords) {
            val box_i = boxCoord[0]; val box_j = boxCoord[1]
            val tileList : TileList = mutableListOf()
            for (tile in boxCoords) {
                val tile_i = tile[0]; val tile_j = tile[1]
                val i = 3 * box_i + tile_i;  val j = 3 * box_j + tile_j
                tileList.add(board.grid[i][j])
            }
            val candidateMap : MutableMap<Int, Int> = generateCandidateCountMapping(tileList)

            changes += fillIsolatedCandidates(candidateMap, tileList)
        }

        return changes; // return num of candidate removals
    }

    /*
         L3 : Checks each row, column and 3x3 box for any possible candidate subset/groups that
         will be eliminated from other tiles not part of the candidate subset/group

         e.g. (1, 6, 8, 9), (1, 6), (1, 6) -> first tile will be (8, 9)
     */
    fun checkGroupedCandidates() : Int {
        var changes = 0

        // row check
        for (i in 0..8) {
            val tileList : TileList = mutableListOf()
            for (j in 0..8) tileList.add(board.grid[i][j])
            val candidateSet = generateCandidateSet(tileList)

            if (candidateSet.size < 4) continue

            // generate all 2 to k - 2 num combis, get valid groups and eliminate candidates accordingly
            val combinations = generateAllCombinations(candidateSet)
            changes += eliminateFromCandidateGroups(combinations, tileList)
        }

        // col check
        for (j in 0..8) {
            val tileList : TileList = mutableListOf()
            for (i in 0..8) tileList.add(board.grid[i][j])
            val candidateSet = generateCandidateSet(tileList)

            if (candidateSet.size < 4) continue

            // generate all 2 to k - 2 num combis, get valid groups and eliminate candidates accordingly
            val combinations = generateAllCombinations(candidateSet)
            changes += eliminateFromCandidateGroups(combinations, tileList)
        }

        // box check
        for (boxCoord in boxCoords) {
            // obtain set of all candidates and set of all tiles with candidates
            val box_i = boxCoord[0]; val box_j = boxCoord[1]
            val tileList : TileList = mutableListOf()
            for (tile in boxCoords) {
                val tile_i = tile[0]; val tile_j = tile[1]
                val i = 3 * box_i + tile_i; val j = 3 * box_j + tile_j
                if (board.grid[i][j].tileNum == 0) {
                    tileList.add(board.grid[i][j])
                }
            }
            val candidateSet = generateCandidateSet(tileList)

            if (candidateSet.size < 4) continue

            // generate all 2 to k - 2 num combis and attempt to find groups of candidates
            val combinations = generateAllCombinations(candidateSet)
            changes += eliminateFromCandidateGroups(combinations, tileList)
        }

        return changes
    }

    /*
        L4 : Pointer Pairs/Trios: Box candidate elimination from row/col alignment and vice versa
            e.g.
            col     1      2       3
            ----------------------------
                (1, 2, 3)  X   (1, 2, 4)
                     X     X   (1, 2, 3, 4)
                (1, 2, 3)  X      X
            -
            all other candidates that are 4 will be removed in column 3
     */
    fun l4Elimination() : Int {
        var changes = 0

        // row check
        for (i in 0..8) {
            val tileList : TileList = mutableListOf()
            for (j in 0..8) if (board.grid[i][j].tileNum == 0) tileList.add(board.grid[i][j])
            val candidateSet : MutableSet<Int> = generateCandidateSet(tileList)

            val boxElim = inSameBox(candidateSet, tileList)

            if (boxElim.isEmpty()) continue
            for (procedure in boxElim) {
                val candidate = procedure[0]
                val box_i = procedure[1]; val box_j = procedure[2]
                for (boxCoord in boxCoords) {
                    val ri = boxCoord[0]; val rj = boxCoord[1]
                    if (3 * box_i + ri == i) continue
                    if (board.grid[3 * box_i + ri][3 * box_j + rj].candidates.remove(candidate)) changes++
                }
            }
        }

        // col check
        for (j in 0..8) {
            val tileList : TileList = mutableListOf()
            for (i in 0..8) if (board.grid[i][j].tileNum == 0) tileList.add(board.grid[i][j])
            val candidateSet : MutableSet<Int> = generateCandidateSet(tileList)

            val boxElim = inSameBox(candidateSet, tileList)

            if (boxElim.isEmpty()) continue
            for (procedure in boxElim) {
                val candidate = procedure[0]
                val box_i = procedure[1];  val box_j = procedure[2]
                for (boxCoord in boxCoords) {
                    val ri = boxCoord[0]; val rj = boxCoord[1]
                    if (3 * box_j + rj == j) continue
                    if (board.grid[3 * box_i + ri][3 * box_j + rj].candidates.remove(candidate)) changes++
                }
            }
        }

        // box check
        for (boxCoord in boxCoords) {
            // obtain set of all candidates and set of all tiles with candidates
            val box_i = boxCoord[0]; val box_j = boxCoord[1]

            val tileList : TileList = mutableListOf()
            for (tile in boxCoords) {
                val tile_i = tile[0]; val tile_j = tile[1]
                val i = 3 * box_i + tile_i;  val j = 3 * box_j + tile_j
                if (board.grid[i][j].tileNum == 0) tileList.add(board.grid[i][j])
            }

            val candidateSet: MutableSet<Int> = generateCandidateSet(tileList)

            val rowElim = inSameRow(candidateSet, tileList)
            val colElim = inSameCol(candidateSet, tileList)

            if (rowElim.isNotEmpty()) {
                for (r in rowElim) {
                    val candidate = r[0]
                    val rowNum = r[1]
                    for (j in 0..8) {
                        if (j / 3 == box_j) continue
                        if (board.grid[rowNum][j].candidates.remove(candidate)) changes++
                    }
                }
            }

            if (colElim.isNotEmpty()) {
                for (r in colElim) {
                    val candidate = r[0]
                    val colNum = r[1]
                    for (i in 0..8) {
                        if (i / 3 == box_i) continue
                        if (board.grid[i][colNum].candidates.remove(candidate)) changes++
                    }
                }
            }
        }

        return changes
    }

    /*
        TODO: L5: Swordfish and Y-Fish
     */
    fun l5Elimination(): Int {
        var changes = 0

        // Swordfish, Row Check first. Return { { col removed : row saved } }
        for (c in 1..9) {
            val colElims = rowCheckColumnElimSwordfish(c)
            if (colElims.isEmpty()) continue
            for (columns in colElims.keys) {
                val rows = colElims[columns]
                for (j in columns) {
                    for (i in 0..8) {
                        if (rows != null) {
                            if (i in rows) continue
                            if (board.grid[i][j].candidates.remove(c)) changes++
                        } else continue
                    }
                }
            }
        }

        // Swordfish, Col Check first
        for (c in 1..9) {
            val rowElims = colCheckRowElimSwordfish(c)
            if (rowElims.isEmpty()) continue
            for (rows in rowElims.keys) {
                val columns = rowElims[rows]
                for (i in rows) {
                    for (j in 0..8) {
                        if (columns != null) {
                            if (j in columns) continue
                            if (board.grid[i][j].candidates.remove(c)) changes++
                        } else continue
                    }
                }
            }
        }



        return changes
    }

    // OPERATIONS
    // Fill a tile and remove candidates in row, col, and box
    private fun fillTile(num : Int, i : Int, j : Int) {
        board.grid[i][j].tileNum = num

        // remove candidates in row/col
        for (k in 0..8) {
            board.grid[i][k].candidates.remove(num)
            board.grid[k][j].candidates.remove(num)
        }

        // remove candidates in 3x3 box
        val ibox = i / 3
        val jbox = j / 3
        for (k1 in 0..2) {
            for (k2 in 0..2) {
                board.grid[3 * ibox + k1][3 * jbox + k2].candidates.remove(num)
            }
        }
    }

    // Eliminate tile candidates based on nums in row, col and box
    private fun eliminateCandidatesInTile(i : Int, j : Int) : Int {

        var numOfCandidatesRemoved = 0

        // Check row and column
        for (k in 0..8) {
            val rowTile = board.grid[i][k].tileNum
            val colTile = board.grid[k][j].tileNum
            if (board.grid[i][j].candidates.remove(rowTile)) numOfCandidatesRemoved++
            if (board.grid[i][j].candidates.remove(colTile)) numOfCandidatesRemoved++
        }

        // Check box
        val ibox = i / 3
        val jbox = j / 3
        for (k1 in 0..2) {
            for (k2 in 0..2) {
                val boxTile = board.grid[3 * ibox + k1][3 * jbox + k2].tileNum
                if (board.grid[i][j].candidates.remove(boxTile))
                    numOfCandidatesRemoved++
            }
        }

        // if no candidates remain while TileNum = 0, end = -1
        if (board.grid[i][j].tileNum == 0 && board.grid[i][j].candidates.isEmpty()) {
            board.end = -1
        }

        return numOfCandidatesRemoved
    }

    // SET/LIST/MAP GENERATION
    private fun generateCandidateCountMapping(tileList : TileList) : MutableMap<Int, Int> {
        val candidateCount = mutableMapOf<Int, Int>()
        for (tile in tileList) {
            if (tile.tileNum == 0) {
                for (c in tile.candidates) {
                    candidateCount[c] = candidateCount.getOrDefault(c, 0) + 1
                }
            }
        }

        return candidateCount
    }

    private fun generateCandidateSet(tileList : TileList) : MutableSet<Int> {
        val candidateSet : IntSet = mutableSetOf()
        for (tile in tileList) {
            candidateSet.add(tile.tileNum)
        }
        return candidateSet
    }

    private fun generateAllCombinations(candidateSet : IntSet) : List<List<Int>> {
        val candidateList = candidateSet.toList()
        val k = candidateList.size

        val result = mutableListOf<List<Int>>()

        for (size in 2..(k - 2)) {
            result.addAll(generateCombinations(candidateList, size))
        }

        return result
    }

    private fun generateCombinations(candidateList : List<Int>, size : Int) : List<List<Int>> {
        if (size == 0) return listOf(emptyList())
        if (candidateList.isEmpty()) return emptyList()

        val first = candidateList.first()
        val rest = candidateList.drop(1)

        val withFirst = generateCombinations(rest, size - 1).map { listOf(first) + it }
        val withoutFirst = generateCombinations(rest, size)

        return withFirst + withoutFirst
    }

    // L2 Utility Functions (Isolated Candidates)
    private fun fillIsolatedCandidates(candidateMap : MutableMap<Int, Int>, tileList : TileList) : Int {
        var changes = 0
        for (k in candidateMap.keys) {
            if (candidateMap[k] == 1) {
                for (tile in tileList) {
                    if (tile.candidates.contains(k)) {
                        tile.candidates.clear()
                        fillTile(k, tile.i, tile.j)
                        changes++
                    }
                }
            }
        }
        return changes
    }

    // L3 Utility Functions (Combinations)
    private fun eliminateFromCandidateGroups(combinations : List<List<Int>> ,tileList : TileList) : Int {
        var changes = 0
        for (combi in combinations) {
            val tilesInGroup: TileList = mutableListOf()
            val xcount = combi.size
            for (tile in tileList) {
                var k = 1
                for (candidate in tile.candidates) if (candidate in combi == false) k = 0
                if (k == 1) tilesInGroup.add(tile)
            }

            if (tilesInGroup.size == xcount) {
                for (tile in tileList) {
                    if (tile in tilesInGroup == false) {
                        for (m in combi) {
                            if (tile.candidates.remove(m)) changes++
                        }
                    }
                }
            }
        }
        return changes
    }

    // L4 Utility Functions (Pointer Pairs/Triples)
    private fun inSameRow(candidateSet : IntSet, tileList : TileList) : IntList2D {
        val candidateList = candidateSet.toMutableList()
        val sameRowNums : IntList2D = mutableListOf()

        for (c in candidateList) {
            val rowSet = mutableSetOf<Int>()
            var rowNum = -1
            for (tile in tileList) {
                if (c in tile.candidates) {
                    rowSet.add(tile.i)
                    rowNum = tile.i
                }
                if (rowSet.size >= 2) break
            }
            if (rowSet.size == 1) sameRowNums.add(arrayOf(c, rowNum).toMutableList())
        }
        return sameRowNums
    }

    private fun inSameCol(candidateSet : IntSet, tileList : TileList) : IntList2D {
        val candidateList = candidateSet.toMutableList()
        val sameRowNums : IntList2D = mutableListOf()

        for (c in candidateList) {
            val rowSet = mutableSetOf<Int>()
            var rowNum = -1
            for (tile in tileList) {
                if (c in tile.candidates) {
                    rowSet.add(tile.j)
                    rowNum = tile.j
                }
                if (rowSet.size >= 2) break
            }
            if (rowSet.size == 1) sameRowNums.add(arrayOf(c, rowNum).toMutableList())
        }
        return sameRowNums
    }

    private fun inSameBox(candidateSet : IntSet, tileList : TileList) : IntList2D {
        val candidateList = candidateSet.toMutableList()
        val sameBoxNums : IntList2D = mutableListOf()

        for (c in candidateList) {
            val boxSet : IntList2D = mutableListOf()
            var boxCoord : IntList = mutableListOf()
            for (tile in tileList) {
                if (c in board.grid[tile.i][tile.j].candidates) {
                    boxSet.coordAdd(arrayOf(tile.i / 3, tile.j / 3).toMutableList())
                    boxCoord = intArrayOf(tile.i / 3, tile.j / 3).toMutableList()
                }
                if (boxSet.size >= 2) break
            }
            if (boxSet.size == 1) sameBoxNums.add(arrayOf(c, boxCoord[0], boxCoord[1]).toMutableList())
        }
        return sameBoxNums
    }

    private fun IntList2D.coordAdd(coord : IntList) {
        if (this.isEmpty()) {
            this.add(coord)
            return
        }
        for (c in this) {
            val i = c[0]; val j = c[1]
            if (i == coord[0] && j == coord[1]) return
        }
        this.add(coord)
    }

    // L5 Utility Functions (Swordfish / Y-Wing)
    // returns row and cols to be saved/elim-ed for each candidate, columns first
    private fun rowCheckColumnElimSwordfish(c : Int) : MutableMap<IntList, IntList> {

        // return val: columns to be removed, rows to be saved
        val colElimsRowSaved : MutableMap<IntList, IntList> = mutableMapOf()

        // 1.) Create Mapping of Each Row : Columns containing candidate x
        val rowColsWithCandidateMap : MutableMap<Int, IntList> = mutableMapOf()

        // Creation of Mapping
        for (i in 0..8) {
            // for each row, take note of each column containing candidate c
            val colsWithCandidate : IntList = mutableListOf()
            for (j in 0..8)  if (c in board.grid[i][j].candidates) colsWithCandidate.add(j)
            if (colsWithCandidate.size >= 2) rowColsWithCandidateMap[i] = colsWithCandidate
        }

        // 2.) Generate all possible column combinations from the union of columns in the mapping
        val columnSet : IntSet = mutableSetOf()
        if (rowColsWithCandidateMap.size <= 1) return mutableMapOf()

        // Creation of all possible column combinations
        for (i in rowColsWithCandidateMap.keys) {
            for (j in rowColsWithCandidateMap[i]!!) columnSet.add(j)
        }
        if (columnSet.size < 4) return mutableMapOf()
        val allColumnCombinations = generateAllCombinations(columnSet)


        // 3.) For each column combination with k elements (2 <= x <= k - 2), check how many
        // key rows in the mapping are a subset of the column combination.
        for (columnCombi in allColumnCombinations) {
            val rowsWithCombi : IntList = mutableListOf()
            val combiSize = columnCombi.size
            for (i in rowColsWithCandidateMap.keys) {
                var k = 1
                val colList = rowColsWithCandidateMap[i]
                if (colList != null) {
                    for (candidate in colList) if (candidate !in columnCombi) k = 0
                } else k = 0
                if (k == 1) rowsWithCombi.add(i)
            }
            if (combiSize == rowsWithCombi.size) {
                colElimsRowSaved[columnCombi.toMutableList()] = rowsWithCombi
            }
        }

        return colElimsRowSaved
    }

    // TODO: edit naming conventions of both swordfish

    private fun colCheckRowElimSwordfish(c : Int) : MutableMap<IntList, IntList> {

        // return val: rows removed, columns saved
        val rowElimsColsSaved: MutableMap<IntList, IntList> = mutableMapOf()

        // 1.) Create Mapping of Each Column : Rows containing candidate x
        val colRowsWithCandidateMap: MutableMap<Int, IntList> = mutableMapOf()

        // Creation of Mapping
        for (j in 0..8) {
            // for each column, take note of each row containing candidate c
            val rowsWithCandidate: IntList = mutableListOf()
            for (i in 0..8) if (c in board.grid[i][j].candidates) rowsWithCandidate.add(i)
            if (rowsWithCandidate.size >= 2) colRowsWithCandidateMap[j] = rowsWithCandidate
        }

        // 2.) Generate all possible row combinations from the union of columns in the mapping
        val rowSet: IntSet = mutableSetOf()
        if (colRowsWithCandidateMap.size <= 1) return mutableMapOf()

        // Creation of all possible row combinations
        for (j in colRowsWithCandidateMap.keys) {
            for (i in colRowsWithCandidateMap[j]!!) rowSet.add(i)
        }
        if (rowSet.size < 4) return mutableMapOf()
        val allRowCombinations = generateAllCombinations(rowSet)


        // 3.) For each row combination with k elements (2 <= x <= k - 2), check how many
        // key columns in the mapping are a subset of the column combination.
        for (rowCombi in allRowCombinations) {
            val colsWithCombi: IntList = mutableListOf()
            val combiSize = rowCombi.size
            for (j in colRowsWithCandidateMap.keys) {
                var k = 1
                val colList = colRowsWithCandidateMap[j]
                if (colList != null) {
                    for (candidate in colList) if (candidate !in rowCombi) k = 0
                } else k = 0
                if (k == 1) colsWithCombi.add(j)
            }
            if (combiSize == colsWithCombi.size) {
                rowElimsColsSaved[rowCombi.toMutableList()] = colsWithCombi
            }
        }

        return rowElimsColsSaved
    }
}