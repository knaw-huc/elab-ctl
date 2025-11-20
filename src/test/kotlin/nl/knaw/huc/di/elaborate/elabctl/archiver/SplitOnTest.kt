package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SplitOnTest {

    @Test
    fun `test splitOn`() {
        val before = listOf(1, 2, 3, 4, 5)
        val expected = listOf(
            listOf(1, 2),
            listOf(3, 4),
            listOf(5),
        )
        val segments = before.splitOn { it % 2 == 1 }
        assertEquals(expected, segments)
    }

}