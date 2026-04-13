# Orchestra Pattern Demo: Bioinformatics Pipeline

This repository demonstrates the Orchestra pattern for bioinformatics data processing pipelines using 16S rRNA genus-level taxonomic count data.

## Orchestra Pattern Overview

The Orchestra pattern organizes data processing into discrete, sequential steps, where each step:
1. Takes input from the previous step
2. Performs a specific transformation
3. Produces output for the next step
4. Is independently executable and testable

## Pipeline Steps

### Step 1: Data Download
- **Input**: Remote data source (GitHub repository)
- **Process**: Download raw genus-level count data
- **Output**: `output/1_raw_data/genus_taxaAsColumns.txt`

### Step 2: Data Normalization  
- **Input**: Raw count data from Step 1
- **Process**: Normalize using log10 transformation
- **Formula**: `normalizedData = log10(relativeAbundance × avgSequencingDepth + 1)`
- **Output**: `output/2_normalized/genus_taxaAsColumns_normalized.txt`

## Data Description

- **Source**: Urban vs Rural China 16S rRNA study
- **Data Type**: Genus-level taxonomic counts
- **Samples**: 160 samples
- **Features**: 346 bacterial genera
- **Sequencing Depth Range**: 42,728 - 140,895 reads per sample
- **Average Sequencing Depth**: 72,601.52 reads per sample

## Directory Structure

```
OrchestraSimpleDemo/
├── README.md
├── normalize_data.py              # Step 2: Normalization script
└── output/                        # Output directory (not in git)
    ├── 1_raw_data/                # Step 1 output
    │   └── genus_taxaAsColumns.txt
    └── 2_normalized/              # Step 2 output
        └── genus_taxaAsColumns_normalized.txt
```

## Running the Pipeline

### Prerequisites
- Python 3.x
- pandas
- numpy

### Step 1: Download Data
```bash
mkdir -p output/1_raw_data output/2_normalized
curl -o output/1_raw_data/genus_taxaAsColumns.txt \
  "https://raw.githubusercontent.com/kwinglee/UrbanRuralChina/master/16SrRNA/inputData/RDP/genus_taxaAsColumns.txt"
```

### Step 2: Normalize Data
```bash
python normalize_data.py
```

## Normalization Details

The normalization transforms raw counts to log-scaled relative abundances:

1. **Calculate relative abundance**: `count / total_reads_per_sample`
2. **Scale by average depth**: `relative_abundance × average_sequencing_depth`
3. **Log transform**: `log10(scaled_abundance + 1)`

This approach:
- Accounts for varying sequencing depths between samples
- Reduces the impact of highly abundant taxa
- Makes data more suitable for downstream statistical analysis
- Handles zero counts (adds 1 before log transformation)

## Key Features of This Orchestra Implementation

1. **Modularity**: Each step is a separate, independent process
2. **Data Flow**: Clear input → process → output for each step  
3. **Reproducibility**: Scripts can be run independently with known inputs
4. **Scalability**: Easy to add additional processing steps
5. **Testability**: Each step can be validated independently

## Next Steps (Future Workshop Extensions)

Potential additional Orchestra steps:
- **Step 3**: Quality filtering (remove low-abundance genera)
- **Step 4**: Statistical analysis (PCA, PERMANOVA)
- **Step 5**: Visualization (ordination plots, heatmaps)
- **Step 6**: Report generation

## Workshop Notes

- The `output/` directory is excluded from git (add to .gitignore)
- Raw data is preserved for reproducibility
- Intermediate results are stored for debugging/validation
- Each step logs its progress and key statistics