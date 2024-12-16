import jdk.internal.org.jline.reader.Candidate

/*
    Stores tile values and candidates
 */

data class Tile(
    var tileNum : Int = 0
) {
    var candidates : MutableList<Int> = mutableListOf()
    var i : Int = 0
    var j : Int = 0
}