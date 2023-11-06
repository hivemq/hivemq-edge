package com.hivemq.api.model.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class Payload {

    public enum ContentType {

        JSON ("application/json"),
        PLAIN_TEXT ("text/plain"),
        XML ("text/xml"),
        CSV ("text/csv");

        ContentType (final String contentType){
            this.contentType = contentType;
        }

        @JsonProperty("contentType")
        @Schema(description = "The official representation of the content type")
        final String contentType;

        public String getContentType() {
            return contentType;
        }
    }

    @JsonProperty("contentType")
    @Schema(description = "The content type of the payload that the event contains",
            required = true)
    private @NotNull ContentType contentType;

    @JsonProperty("content")
    @Schema(description = "The content of the payload encoded as a string")
    private @NotNull String content;

    public Payload(@JsonProperty("contentType") final ContentType contentType,
                   @JsonProperty("content") final String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }

    public static Payload from(ContentType contentType, String data){
        Preconditions.checkNotNull(contentType);
        Preconditions.checkNotNull(data);
        return new Payload(contentType, data);
    }

    public static Payload fromObject(ObjectMapper mapper,  Object data){
        try {
            Preconditions.checkNotNull(mapper);
            Preconditions.checkNotNull(data);
            return new Payload(ContentType.JSON, mapper.writeValueAsString(data));
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}

