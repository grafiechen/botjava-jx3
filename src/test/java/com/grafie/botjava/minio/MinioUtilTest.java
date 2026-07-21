package com.grafie.botjava.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadWithUniquePngObjectAndReturnPublicDomainUrl() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenReturn(true);
        Path image = tempDir.resolve("source.png");
        Files.write(image, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});
        MinioUtil minio = new MinioUtil(client, "qbot", "https://media.example.com/");

        String url = minio.uploadFile(image.toFile(), "result.png");

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertEquals("qbot", args.bucket());
        assertEquals("image/png", args.contentType());
        assertTrue(args.object().matches(
                "result-\\d{17}-[0-9a-f-]{36}\\.png"), args.object());
        assertEquals("https://media.example.com/qbot/" + args.object(), url);
    }

    @Test
    void shouldRejectMissingOrEmptyFilesAndBlankPublicUrl() throws Exception {
        MinioClient client = mock(MinioClient.class);
        assertThrows(IllegalArgumentException.class,
                () -> new MinioUtil(client, "qbot", " "));

        MinioUtil minio = new MinioUtil(client, "qbot", "https://media.example.com");
        Path empty = tempDir.resolve("empty.png");
        Files.createFile(empty);
        assertThrows(IllegalArgumentException.class,
                () -> minio.uploadFile(empty.toFile(), "empty.png"));
    }
}
