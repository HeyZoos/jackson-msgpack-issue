package com.baronswindle;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackMapper;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

public class MessagePackClient {
    final WebTarget target;

    public MessagePackClient(WebTarget target) {
        this.target = target
                .register(JacksonMessagePackProvider.class);
    }

    Map<String, Object> getResponse() throws IOException {
        var response = this.target
                .request("application/x-msgpack")
                .get();

        System.out.println("[getResponse] Response Content-Length: " + response.getLength());

        byte[] responseBytes = response.readEntity(byte[].class);

        System.out.println("[getResponse] Response Bytes Length: " + responseBytes.length);
        System.out.print("[getResponse] Response Bytes: ");
        for (byte b : responseBytes) {
            System.out.printf("%02x ", b);
        }
        System.out.println();

        ObjectMapper mapper = new MessagePackMapper();
        return mapper.readValue(responseBytes, new TypeReference<>() {});
    }


    @Provider
    @Consumes("application/x-msgpack")
    @Produces("application/x-msgpack")
    public static class JacksonMessagePackProvider extends JacksonJsonProvider {
        public JacksonMessagePackProvider() {
            super(new MessagePackMapper());
        }

        @Override
        protected boolean hasMatchingMediaType(MediaType mediaType) {
            return mediaType.getSubtype().equals("x-msgpack");
        }
    }
}
