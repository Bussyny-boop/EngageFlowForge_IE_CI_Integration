package com.example.exceljson;

import com.example.exceljson.jobs.JobRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void runsKnownJobViaJobRunner() {
        int status = new JobRunner().run("fail");
        assertEquals(0, status);
    }

    @Test
    void unknownJobReturnsNonZero() {
        int status = new JobRunner().run("not-a-job");
        assertNotEquals(0, status);
    }
}
