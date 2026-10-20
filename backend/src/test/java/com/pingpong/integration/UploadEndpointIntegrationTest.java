package com.pingpong.integration;

import com.pingpong.PingpongRecommendationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "upload.path=target/test-uploads/",
    "spring.servlet.multipart.location=target/test-multipart/"
})
class UploadEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadEquipmentImageShouldSucceedInApplicationContext() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "context-upload.png",
            "image/png",
            createPngBytes()
        );

        mockMvc.perform(multipart("/upload/equipment-image").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").exists());
    }

    private byte[] createPngBytes() throws Exception {
        BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, new Color(16, 59, 43).getRGB());
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
