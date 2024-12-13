import jdk.internal.org.jline.reader.Candidate

typealias TileGrid = MutableList<MutableList<Tile>>
typealias TileList = MutableList<Tile>
typealias IntList = MutableList<Int>
typealias IntSet = MutableSet<Int>

class Solver(var board : Board) {
    val boxCoords : MutableList<MutableList<Int>> = mutableListOf()

    init {
        for (i in 0..2)
            for (j in 0..2)
                boxCoords.add(mutableListOf(i, j))
    }

    // eliminate tile candidates based on nums in row, col and box
    fun eliminateTileCandidates(i : Int, j : Int) : Int {

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

    // fill a tile and remove candidates in row, col, and box
    fun fillTile(num : Int, i : Int, j : Int) {
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

    // Fill the whole grid with candidates
    fun candidateFill() {
        for (i in 0..8) {
            for (j in 0..8) {
                val candidates : IntList = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toMutableList()
                if (board.grid[i][j].tileNum == 0) {
                    board.grid[i][j].candidates = candidates
                }
                eliminateTileCandidates(i, j)
            }
        }
    }

    // Checks the whole board for tiles with single candidates and fills them up
    fun checkSingleCandidates() {
        for (i in 0..8) {
            for (j in 0..8) {
                if (board.grid[i][j].tileNum == 0 && board.grid[i][j].candidates.count() == 1) {
                    var num = board.grid[i][j].candidates.first()
                    board.grid[i][j].candidates.remove(num)
                    fillTile(num, i, j)
                }
            }
        }
    }

    /*
         Checks the whole board for isolated candidates - these are candidates that only
         have one of itself in either a row, column or 3x3 box. Used in L2.

         @return = number of changes after checking and filling isolated candidates
     */
    fun checkIsolatedCandidates() : Int {
        var changes = 0;
        // row check
        for (i in 0..8) {
            val candidateMap : MutableMap<Int, Int> = mutableMapOf()
            // fill up candidate count in the candidate map
            for (j in 0..8) {
                if (board.grid[i][j].tileNum == 0) {
                    for (k in board.grid[i][j].candidates) {
                        candidateMap[k] = candidateMap.getOrDefault(k, 0) + 1
                    }
                }
            }
            // fill tiles that have isolated candidate
            for (k in candidateMap.keys) {
                if (candidateMap[k] == 1) {
                    for (j in 0..8) {
                        if (board.grid[i][j].candidates.contains(k)) {
                            board.grid[i][j].candidates.clear()
                            fillTile(k, i, j)
                            changes++
                        }
                    }
                }
            }
        }

        // column check
        for (j in 0..8) {
            val candidateMap : MutableMap<Int, Int> = mutableMapOf()
            for (i in 0..8) {
                if (board.grid[i][j].tileNum == 0) {
                    for (k in board.grid[i][j].candidates) {
                        candidateMap[k] = candidateMap.getOrDefault(k, 0) + 1
                    }
                }
            }
            for (k in candidateMap.keys) {
                if (candidateMap[k] == 1) {
                    for (i in 0..8) {
                        if (board.grid[i][j].candidates.contains(k)) {
                            board.grid[i][j].candidates.clear()
                            fillTile(k, i, j)
                            changes++
                        }
                    }
                }
            }
        }

        // box check
        for (boxCoord in boxCoords) {
            val box_i = boxCoord[0]
            val box_j = boxCoord[1]
            val candidateMap : MutableMap<Int, Int> = mutableMapOf()
            for (tile in boxCoords) {
                val tile_i = tile[0]
                val tile_j = tile[1]
                val i = 3 * box_i + tile_i
                val j = 3 * box_j + tile_j
                if (board.grid[i][j].tileNum == 0) {
                    for (k in board.grid[i][j].candidates) {
                        candidateMap[k] = candidateMap.getOrDefault(k, 0) + 1
                    }
                }
            }
            for (k in candidateMap.keys) {
                if (candidateMap[k] == 1) {
                    for (tile in boxCoords) {
                        val tile_i = tile[0]
                        val tile_j = tile[1]
                        val i = 3 * box_i + tile_i
                        val j = 3 * box_j + tile_j
                        if (board.grid[i][j].candidates.contains(k)) {
                            board.grid[i][j].candidates.clear()
                            fillTile(k, i, j)
                            changes++
                        }
                    }
                }
            }
        }

        return changes; // return num of candidate removals
    }

    /*
        TODO: Refactor and Make this for Col and Row
     */
    fun checkGroupedCandidates() : Int {
        var changes = 0

        // row check
        for (i in 0..8) {
            val candidateSet : MutableSet<Int> = mutableSetOf()
            val tileCoordSet : MutableList<MutableList<Int>> = mutableListOf()
            for (j in 0..8) {
                if (board.grid[i][j].tileNum == 0) {
                    tileCoordSet.add(mutableListOf(i, j))
                    for (k in board.grid[i][j].candidates) {
                        candidateSet.add(k)
                    }
                }
            }
            // generate all 2 to k - 2 num combis and attempt to find groups of candidates
            val combinations = generateAllCombinations(candidateSet)
            for (combi in combinations) {
                var tileInGroup = 0
                val tileInGroupCoordList = mutableListOf<MutableList<Int>>()
                val xcount = combi.size
                for (coords in tileCoordSet) {
                    val i = coords[0]
                    val j = coords[1]
                    var k = 1
                    for (candidate in board.grid[i][j].candidates) {
                        if (candidate in combi) {}
                        else k = 0
                    }
                    if (k == 1) {
                        tileInGroup++
                        tileInGroupCoordList.add(mutableListOf(i, j))
                    }
                }
                // once grouped, get em numby
                // once grouped successfully, remove grouped candidates from tiles not in the group
                if (tileInGroup == xcount) {
                    for (tileCoord in tileCoordSet) {
                        val i = tileCoord[0]
                        val j = tileCoord[1]
                        if (tileCoord in tileInGroupCoordList) {}
                        else {
                            for (m in combi) {
                                board.grid[i][j].candidates.remove(m)
                                changes++
                            }
                        }
                    }
                }
            }
        }

        // col check
        for (j in 0..8) {
            val candidateSet : MutableSet<Int> = mutableSetOf()
            val tileCoordSet : MutableList<MutableList<Int>> = mutableListOf()
            for (i in 0..8) {
                if (board.grid[i][j].tileNum == 0) {
                    tileCoordSet.add(mutableListOf(i, j))
                    for (k in board.grid[i][j].candidates) {
                        candidateSet.add(k)
                    }
                }
            }
            // generate all 2 to k - 2 num combis and attempt to find groups of candidates
            val combinations = generateAllCombinations(candidateSet)
            for (combi in combinations) {
                var tileInGroup = 0
                val tileInGroupCoordList = mutableListOf<MutableList<Int>>()
                val xcount = combi.size
                for (coords in tileCoordSet) {
                    val i = coords[0]
                    val j = coords[1]
                    var k = 1
                    for (candidate in board.grid[i][j].candidates) {
                        if (candidate in combi) {}
                        else k = 0
                    }
                    if (k == 1) {
                        tileInGroup++
                        tileInGroupCoordList.add(mutableListOf(i, j))
                    }
                }
                // once grouped, get em numby
                // once grouped successfully, remove grouped candidates from tiles not in the group
                if (tileInGroup == xcount) {
                    for (tileCoord in tileCoordSet) {
                        val i = tileCoord[0]
                        val j = tileCoord[1]
                        if (tileCoord in tileInGroupCoordList) {}
                        else {
                            for (m in combi) {
                                board.grid[i][j].candidates.remove(m)
                                changes++
                            }
                        }
                    }
                }
            }
        }

        // box check
        for (boxCoord in boxCoords) {
            // obtain set of all candidates and set of all tiles with candidates
            val box_i = boxCoord[0]
            val box_j = boxCoord[1]
            val candidateSet : MutableSet<Int> = mutableSetOf()
            val tileCoordSet : MutableList<MutableList<Int>> = mutableListOf()
            for (tile in boxCoords) {
                val tile_i = tile[0]
                val tile_j = tile[1]
                val i = 3 * box_i + tile_i
                val j = 3 * box_j + tile_j
                if (board.grid[i][j].tileNum == 0) {
                    tileCoordSet.add(mutableListOf(i, j))
                    for (k in board.grid[i][j].candidates) {
                        candidateSet.add(k)
                    }
                }
            }

            if (candidateSet.size < 4) continue

            // generate all 2 to k - 2 num combis and attempt to find groups of candidates
            val combinations = generateAllCombinations(candidateSet)
            for (combi in combinations) {
                var tileInGroup = 0
                val tileInGroupCoordList = mutableListOf<MutableList<Int>>()
                val xcount = combi.size
                for (coords in tileCoordSet) {
                    val i = coords[0]
                    val j = coords[1]
                    var k = 1
                    for (candidate in board.grid[i][j].candidates) {
                        if (candidate in combi) {}
                        else k = 0
                    }
                    if (k == 1) {
                        tileInGroup++
                        tileInGroupCoordList.add(mutableListOf(i, j))
                    }
                }
                // once grouped, get em numby
                // once grouped successfully, remove grouped candidates from tiles not in the group
                if (tileInGroup == xcount) {
                    for (tileCoord in tileCoordSet) {
                        val i = tileCoord[0]
                        val j = tileCoord[1]
                        if (tileCoord in tileInGroupCoordList) {}
                        else {
                            for (m in combi) {
                                board.grid[i][j].candidates.remove(m)
                                changes++
                            }
                        }
                    }
                }
            }
        }
        return changes
    }

    // Utility Functions
    /*
        TODO: understand this haha, just found this on the internet
     */
    fun generateAllCombinations(candidateSet : IntSet) : List<List<Int>> {
        val candidateList = candidateSet.toList()
        val k = candidateList.size

        val result = mutableListOf<List<Int>>()

        for (size in 2..(k - 2)) {
            result.addAll(generateCombinations(candidateList, size))
        }

        return result
    }

    fun generateCombinations(candidateList : List<Int>, size : Int) : List<List<Int>> {
        if (size == 0) return listOf(emptyList())
        if (candidateList.isEmpty()) return emptyList()

        val first = candidateList.first()
        val rest = candidateList.drop(1)

        val withFirst = generateCombinations(rest, size - 1).map { listOf(first) + it }
        val withoutFirst = generateCombinations(rest, size)

        return withFirst + withoutFirst
    }

}