package com.convertai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpisodeIndexExtractorTest {
    @Test
    void extractsIndexFromCommonNames() {
        assertEquals(1, EpisodeIndexExtractor.extractIndex("猫和老鼠-01.mp4").orElseThrow());
        assertEquals(2, EpisodeIndexExtractor.extractIndex("[2]猫和老鼠.mp4").orElseThrow());
        assertEquals(12, EpisodeIndexExtractor.extractIndex("猫和老鼠 第12集.mkv").orElseThrow());
        assertEquals(8, EpisodeIndexExtractor.extractIndex("Tom.And.Jerry.EP08.mov").orElseThrow());
        assertEquals(23, EpisodeIndexExtractor.extractIndex("短剧_第023话_高清.mp4").orElseThrow());
        assertEquals(7, EpisodeIndexExtractor.extractIndex("S01E07-猫和老鼠.mp4").orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoNumberExists() {
        assertTrue(EpisodeIndexExtractor.extractIndex("猫和老鼠.mp4").isEmpty());
    }
}
