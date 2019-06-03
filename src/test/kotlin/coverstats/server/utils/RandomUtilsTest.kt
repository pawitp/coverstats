package coverstats.server.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RandomUtilsTest {

    @Test
    fun test_generateToken_should_generate_token_of_length_27() {
        assertEquals(27, generateToken().length)
    }

    @Test
    fun test_generateToken_should_generate_unique_tokens() {
        val token1 = generateToken()
        val token2 = generateToken()
        assertNotEquals(token1, token2)
    }

}