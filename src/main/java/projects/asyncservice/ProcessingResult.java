package projects.asyncservice;

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
public class ProcessingResult {
    private String processedData;
    private String message;
    private long timestamp;
    private int complexity;
}
