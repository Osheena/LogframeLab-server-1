package com.arqaam.logframelab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MLSimilarIndicatorRequest {
    private String indicator;
    private Double threshold;
}
