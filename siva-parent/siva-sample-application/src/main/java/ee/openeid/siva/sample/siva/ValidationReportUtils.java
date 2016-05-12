package ee.openeid.siva.sample.siva;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidationReportUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationReportUtils.class);
    private static final String INVALID_CONTAINER = "INVALID";

    private ValidationReportUtils() {
    }

    public static String getValidateFilename(final String reportJSON) {
        try {
            final String documentName = JsonPath.read(reportJSON, "$.documentName");
            return documentName == null ? "" : documentName;
        } catch (final PathNotFoundException ex) {
            LOGGER.warn("documentName not present in JSON: ", ex);
            return "";
        }
    }

    public static String getOverallValidationResult(final String reportJSON) {
        try {
            final Integer validSignatureCount = JsonPath.read(reportJSON, "$.validSignaturesCount");
            final Integer totalSignatureCount = JsonPath.read(reportJSON, "$.signaturesCount");
            if (validSignatureCount == null || totalSignatureCount == null) {
                return "INVALID";
            }

            return validSignatureCount.equals(totalSignatureCount) && totalSignatureCount > 0 ?  "VALID" : INVALID_CONTAINER;
        } catch (final PathNotFoundException ex) {
            LOGGER.warn("JSON parsing failed when validating overall validation result: ", ex);
            return INVALID_CONTAINER;
        }
    }
}
