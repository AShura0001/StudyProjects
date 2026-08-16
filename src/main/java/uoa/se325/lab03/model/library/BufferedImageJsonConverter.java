package uoa.se325.lab03.model.library;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BufferedImageJsonConverter {

    public static class Serializer extends JsonSerializer<BufferedImage> {
        @Override
        public void serialize(BufferedImage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(value, "png", bos);
            gen.writeBinary(bos.toByteArray());
        }
    }

    public static class Deserializer extends JsonDeserializer<BufferedImage> {
        @Override
        public BufferedImage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            byte[] imageBytes = p.getBinaryValue();
            if (imageBytes == null || imageBytes.length == 0) {
                return null;
            }
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                return ImageIO.read(bis);
            }
        }
    }
}
