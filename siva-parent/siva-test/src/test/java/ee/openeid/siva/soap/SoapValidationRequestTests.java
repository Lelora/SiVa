package ee.openeid.siva.soap;

import com.sun.xml.internal.messaging.saaj.soap.Envelope;
import ee.openeid.siva.integrationtest.configuration.IntegrationTest;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class SoapValidationRequestTests extends SiVaSoapTests {

    @Before
    public void DirectoryBackToDefault() {
        setTestFilesDirectory(DEFAULT_TEST_FILES_DIRECTORY);
    }

    private static final String DEFAULT_TEST_FILES_DIRECTORY = "document_format_test_files/";

    private String testFilesDirectory = DEFAULT_TEST_FILES_DIRECTORY;

    public void setTestFilesDirectory(String testFilesDirectory) {
        this.testFilesDirectory = testFilesDirectory;
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-1
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Empty request body
     *
     * Expected Result: Error is returned stating mismatch with required elements
     *
     * File:
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestEmptyInputs() {
        post(validationRequestForDocumentExtended("", "", "", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(CLIENT_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is("Unmarshalling Error: cvc-enumeration-valid: Value '' is not facet-valid with respect to enumeration '[PDF, XROAD, BDOC, DDOC]'. It must be a value from the enumeration. "));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-2
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with not base64 string as document
     *
     * Expected Result: Error is returned stating encoding problem
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestNonBase64Input() {
        String encodedString = ",:";
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.ddoc", "DDOC", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-3
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Verification of wrong document type as input
     *
     * Expected Result: Correct error code is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestInvalidDocumentType() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "CDOC", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(CLIENT_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is("Unmarshalling Error: cvc-enumeration-valid: Value 'CDOC' is not facet-valid with respect to enumeration '[PDF, XROAD, BDOC, DDOC]'. It must be a value from the enumeration. "));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Verification of case insensitivity in document type
     *
     * Expected Result: Report is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test @Ignore //TODO: VAL-299
    public void validationRequestCaseChangeDocumentType() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "bdoC",""))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.DocumentName",Matchers.is("Valid_IDCard_MobID_signatures.bdoc"));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-5
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request has XML as document type (special case, XML is similar to ddoc and was a accepted document type in earlier versions)
     *
     * Expected Result: Error is given
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestXmlDocument() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "xml", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(CLIENT_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is("Unmarshalling Error: cvc-enumeration-valid: Value 'xml' is not facet-valid with respect to enumeration '[PDF, XROAD, BDOC, DDOC]'. It must be a value from the enumeration. "));

    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-6
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request has long filename field
     *
     * Expected Result: Report is returned with the same filename
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestLongFilename() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));

        String filename =
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong" +
                "FilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLongFilenameToBeInsertedLong";

        post(validationRequestForDocumentExtended(encodedString, filename, "BDOC",""))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.DocumentName",Matchers.is(filename));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-7
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Totally empty request body is sent
     *
     * Expected Result: Error is given
     *
     * File: None
     *
     ***/
    @Test
    public void validationRequestEmptyBody() {
        String emptyRequestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(emptyRequestBody)
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is(BODY_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-8
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with more parameters than expected is sent
     *
     * Expected Result: Error is given or extra parameters are ignored?
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestExtraKeyBetweenValues() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        String requestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <soap:ValidateDocument>\n" +
                "         <soap:ValidationRequest>\n" +
                "            <Document>" + encodedString + "</Document>\n" +
                "            <Filename>Valid_IDCard_MobID_signatures.bdoc</Filename>\n" +
                "            <DocumentType>BDOC</DocumentType>\n" +
                "            <DocumentVersion>V1.3</DocumentVersion>\n" +
                "         </soap:ValidationRequest>\n" +
                "      </soap:ValidateDocument>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(requestBody)
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(CLIENT_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is("Unmarshalling Error: cvc-complex-type.2.4.a: Invalid content was found starting with element 'DocumentVersion'. One of '{SignaturePolicy}' is expected. "));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-9
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with more parameters than expected is sent
     *
     * Expected Result: Error is given or extra parameters are ignored?
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestExtraKeyAtTheEnd() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        String requestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <soap:ValidateDocument>\n" +
                "         <soap:ValidationRequest>\n" +
                "            <Document>" + encodedString + "</Document>\n" +
                "            <Filename>Valid_IDCard_MobID_signatures.bdoc</Filename>\n" +
                "            <DocumentType>BDOC</DocumentType>\n" +
                "            <SignaturePolicy>EE</SignaturePolicy>\n" +
                "            <DocumentVersion>V1.3</DocumentVersion>\n" +
                "         </soap:ValidationRequest>\n" +
                "      </soap:ValidateDocument>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(requestBody)
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(CLIENT_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is("Unmarshalling Error: cvc-complex-type.2.4.d: Invalid content was found starting with element 'DocumentVersion'. No child element is expected at this point. "));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-10
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with special chars is sent
     *
     * Expected Result: Validation report is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestUnusualChars() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "ÕValid_IDCard_MobID_signatures.bdocÄÖÜ", "BDOC",""))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.DocumentName",Matchers.is("ÕValid_IDCard_MobID_signatures.bdocÄÖÜ"));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-11
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with no optional SignaturePolicy field
     *
     * Expected Result: Validation report is returned using default policy
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestNoPolicyKey() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        String requestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <soap:ValidateDocument>\n" +
                "         <soap:ValidationRequest>\n" +
                "            <Document>" + encodedString + "</Document>\n" +
                "            <Filename>Valid_IDCard_MobID_signatures.bdoc</Filename>\n" +
                "            <DocumentType>BDOC</DocumentType>\n" +
                "         </soap:ValidationRequest>\n" +
                "      </soap:ValidateDocument>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(requestBody)
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.ValidSignaturesCount", Matchers.is("2"));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-12
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request with XML expansion
     *
     * Expected Result: Error is returned and Entity is not handled
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestWithXmlExpansionAttack() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        String requestBody =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                        "<!ENTITY lol \"lol\">\n" +
                        "<!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
                        "<!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
                        "<!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
                        "<!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
                        "<!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
                        "<!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
                        "<!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
                        "<!ENTITY xxe \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
                        "]>" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <soap:ValidateDocument>\n" +
                "         <soap:ValidationRequest>\n" +
                "            <Document>" + encodedString + "</Document>\n" +
                "            <Filename>&xxe</Filename>\n" +
                "            <DocumentType>BDOC</DocumentType>\n" +
                "         </soap:ValidationRequest>\n" +
                "      </soap:ValidateDocument>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(requestBody)
                .then()
                .body("Envelope.Body.Fault.faultcode", Matchers.is("soap:Client"))
                .body("Envelope.Body.Fault.faultstring", Matchers.containsString("Error reading XMLStreamReader: Unrecognized XML directive; expected CDATA or comment"));
    }

    /***
     *
     * TestCaseID: Soap-ValidationRequest-13
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Request XML external entity attack
     *
     * Expected Result: Error message is returned and Doctype field is not handled
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestXmlEntityAttack() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        String requestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.webapp.siva.openeid.ee/\">\n" +
                "<!DOCTYPE SignaturePolicy PUBLIC \"-//VSR//PENTEST//EN\" \"http://localhost:1234/\">" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <soap:ValidateDocument>\n" +
                "         <soap:ValidationRequest>\n" +
                "            <Document>" + encodedString + "</Document>\n" +
                "            <Filename>Valid_IDCard_MobID_signatures.bdoc</Filename>\n" +
                "            <DocumentType>BDOC</DocumentType>\n" +
                "            <SigmaturePolicy>" +""+ "</SignaturePolicy>"+
                "         </soap:ValidationRequest>\n" +
                "      </soap:ValidateDocument>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        post(requestBody)
                .then()
                .body("Envelope.Body.Fault.faultcode", Matchers.is("soap:Client"))
                .body("Envelope.Body.Fault.faultstring", Matchers.containsString("Error reading XMLStreamReader: Unrecognized XML directive; expected CDATA or comment"));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-1
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Input random base64 string as document with bdoc document type
     *
     * Expected Result: Error is returned stating problem in document
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestRandomInputAsBdocDocument() {
        String encodedString = "ZCxTgQxDET7/lNizNZ4hrB1Ug8I0kKpVDkHEgWqNjcKFMD89LsIpdCkpUEsFBgAAAAAFAAUAPgIAAEM3AAAAAA==";
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "BDOC", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-2
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Verification of filename value (filename do not match the actual file)
     *
     * Expected Result: The same filename is returned as sent in the request
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void validationRequestWrongFilename() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "TotallyRandomFilename.exe", "BDOC",""))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.DocumentName",Matchers.is("TotallyRandomFilename.exe"));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-3
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (bdoc and pdf)
     *
     * Expected Result: Error is returned
     *
     * File: PdfValidSingleSignature.pdf
     *
     ***/
    @Test
    public void bdocValidationRequestNotMatchingDocumentTypeAndActualFilePdf() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("PdfValidSingleSignature.pdf"));
        post(validationRequestForDocumentExtended(encodedString, "PdfValidSingleSignature.pdf", "BDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (bdoc and ddoc)
     *
     * Expected Result: Error is returned
     *
     * File: igasugust1.3.ddoc
     *
     ***/
    @Test
    public void bdocValidationRequestNotMatchingDocumentTypeAndActualFileDdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("igasugust1.3.ddoc"));
        post(validationRequestForDocumentExtended(encodedString, "igasugust1.3.ddoc", "BDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-5
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Bdoc file, not existing value in signaturePolicy
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void bdocValidationRequestWrongSignaturePolicy() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "BDOC", "RUS"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.containsString("Invalid signature policy: RUS; Available abstractPolicies: [EE]"));
    }

    /***
     *
     * TestCaseID: Soap-BdocValidationRequest-6
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Bdoc file, policy fiels should be case insensitive
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test @Ignore //TODO: VAL-297
    public void bdocValidationRequestCaseInsensitivePolicy() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "BDOC", "ee"))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.ValidSignaturesCount", Matchers.is("3"));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-7
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (bdoc and xroad)
     *
     * Expected Result: Error is returned
     *
     * File: xroad-simple.asice
     *
     ***/
    @Test
    public void bdocValidationRequestNotMatchingDocumentTypeAndActualFileXroad() {
        setTestFilesDirectory("xroad/");
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("xroad-simple.asice"));
        post(validationRequestForDocumentExtended(encodedString, "xroad-simple.asice", "DDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-1
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Input random base64 string as document with ddoc document type
     *
     * Expected Result: Error is returned stating problem in document
     *
     * File:
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestRandomInputAsDdocDocument() {
        String encodedString = "ZCxTgQxDET7/lNizNZ4hrB1Ug8I0kKpVDkHEgWqNjcKFMD89LsIpdCkpUEsFBgAAAAAFAAUAPgIAAEM3AAAAAA==";
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.ddoc", "DDOC", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-2
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (ddoc and bdoc)
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test ////TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestNotMatchingDocumentTypeAndActualFileDdocBdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "DDOC", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-3
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (ddoc and bdoc)
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void ddocValidationRequestNotMatchingDocumentTypeAndActualFileBdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "DDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (ddoc and pdf)
     *
     * Expected Result: Error is returned
     *
     * File: PdfValidSingleSignature.pdf
     *
     ***/
    @Test
    public void ddocValidationRequestNotMatchingDocumentTypeAndActualFilePdf() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("PdfValidSingleSignature.pdf"));
        post(validationRequestForDocumentExtended(encodedString, "PdfValidSingleSignature.pdf", "DDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-5
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Ddoc file, not existing value in signaturePolicy
     *
     * Expected Result: DDOC do not support signature policy selection, value is ignored
     *
     * File: igasugust1.3.ddoc
     *
     ***/
    @Test
    public void ddocValidationRequestWrongSignaturePolicy() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("igasugust1.3.ddoc"));
        post(validationRequestForDocumentExtended(encodedString, "igasugust1.3.ddoc", "DDOC", "RUS"))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.ValidSignaturesCount", Matchers.is("3"));
    }

    /***
     *
     * TestCaseID: Soap-DdocValidationRequest-6
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (ddoc and xroad)
     *
     * Expected Result: Error is returned
     *
     * File: xroad-attachment.asice
     *
     ***/
    @Test
    public void ddocValidationRequestNotMatchingDocumentTypeAndActualFileXroad() {
        setTestFilesDirectory("xroad/");
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("xroad-attachment.asice"));
        post(validationRequestForDocumentExtended(encodedString, "xroad-attachment.asice", "DDOC", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-PdfValidationRequest-1
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Input random base64 string as document with pdf document type
     *
     * Expected Result: Error is returned stating problem in document
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void validationRequestRandomInputAsPdfDocument() {
        String encodedString = "ZCxTgQxDET7/lNizNZ4hrB1Ug8I0kKpVDkHEgWqNjcKFMD89LsIpdCkpUEsFBgAAAAAFAAUAPgIAAEM3AAAAAA==";
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.pdf", "PDF", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode",Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring",Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-PdfValidationRequest-2
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (pdf and bdoc)
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void pdfValidationRequestNotMatchingDocumentTypeAndActualFileBdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "PDF", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-PdfValidationRequest-3
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (pdf and ddoc)
     *
     * Expected Result: Error is returned
     *
     * File: igasugust1.3.ddoc
     *
     ***/
    @Test
    public void pdfValidationRequestNotMatchingDocumentTypeAndActualFileDdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("igasugust1.3.ddoc"));
        post(validationRequestForDocumentExtended(encodedString, "igasugust1.3.ddoc", "PDF", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-PdfValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (pdf and xroad)
     *
     * Expected Result: Error is returned
     *
     * File: xroad-batchsignature.asice
     *
     ***/
    @Test //TODO: When VAL-290 is fixed then HttpStatus needs to be changed to BAD_REQUEST
    public void pdfValidationRequestNotMatchingDocumentTypeAndActualFileXroad() {
        setTestFilesDirectory("xroad/");
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("xroad-batchsignature.asice"));
        post(validationRequestForDocumentExtended(encodedString, "xroad-batchsignature.asice", "PDF", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-PdfValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: PDF file, not existing value in signaturePolicy
     *
     * Expected Result: Error is returned
     *
     * File: PdfValidSingleSignature.pdf
     *
     ***/
    @Test
    public void pdfValidationRequestWrongSignaturePolicy() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("PdfValidSingleSignature.pdf"));
        post(validationRequestForDocumentExtended(encodedString, "PdfValidSingleSignature.pdf", "PDF", "RUS"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is("Invalid signature policy: RUS; Available abstractPolicies: [EE, EU]"));
    }

    /***
     *
     * TestCaseID: Soap-XroadValidationRequest-1
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: X-road file, not existing value in signaturePolicy
     *
     * Expected Result: X-Road do not support signature policy selection, value is ignored
     *
     * File: xroad-simple.asice
     *
     ***/
    @Test
    public void xroadValidationRequestWrongSignaturePolicy() {
        setTestFilesDirectory("xroad/");
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("xroad-simple.asice"));
        post(validationRequestForDocumentExtended(encodedString, "xroad-simple.asice", "XROAD", "RUS"))
                .then()
                .body("Envelope.Body.ValidateDocumentResponse.ValidationReport.ValidSignaturesCount", Matchers.is("1"));
    }

    /***
     *
     * TestCaseID: Soap-XroadValidationRequest-2
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (xroad and ddoc)
     *
     * Expected Result: Error is returned
     *
     * File: igasugust1.3.ddoc
     *
     ***/
    @Test
    public void xroadValidationRequestNotMatchingDocumentTypeAndActualFileDdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("igasugust1.3.ddoc"));
        post(validationRequestForDocumentExtended(encodedString, "igasugust1.3.ddoc", "XROAD", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-XroadValidationRequest-3
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (pdf and bdoc)
     *
     * Expected Result: Error is returned
     *
     * File: Valid_IDCard_MobID_signatures.bdoc
     *
     ***/
    @Test
    public void xroadValidationRequestNotMatchingDocumentTypeAndActualFileBdoc() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("Valid_IDCard_MobID_signatures.bdoc"));
        post(validationRequestForDocumentExtended(encodedString, "Valid_IDCard_MobID_signatures.bdoc", "XROAD", "EE"))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    /***
     *
     * TestCaseID: Soap-XroadValidationRequest-4
     *
     * TestType: Automated
     *
     * RequirementID: http://open-eid.github.io/SiVa/siva/interface_description/
     *
     * Title: Mismatch in documentType and actual document (bdoc and pdf)
     *
     * Expected Result: Error is returned
     *
     * File: PdfValidSingleSignature.pdf
     *
     ***/
    @Test
    public void xroadValidationRequestNotMatchingDocumentTypeAndActualFilePdf() {
        String encodedString = Base64.encodeBase64String(readFileFromTestResources("PdfValidSingleSignature.pdf"));
        post(validationRequestForDocumentExtended(encodedString, "PdfValidSingleSignature.pdf", "XROAD", ""))
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("Envelope.Body.Fault.faultcode", Matchers.is(SERVER_FAULT))
                .body("Envelope.Body.Fault.faultstring", Matchers.is(DOCUMENT_MALFORMED));
    }

    @Override
    protected String getTestFilesDirectory() {
        return testFilesDirectory;
    }
}
