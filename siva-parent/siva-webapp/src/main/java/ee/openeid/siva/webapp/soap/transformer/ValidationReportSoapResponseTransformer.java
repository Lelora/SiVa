/*
 * Copyright 2019 Riigi Infosüsteemide Amet
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package ee.openeid.siva.webapp.soap.transformer;

import ee.openeid.siva.validation.document.report.DetailedReport;
import ee.openeid.siva.validation.document.report.DiagnosticReport;
import ee.openeid.siva.validation.document.report.SimpleReport;
import ee.openeid.siva.validation.document.report.TimeStampTokenValidationData;
import ee.openeid.siva.validation.document.report.ValidatedDocument;
import ee.openeid.siva.webapp.soap.response.Error;
import ee.openeid.siva.webapp.soap.response.Indication;
import ee.openeid.siva.webapp.soap.response.Info;
import ee.openeid.siva.webapp.soap.response.Policy;
import ee.openeid.siva.webapp.soap.response.SignatureScope;
import ee.openeid.siva.webapp.soap.response.SignatureValidationData;
import ee.openeid.siva.webapp.soap.response.SubjectDistinguishedName;
import ee.openeid.siva.webapp.soap.response.TimeStampTokenData;
import ee.openeid.siva.webapp.soap.response.ValidatedDocumentData;
import ee.openeid.siva.webapp.soap.response.ValidationConclusion;
import ee.openeid.siva.webapp.soap.response.ValidationReport;
import ee.openeid.siva.webapp.soap.response.ValidationWarning;
import ee.openeid.siva.webapp.soap.response.Warning;
import ee.openeid.siva.webapp.soap.transformer.report.DetailedReportTransformer;
import ee.openeid.siva.webapp.soap.transformer.report.DiagnosticDataTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationReportSoapResponseTransformer {

    private static final DetailedReportTransformer DETAILED_REPORT_TRANSFORMER = new DetailedReportTransformer();
    private static final DiagnosticDataTransformer DIAGNOSTIC_DATA_TRANSFORMER = new DiagnosticDataTransformer();

    private static Policy toSoapResponsePolicy(ee.openeid.siva.validation.document.report.Policy policy) {
        Policy responsePolicy = new Policy();
        responsePolicy.setPolicyDescription(policy.getPolicyDescription());
        responsePolicy.setPolicyName(policy.getPolicyName());
        responsePolicy.setPolicyUrl(policy.getPolicyUrl());
        return responsePolicy;
    }

    private static Info toSoapResponseSignatureInfo(ee.openeid.siva.validation.document.report.Info signatureInfo) {
        if (signatureInfo == null)
            return null;
        Info responseSignatureInfo = new Info();
        responseSignatureInfo.setBestSignatureTime(signatureInfo.getBestSignatureTime());
        return responseSignatureInfo;
    }

    public ValidationReport toSoapResponse(SimpleReport report) {
        ValidationReport validationReport = new ValidationReport();
        ValidationConclusion responseValidationConclusion = toSoapValidationConclusion(report.getValidationConclusion());
        validationReport.setValidationConclusion(responseValidationConclusion);

        if (report instanceof DetailedReport) {
            validationReport.setValidationProcess(DETAILED_REPORT_TRANSFORMER.transform(((DetailedReport) report).getValidationProcess()));

        } else if (report instanceof DiagnosticReport) {
            validationReport.setDiagnosticData(DIAGNOSTIC_DATA_TRANSFORMER.transform(((DiagnosticReport) report).getDiagnosticData()));
        }
        return validationReport;
    }

    private ValidationConclusion toSoapValidationConclusion(ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion) {
        ValidationConclusion responseValidationConclusion = new ValidationConclusion();
        responseValidationConclusion.setSignatureForm(validationConclusion.getSignatureForm());
        responseValidationConclusion.setPolicy(toSoapResponsePolicy(validationConclusion.getPolicy()));
        responseValidationConclusion.setValidationLevel(validationConclusion.getValidationLevel());
        if (validationConclusion.getValidatedDocument() != null)
            responseValidationConclusion.setValidatedDocument(toSoapValidatedDocument(validationConclusion.getValidatedDocument()));
        responseValidationConclusion.setSignaturesCount(validationConclusion.getSignaturesCount());
        if (validationConclusion.getSignatures() != null)
            responseValidationConclusion.setSignatures(toSoapResponseSignatures(validationConclusion.getSignatures()));
        if (validationConclusion.getValidationWarnings() != null)
            responseValidationConclusion.setValidationWarnings(toSoapResponseValidationWarnings(validationConclusion.getValidationWarnings()));

        responseValidationConclusion.setValidSignaturesCount(validationConclusion.getValidSignaturesCount());
        responseValidationConclusion.setValidationTime(validationConclusion.getValidationTime());
        if (validationConclusion.getTimeStampTokens() != null)
            responseValidationConclusion.setTimeStampTokens(toSoapResponseResponseTimeStamps(validationConclusion.getTimeStampTokens()));
        return responseValidationConclusion;
    }

    private ValidatedDocumentData toSoapValidatedDocument(ValidatedDocument validatedDocument) {
        ValidatedDocumentData validatedDocumentData = new ValidatedDocumentData();
        validatedDocumentData.setFilename(validatedDocument.getFilename());
        validatedDocumentData.setFileHash(validatedDocument.getFileHash());
        validatedDocumentData.setHashAlgo(validatedDocument.getHashAlgo());
        return validatedDocumentData;

    }

    private ValidationConclusion.ValidationWarnings toSoapResponseValidationWarnings(List<ee.openeid.siva.validation.document.report.ValidationWarning> validationWarnings) {
        ValidationConclusion.ValidationWarnings responseValidationWarnings = new ValidationConclusion.ValidationWarnings();
        validationWarnings.stream()
                .map(this::mapValidationWarning)
                .forEach(validationWarning -> responseValidationWarnings.getValidationWarning().add(validationWarning));
        return responseValidationWarnings;
    }

    private ValidationWarning mapValidationWarning(ee.openeid.siva.validation.document.report.ValidationWarning validationWarning) {
        ValidationWarning responseValidationWarning = new ValidationWarning();
        responseValidationWarning.setContent(validationWarning.getContent());
        return responseValidationWarning;
    }

    private ValidationConclusion.TimeStampTokens toSoapResponseResponseTimeStamps(List<TimeStampTokenValidationData> timeStampTokenValidationDataList) {
        ValidationConclusion.TimeStampTokens responseTimeStamps = new ValidationConclusion.TimeStampTokens();

        timeStampTokenValidationDataList.stream()
                .map(this::getTimeStampTokenData)
                .forEach(tst -> responseTimeStamps.getTimeStampToken().add(tst));
        return responseTimeStamps;
    }

    private ValidationConclusion.Signatures toSoapResponseSignatures(List<ee.openeid.siva.validation.document.report.SignatureValidationData> signatures) {
        ValidationConclusion.Signatures responseSignatures = new ValidationConclusion.Signatures();

        for (ee.openeid.siva.validation.document.report.SignatureValidationData signature : signatures) {
            SignatureValidationData responseSignature = getSignatureValidationData(signature);
            responseSignatures.getSignature().add(responseSignature);
        }

        return responseSignatures;
    }

    private TimeStampTokenData getTimeStampTokenData(TimeStampTokenValidationData timeStampTokenValidationData) {
        TimeStampTokenData timeStampTokenData = new TimeStampTokenData();
        timeStampTokenData.setIndication(Indication.valueOf(timeStampTokenValidationData.getIndication().name()));
        timeStampTokenData.setSignedBy(timeStampTokenValidationData.getSignedBy());
        timeStampTokenData.setSignedTime(timeStampTokenValidationData.getSignedTime());
        if (timeStampTokenValidationData.getError() != null)
            timeStampTokenData.setErrors(toSoapResponseTimeStampsErrors(timeStampTokenValidationData.getError()));
        return timeStampTokenData;
    }

    private SignatureValidationData getSignatureValidationData(ee.openeid.siva.validation.document.report.SignatureValidationData signature) {
        SignatureValidationData responseSignature = new SignatureValidationData();
        responseSignature.setId(signature.getId());
        responseSignature.setClaimedSigningTime(signature.getClaimedSigningTime());
        responseSignature.setSignatureFormat(signature.getSignatureFormat());
        responseSignature.setSignatureLevel(signature.getSignatureLevel());
        responseSignature.setSignedBy(signature.getSignedBy());
        responseSignature.setSubjectDistinguishedName(toSoapResponseSignatureSubjectDN(signature.getSubjectDistinguishedName()));
        responseSignature.setIndication(Indication.fromValue(signature.getIndication()));
        responseSignature.setSubIndication(signature.getSubIndication());
        responseSignature.setInfo(toSoapResponseSignatureInfo(signature.getInfo()));
        responseSignature.setErrors(toSoapResponseSignatureErrors(signature.getErrors()));
        responseSignature.setWarnings(toSoapResponseSignatureWarnings(signature.getWarnings()));
        responseSignature.setSignatureScopes(toSoapResponseSignatureScopes(signature.getSignatureScopes()));

        return responseSignature;
    }

    private SubjectDistinguishedName toSoapResponseSignatureSubjectDN(ee.openeid.siva.validation.document.report.SubjectDistinguishedName subjectDistinguishedName) {
        if (subjectDistinguishedName == null) {
            return null;
        }

        SubjectDistinguishedName responseSubjectDN = new SubjectDistinguishedName();
        responseSubjectDN.setCommonName(subjectDistinguishedName.getCommonName());
        responseSubjectDN.setSerialNumber(subjectDistinguishedName.getSerialNumber());
        return responseSubjectDN;
    }

    private TimeStampTokenData.Errors toSoapResponseTimeStampsErrors(List<ee.openeid.siva.validation.document.report.Error> timeStampsErrors) {
        TimeStampTokenData.Errors responseTimeStampsErrors = new TimeStampTokenData.Errors();

        for (ee.openeid.siva.validation.document.report.Error timeStampError : timeStampsErrors) {
            Error responseTimeStampError = new Error();
            responseTimeStampError.setContent(timeStampError.getContent());
            responseTimeStampsErrors.getError().add(responseTimeStampError);
        }
        return responseTimeStampsErrors;
    }

    private SignatureValidationData.Errors toSoapResponseSignatureErrors(List<ee.openeid.siva.validation.document.report.Error> signatureErrors) {
        SignatureValidationData.Errors responseSignatureErrors = new SignatureValidationData.Errors();

        for (ee.openeid.siva.validation.document.report.Error signatureError : signatureErrors) {
            Error responseSignatureError = new Error();
            responseSignatureError.setContent(signatureError.getContent());

            responseSignatureErrors.getError().add(responseSignatureError);
        }

        return responseSignatureErrors;
    }

    private SignatureValidationData.Warnings toSoapResponseSignatureWarnings(List<ee.openeid.siva.validation.document.report.Warning> signatureWarnings) {
        SignatureValidationData.Warnings responseSignatureWarnings = new SignatureValidationData.Warnings();

        for (ee.openeid.siva.validation.document.report.Warning signatureWarning : signatureWarnings) {
            Warning responseSignatureWarning = new Warning();
            responseSignatureWarning.setContent(signatureWarning.getContent());
            responseSignatureWarnings.getWarning().add(responseSignatureWarning);
        }

        return responseSignatureWarnings;
    }

    private SignatureValidationData.SignatureScopes toSoapResponseSignatureScopes(List<ee.openeid.siva.validation.document.report.SignatureScope> signatureScopes) {
        if (signatureScopes == null) {
            return null;
        }

        SignatureValidationData.SignatureScopes responseSignatureScopes = new SignatureValidationData.SignatureScopes();

        for (ee.openeid.siva.validation.document.report.SignatureScope signatureScope : signatureScopes) {
            SignatureScope responseSignatureScope = new SignatureScope();
            responseSignatureScope.setContent(signatureScope.getContent());
            responseSignatureScope.setName(signatureScope.getName());
            responseSignatureScope.setScope(signatureScope.getScope());
            responseSignatureScope.setHashAlgo(signatureScope.getHashAlgo());
            responseSignatureScope.setHash(signatureScope.getHash());
            responseSignatureScopes.getSignatureScope().add(responseSignatureScope);
        }

        return responseSignatureScopes;
    }
}
