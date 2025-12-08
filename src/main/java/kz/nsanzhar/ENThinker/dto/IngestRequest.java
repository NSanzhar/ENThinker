package kz.nsanzhar.ENThinker.dto;

import lombok.Data;

@Data
public class IngestRequest {
    public String text;
    public String subject; // может быть null
}