package kz.nsanzhar.ENThinker.dto;

import lombok.Data;

@Data
public class AskRequest {
    private String question;
    private int topK;
}
