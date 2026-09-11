package com.tankobun.core.extensions.novel

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LnReaderSettingsTest {
    private val schema = JSONObject("""{
      "password":{"label":"Password","type":"Text","value":""},
      "url":{"label":"Site","value":"https://example.invalid"},
      "hideLocked":{"label":"Hide locked","type":"Switch","value":false},
      "quality":{"label":"Quality","type":"Select","value":"high","options":[{"label":"High","value":"high"},{"label":"Low","value":"low"}]},
      "excluded":{"label":"Excluded","type":"CheckboxGroup","value":[],"options":[{"label":"One","value":"one"}]}
    }""")

    @Test fun fourCommunitySettingTypesAndDefaultsArePreserved() {
        val fields = lnReaderSettings(schema, JSONObject())
        assertEquals(5, fields.size)
        assertTrue(fields.single { it.key == "password" }.sensitive)
        assertEquals(false, fields.single { it.key == "hideLocked" }.value)
        validateLnReaderSettings(schema, JSONObject("""{"quality":"low","hideLocked":true,"excluded":["one"],"password":"fictional"}"""))
        assertThrows(IllegalArgumentException::class.java) { validateLnReaderSettings(schema, JSONObject("""{"quality":"unknown"}""")) }
        assertThrows(IllegalArgumentException::class.java) { validateLnReaderSettings(schema, JSONObject("""{"hideLocked":"true"}""")) }
    }

    @Test fun portablePreferencesExcludeFreeTextAndCredentials() {
        val values = JSONObject("""{"quality":"low","hideLocked":true,"excluded":["one"],"password":"fictional","url":"https://example.invalid/?private=data"}""")
        val backup = safeLnReaderBackupValues(schema, values)
        assertEquals(3, backup.length())
        assertFalse(backup.has("password")); assertFalse(backup.has("url"))
        assertEquals("low", backup.getString("quality"))
    }

    @Test fun websiteOriginsCannotAliasOtherSitesOrLocalFiles() {
        assertEquals("https://example.invalid", webOrigin("https://EXAMPLE.invalid:443/login?return=reader"))
        assertEquals("http://127.0.0.1:8080", webOrigin("http://127.0.0.1:8080/page"))
        assertNull(webOrigin("file:///private/data"))
        assertNull(webOrigin("https://user:pass@example.invalid"))
        assertNotEquals(webOrigin("https://example.invalid"), webOrigin("https://example.invalid.other.test"))
    }
}
