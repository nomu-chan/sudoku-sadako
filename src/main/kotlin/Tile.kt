import jdk.internal.org.jline.reader.Candidate

/*
    Stores tile values and candidates
 */

class Tile {
    var tileNum : Int = 0
    var candidates : MutableList<Int> = mutableListOf()

    fun setTileCandidate(candidatelist : MutableList<Int>) {
        for (i : Int in candidatelist) {
            candidates.add(i)
        }
    }

    fun eraseTileCandidate(candidate : MutableList<Int>) {
        for (i : Int in candidates) {
            candidates.remove(i)
        }
    }

}