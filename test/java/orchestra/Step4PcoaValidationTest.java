package orchestra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class Step4PcoaValidationTest {

    private static final double EPSILON = 0.01;
    private static final List<String> EXPECTED_METADATA_COLUMNS = List.of(
            "sampleID", "readNumber", "patientID", "ruralUrban", "timepoint");

    @Test
    void pcoaResultsMetadataMatchesDirectory3Metadata() throws IOException {
        MetadataTable metadataTable = readMetadataTable(
                Path.of("output", "3_metadata", "genus_taxaAsColumnsLogNorm_WithMetadata.txt"));
        PcoaResultsTable pcoaTable = readPcoaResultsTable(
                Path.of("output", "4_pcoa", "pcoa_results_bray_curtis.txt"));

        assertEquals(EXPECTED_METADATA_COLUMNS, pcoaTable.metadataColumnNames,
                "PCoA results should preserve the five metadata columns from directory 3.");
        assertEquals(metadataTable.samples, pcoaTable.samples,
                "PCoA results should preserve sample order from directory 3.");

        for (int row = 0; row < metadataTable.samples.size(); row++) {
            assertEquals(metadataTable.sampleIds.get(row), pcoaTable.samples.get(row),
                    "Sample ID mismatch at row " + row + ".");
            assertEquals(metadataTable.readNumbers.get(row), pcoaTable.readNumbers.get(row),
                    "readNumber mismatch for sample " + metadataTable.samples.get(row) + ".");
            assertEquals(metadataTable.patientIds.get(row), pcoaTable.patientIds.get(row),
                    "patientID mismatch for sample " + metadataTable.samples.get(row) + ".");
            assertEquals(metadataTable.ruralUrban.get(row), pcoaTable.ruralUrban.get(row),
                    "ruralUrban mismatch for sample " + metadataTable.samples.get(row) + ".");
            assertEquals(metadataTable.timepoints.get(row), pcoaTable.timepoints.get(row),
                    "timepoint mismatch for sample " + metadataTable.samples.get(row) + ".");
        }
    }

    @Test
    void brayCurtisDistanceMatrixMatchesDirectory3Data() throws IOException {
        MetadataTable metadataTable = readMetadataTable(
                Path.of("output", "3_metadata", "genus_taxaAsColumnsLogNorm_WithMetadata.txt"));
        SquareMatrixTable distanceTable = readSquareMatrixTable(
                Path.of("output", "4_pcoa", "bray_curtis_distance_matrix.txt"));

        assertEquals(metadataTable.samples, distanceTable.labels,
                "Distance matrix labels should match sample order from directory 3.");

        double[][] expected = computeBrayCurtisDistanceMatrix(metadataTable.genusValues);
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected.length; j++) {
                assertEquals(expected[i][j], distanceTable.values[i][j], EPSILON,
                        "Distance mismatch at (" + metadataTable.samples.get(i) + ", "
                                + metadataTable.samples.get(j) + ").");
            }
        }
    }

    @Test
    void pcoaCoordinatesAndVarianceMatchDirectory3Data() throws IOException {
        MetadataTable metadataTable = readMetadataTable(
                Path.of("output", "3_metadata", "genus_taxaAsColumnsLogNorm_WithMetadata.txt"));
        PcoaResultsTable pcoaTable = readPcoaResultsTable(
                Path.of("output", "4_pcoa", "pcoa_results_bray_curtis.txt"));
        VarianceTable varianceTable = readVarianceTable(
                Path.of("output", "4_pcoa", "pcoa_variance_explained.txt"));

        ComputedPcoa computed = computePcoa(metadataTable.genusValues, 10);

        assertEquals(computed.axisNames, pcoaTable.axisNames,
                "PCoA axis names should match expected output.");
        assertEquals(computed.axisNames, varianceTable.axisNames,
                "Variance file axis names should match expected output.");

        double[][] alignedExpectedCoordinates = alignAxisSigns(computed.coordinates, pcoaTable.coordinates);
        for (int row = 0; row < alignedExpectedCoordinates.length; row++) {
            for (int axis = 0; axis < alignedExpectedCoordinates[row].length; axis++) {
                assertEquals(alignedExpectedCoordinates[row][axis], pcoaTable.coordinates[row][axis], EPSILON,
                        "PCoA coordinate mismatch at sample '" + metadataTable.samples.get(row)
                                + "', axis '" + computed.axisNames.get(axis) + "'.");
            }
        }

        double cumulative = 0.0;
        for (int axis = 0; axis < computed.percentExplained.length; axis++) {
            cumulative += computed.percentExplained[axis];
            assertEquals(computed.percentExplained[axis], varianceTable.percentVariance[axis], EPSILON,
                    "Percent variance mismatch for axis " + computed.axisNames.get(axis) + ".");
            assertEquals(cumulative, varianceTable.cumulativeVariance[axis], EPSILON,
                    "Cumulative variance mismatch for axis " + computed.axisNames.get(axis) + ".");
        }
    }

    private static MetadataTable readMetadataTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = splitTabLine(lines.get(0));
        int metadataColumnCount = 5;
        int genusColumnCount = header.length - metadataColumnCount;

        List<String> samples = new ArrayList<>();
        List<String> sampleIds = new ArrayList<>();
        List<String> readNumbers = new ArrayList<>();
        List<String> patientIds = new ArrayList<>();
        List<String> ruralUrban = new ArrayList<>();
        List<String> timepoints = new ArrayList<>();
        double[][] genusValues = new double[lines.size() - 1][genusColumnCount];

        for (int row = 1; row < lines.size(); row++) {
            String[] cells = splitTabLine(lines.get(row));
            int outputRow = row - 1;
            samples.add(cells[0]);
            sampleIds.add(cells[0]);
            readNumbers.add(cells[1]);
            patientIds.add(cells[2]);
            ruralUrban.add(cells[3]);
            timepoints.add(cells[4]);
            for (int col = 0; col < genusColumnCount; col++) {
                genusValues[outputRow][col] = Double.parseDouble(cells[metadataColumnCount + col]);
            }
        }

        return new MetadataTable(samples, sampleIds, readNumbers, patientIds, ruralUrban, timepoints, genusValues);
    }

    private static PcoaResultsTable readPcoaResultsTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = splitTabLine(lines.get(0));

        List<String> metadataColumnNames = Arrays.asList(Arrays.copyOfRange(header, 0, 5));
        int axisStart = 5;
        List<String> axisNames = Arrays.asList(Arrays.copyOfRange(header, axisStart, header.length));

        List<String> samples = new ArrayList<>();
        List<String> readNumbers = new ArrayList<>();
        List<String> patientIds = new ArrayList<>();
        List<String> ruralUrban = new ArrayList<>();
        List<String> timepoints = new ArrayList<>();
        double[][] coordinates = new double[lines.size() - 1][axisNames.size()];

        for (int row = 1; row < lines.size(); row++) {
            String[] cells = splitTabLine(lines.get(row));
            int outputRow = row - 1;
            samples.add(cells[0]);
            readNumbers.add(cells[1]);
            patientIds.add(cells[2]);
            ruralUrban.add(cells[3]);
            timepoints.add(cells[4]);
            for (int axis = 0; axis < axisNames.size(); axis++) {
                coordinates[outputRow][axis] = Double.parseDouble(cells[axisStart + axis]);
            }
        }

        return new PcoaResultsTable(
                metadataColumnNames, axisNames, samples, readNumbers, patientIds, ruralUrban, timepoints, coordinates);
    }

    private static SquareMatrixTable readSquareMatrixTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = splitTabLine(lines.get(0));
        List<String> labels = Arrays.asList(Arrays.copyOfRange(header, 1, header.length));
        double[][] values = new double[labels.size()][labels.size()];

        for (int row = 1; row < lines.size(); row++) {
            String[] cells = splitTabLine(lines.get(row));
            for (int col = 1; col < cells.length; col++) {
                values[row - 1][col - 1] = Double.parseDouble(cells[col]);
            }
        }

        return new SquareMatrixTable(labels, values);
    }

    private static VarianceTable readVarianceTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<String> axisNames = new ArrayList<>();
        double[] percentVariance = new double[lines.size() - 1];
        double[] cumulativeVariance = new double[lines.size() - 1];

        for (int row = 1; row < lines.size(); row++) {
            String[] cells = splitTabLine(lines.get(row));
            int outputRow = row - 1;
            axisNames.add(cells[0]);
            percentVariance[outputRow] = Double.parseDouble(cells[1]);
            cumulativeVariance[outputRow] = Double.parseDouble(cells[2]);
        }

        return new VarianceTable(axisNames, percentVariance, cumulativeVariance);
    }

    private static double[][] computeBrayCurtisDistanceMatrix(double[][] values) {
        int n = values.length;
        double[][] distances = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double distance = brayCurtis(values[i], values[j]);
                distances[i][j] = distance;
                distances[j][i] = distance;
            }
        }
        return distances;
    }

    private static double brayCurtis(double[] u, double[] v) {
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < u.length; i++) {
            numerator += Math.abs(u[i] - v[i]);
            denominator += u[i] + v[i];
        }
        if (denominator == 0.0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private static ComputedPcoa computePcoa(double[][] values, int nAxes) {
        double[][] distanceMatrix = computeBrayCurtisDistanceMatrix(values);
        int n = distanceMatrix.length;
        double[][] squaredDistances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                squaredDistances[i][j] = distanceMatrix[i][j] * distanceMatrix[i][j];
            }
        }

        double[][] h = new double[n][n];
        double inverseN = 1.0 / n;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                h[i][j] = (i == j ? 1.0 : 0.0) - inverseN;
            }
        }

        double[][] b = multiply(multiply(h, squaredDistances), h);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                b[i][j] *= -0.5;
            }
        }

        EigenDecomposition eigen = jacobiEigenDecomposition(b);
        Integer[] order = new Integer[eigen.eigenvalues.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> eigen.eigenvalues[i]).reversed());

        List<Double> positiveEigenvalues = new ArrayList<>();
        List<double[]> positiveEigenvectors = new ArrayList<>();
        for (int index : order) {
            if (eigen.eigenvalues[index] > 1e-8) {
                positiveEigenvalues.add(eigen.eigenvalues[index]);
                positiveEigenvectors.add(column(eigen.eigenvectors, index));
            }
        }

        int axisCount = Math.min(nAxes, positiveEigenvalues.size());
        double[][] coordinates = new double[n][axisCount];
        double[] percentExplained = new double[axisCount];
        double totalVariance = 0.0;
        for (double eigenvalue : positiveEigenvalues) {
            totalVariance += Math.abs(eigenvalue);
        }

        List<String> axisNames = new ArrayList<>();
        for (int axis = 0; axis < axisCount; axis++) {
            double eigenvalue = positiveEigenvalues.get(axis);
            double scale = Math.sqrt(eigenvalue);
            double[] eigenvector = positiveEigenvectors.get(axis);
            axisNames.add("PCo" + (axis + 1));
            for (int row = 0; row < n; row++) {
                coordinates[row][axis] = eigenvector[row] * scale;
            }
            percentExplained[axis] = eigenvalue / totalVariance * 100.0;
        }

        return new ComputedPcoa(axisNames, coordinates, percentExplained);
    }

    private static EigenDecomposition jacobiEigenDecomposition(double[][] input) {
        int n = input.length;
        double[][] a = copyMatrix(input);
        double[][] v = identityMatrix(n);
        int maxIterations = 100 * n * n;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int p = 0;
            int q = 1;
            double max = 0.0;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double value = Math.abs(a[i][j]);
                    if (value > max) {
                        max = value;
                        p = i;
                        q = j;
                    }
                }
            }

            if (max < 1e-12) {
                break;
            }

            double app = a[p][p];
            double aqq = a[q][q];
            double apq = a[p][q];
            double tau = (aqq - app) / (2.0 * apq);
            double t = Math.signum(tau) / (Math.abs(tau) + Math.sqrt(1.0 + tau * tau));
            if (tau == 0.0) {
                t = 1.0;
            }
            double c = 1.0 / Math.sqrt(1.0 + t * t);
            double s = t * c;

            for (int k = 0; k < n; k++) {
                if (k != p && k != q) {
                    double aik = a[k][p];
                    double akq = a[k][q];
                    a[k][p] = c * aik - s * akq;
                    a[p][k] = a[k][p];
                    a[k][q] = s * aik + c * akq;
                    a[q][k] = a[k][q];
                }
            }

            a[p][p] = c * c * app - 2.0 * s * c * apq + s * s * aqq;
            a[q][q] = s * s * app + 2.0 * s * c * apq + c * c * aqq;
            a[p][q] = 0.0;
            a[q][p] = 0.0;

            for (int k = 0; k < n; k++) {
                double vip = v[k][p];
                double viq = v[k][q];
                v[k][p] = c * vip - s * viq;
                v[k][q] = s * vip + c * viq;
            }
        }

        double[] eigenvalues = new double[n];
        for (int i = 0; i < n; i++) {
            eigenvalues[i] = a[i][i];
        }
        return new EigenDecomposition(eigenvalues, v);
    }

    private static double[][] alignAxisSigns(double[][] expected, double[][] actual) {
        double[][] aligned = copyMatrix(expected);
        int rows = expected.length;
        int axes = expected[0].length;

        for (int axis = 0; axis < axes; axis++) {
            double dot = 0.0;
            for (int row = 0; row < rows; row++) {
                dot += aligned[row][axis] * actual[row][axis];
            }
            if (dot < 0.0) {
                for (int row = 0; row < rows; row++) {
                    aligned[row][axis] *= -1.0;
                }
            }
        }
        return aligned;
    }

    private static double[][] multiply(double[][] left, double[][] right) {
        int rows = left.length;
        int columns = right[0].length;
        int shared = right.length;
        double[][] result = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < shared; k++) {
                double leftValue = left[i][k];
                for (int j = 0; j < columns; j++) {
                    result[i][j] += leftValue * right[k][j];
                }
            }
        }
        return result;
    }

    private static double[] column(double[][] matrix, int index) {
        double[] column = new double[matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            column[row] = matrix[row][index];
        }
        return column;
    }

    private static double[][] copyMatrix(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        return copy;
    }

    private static double[][] identityMatrix(int size) {
        double[][] identity = new double[size][size];
        for (int i = 0; i < size; i++) {
            identity[i][i] = 1.0;
        }
        return identity;
    }

    private static String[] splitTabLine(String line) {
        return line.split("\t", -1);
    }

    private record MetadataTable(
            List<String> samples,
            List<String> sampleIds,
            List<String> readNumbers,
            List<String> patientIds,
            List<String> ruralUrban,
            List<String> timepoints,
            double[][] genusValues) {
    }

    private record PcoaResultsTable(
            List<String> metadataColumnNames,
            List<String> axisNames,
            List<String> samples,
            List<String> readNumbers,
            List<String> patientIds,
            List<String> ruralUrban,
            List<String> timepoints,
            double[][] coordinates) {
    }

    private record SquareMatrixTable(List<String> labels, double[][] values) {
    }

    private record VarianceTable(List<String> axisNames, double[] percentVariance, double[] cumulativeVariance) {
    }

    private record ComputedPcoa(List<String> axisNames, double[][] coordinates, double[] percentExplained) {
    }

    private record EigenDecomposition(double[] eigenvalues, double[][] eigenvectors) {
    }
}
