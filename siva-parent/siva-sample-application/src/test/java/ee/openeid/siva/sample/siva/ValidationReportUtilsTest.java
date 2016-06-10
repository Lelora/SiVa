package ee.openeid.siva.sample.siva;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationReportUtilsTest {

    @Test
    public void testConstructorIsPrivate() throws Exception {
        final Constructor<ValidationReportUtils> constructor = ValidationReportUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void documentNameJsonKeyPresentReturnsDocumentNameValue() throws Exception {
        final String json = "{\"documentName\":\"valid_value.bdoc\"}";
        assertEquals("valid_value.bdoc", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void documentNameJsonKeyNotPresentReturnsEmptyString() throws Exception {
        final String json = "{\"randomKey\": \"randomValue\"}";
        assertEquals("", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void documentNameJsonKeyIsNullReturnsEmptyString() throws Exception {
        final String json = "{\"documentName\": null}";
        assertEquals("", ValidationReportUtils.getValidateFilename(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysArePresentReturnsValid() throws Exception {
        final String json = "{\"validSignaturesCount\": 1, \"signaturesCount\": 1}";
        assertEquals("VALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysPresentReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": 0, \"signaturesCount\": 1}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredJsonKeysWithValuesZeroReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": 0, \"signaturesCount\": 0}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredKeysPresentValuesNullReturnsInvalid() throws Exception {
        final String json = "{\"validSignaturesCount\": null, \"signaturesCount\": null}";
        assertEquals("ERROR", ValidationReportUtils.getOverallValidationResult(json));
    }

    @Test
    public void overallValidationRequiredKeysNotPresentReturnsInvalid() throws Exception {
        final String json = "{\"randomKey\": \"randomValue\"}";
        assertEquals("INVALID", ValidationReportUtils.getOverallValidationResult(json));
    }
}