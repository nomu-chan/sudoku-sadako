
fun main() {
    var int2Darr : IntList2D = mutableListOf()
    val coordSet : IntList2D = mutableListOf()

    val coords : IntList2D = arrayOf(intArrayOf(1, 2).toMutableList(), intArrayOf(1, 2).toMutableList()).toMutableList()

    for (coord in coords) {
        val i = coord[0]
        val j = coord[1]
        coordSet.add(arrayOf(1, 2).toMutableList())
    }

    int2Darr = coordSet.toMutableList()

    for (i in int2Darr) {
        println("${i[0]} ${i[1]}")
    }

}