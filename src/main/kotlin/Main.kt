fun main(args: Array<String>) {
    val grid = Board()

    // input puzzle txt file
    grid.setUpTiles()
    grid.printGrid()

    println("--")

    print("Select AI Level: ")
    val opt : Int = readLine()!!.toInt()
    when {
        opt == 1 -> grid.solveBoardL1()
        opt == 2 -> grid.solveBoardL2()
        opt == 3 -> grid.solveBoardL3()
        opt == 4 -> grid.solveBoardL4()
        opt == 5 -> grid.solveBoardL5()
        opt == -5 -> grid.pureSwordfish()
    }
    grid.printGrid()

}