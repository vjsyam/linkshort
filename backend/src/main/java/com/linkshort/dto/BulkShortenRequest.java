package com.linkshort.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for bulk URL shortening.
 */
public class BulkShortenRequest {

    @NotEmpty(message = "URL list cannot be empty")
    @Size(max = 20, message = "Maximum 20 URLs per bulk request")
    private List<String> urls;

    public BulkShortenRequest() {}

    public List<String> getUrls() { return urls; }
    public void setUrls(List<String> urls) { this.urls = urls; }
}
