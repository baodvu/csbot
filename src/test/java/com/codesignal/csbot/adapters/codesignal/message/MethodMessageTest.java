package com.codesignal.csbot.adapters.codesignal.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class MethodMessageTest {
    @Test
    public void whenSerializingUsingJsonSerialize_thenCorrect() throws JsonProcessingException {
        MethodMessage message = new MethodMessage("GetUserFeedMessage", List.of("a", "b"));

        String expected = "{\"msg\":\"method\",\"id\":\"0\",\"method\":\"GetUserFeedMessage\",\"params\":[\"a\",\"b\"]}";
        String actual = new ObjectMapper().writeValueAsString(message);
        assertEquals(expected, actual);
    }
}