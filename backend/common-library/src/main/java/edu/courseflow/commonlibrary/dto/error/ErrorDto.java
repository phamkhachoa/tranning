package edu.courseflow.commonlibrary.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC-7807-flavoured error body returned by every service. Stable shape so that all clients
 * (React, Next.js, Flutter) can parse failures the same way.
 *
 * TODO(training-day-18-impl): Treat this record as the shared API error contract.
 * Step 1: Keep field names stable across services; client apps should not parse exception text.
 * Step 2: Prefer stable errorCode values for important business errors such as duplicate slug,
 *         invalid transition, forbidden resource access and rate limit.
 * Step 3: If changing this shape, document the migration and check admin web, learner web and mobile app.
 */
public record ErrorDto(String statusCode,
                       String title,
                       String detail,
                       @JsonInclude(JsonInclude.Include.NON_NULL) String errorCode,
                       List<String> fieldErrors) {

    public ErrorDto {
        if (fieldErrors == null) {
            fieldErrors = new ArrayList<>();
        }
    }

    public ErrorDto(String statusCode, String title, String detail) {
        this(statusCode, title, detail, null, new ArrayList<>());
    }

    public ErrorDto(String statusCode, String title, String detail, List<String> fieldErrors) {
        this(statusCode, title, detail, null, fieldErrors);
    }
}
