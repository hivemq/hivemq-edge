package com.hivemq.api.errors.topicfilters;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class TopicFilterNotFoundError extends ProblemDetails {
    public TopicFilterNotFoundError(String error) {
        super(
                "TopicFilterNotFound",
                "TopicFilter not found",
                "No TopicFilter with the given id was found",
                HttpStatus.NOT_FOUND_404,
                List.of(new Error(error)));
    }
}
