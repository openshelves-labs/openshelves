package com.openshelves.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookMetadata {

    private Long id;


    // Details
    private String title;
    private String subtitle;
    private String description;
    private List<String> authors;
    private String language;
    private String coverImageUrl;
    private Integer pageCount;

    // Publication
    private String publisher;
    private Integer publicationYear;

    // Identifiers
    private String isbn10;
    private String isbn13;
    private String asin;
    private String olid;

    // Classification
    private String deweyDecimal;
    private String lcClassification;
}
