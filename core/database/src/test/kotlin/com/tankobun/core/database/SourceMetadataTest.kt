package com.tankobun.core.database

import com.tankobun.core.model.SourceBinding
import com.tankobun.core.model.SourceChapter
import org.junit.Assert.*
import org.junit.Test

class SourceMetadataTest {
    private val memo = """{"cursor":[1,"two"],"config":{"enabled":true}}"""

    @Test fun bindingAndChapterMetadataSurviveDatabaseMapping() {
        val binding = SourceBinding(1, 2, "fixture.package", "/manga", "Fixture", null, 10, memo)
        val chapter = SourceChapter(2, "/manga", "/chapter", "Chapter", 1f, null, null, memo)
        assertEquals(binding, binding.toEntity().toModel())
        assertEquals(chapter, chapter.toEntity(10).toModel())
    }

    @Test fun oldRowsWithoutMetadataRemainReadable() {
        assertNull(SourceBindingEntity(1, 2, "fixture.package", "/manga", "Fixture", null, 10).toModel().memoJson)
        assertNull(SourceChapterEntity(2, "/manga", "/chapter", "Chapter", 1f, null, null, 10).toModel().memoJson)
    }
}
