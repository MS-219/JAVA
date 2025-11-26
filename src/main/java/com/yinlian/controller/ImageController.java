package com.yinlian.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private static final Path FACE_DIR = Paths.get("data", "faces");

    @GetMapping(value = "/device/faces/{filename:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFaceImage(@PathVariable String filename) {
        try {
            Path path = FACE_DIR.resolve(filename);
            if (!Files.exists(path)) {
                logger.warn("Face image not found: {}", filename);
                return new byte[0];
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            logger.error("Failed to read face image: " + filename, e);
            return new byte[0];
        }
    }
}
