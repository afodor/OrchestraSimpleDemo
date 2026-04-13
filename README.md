# Orchestra Pattern Demo: Complete Bioinformatics Pipeline

This repository demonstrates a complete 5-step Orchestra pattern for bioinformatics data processing, analyzing gut microbiome differences between urban and rural Chinese populations using 16S rRNA genus-level taxonomic data.

## 🎼 Orchestra Pattern Overview

The Orchestra pattern organizes data processing into discrete, sequential steps, where each step:
1. Takes input from the previous step
2. Performs a specific transformation
3. Produces output for the next step
4. Is independently executable and testable

## 📊 Complete Pipeline Steps

### Step 1: Raw Data Download
- **Input**: Remote data source (GitHub repository)
- **Process**: Download raw genus-level count data
- **Output**: `output/1_raw_data/genus_taxaAsColumns.txt`

### Step 2: Data Normalization  
- **Input**: Raw count data from Step 1
- **Process**: Normalize using log10 transformation
- **Formula**: `normalizedData = log10(relativeAbundance × avgSequencingDepth + 1)`
- **Output**: `output/2_normalized/genus_taxaAsColumns_normalized.txt`

### Step 3: Metadata Integration
- **Input**: Normalized data + sample metadata
- **Process**: Download and integrate sample metadata (rural/urban, timepoint, patient ID)
- **Output**: `output/3_metadata/genus_taxaAsColumnsLogNorm_WithMetadata.txt`

### Step 4: PCoA Analysis
- **Input**: Metadata-integrated normalized data
- **Process**: Principal Coordinate Analysis using Bray-Curtis dissimilarity
- **Analysis**: First timepoint, read 1 only (20 rural vs 20 urban samples)
- **Output**: `output/4_pcoa/` (PCoA coordinates, variance explained, distance matrix, plots)

### Step 5: Scientific Report
- **Input**: PCoA results and analysis outputs
- **Process**: Generate comprehensive scientific paper with methods and results
- **Output**: `output/5_report/Urban_Rural_Microbiome_Analysis_Report.pdf`

## 🔬 Analysis Results

### Data Summary
- **Source**: Urban vs Rural China 16S rRNA study
- **Final Analysis**: 40 samples (20 rural, 20 urban, first timepoint, read 1)
- **Features**: 347 bacterial genera
- **Sequencing Depth**: 42,728 - 140,895 reads per sample (avg: 72,601.56)

### Key Findings
- **PCo1**: 15.0% variance explained
- **PCo2**: 11.3% variance explained  
- **Combined**: 26.3% of total microbiome variation captured
- **First 5 axes**: 44.0% cumulative variance
- **Result**: Clear clustering differences between urban and rural gut microbiomes

## 📁 Directory Structure

```
OrchestraSimpleDemo/
├── README.md
├── normalize_data.py              # Step 2: Normalization
├── pcoa_analysis.py               # Step 4: PCoA Analysis  
├── generate_pdf_report.py         # Step 5: PDF Report Generation
├── orchestra_pipeline.py          # Complete pipeline manager
├── test/                          # Comprehensive test suite
└── output/                        # Analysis outputs (not in git)
    ├── 1_raw_data/                # Raw genus count data
    ├── 2_normalized/              # Log-normalized data
    ├── 3_metadata/                # Metadata-integrated data
    ├── 4_pcoa/                    # PCoA results, plots, distance matrix
    └── 5_report/                  # Scientific report (PDF + Markdown)
```

## 🚀 Running the Complete Pipeline

### Prerequisites
- Python 3.x with packages: pandas, numpy, scipy, matplotlib

### Option 1: Run Complete Pipeline
```bash
python orchestra_pipeline.py
```

### Option 2: Run Individual Steps
```bash
# Step 1: Download raw data
mkdir -p output/1_raw_data
curl -o output/1_raw_data/genus_taxaAsColumns.txt \
  "https://raw.githubusercontent.com/kwinglee/UrbanRuralChina/master/16SrRNA/inputData/RDP/genus_taxaAsColumns.txt"

# Step 2: Normalize data  
python normalize_data.py

# Step 3: Download metadata
mkdir -p output/3_metadata
curl -o output/3_metadata/genus_taxaAsColumnsLogNorm_WithMetadata.txt \
  "https://raw.githubusercontent.com/kwinglee/UrbanRuralChina/master/16SrRNA/inputData/RDP/genus_taxaAsColumnsLogNorm_WithMetadata.txt"

# Step 4: PCoA analysis
python pcoa_analysis.py

# Step 5: Generate PDF report
python generate_pdf_report.py
```

## 📈 Scientific Methods

### Data Normalization
Formula: `log₁₀(relativeAbundance × avgSequencingDepth + 1)`
- Accounts for varying sequencing depths
- Reduces impact of highly abundant taxa  
- Handles zero counts appropriately

### Principal Coordinate Analysis
- **Distance Metric**: Bray-Curtis dissimilarity
- **Sample Filtering**: First timepoint, read 1 only
- **Dimensionality**: 10 principal coordinate axes extracted
- **Variance Analysis**: Percent variance explained per axis

### Statistical Approach  
- Focus on technical replicate 1 eliminates sequencing artifacts
- Bray-Curtis appropriate for microbiome community data
- Log-normalization handles compositional data properties
- First timepoint analysis avoids temporal confounding

## 🎯 Key Features

### Orchestra Pattern Benefits
1. **Modularity**: Each step is independent and testable
2. **Reproducibility**: Complete data provenance tracking
3. **Scalability**: Easy to add new analysis steps
4. **Quality Control**: Comprehensive test suite included
5. **Documentation**: Scientific paper with full methods

### Analysis Innovations
- **Read-level filtering**: Eliminates technical replication effects  
- **Metadata integration**: Links biological and technical factors
- **Publication-quality output**: Professional PDF report with figures
- **Statistical rigor**: Proper variance partitioning and visualization

## 🧪 Testing

Comprehensive test suite validates:
- Raw data download integrity
- Normalization mathematical correctness  
- PCoA algorithm implementation
- Reference file validation
- End-to-end pipeline execution

```bash
cd test/
java -jar lib/junit-platform-console-standalone-1.10.2.jar --class-path . --scan-classpath
```

## 📚 Workshop Applications

This demonstrates:
- **Iterative Development**: Bug discovery and fixing through testing
- **Specialized Roles**: Different agents handling development vs testing
- **Quality Assurance**: Multiple validation layers
- **Scientific Rigor**: Publication-ready analysis and documentation
- **Reproducible Research**: Complete workflow documentation

Perfect for teaching:
- Bioinformatics pipeline development
- Statistical analysis of microbiome data
- Scientific writing and visualization
- Software testing and quality assurance
- Collaborative development practices

## 📖 Citations

- Data source: Urban Rural China 16S rRNA study (GitHub: kwinglee/UrbanRuralChina)
- Analysis framework: Orchestra Pattern Bioinformatics Pipeline  
- Repository: https://github.com/afodor/OrchestraSimpleDemo