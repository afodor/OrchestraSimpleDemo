package orchestra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class Step2ReferenceNormalizedFileTest {

    private static final double EPSILON = 1e-6;
    private static final URI SOURCE_URI = URI.create(
            "https://raw.githubusercontent.com/kwinglee/UrbanRuralChina/master/16SrRNA/inputData/RDP/genus_taxaAsColumnsLogNorm_WithMetadata.txt");

    @Test
    void normalizedOutputMatchesReferenceNormalizedSpreadsheet() throws IOException, InterruptedException {
        Table localTable = readLocalNormalizedTable(Path.of("output", "2_normalized", "genus_taxaAsColumns_normalized.txt"));
        Table referenceTable = readReferenceNormalizedTable(downloadReferenceFile());

        assertEquals(localTable.samples, referenceTable.samples,
                "Sample order should match the reference normalized spreadsheet.");
        assertEquals(localTable.taxa, referenceTable.taxa,
                "Taxa columns should match the reference normalized spreadsheet.");

        for (int row = 0; row < localTable.samples.size(); row++) {
            for (int col = 0; col < localTable.taxa.size(); col++) {
                assertEquals(referenceTable.values.get(row)[col], localTable.values.get(row)[col], EPSILON,
                        "Mismatch at sample '" + localTable.samples.get(row) + "', taxon '"
                                + localTable.taxa.get(col) + "'.");
            }
        }
    }

    private static String downloadReferenceFile() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder(SOURCE_URI)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertTrue(response.statusCode() == 200,
                "Expected HTTP 200 while downloading reference normalized file, got " + response.statusCode());
        return response.body();
    }

    private static Table readLocalNormalizedTable(Path path) throws IOException {
        return parseTable(Files.readAllLines(path), 1);
    }

    private static Table readReferenceNormalizedTable(String fileContents) {
        List<String> lines = fileContents.lines().toList();
        return parseTable(lines, 5);
    }

    private static Table parseTable(List<String> lines, int dataStartColumn) {
        String[] header = splitTabLine(lines.get(0));

        List<String> taxa = new ArrayList<>();
        for (int i = dataStartColumn; i < header.length; i++) {
            taxa.add(header[i]);
        }

        List<String> samples = new ArrayList<>();
        List<double[]> values = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] cells = splitTabLine(lines.get(i));
            samples.add(cells[0]);

            double[] row = new double[taxa.size()];
            for (int j = 0; j < taxa.size(); j++) {
                row[j] = Double.parseDouble(cells[dataStartColumn + j]);
            }
            values.add(row);
        }

        return new Table(samples, taxa, values);
    }

    private static String[] splitTabLine(String line) {
        return line.split("\t", -1);
    }

    private record Table(List<String> samples, List<String> taxa, List<double[]> values) {
    }
}
