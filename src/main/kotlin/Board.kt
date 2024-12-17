import java.io.File
import java.io.FileNotFoundException
import kotlin.time.*

/*
    Stores the board
 */

class Board {
    val grid = MutableList(9) { row -> MutableList(9) { col -> Tile() } }
    var end = 0

    // initialize board so that tiles include coordinates i, j
    init {
        for (i in 0..8) {
            for (j in 0..8) {
                grid[i][j].i = i
                grid[i][j].j = j
            }
        }

    }

    fun printGrid() {
        for (i in 0..8) {
            for (j in 0..8) {
                print("${grid[i][j].tileNum} ")
            }
            println()
        }
    }

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

    fun setUpTiles() {
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
        in-box candidate row/col elimination and vice-versa
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
            println("Changes0 (L2): $changes")

            if (changes == 0) {
                changes += solve.checkGroupedCandidates()
                changes += solve.checkIsolatedCandidates()
                println("Changes1 (L3+2): $changes")
                if (changes == 0) {
                    changes += solve.l4Elimination()
                    println("Changes2 (L4/4): $changes") // test
                    changes += solve.checkGroupedCandidates()
                    println("Changes3 (L3/4): $changes")
                    changes += solve.checkIsolatedCandidates()
                    println("Changes4 (L2/4): $changes")
                    changes += solve.checkSingleCandidates()
                    println("Changes5 (L1/4): $changes")
                }
                if (changes == 0) noChangesInBoard++
            }

            printGridCandidates() // test
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

    fun solveBoardL5() {
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
            println("Changes0 (L2): $changes")

            if (changes == 0) {
                changes += solve.checkGroupedCandidates()
                changes += solve.checkIsolatedCandidates()
                println("Changes1 (L3+2): $changes")
                if (changes == 0) {
                    changes += solve.l5Elimination()
                    println("Changes2 (L5/5): $changes")
                    changes += solve.l4Elimination()
                    println("Changes3 (L4/5): $changes") // test
                    changes += solve.checkGroupedCandidates()
                    println("Changes4 (L3/5): $changes")
                    changes += solve.checkIsolatedCandidates()
                    println("Changes5 (L2/5): $changes")
                    changes += solve.checkSingleCandidates()
                    println("Changes6 (L1/5): $changes")
                }
                if (changes == 0) noChangesInBoard++
            }

            printGridCandidates() // test
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

    fun pureSwordfish() {
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
            changes += solve.l5Elimination()
            println("Changes0 (L5): $changes")

            if (changes == 0) noChangesInBoard++
            printGridCandidates() // test
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

    fun pureYWing() {

    }
}