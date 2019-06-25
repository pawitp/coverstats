package coverstats.server.utils

private val chunkRegex = Regex("""^@@ -(\d+),(\d+) \+(\d+),(\d+) @@""")

fun linesChangedInPatch(patch: String): List<Int> {
    val changedLines = mutableListOf<Int>()
    var currentLine = 0
    patch.lineSequence().forEach { line ->
        when {
            line.startsWith("@") -> {
                // New chunk
                currentLine = chunkRegex.find(line)!!.groupValues[3].toInt()
            }
            line.startsWith(" ") -> {
                // Unchanged line (present in both old and new)
                currentLine += 1
            }
            line.startsWith("+") -> {
                // Added line
                changedLines.add(currentLine)
                currentLine += 1
            }
            line.startsWith("-") -> {
                // Removed line - do nothing
            }
        }
    }
    return changedLines
}