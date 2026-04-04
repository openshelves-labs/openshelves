package com.openshelves.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookSummary {

    private Long id;
    private String title;
    private String subtitle;
    private List<String> authors;
}
