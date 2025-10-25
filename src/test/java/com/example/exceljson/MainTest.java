package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void runsKnownJob() {
        Integer status = Main.maybeRunJob(new String[]{"fail"});
        assertNotNull(status);
        assertEquals(0, status);
    }

    @Test
    void unknownJobFallsBackToUi() {
        assertNull(Main.maybeRunJob(new String[]{"--ui"}));
    }
}
