package coverstats.server.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlUtilsTest {

    @Test
    fun test_generateUrl_should_omit_default_port_for_http() {
        val result = generateUrl("http", "google.com", 80, "/search")
        assertEquals("http://google.com/search", result)
    }

    @Test
    fun test_generateUrl_should_omit_default_port_for_https() {
        val result = generateUrl("https", "google.com", 443, "/search")
        assertEquals("https://google.com/search", result)
    }

    @Test
    fun test_generateUrl_should_include_non_standard_port() {
        val result = generateUrl("https", "google.com", 8443, "/search")
        assertEquals("https://google.com:8443/search", result)
    }

}