package com.baronswindle;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.msgpack.jackson.dataformat.MessagePackMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@WireMockTest
public class ClientTest {
    private static final String RESPONSE_AS_JSON = """
                {
                    "key1": "value1",
                    "key2": "value2"
                }
            """;

    private JsonClient jsonClient;
    private MessagePackClient messagePackClient;

    @BeforeEach
    void setUpClient(WireMockRuntimeInfo wmRuntimeInfo) {
        final var target = ClientBuilder.newClient().target(wmRuntimeInfo.getHttpBaseUrl());
        jsonClient = new JsonClient(target.path("/json"));
        messagePackClient = new MessagePackClient(target.path("/msgpack"));
    }

    @BeforeEach
    void setUpServer() {
        byte[] bytes = convertJsonStringToMessagePack(RESPONSE_AS_JSON);

        stubFor(get("/json")
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON))
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody(RESPONSE_AS_JSON)));

        stubFor(get("/msgpack")
                .withHeader(HttpHeaders.ACCEPT, containing("application/x-msgpack"))
                .willReturn(ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/x-msgpack")
//                        .withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                        .withBody(bytes)));
    }

    @Test
    void testJson() {
        final var response = assertDoesNotThrow(() -> jsonClient.getResponse());
        assertEquals("value1", response.get("key1"));
        assertEquals("value2", response.get("key2"));
    }

    @Test
    void testMessagePack() {
        final var response = assertDoesNotThrow(() -> messagePackClient.getResponse());
        assertEquals("value1", response.get("key1"));
        assertEquals("value2", response.get("key2"));
    }

    private static byte[] convertJsonStringToMessagePack(String jsonString) {
        try {
            final var mapper = new MessagePackMapper();
            byte[] bytes = mapper.writeValueAsBytes(mapper.readValue(jsonString, JsonNode.class));
            System.out.println("[convertJsonStringToMessagePack] Length: " + bytes.length);
            System.out.print("[convertJsonStringToMessagePack] Bytes: ");
            for (byte b : bytes) {
                System.out.printf("%02x ", b);
            }
            System.out.println();
            return bytes;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert JSON string to Message Pack", ex);
        }
    }
}
