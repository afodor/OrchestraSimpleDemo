package orchestra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class Step1RawDataMirrorTest {

    private static final URI SOURCE_URI = URI.create(
            "https://raw.githubusercontent.com/kwinglee/UrbanRuralChina/master/16SrRNA/inputData/RDP/genus_taxaAsColumns.txt");

    @Test
    void localStep1OutputExactlyMatchesTheSourceFile() throws IOException, InterruptedException {
        Path localFile = Path.of("output", "1_raw_data", "genus_taxaAsColumns.txt");

        assertTrue(Files.exists(localFile), "Expected Step 1 output file to exist: " + localFile.toAbsolutePath());

        byte[] localBytes = Files.readAllBytes(localFile);
        byte[] remoteBytes = downloadSourceFile();

        assertArrayEquals(remoteBytes, localBytes,
                "The Step 1 output file must exactly match the UrbanRuralChina source file.");
    }

    private static byte[] downloadSourceFile() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder(SOURCE_URI)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertTrue(response.statusCode() == 200,
                "Expected HTTP 200 while downloading source file, got " + response.statusCode());
        return response.body();
    }
}
