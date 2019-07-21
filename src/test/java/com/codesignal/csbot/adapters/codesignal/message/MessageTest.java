package com.codesignal.csbot.adapters.codesignal.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class MessageTest {
    @Test
    public void whenSerializingUsingJsonSerialize_thenCorrect() throws JsonProcessingException {
        Message message = new Message("method");

        String expected = "{\"msg\":\"method\",\"id\":\"0\"}";
        String actual = new ObjectMapper().writeValueAsString(message);
        assertEquals(expected, actual);
    }

    @Test
    public void whenDeserializingOneSingleInstance_thenCorrect() throws IOException {
        String json = "{ \"msg\" : \"result\", \"id\" : \"0\" }";
        Message object = new ObjectMapper().readValue(json, Message.class);
        assertEquals("result", object.getMsg());
    }

    @Test
    public void whenDeserializingMultipleInstance_thenCorrect() throws IOException {
        String json = "[\"{\\\"msg\\\":\\\"result\\\",\\\"id\\\":\\\"1\\\",\\\"result\\\":1563673860252}\"]";
        String[] objects = new ObjectMapper().readValue(json, String[].class);
        Message msg = new ObjectMapper().readValue(objects[0], Message.class);

        assertEquals("result", msg.getMsg());
        assertEquals("1", msg.getId());
    }
}
