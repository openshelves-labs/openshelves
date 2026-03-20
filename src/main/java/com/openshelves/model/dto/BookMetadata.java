package com.openshelves.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookMetadata {

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
