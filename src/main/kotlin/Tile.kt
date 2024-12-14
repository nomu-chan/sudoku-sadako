import jdk.internal.org.jline.reader.Candidate

/*
    Stores tile values and candidates
 */

class Tile {
    var tileNum : Int = 0
    var candidates : MutableList<Int> = mutableListOf()
}