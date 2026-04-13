#!/usr/bin/env python3
"""
Orchestra Pattern Step 4: PCoA Analysis
Performs Principal Coordinate Analysis using Bray-Curtis dissimilarity
on normalized genus-level data
"""

import pandas as pd
import numpy as np
from scipy.spatial.distance import pdist, squareform
from scipy.linalg import eigh
import matplotlib.pyplot as plt
import seaborn as sns
import os

def bray_curtis_distance(u, v):
    """
    Calculate Bray-Curtis dissimilarity between two samples
    BC = sum(|u_i - v_i|) / sum(|u_i + v_i|)
    """
    numerator = np.sum(np.abs(u - v))
    denominator = np.sum(u + v)
    if denominator == 0:
        return 0
    return numerator / denominator

def perform_pcoa(data, n_axes=10):
    """
    Perform Principal Coordinate Analysis (PCoA) on the data
    """
    print("Calculating Bray-Curtis distance matrix...")

    # Calculate pairwise Bray-Curtis distances
    distances = pdist(data.values, metric=bray_curtis_distance)
    distance_matrix = squareform(distances)

    print("Performing PCoA decomposition...")

    # Convert distances to squared distances for PCoA
    squared_distances = distance_matrix ** 2

    # Double centering matrix
    n = squared_distances.shape[0]
    H = np.eye(n) - np.ones((n, n)) / n
    B = -0.5 * H @ squared_distances @ H

    # Eigenvalue decomposition
    eigenvalues, eigenvectors = eigh(B)

    # Sort by eigenvalues (descending)
    idx = np.argsort(eigenvalues)[::-1]
    eigenvalues = eigenvalues[idx]
    eigenvectors = eigenvectors[:, idx]

    # Take only positive eigenvalues
    positive_idx = eigenvalues > 1e-8
    eigenvalues = eigenvalues[positive_idx]
    eigenvectors = eigenvectors[:, positive_idx]

    # Calculate coordinates
    coordinates = eigenvectors[:, :n_axes] * np.sqrt(eigenvalues[:n_axes])

    # Calculate percent variance explained
    total_variance = np.sum(np.abs(eigenvalues))
    percent_explained = (eigenvalues / total_variance * 100)[:n_axes]

    return coordinates, percent_explained, distance_matrix

def create_pcoa_results(input_file, output_dir):
    """
    Main function to perform PCoA analysis and save results
    """
    print(f"Reading data from: {input_file}")

    # Read the metadata file without index_col to see actual structure
    df = pd.read_csv(input_file, sep='\t')

    # Set sample IDs as index
    df = df.set_index(df.columns[0])  # Use first column as index

    # Extract metadata columns (first 4 columns: readNumber, patientID, ruralUrban, timepoint)
    metadata_cols = df.columns[:4].tolist()  # readNumber, patientID, ruralUrban, timepoint
    metadata = df[metadata_cols].copy()

    # Extract genus data (all columns starting from column 4 - includes Pediococcus!)
    genus_data = df.iloc[:, 4:].astype(float)  # Start from Pediococcus (index 4)

    print(f"Data shape: {genus_data.shape}")
    print(f"Metadata shape: {metadata.shape}")

    # Perform PCoA
    coordinates, percent_explained, distance_matrix = perform_pcoa(genus_data, n_axes=10)

    # Create results dataframe
    pcoa_columns = [f'PCo{i+1}' for i in range(coordinates.shape[1])]
    pcoa_df = pd.DataFrame(coordinates,
                          index=genus_data.index,
                          columns=pcoa_columns)

    # Combine with metadata
    results_df = pd.concat([metadata, pcoa_df], axis=1)

    # Save results
    output_file = os.path.join(output_dir, 'pcoa_results_bray_curtis.txt')
    results_df.to_csv(output_file, sep='\t', index=True)

    # Save percent variance explained
    variance_file = os.path.join(output_dir, 'pcoa_variance_explained.txt')
    variance_df = pd.DataFrame({
        'Axis': [f'PCo{i+1}' for i in range(len(percent_explained))],
        'Percent_Variance': percent_explained,
        'Cumulative_Variance': np.cumsum(percent_explained)
    })
    variance_df.to_csv(variance_file, sep='\t', index=False)

    # Save distance matrix
    distance_file = os.path.join(output_dir, 'bray_curtis_distance_matrix.txt')
    distance_df = pd.DataFrame(distance_matrix,
                              index=genus_data.index,
                              columns=genus_data.index)
    distance_df.to_csv(distance_file, sep='\t', index=True)

    print(f"PCoA analysis completed successfully!")
    print(f"Results saved to: {output_file}")
    print(f"Variance explained saved to: {variance_file}")
    print(f"Distance matrix saved to: {distance_file}")

    # Print summary
    print(f"\nPCoA Summary:")
    print(f"Total samples analyzed: {coordinates.shape[0]}")
    print(f"First 5 axes explain: {np.sum(percent_explained[:5]):.1f}% of variance")
    print(f"PCo1: {percent_explained[0]:.1f}%")
    print(f"PCo2: {percent_explained[1]:.1f}%")

    return results_df, variance_df

def create_first_timepoint_plot(results_df, output_dir):
    """
    Create PCoA plot for first timepoint, read 1 only
    """
    # Filter for first timepoint AND read 1 only
    first_timepoint = results_df[
        (results_df['timepoint'] == 'first_A') &
        (results_df['readNumber'] == 1)
    ].copy()

    if len(first_timepoint) == 0:
        print("Warning: No samples found with timepoint 'first_A' and readNumber 1")
        return

    print(f"Creating plot for {len(first_timepoint)} first timepoint (read 1) samples")

    # Create the plot
    plt.figure(figsize=(10, 8))

    # Create scatter plot colored by rural/urban
    colors = {'rural': '#1E90FF', 'urban': '#DC143C'}  # Blue and red
    for location in first_timepoint['ruralUrban'].unique():
        subset = first_timepoint[first_timepoint['ruralUrban'] == location]
        plt.scatter(subset['PCo1'], subset['PCo2'],
                   label=location.capitalize(),
                   color=colors[location],
                   alpha=0.7, s=80)

    plt.xlabel('PCo1')
    plt.ylabel('PCo2')
    plt.title('Principal Coordinate Analysis (PCoA) - First Timepoint (Read 1)\nBray-Curtis Dissimilarity')
    plt.legend()
    plt.grid(True, alpha=0.3)

    # Save plot
    plot_file = os.path.join(output_dir, 'pcoa_first_timepoint_read1.png')
    plt.savefig(plot_file, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"First timepoint (read 1) plot saved to: {plot_file}")

    return first_timepoint

if __name__ == "__main__":
    # File paths
    input_file = "output/3_metadata/genus_taxaAsColumnsLogNorm_WithMetadata.txt"
    output_dir = "output/4_pcoa"

    # Create output directory if needed
    os.makedirs(output_dir, exist_ok=True)

    # Perform PCoA analysis
    results_df, variance_df = create_pcoa_results(input_file, output_dir)

    # Create first timepoint plot
    first_timepoint_data = create_first_timepoint_plot(results_df, output_dir)

    print(f"\nFirst timepoint summary:")
    print(f"Samples: {len(first_timepoint_data)}")
    print(f"Rural samples: {len(first_timepoint_data[first_timepoint_data['ruralUrban'] == 'rural'])}")
    print(f"Urban samples: {len(first_timepoint_data[first_timepoint_data['ruralUrban'] == 'urban'])}")