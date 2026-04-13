#!/usr/bin/env python3
"""
Orchestra Pattern Step 2: Data Normalization (FIXED VERSION)
Normalizes genus count data using the formula:
normalizedData = log10(relativeAbundance * avgSequencingDepth + 1)
"""

import pandas as pd
import numpy as np
import os

def normalize_genus_data(input_file, output_file):
    """
    Normalize genus count data according to the specified formula
    """
    print(f"Reading data from: {input_file}")

    # Read the data properly - the first column is row numbers, second is sample names
    df = pd.read_csv(input_file, sep='\t', index_col=0)

    print(f"Data shape: {df.shape}")
    print(f"Number of samples: {df.shape[0]}")
    print(f"Number of genera: {df.shape[1]}")  # All columns are genera

    # Properly separate sample names and count data
    sample_names = df.index  # Sample names are in the index
    count_data = df.astype(float)  # ALL columns are genus counts (no sample column to skip!)

    print(f"Sample names (first 5): {sample_names[:5].tolist()}")
    print(f"Count data shape: {count_data.shape}")

    # Calculate total reads per sample (sequencing depth)
    sequencing_depths = count_data.sum(axis=1)
    print(f"Sequencing depth range: {sequencing_depths.min():.0f} - {sequencing_depths.max():.0f}")

    # Calculate average sequencing depth across all samples
    avg_sequencing_depth = sequencing_depths.mean()
    print(f"Average sequencing depth: {avg_sequencing_depth:.2f}")

    # Calculate relative abundance (count / total_reads_per_sample)
    relative_abundance = count_data.div(sequencing_depths, axis=0)

    # Apply normalization formula: log10(relativeAbundance * avgSequencingDepth + 1)
    normalized_data = np.log10(relative_abundance * avg_sequencing_depth + 1)

    # Create result DataFrame - sample names are already in the index
    result = normalized_data.copy()
    # No need to insert sample column - it's already in the index!

    print(f"Saving normalized data to: {output_file}")
    result.to_csv(output_file, sep='\t', index=True)

    print("Normalization completed successfully!")
    print(f"Normalized values range: {normalized_data.min().min():.4f} - {normalized_data.max().max():.4f}")

    return result

if __name__ == "__main__":
    # File paths
    input_file = "output/1_raw_data/genus_taxaAsColumns.txt"
    output_file = "output/2_normalized/genus_taxaAsColumns_normalized.txt"

    # Create output directory if it doesn't exist
    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    # Perform normalization
    normalized_df = normalize_genus_data(input_file, output_file)

    print(f"\nFirst few rows of normalized data:")
    print(normalized_df.head())