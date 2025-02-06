package com.github.opdsko_spring.seaweedfs;

import seaweedfs.client.FilerClient;
import seaweedfs.client.SeaweedOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        FilerClient localhost = new FilerClient("localhost", 8888, 18888);
        try (var outputStream = new SeaweedOutputStream(localhost, "/test");
             var input = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
