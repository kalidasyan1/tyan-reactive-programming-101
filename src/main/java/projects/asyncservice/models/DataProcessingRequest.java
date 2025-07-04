package projects.asyncservice.models;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for processing tasks
 * Contains data to be processed and complexity indicator
 */
@Data
@NoArgsConstructor
public class DataProcessingRequest {
    private String data;
    private int complexity = 1; // 1-10 scale, defaults to simple

    public DataProcessingRequest(String data, int complexity) {
        this.data = data;
        this.complexity = Math.max(1, Math.min(10, complexity)); // Clamp between 1-10
    }

    public void setComplexity(int complexity) {
        this.complexity = Math.max(1, Math.min(10, complexity)); // Clamp between 1-10
    }
}
