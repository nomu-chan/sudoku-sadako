typealias TileGrid = MutableList<MutableList<Tile>>
typealias IntList = MutableList<Int>

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
            for (j in 0..8) {
                if (board.grid[i][j].tileNum == 0) {
                    for (k in board.grid[i][j].candidates) {
                        candidateMap[k] = candidateMap.getOrDefault(k, 0) + 1
                    }
                }
            }
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

}