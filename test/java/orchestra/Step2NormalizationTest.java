package orchestra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class Step2NormalizationTest {

    private static final double EPSILON = 1e-6;

    @Test
    void normalizedOutputMatchesFormulaForEveryCell() throws IOException {
        RawTable rawTable = readRawTable(Path.of("output", "1_raw_data", "genus_taxaAsColumns.txt"));
        NormalizedTable normalizedTable = readNormalizedTable(
                Path.of("output", "2_normalized", "genus_taxaAsColumns_normalized.txt"));

        assertEquals(rawTable.samples.size(), normalizedTable.samples.size(),
                "Normalized row count should match raw row count.");
        assertEquals(rawTable.taxa.size(), normalizedTable.taxa.size(),
                "Normalized column count should match raw taxa count.");
        assertEquals(rawTable.taxa, normalizedTable.taxa,
                "Normalized header should preserve the same taxa order as the raw input.");
        assertEquals(rawTable.samples, normalizedTable.samples,
                "Normalized rows should preserve the same sample order as the raw input.");

        double averageSequencingDepth = rawTable.averageSequencingDepth();

        for (int row = 0; row < rawTable.samples.size(); row++) {
            double sequencingDepth = rawTable.rowSum(row);
            for (int col = 0; col < rawTable.taxa.size(); col++) {
                double rawCount = rawTable.values.get(row)[col];
                double relativeAbundance = rawCount / sequencingDepth;
                double expected = Math.log10(relativeAbundance * averageSequencingDepth + 1.0);
                double actual = normalizedTable.values.get(row)[col];

                assertEquals(expected, actual, EPSILON,
                        "Mismatch at sample '" + rawTable.samples.get(row) + "', taxon '"
                                + rawTable.taxa.get(col) + "'.");
            }
        }
    }

    private static RawTable readRawTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = splitTabLine(lines.get(0));

        List<String> taxa = new ArrayList<>();
        for (int i = 1; i < header.length; i++) {
            taxa.add(header[i]);
        }

        List<String> samples = new ArrayList<>();
        List<double[]> values = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] cells = splitTabLine(lines.get(i));
            samples.add(cells[0]);
            double[] row = new double[taxa.size()];
            for (int j = 1; j < cells.length; j++) {
                row[j - 1] = Double.parseDouble(cells[j]);
            }
            values.add(row);
        }

        return new RawTable(samples, taxa, values);
    }

    private static NormalizedTable readNormalizedTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = splitTabLine(lines.get(0));

        List<String> taxa = new ArrayList<>();
        for (int i = 1; i < header.length; i++) {
            taxa.add(header[i]);
        }

        List<String> samples = new ArrayList<>();
        List<double[]> values = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] cells = splitTabLine(lines.get(i));
            samples.add(cells[0]);
            double[] row = new double[taxa.size()];
            for (int j = 0; j < taxa.size(); j++) {
                row[j] = Double.parseDouble(cells[j + 1]);
            }
            values.add(row);
        }

        return new NormalizedTable(samples, taxa, values);
    }

    private static String[] splitTabLine(String line) {
        return line.split("\t", -1);
    }

    private record RawTable(List<String> samples, List<String> taxa, List<double[]> values) {
        double averageSequencingDepth() {
            double total = 0.0;
            for (int row = 0; row < values.size(); row++) {
                total += rowSum(row);
            }
            return total / values.size();
        }

        double rowSum(int rowIndex) {
            double sum = 0.0;
            for (double value : values.get(rowIndex)) {
                sum += value;
            }
            return sum;
        }
    }

    private record NormalizedTable(List<String> samples, List<String> taxa, List<double[]> values) {
    }
}
