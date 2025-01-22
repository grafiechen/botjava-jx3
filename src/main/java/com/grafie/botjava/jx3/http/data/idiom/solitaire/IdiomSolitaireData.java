package com.grafie.botjava.jx3.http.data.idiom.solitaire;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 成语接龙
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class IdiomSolitaireData {
    @JsonProperty("question")
    private IdiomDetailData question;
    @JsonProperty("answer")
    private IdiomDetailData answer;
}

