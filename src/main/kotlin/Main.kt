fun main(args: Array<String>) {
    val grid = Board()

    // input puzzle txt file
    grid.setTiles()
    grid.printGrid()

    println("--")

    print("Select AI Level: ")
    val opt : Int = readLine()!!.toInt()
    when {
        opt == 1 -> grid.solveBoardL1()
        opt == 2 -> grid.solveBoardL2()
    }
    grid.printGrid()

}