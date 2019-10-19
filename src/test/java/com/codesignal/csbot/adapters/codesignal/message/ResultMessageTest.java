package com.codesignal.csbot.adapters.codesignal.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class ResultMessageTest {
    @Test
    public void whenSerializingUsingJsonSerialize_thenCorrect() throws IOException {
        Message message = new ResultMessage(new ObjectMapper().readTree("1563673860252"));

        String expected = "{\"msg\":\"result\",\"id\":\"0\",\"result\":1563673860252}";
        String actual = new ObjectMapper().writeValueAsString(message);
        assertEquals(expected, actual);
    }

    @Test
    public void whenDeserializingOneSingleInstance_thenCorrect() throws IOException {
        String json = "{ \"msg\" : \"result\", \"id\" : \"0\", \"result\" : 1563673860252}";
        ResultMessage object = new ObjectMapper().readValue(json, ResultMessage.class);
        assertEquals(1563673860252L, object.getResult().asLong());
    }

    @Test
    public void whenDeserializingMultipleInstance_thenCorrect() throws IOException {
        String json = "[\"{\\\"msg\\\":\\\"result\\\",\\\"id\\\":\\\"1\\\",\\\"result\\\":1563673860252}\"]";
        String[] objects = new ObjectMapper().readValue(json, String[].class);
        ResultMessage msg = new ObjectMapper().readValue(objects[0], ResultMessage.class);

        assertEquals("result", msg.getMsg());
        assertEquals("1", msg.getId());
        assertEquals(1563673860252L, msg.getResult().asLong());
    }
}