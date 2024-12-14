import java.io.File
import java.io.FileNotFoundException
import kotlin.time.*
import java.io.IOException
import java.io.InputStream

/*
    Stores the board
 */

class Board {
    val grid = MutableList(9) { row -> MutableList(9) { col -> Tile() } }
    var end = 0

    fun printGrid() {
        for (i in 0..8) {
            for (j in 0..8) {
                print("${grid[i][j].tileNum} ")
            }
            println()
        }
    }

    /*
    fun printGridCandidates() {
        for (i in 0..8) {
            for (j in 0..8) {
                // line spacer
                if (i > 1 && i % 3 == 1) println("-------------------------------")
                // tile placement
                for (k in 0..2) {
                    if (grid[i][j].tileNum != 0) {
                        val checkNum : MutableList<Int> = intArrayOf(3 * k, 3 * k + 1, 3 * k + 2).toMutableList()
                        for (i in checkNum) {
                            if
                        }
                    } else {
                        print("  ")
                    }
                    if (j < 2 && j % 3 == 1) println("| ")
                }

            }
        }
    }

     */

    fun printGridCandidates() {
        for (i in 0..8) {
            for (j in 0..8) {
                if (grid[i][j].tileNum != 0) {
                    print("[${grid[i][j].tileNum}] ")
                } else {
                    print("{")
                    for (k in grid[i][j].candidates)
                        print("$k, ")
                    print("} ")
                }
            }
            println()
        }
    }

    fun setTiles() {
        var i : Int
        var file : File? = null

        while (file == null) {
            try {
                val filename : String? = readLine()

                val resource = object {}.javaClass.getResource("/$filename")
                    ?: throw FileNotFoundException("File not found in resources.")

                file = File(resource.toURI())

            } catch (e: FileNotFoundException) {
                println("Error: ${e.message}")
                file == null
            }
        }

        i = 0
        file.forEachLine {
            val tiles = it.split(" ").toMutableList()
            for (j in 0..8) {
                grid[i][j].tileNum = tiles[j].toInt()
            }
            i++
        }

    }

    fun isGridCompleted() {
        var k = 0
        for (i in 0..8) {
            for (j in 0..8) {
                if (grid[i][j].tileNum != 0) {
                    k++
                } else if (grid[i][j].tileNum == 0 && grid[i][j].candidates.isEmpty()) {
                    end = -1
                }
            }
        }
        if (k == 81)
            end = 1
    }

    fun solveBoardL1() {
        while (end == 0) {
            val solve = Solver(this)

            val timeSource = TimeSource.Monotonic
            val mark1 = timeSource.markNow()

            solve.candidateFill()
            // printGridCandidates()
            // println("--")
            println("loop starts")

            while(end == 0) {
                solve.checkSingleCandidates()
                // printGridCandidates()
                // println("--")
                isGridCompleted()
            }

            val mark2 = timeSource.markNow()
            val elapsed = mark2 - mark1
            println("$elapsed")
        }
    }

    // Solver: Isolated Candidate Removal
    fun solveBoardL2() {
        val solve = Solver(this)

        // time tracking
        val timeSource = TimeSource.Monotonic
        val mark1 = timeSource.markNow()

        // repeated iteration tracking
        var noChangesInBoard = 0
        var changes = 0

        solve.candidateFill()
        printGridCandidates() // test
        println("--") // test
        println("loop starts")
        println("--")

        while(end == 0 && noChangesInBoard != 1) {
            changes = 0
            changes += solve.checkIsolatedCandidates()
            printGridCandidates() // test
            println("--") // test

            if (changes == 0) noChangesInBoard++
            else noChangesInBoard--

            isGridCompleted()
        }

        val mark2 = timeSource.markNow()
        val elapsed = mark2 - mark1
        println("$elapsed")
        when {
            end == 1 -> println("complete")
            end == -1 -> println("error")
            else -> println("incomplete")
        }
    }

    // Solver: Candidate Removal from Grouped Candidate
    /*
        e.g. tiles with candidates (2, 5) (2, 5), (2, 5, 6, 7), (2, 5, 6, 7).
        The latter 2 tiles will become (6, 7)
     */
    fun solveBoardL3() {
        val solve = Solver(this)

        // time tracking
        val timeSource = TimeSource.Monotonic
        val mark1 = timeSource.markNow()

        // repeated iteration tracking
        var noChangesInBoard = 0
        var changes = 0

        solve.candidateFill()
        printGridCandidates() // test
        println("--") // test
        println("loop starts")
        println("--")

        while(end == 0 && noChangesInBoard != 1) {
            changes = 0
            changes += solve.checkIsolatedCandidates()
            printGridCandidates() // test
            println("--") // test

            if (changes == 0) {
                solve.checkGroupedCandidates()
                changes += solve.checkIsolatedCandidates()
                if (changes == 0) noChangesInBoard++
            }

            isGridCompleted()
        }

        val mark2 = timeSource.markNow()
        val elapsed = mark2 - mark1
        println("$elapsed")
        when {
            end == 1 -> println("complete")
            end == -1 -> println("error")
            else -> println("incomplete")
        }
    }

    /*
        TODO: in-box candidate row/col elimination and vice-versa
            e.g.
            col     1      2       3
            ----------------------------
                (1, 2, 3)  X   (1, 2, 4)
                     X     X   (1, 2, 3, 4)
                (1, 2, 3)  X      X
            -
            all other candidates that are 4 will be removed in column 3
     */
    fun solveBoardL4() {
        val solve = Solver(this)

        // time tracking
        val timeSource = TimeSource.Monotonic; val mark1 = timeSource.markNow()

        // repeated iteration tracking
        var noChangesInBoard = 0; var changes = 0

        solve.candidateFill()
        printGridCandidates() // test
        println("--") // test
        println("loop starts")
        println("--")

        while(end == 0 && noChangesInBoard != 1) {
            changes = 0
            changes += solve.checkIsolatedCandidates()
            printGridCandidates() // test

            if (changes == 0) {
                solve.checkGroupedCandidates()
                changes += solve.checkIsolatedCandidates()
                println("Changes1: $changes")
                if (changes == 0) {
                    changes += solve.L4Elimination()
                    println("Changes2: $changes") // test
                    changes += solve.checkGroupedCandidates()
                    println("Changes3: $changes")
                    changes += solve.checkIsolatedCandidates()
                    println("Changes4: $changes")
                }
                if (changes == 0) noChangesInBoard++
            }
            println("--") // test

            isGridCompleted()
        }

        val mark2 = timeSource.markNow()
        val elapsed = mark2 - mark1
        println("$elapsed")
        when {
            end == 1 -> println("complete")
            end == -1 -> println("error")
            else -> println("incomplete")
        }
    }
}