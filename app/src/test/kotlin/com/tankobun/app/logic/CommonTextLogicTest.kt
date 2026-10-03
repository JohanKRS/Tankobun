package com.tankobun.app.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonTextLogicTest {
    @Test
    fun flattensAniListHtml() {
        assertEquals(
            "A boy & \"his\" sword. It's fine.",
            "A <i>boy</i> &amp; &quot;his&quot; sword.<br><br>It&#039;s fine.".plainMediaDescription(),
        )
    }

    @Test
    fun dropsMangaBakaPublisherLinkLines() {
        assertEquals(
            "The young boy Ilias is the strongest solo explorer.",
            "[title](https://www.cmoa.jp/title/363986/)\nThe young boy Ilias is the strongest solo explorer."
                .plainMediaDescription(),
        )
    }

    @Test
    fun keepsLinkTextInsideSentencesAndBracketedPrologues() {
        assertEquals(
            "Read it on Naver Webtoon first. [Prevent the villain] was the mission.",
            "Read it on [Naver Webtoon](https://comic.naver.com/webtoon) first.\n\n[Prevent the villain] was the mission."
                .plainMediaDescription(),
        )
    }

    @Test
    fun removesMarkdownEmphasisRulesAndOrphanLabels() {
        assertEquals("", "**Original Webtoon:**  \n[Naver Webtoon](https://comic.naver.com), [Naver Series](https://series.naver.com)".plainMediaDescription())
        assertEquals(
            "Callie is nothing special. (Source: Webtoon)",
            "Callie is **nothing** special.\n\n---\n\n(Source: Webtoon)".plainMediaDescription(),
        )
    }

    @Test
    fun dropsBareUrlsAndLabelledLinkLists() {
        assertEquals("", "[Comic Gardo](https://comic-gardo.com/episode/1)\nhttps://www.cmoa.jp/title/368098".plainMediaDescription())
        assertEquals(
            "From INKR: From the creator of Yu-Gi-Oh R.",
            "Official English: [Comikey](https://comikey.com/comics/1/), [INKR](https://inkr.com/title/669)\n\nFrom INKR:  \nFrom the creator of Yu-Gi-Oh R."
                .plainMediaDescription(),
        )
    }

    @Test
    fun handlesMissingDescriptions() {
        assertEquals("", null.plainMediaDescription())
    }
}
