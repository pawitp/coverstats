package coverstats.server.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class PatchUtilsTest {

    @Test
    fun test_linesChangedInPatch_should_return_lines_changed_or_added_in_patch() {
        val patch = javaClass.getResource("/examplePatch.patch").readText()
        val lines = linesChangedInPatch(patch)
        assertEquals(listOf(5, 63, 64, 66, 67, 68), lines)
    }

    @Test
    fun test_linesChangedInPatch_should_return_empty_for_deleted_file() {
        val patch = javaClass.getResource("/examplePatchDelete.patch").readText()
        val lines = linesChangedInPatch(patch)
        assertEquals(listOf(), lines)
    }

    @Test
    fun test_linesChangedInPatch_should_return_all_lines_for_added_file() {
        val patch = javaClass.getResource("/examplePatchAdd.patch").readText()
        val lines = linesChangedInPatch(patch)
        assertEquals((1 .. 4).toList(), lines)
    }
}