package kz.nsanzhar.ENThinker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RagResponse {
    private String answer;
    private List<SourceInfo> sources;
    private int totalSourcesFound;

    @Data
    @AllArgsConstructor
    public static class SourceInfo {
        private String text;
        private String subject;
        private double score;
        private String id;
    }
}
