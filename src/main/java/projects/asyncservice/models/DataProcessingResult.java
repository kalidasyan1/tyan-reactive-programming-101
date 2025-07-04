package projects.asyncservice.models;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Simplified result of processing operation
 * Contains the essential processed data and metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataProcessingResult {
    private String processedData;
    private String message;
    private long timestamp;
    private int complexity;
}
