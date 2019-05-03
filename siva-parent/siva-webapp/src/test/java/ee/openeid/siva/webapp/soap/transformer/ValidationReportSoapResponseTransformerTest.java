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
import ee.openeid.siva.validation.document.report.SignatureValidationData;
import ee.openeid.siva.validation.document.report.SimpleReport;
import ee.openeid.siva.validation.document.report.SubjectDistinguishedName;
import ee.openeid.siva.validation.document.report.TimeStampTokenValidationData;
import ee.openeid.siva.validation.document.report.ValidatedDocument;
import ee.openeid.siva.webapp.soap.response.DiagnosticData;
import ee.openeid.siva.webapp.soap.response.ValidationConclusion;
import ee.openeid.siva.webapp.soap.response.ValidationReport;
import eu.europa.esig.dss.jaxb.detailedreport.XmlTLAnalysis;
import eu.europa.esig.dss.jaxb.diagnostic.XmlBasicSignature;
import eu.europa.esig.dss.jaxb.diagnostic.XmlCertificate;
import eu.europa.esig.dss.jaxb.diagnostic.XmlChainItem;
import eu.europa.esig.dss.jaxb.diagnostic.XmlContainerInfo;
import eu.europa.esig.dss.jaxb.diagnostic.XmlDigestAlgoAndValue;
import eu.europa.esig.dss.jaxb.diagnostic.XmlDigestMatcher;
import eu.europa.esig.dss.jaxb.diagnostic.XmlManifestFile;
import eu.europa.esig.dss.jaxb.diagnostic.XmlPolicy;
import eu.europa.esig.dss.jaxb.diagnostic.XmlSignature;
import eu.europa.esig.dss.jaxb.diagnostic.XmlSignatureProductionPlace;
import eu.europa.esig.dss.jaxb.diagnostic.XmlSigningCertificate;
import eu.europa.esig.dss.jaxb.diagnostic.XmlStructuralValidation;
import eu.europa.esig.dss.jaxb.diagnostic.XmlTimestamp;
import eu.europa.esig.dss.jaxb.diagnostic.XmlTrustedList;
import eu.europa.esig.dss.validation.DigestMatcherType;
import eu.europa.esig.dss.validation.diagnostic.BasicSignature;
import eu.europa.esig.dss.validation.diagnostic.Certificate;
import eu.europa.esig.dss.validation.diagnostic.CertificateChain;
import eu.europa.esig.dss.validation.diagnostic.ContainerInfo;
import eu.europa.esig.dss.validation.diagnostic.DigestMatcher;
import eu.europa.esig.dss.validation.diagnostic.Signature;
import eu.europa.esig.dss.validation.diagnostic.SigningCertificate;
import eu.europa.esig.dss.validation.diagnostic.Timestamp;
import eu.europa.esig.dss.validation.diagnostic.TrustedList;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class ValidationReportSoapResponseTransformerTest {

    private ValidationReportSoapResponseTransformer transformer = new ValidationReportSoapResponseTransformer();

    @Test
    public void qualifiedSimpleReportIsCorrectlyTransformedToSoapResponseReport() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        SimpleReport simpleReport = new SimpleReport();
        simpleReport.setValidationConclusion(validationConclusion);

        ValidationReport soapValidationReport = transformer.toSoapResponse(simpleReport);
        assertValidationConclusion(validationConclusion, soapValidationReport.getValidationConclusion());
        assertNull(soapValidationReport.getValidationProcess());
        assertNull(soapValidationReport.getDiagnosticData());
    }

    @Test
    public void qualifiedDetailedReportIsCorrectlyTransformedToSoapResponseReport() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        eu.europa.esig.dss.jaxb.detailedreport.DetailedReport validationProcess = createMockedValidationProcess();
        DetailedReport detailedReport = new DetailedReport(validationConclusion, validationProcess);
        ValidationReport responseValidationReport = transformer.toSoapResponse(detailedReport);
        assertValidationConclusion(validationConclusion, responseValidationReport.getValidationConclusion());
        assertNull(responseValidationReport.getDiagnosticData());
        Assert.assertEquals("EE", responseValidationReport.getValidationProcess().getTLAnalysis().get(0).getCountryCode());
    }

    @Test
    public void qualifiedDiagnosticDataIsCorrectlyTransformedToSoapResponseReport() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData diagnosticData = createMockedDiagnosticData();
        DiagnosticReport diagnosticReport = new DiagnosticReport(validationConclusion, diagnosticData);

        ValidationReport responseValidationReport = transformer.toSoapResponse(diagnosticReport);
        assertValidationConclusion(validationConclusion, responseValidationReport.getValidationConclusion());
        assertDiagnosticData(diagnosticData, responseValidationReport.getDiagnosticData());
        assertNull(responseValidationReport.getValidationProcess());
    }

    @Test
    public void qualifiedDetailedReportIsNull() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        SimpleReport simpleReport = new SimpleReport(validationConclusion);
        ValidationReport responseValidationReport = transformer.toSoapResponse(simpleReport);
        ValidationConclusion responseValidationConclusion = responseValidationReport.getValidationConclusion();
        Assert.assertEquals(validationConclusion.getSignatures().get(0).getIndication(), responseValidationConclusion.getSignatures().getSignature().get(0).getIndication().value());
    }

    @Test
    public void detailedReportWithNullValidationProcess() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        DetailedReport detailedReport = new DetailedReport(validationConclusion, null);
        ValidationReport responseValidationReport = transformer.toSoapResponse(detailedReport);
        assertValidationConclusion(validationConclusion, responseValidationReport.getValidationConclusion());
        assertNull(responseValidationReport.getValidationProcess());
        assertNull(responseValidationReport.getDiagnosticData());
    }

    @Test
    public void diagnosticReportWithNullDiagnosticData() {
        ee.openeid.siva.validation.document.report.ValidationConclusion validationConclusion = createMockedValidationConclusion();
        DiagnosticReport diagnosticReport = new DiagnosticReport(validationConclusion, null);
        ValidationReport responseValidationReport = transformer.toSoapResponse(diagnosticReport);
        assertValidationConclusion(validationConclusion, responseValidationReport.getValidationConclusion());
        assertNull(responseValidationReport.getValidationProcess());
        assertNull(responseValidationReport.getDiagnosticData());
    }

    private void assertValidationConclusion(ee.openeid.siva.validation.document.report.ValidationConclusion dssValidationConclusion, ValidationConclusion soapValidationConclusion) {
        assertEquals(dssValidationConclusion.getValidatedDocument().getFilename(), soapValidationConclusion.getValidatedDocument().getFilename());
        assertEquals(dssValidationConclusion.getValidatedDocument().getFileHash(), soapValidationConclusion.getValidatedDocument().getFileHash());
        assertEquals(dssValidationConclusion.getValidatedDocument().getHashAlgo(), soapValidationConclusion.getValidatedDocument().getHashAlgo());
        assertEquals(dssValidationConclusion.getSignatureForm(), soapValidationConclusion.getSignatureForm());
        assertEquals(dssValidationConclusion.getValidationTime(), soapValidationConclusion.getValidationTime());
        assertEquals(dssValidationConclusion.getValidationWarnings().get(0).getContent(), soapValidationConclusion.getValidationWarnings().getValidationWarning().get(0).getContent());
        assertEquals(dssValidationConclusion.getSignaturesCount(), soapValidationConclusion.getSignaturesCount());
        assertEquals(dssValidationConclusion.getValidSignaturesCount(), soapValidationConclusion.getValidSignaturesCount());
        assertEquals(dssValidationConclusion.getPolicy().getPolicyDescription(), soapValidationConclusion.getPolicy().getPolicyDescription());
        assertEquals(dssValidationConclusion.getPolicy().getPolicyName(), soapValidationConclusion.getPolicy().getPolicyName());
        assertEquals(dssValidationConclusion.getPolicy().getPolicyUrl(), soapValidationConclusion.getPolicy().getPolicyUrl());

        assertEquals(dssValidationConclusion.getValidationLevel(), soapValidationConclusion.getValidationLevel());

        assertEquals(dssValidationConclusion.getTimeStampTokens().get(0).getIndication().name(), soapValidationConclusion.getTimeStampTokens().getTimeStampToken().get(0).getIndication().name());
        assertEquals(dssValidationConclusion.getTimeStampTokens().get(0).getError().get(0).getContent(), soapValidationConclusion.getTimeStampTokens().getTimeStampToken().get(0).getErrors().getError().get(0).getContent());
        assertEquals(dssValidationConclusion.getTimeStampTokens().get(0).getSignedBy(), soapValidationConclusion.getTimeStampTokens().getTimeStampToken().get(0).getSignedBy());
        assertEquals(dssValidationConclusion.getTimeStampTokens().get(0).getSignedTime(), soapValidationConclusion.getTimeStampTokens().getTimeStampToken().get(0).getSignedTime());

        SignatureValidationData dssSignature = dssValidationConclusion.getSignatures().get(0);
        ee.openeid.siva.webapp.soap.response.SignatureValidationData responseSignature = soapValidationConclusion.getSignatures().getSignature().get(0);
        assertEquals(dssSignature.getClaimedSigningTime(), responseSignature.getClaimedSigningTime());
        assertEquals(dssSignature.getId(), responseSignature.getId());
        assertEquals(dssSignature.getIndication(), responseSignature.getIndication().value());
        assertEquals(dssSignature.getSignatureFormat(), responseSignature.getSignatureFormat());
        assertEquals(dssSignature.getSignatureLevel(), responseSignature.getSignatureLevel());
        assertEquals(dssSignature.getSignedBy(), responseSignature.getSignedBy());
        assertEquals(dssSignature.getSubIndication(), responseSignature.getSubIndication());
        assertEquals(dssSignature.getInfo().getBestSignatureTime(), responseSignature.getInfo().getBestSignatureTime());
        assertEquals(dssSignature.getErrors().get(0).getContent(), responseSignature.getErrors().getError().get(0).getContent());
        assertEquals(dssSignature.getWarnings().get(0).getContent(), responseSignature.getWarnings().getWarning().get(0).getContent());
        assertEquals(dssSignature.getSignatureScopes().get(0).getContent(), responseSignature.getSignatureScopes().getSignatureScope().get(0).getContent());
        assertEquals(dssSignature.getSignatureScopes().get(0).getName(), responseSignature.getSignatureScopes().getSignatureScope().get(0).getName());
        assertEquals(dssSignature.getSignatureScopes().get(0).getScope(), responseSignature.getSignatureScopes().getSignatureScope().get(0).getScope());
        assertEquals(dssSignature.getSubjectDistinguishedName().getCommonName(), responseSignature.getSubjectDistinguishedName().getCommonName());
        assertEquals(dssSignature.getSubjectDistinguishedName().getSerialNumber(), responseSignature.getSubjectDistinguishedName().getSerialNumber());
    }

    private void assertDiagnosticData(eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData dssDiagnosticData, DiagnosticData soapDiagnosticData) {
        assertEquals(dssDiagnosticData.getDocumentName(), soapDiagnosticData.getDocumentName());
        assertDateEquals(dssDiagnosticData.getValidationDate(), soapDiagnosticData.getValidationDate());
        assertContainerInfo(dssDiagnosticData.getContainerInfo(), soapDiagnosticData.getContainerInfo());
        assertTrustedList(dssDiagnosticData.getListOfTrustedLists(), soapDiagnosticData.getListOfTrustedLists());

        for (int i = 0; i < dssDiagnosticData.getTrustedLists().size(); i++) {
            assertTrustedList(dssDiagnosticData.getTrustedLists().get(i), soapDiagnosticData.getTrustedLists().getTrustedList().get(i));
        }

        for (int i = 0; i < dssDiagnosticData.getSignatures().size(); i++) {
            assertSignature(dssDiagnosticData.getSignatures().get(i), soapDiagnosticData.getSignatures().getSignature().get(i));
        }

        for (int i = 0; i < dssDiagnosticData.getUsedCertificates().size(); i++) {
            assertUserCertificate(dssDiagnosticData.getUsedCertificates().get(i), soapDiagnosticData.getUsedCertificates().getCertificate().get(i));
        }
    }

    private void assertContainerInfo(XmlContainerInfo dssContainerInfo, ContainerInfo containerInfo) {
        assertEquals(dssContainerInfo.getContainerType(), containerInfo.getContainerType());
        assertEquals(dssContainerInfo.getMimeTypeContent(), containerInfo.getMimeTypeContent());
        assertEquals(dssContainerInfo.getZipComment(), containerInfo.getZipComment());

        for (int i = 0; i < dssContainerInfo.getContentFiles().size(); i++) {
            assertEquals(dssContainerInfo.getContentFiles().get(i), containerInfo.getContentFiles().getContentFile().get(i));
        }

        for (int i = 0; i < dssContainerInfo.getManifestFiles().size(); i++) {
            assertEquals(dssContainerInfo.getManifestFiles().get(i).getFilename(), containerInfo.getManifestFiles().getManifestFile().get(i).getFilename());
            assertEquals(dssContainerInfo.getManifestFiles().get(i).getSignatureFilename(), containerInfo.getManifestFiles().getManifestFile().get(i).getSignatureFilename());

            for (int j = 0; j < dssContainerInfo.getManifestFiles().get(i).getEntries().size(); j++) {
                assertEquals(dssContainerInfo.getManifestFiles().get(i).getEntries().get(j), containerInfo.getManifestFiles().getManifestFile().get(i).getEntries().getEntry().get(j));
            }
        }
    }

    private void assertTrustedList(XmlTrustedList dssTrustedList, TrustedList trustedList) {
        assertEquals(dssTrustedList.getCountryCode(), trustedList.getCountryCode());
        assertDateEquals(dssTrustedList.getIssueDate(), trustedList.getIssueDate());
        assertDateEquals(dssTrustedList.getLastLoading(), trustedList.getLastLoading());
        assertDateEquals(dssTrustedList.getNextUpdate(), trustedList.getNextUpdate());
        assertEquals(dssTrustedList.getSequenceNumber(), trustedList.getSequenceNumber());
        assertEquals(dssTrustedList.getUrl(), trustedList.getUrl());
        assertEquals(dssTrustedList.getVersion(), trustedList.getVersion());
    }

    private void assertSignature(XmlSignature dssSignature, Signature signature) {
        assertEquals(dssSignature.getId(), signature.getId());
        assertEquals(dssSignature.getParentId(), signature.getParentId());
        assertEquals(dssSignature.getErrorMessage(), signature.getErrorMessage());
        assertEquals(dssSignature.getContentHints(), signature.getContentHints());
        assertEquals(dssSignature.getContentIdentifier(), signature.getContentIdentifier());
        assertEquals(dssSignature.getContentType(), signature.getContentType());
        assertEquals(dssSignature.getSignatureFilename(), signature.getSignatureFilename());
        assertEquals(dssSignature.getSignatureFormat(), signature.getSignatureFormat());
        assertEquals(dssSignature.getPolicy().getId(), signature.getPolicy().getId());
        assertEquals(dssSignature.getPolicy().getNotice(), signature.getPolicy().getNotice());
        assertEquals(dssSignature.getPolicy().getProcessingError(), signature.getPolicy().getProcessingError());
        assertEquals(dssSignature.getPolicy().getUrl(), signature.getPolicy().getUrl());
        assertEquals(dssSignature.getPolicy().getDigestAlgoAndValue().getDigestValue(), signature.getPolicy().getDigestAlgoAndValue().getDigestValue());
        assertEquals(dssSignature.getPolicy().getDigestAlgoAndValue().getDigestMethod(), signature.getPolicy().getDigestAlgoAndValue().getDigestMethod());
        assertEquals(dssSignature.getPolicy().isAsn1Processable(), signature.getPolicy().isAsn1Processable());
        assertEquals(dssSignature.getPolicy().isDigestAlgorithmsEqual(), signature.getPolicy().isDigestAlgorithmsEqual());
        assertEquals(dssSignature.getPolicy().isIdentified(), signature.getPolicy().isIdentified());
        assertEquals(dssSignature.getPolicy().isStatus(), signature.getPolicy().isStatus());
        assertEquals(dssSignature.getStructuralValidation().getMessage(), signature.getStructuralValidation().getMessage());
        assertEquals(dssSignature.getStructuralValidation().isValid(), signature.getStructuralValidation().isValid());
        assertEquals(dssSignature.getSignatureProductionPlace().getAddress(), signature.getSignatureProductionPlace().getAddress());
        assertEquals(dssSignature.getSignatureProductionPlace().getCity(), signature.getSignatureProductionPlace().getCity());
        assertEquals(dssSignature.getSignatureProductionPlace().getCountryName(), signature.getSignatureProductionPlace().getCountryName());
        assertEquals(dssSignature.getSignatureProductionPlace().getPostalCode(), signature.getSignatureProductionPlace().getPostalCode());
        assertEquals(dssSignature.getSignatureProductionPlace().getStateOrProvince(), signature.getSignatureProductionPlace().getStateOrProvince());
        assertEquals(dssSignature.isCounterSignature(), signature.isCounterSignature());
        assertEquals(dssSignature.getClaimedRoles(), signature.getClaimedRoles().getClaimedRoles());
        assertEquals(dssSignature.getCommitmentTypeIndication(), signature.getCommitmentTypeIndication().getIndication());

        assertDateEquals(dssSignature.getDateTime(), signature.getDateTime());

        for (int i = 0; i < dssSignature.getCertificateChain().size(); i++) {
            assertCertificateChainItem(dssSignature.getCertificateChain().get(i), signature.getCertificateChain().getChainItem().get(i));
        }

        for (int i = 0; i < dssSignature.getTimestamps().size(); i++) {
            assertTimestamp(dssSignature.getTimestamps().get(i), signature.getTimestamps().getTimestamp().get(i));
        }

        assertBasicSignature(dssSignature.getBasicSignature(), signature.getBasicSignature());
        assertSigningCertificate(dssSignature.getSigningCertificate(), signature.getSigningCertificate());
    }

    private void assertTimestamp(XmlTimestamp dssXmlTimestamp, Timestamp timestamp) {
        assertEquals(dssXmlTimestamp.getId(), timestamp.getId());
        assertEquals(dssXmlTimestamp.getType(), timestamp.getType());
        assertEquals(dssXmlTimestamp.getBase64Encoded(), timestamp.getBase64Encoded());

        for (int i = 0; i < dssXmlTimestamp.getCertificateChain().size(); i++) {
            assertEquals(dssXmlTimestamp.getCertificateChain().get(i).getId(), timestamp.getCertificateChain().getChainItem().get(i).getId());
            assertEquals(dssXmlTimestamp.getCertificateChain().get(i).getSource(), timestamp.getCertificateChain().getChainItem().get(i).getSource());
        }

        assertDigestMatcher(dssXmlTimestamp.getDigestMatcher(), timestamp.getDigestMatcher());
        assertBasicSignature(dssXmlTimestamp.getBasicSignature(), timestamp.getBasicSignature());
        assertSigningCertificate(dssXmlTimestamp.getSigningCertificate(), timestamp.getSigningCertificate());
    }

    private void assertDigestMatcher(XmlDigestMatcher dssDigestMatcher, DigestMatcher digestMatcher) {
        assertEquals(dssDigestMatcher.getName(), digestMatcher.getName());
        assertEquals(dssDigestMatcher.getDigestMethod(), digestMatcher.getDigestMethod());
        assertEquals(dssDigestMatcher.getDigestValue(), digestMatcher.getDigestValue());
        assertEquals(dssDigestMatcher.getType().name(), digestMatcher.getType().name());
        assertSame(dssDigestMatcher.isDataFound(), digestMatcher.isDataFound());
        assertSame(dssDigestMatcher.isDataIntact(), digestMatcher.isDataIntact());
    }

    private void assertCertificateChainItem(XmlChainItem dssChainItem, CertificateChain.ChainItem chainItem) {
        assertEquals(dssChainItem.getId(), chainItem.getId());
        assertEquals(dssChainItem.getSource(), chainItem.getSource());
    }

    private void assertBasicSignature(XmlBasicSignature dssBasicSignature, BasicSignature basicSignature) {
        assertEquals(dssBasicSignature.isSignatureIntact(), basicSignature.isSignatureIntact());
        assertEquals(dssBasicSignature.isSignatureValid(), basicSignature.isSignatureValid());
        assertEquals(dssBasicSignature.getEncryptionAlgoUsedToSignThisToken(), basicSignature.getEncryptionAlgoUsedToSignThisToken());
        assertEquals(dssBasicSignature.getKeyLengthUsedToSignThisToken(), basicSignature.getKeyLengthUsedToSignThisToken());
        assertEquals(dssBasicSignature.getMaskGenerationFunctionUsedToSignThisToken(), basicSignature.getMaskGenerationFunctionUsedToSignThisToken());
    }

    private void assertSigningCertificate(XmlSigningCertificate dssSigningCertificate, SigningCertificate signingCertificate) {
        assertEquals(dssSigningCertificate.getId(), signingCertificate.getId());
        assertEquals(dssSigningCertificate.isIssuerSerialMatch(), signingCertificate.isIssuerSerialMatch());
        assertEquals(dssSigningCertificate.isDigestValuePresent(), signingCertificate.isDigestValuePresent());
        assertEquals(dssSigningCertificate.isAttributePresent(), signingCertificate.isAttributePresent());
        assertEquals(dssSigningCertificate.isDigestValueMatch(), signingCertificate.isDigestValueMatch());
    }

    private void assertUserCertificate(XmlCertificate dssXmlCertificate, Certificate certificate) {
        assertEquals(dssXmlCertificate.getId(), certificate.getId());
        assertEquals(dssXmlCertificate.getCommonName(), certificate.getCommonName());
        assertEquals(dssXmlCertificate.getCountryName(), certificate.getCountryName());
        assertEquals(dssXmlCertificate.getSerialNumber(), certificate.getSerialNumber());
        assertEquals(dssXmlCertificate.getLocality(), certificate.getLocality());
        assertEquals(dssXmlCertificate.getState(), certificate.getState());
        assertEquals(dssXmlCertificate.getEmail(), dssXmlCertificate.getEmail());
        assertThat(dssXmlCertificate.getBase64Encoded(), equalTo(certificate.getBase64Encoded()));
        assertEquals(dssXmlCertificate.getGivenName(), certificate.getGivenName());
        assertEquals(dssXmlCertificate.getSurname(), certificate.getSurname());
        assertEquals(dssXmlCertificate.getOrganizationalUnit(), certificate.getOrganizationalUnit());
        assertEquals(dssXmlCertificate.getOrganizationName(), certificate.getOrganizationName());
        assertEquals(dssXmlCertificate.getPseudonym(),   certificate.getPseudonym());
        assertEquals(dssXmlCertificate.getPublicKeyEncryptionAlgo(), certificate.getPublicKeyEncryptionAlgo());
        assertEquals(dssXmlCertificate.getPublicKeySize(), certificate.getPublicKeySize());
        assertDateEquals(dssXmlCertificate.getNotAfter(), certificate.getNotAfter());
        assertDateEquals(dssXmlCertificate.getNotBefore(), certificate.getNotBefore());

        for (int i = 0; i < dssXmlCertificate.getCertificateChain().size(); i++) {
            assertCertificateChainItem(dssXmlCertificate.getCertificateChain().get(i), certificate.getCertificateChain().getChainItem().get(i));
        }

        assertBasicSignature(dssXmlCertificate.getBasicSignature(), certificate.getBasicSignature());
        assertSigningCertificate(dssXmlCertificate.getSigningCertificate(), certificate.getSigningCertificate());
    }


    private void assertDateEquals(Date dssDate, XMLGregorianCalendar date) {
        /**
         * At the moment DSS parsed date values are in UTC format but displayed timezone element is not displayed.
         * Adding Z to date string before parsing to epoch.
         * If given assertion fails, then given hack should be removed.
         */
        assertFalse(date.toString().endsWith("Z"));
        assertEquals(dssDate.getTime() / 1000, Instant.parse(date.toString() + "Z").getEpochSecond());
    }

    private eu.europa.esig.dss.jaxb.detailedreport.DetailedReport createMockedValidationProcess() {
        eu.europa.esig.dss.jaxb.detailedreport.DetailedReport euDetailedReport = new eu.europa.esig.dss.jaxb.detailedreport.DetailedReport();
        XmlTLAnalysis xmlTLAnalysis = new XmlTLAnalysis();
        xmlTLAnalysis.setCountryCode("EE");
        euDetailedReport.getTLAnalysis().add(xmlTLAnalysis);
        return euDetailedReport;
    }

    private ee.openeid.siva.validation.document.report.ValidationConclusion createMockedValidationConclusion() {
        ee.openeid.siva.validation.document.report.ValidationConclusion report = new ee.openeid.siva.validation.document.report.ValidationConclusion();
        report.setValidationTime("2016-09-21T15:00:00Z");
        report.setValidationWarnings(createMockedValidationWarnings());
        report.setValidatedDocument(createMockedValidatedDocument());
        report.setSignatureForm("PAdES");
        report.setPolicy(createMockedSignaturePolicy());
        report.setValidationLevel("ARCHIVAL_DATA");
        report.setSignaturesCount(1);
        report.setValidSignaturesCount(1);
        report.setSignatures(createMockedSignatures());
        report.setTimeStampTokens(createMockedTimeStampTokens());
        return report;
    }

    private List<TimeStampTokenValidationData> createMockedTimeStampTokens() {
        List<TimeStampTokenValidationData> timeStampTokens = new ArrayList<>();
        TimeStampTokenValidationData timeStampTokenValidationData = new TimeStampTokenValidationData();
        timeStampTokenValidationData.setSignedTime("2017-09-18T15:00:00Z");
        timeStampTokenValidationData.setSignedBy("Signer");
        timeStampTokenValidationData.setIndication(TimeStampTokenValidationData.Indication.TOTAL_FAILED);
        timeStampTokenValidationData.setError(createMockedSignatureErrors());
        timeStampTokens.add(timeStampTokenValidationData);
        return timeStampTokens;
    }

    private List<ee.openeid.siva.validation.document.report.ValidationWarning> createMockedValidationWarnings() {
        List<ee.openeid.siva.validation.document.report.ValidationWarning> validationWarnings = new ArrayList<>();
        ee.openeid.siva.validation.document.report.ValidationWarning validationWarning = new ee.openeid.siva.validation.document.report.ValidationWarning();
        validationWarning.setContent("some validation warning");
        validationWarnings.add(validationWarning);
        return validationWarnings;
    }

    private ValidatedDocument createMockedValidatedDocument() {
        ValidatedDocument validatedDocument = new ValidatedDocument();
        validatedDocument.setFilename("document.pdf");
        return validatedDocument;
    }

    private ee.openeid.siva.validation.document.report.Policy createMockedSignaturePolicy() {
        ee.openeid.siva.validation.document.report.Policy policy = new ee.openeid.siva.validation.document.report.Policy();
        policy.setPolicyDescription("desc");
        policy.setPolicyName("pol");
        policy.setPolicyUrl("http://pol.eu");
        return policy;
    }

    private List<ee.openeid.siva.validation.document.report.SignatureValidationData> createMockedSignatures() {
        List<ee.openeid.siva.validation.document.report.SignatureValidationData> signatures = new ArrayList<>();
        ee.openeid.siva.validation.document.report.SignatureValidationData signature = new ee.openeid.siva.validation.document.report.SignatureValidationData();
        signature.setCountryCode("EE");
        signature.setSubIndication("");
        signature.setId("S0");
        signature.setIndication(ee.openeid.siva.validation.document.report.SignatureValidationData.Indication.TOTAL_FAILED);
        signature.setClaimedSigningTime("2016-09-21T14:00:00Z");
        signature.setSignatureFormat("PAdES_LT");
        signature.setSignatureLevel("QES");
        signature.setSignedBy("nobody");
        SubjectDistinguishedName subjectDistinguishedName = new SubjectDistinguishedName();
        subjectDistinguishedName.setCommonName("COMMON_NAME");
        subjectDistinguishedName.setSerialNumber("SERIALNUMBER");
        signature.setSubjectDistinguishedName(subjectDistinguishedName);
        signature.setErrors(createMockedSignatureErrors());
        signature.setInfo(createMockedSignatureInfo());
        signature.setSignatureScopes(createMockedSignatureScopes());
        signature.setWarnings(createMockedSignatureWarnings());
        signatures.add(signature);
        return signatures;
    }

    private List<ee.openeid.siva.validation.document.report.Error> createMockedSignatureErrors() {
        List<ee.openeid.siva.validation.document.report.Error> errors = new ArrayList<>();
        ee.openeid.siva.validation.document.report.Error error = new ee.openeid.siva.validation.document.report.Error();
        error.setContent("some error");
        errors.add(error);
        return errors;
    }

    private ee.openeid.siva.validation.document.report.Info createMockedSignatureInfo() {
        ee.openeid.siva.validation.document.report.Info info = new ee.openeid.siva.validation.document.report.Info();
        info.setBestSignatureTime("2016-09-21T14:00:00Z");
        return info;
    }

    private List<ee.openeid.siva.validation.document.report.SignatureScope> createMockedSignatureScopes() {
        List<ee.openeid.siva.validation.document.report.SignatureScope> signatureScopes = new ArrayList<>();
        ee.openeid.siva.validation.document.report.SignatureScope signatureScope = new ee.openeid.siva.validation.document.report.SignatureScope();
        signatureScope.setContent("some content");
        signatureScope.setName("some name");
        signatureScope.setScope("some scope");
        signatureScopes.add(signatureScope);
        return signatureScopes;
    }

    private List<ee.openeid.siva.validation.document.report.Warning> createMockedSignatureWarnings() {
        List<ee.openeid.siva.validation.document.report.Warning> warnings = new ArrayList<>();
        ee.openeid.siva.validation.document.report.Warning warning = new ee.openeid.siva.validation.document.report.Warning();
        warning.setContent("some warning");
        warnings.add(warning);
        return warnings;
    }

    private eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData createMockedDiagnosticData() {
        eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData diagnosticData = new eu.europa.esig.dss.jaxb.diagnostic.DiagnosticData();
        diagnosticData.setDocumentName("TEST_DOCUMENT_NAME");
        diagnosticData.setValidationDate(new Date());
        diagnosticData.setContainerInfo(createMockedContainerInfo());
        XmlTrustedList mockedXmlTrustedList = createMockedXmlTrustedList();
        diagnosticData.setListOfTrustedLists(mockedXmlTrustedList);
        diagnosticData.setTrustedLists(Collections.singletonList(mockedXmlTrustedList));
        diagnosticData.setUsedCertificates(Arrays.asList(createMockedXmlCertificate("1"), createMockedXmlCertificate("2")));
        diagnosticData.setSignatures(Arrays.asList(createMockedXmlSignature("1"), createMockedXmlSignature("2")));
        return diagnosticData;
    }

    private XmlTrustedList createMockedXmlTrustedList() {
        XmlTrustedList xmlTrustedList = new XmlTrustedList();
        xmlTrustedList.setCountryCode("EE");
        xmlTrustedList.setIssueDate(new Date());
        xmlTrustedList.setLastLoading(new Date());
        xmlTrustedList.setNextUpdate(new Date());
        xmlTrustedList.setSequenceNumber(1);
        xmlTrustedList.setUrl("URL");
        xmlTrustedList.setVersion(2);
        xmlTrustedList.setWellSigned(true);
        return xmlTrustedList;
    }

    private XmlContainerInfo createMockedContainerInfo() {
        XmlContainerInfo containerInfo = new XmlContainerInfo();
        containerInfo.setContainerType("CONTAINER_TYPE");
        containerInfo.setMimeTypeContent("CONTAINER_MIME_TYPE");
        containerInfo.setMimeTypeFilePresent(false);
        containerInfo.setZipComment("ZIP_COMMENT");
        containerInfo.setContentFiles(Arrays.asList("COUNTED_FILE_1", "COUNTED_FILE_2"));
        containerInfo.setManifestFiles(Arrays.asList(createMockedXmlManifestFile("FILENAME_1"), createMockedXmlManifestFile("FILENAME_2")));
        return containerInfo;
    }

    private XmlManifestFile createMockedXmlManifestFile(String filename) {
        XmlManifestFile xmlManifestFile = new XmlManifestFile();
        xmlManifestFile.setFilename(filename);
        xmlManifestFile.setSignatureFilename("SIGNATURE_FILENAME");
        xmlManifestFile.setEntries(Arrays.asList("ENTRY_1", "ENTRY_2"));
        return xmlManifestFile;
    }

    private XmlCertificate createMockedXmlCertificate(String id) {
        XmlCertificate xmlCertificate = new XmlCertificate();
        xmlCertificate.setBasicSignature(new XmlBasicSignature());
        xmlCertificate.setBase64Encoded("something".getBytes(StandardCharsets.UTF_8));
        xmlCertificate.setId(id);
        xmlCertificate.setCommonName("COMMON_NAME");
        xmlCertificate.setCountryName("COUNTRY_NAME");
        xmlCertificate.setGivenName("GIVEN_NAME");
        xmlCertificate.setSurname("SURNAME");
        xmlCertificate.setLocality("LOCALITY");
        xmlCertificate.setEmail("EMAIL");
        xmlCertificate.setState("STATE");
        xmlCertificate.setOrganizationalUnit("OU");
        xmlCertificate.setSerialNumber(BigInteger.ONE);
        xmlCertificate.setTrusted(true);
        xmlCertificate.setPseudonym("PSEUDONYM");
        xmlCertificate.setPublicKeyEncryptionAlgo("ALGO");
        xmlCertificate.setBasicSignature(createMockedBasicSignature());
        xmlCertificate.setSigningCertificate(createMockedSigningCertificate());
        xmlCertificate.setPublicKeySize(1000);
        xmlCertificate.setNotAfter(new Date());
        xmlCertificate.setNotBefore(new Date());
        return xmlCertificate;
    }

    private XmlSignature createMockedXmlSignature(String id) {
        XmlSignature signature = new XmlSignature();
        signature.setId(id);
        signature.setContentHints("CONTENTS_HINTS");
        signature.setContentType("CONTENT_TYPE");
        signature.setCounterSignature(true);
        signature.setErrorMessage("ERROR_MESSAGE");

        signature.setBasicSignature(createMockedBasicSignature());

        XmlPolicy policy = new XmlPolicy();
        policy.setId("ID");
        policy.setDigestAlgorithmsEqual(true);
        policy.setAsn1Processable(true);
        policy.setIdentified(true);
        policy.setStatus(true);
        policy.setDigestAlgorithmsEqual(true);
        policy.setNotice("NOTICE");
        policy.setProcessingError("PROCESSING_ERROR");

        XmlDigestAlgoAndValue xmlDigestAlgoAndValue = new XmlDigestAlgoAndValue();
        xmlDigestAlgoAndValue.setDigestMethod("METHOD");
        xmlDigestAlgoAndValue.setDigestValue("VALUE");
        policy.setDigestAlgoAndValue(xmlDigestAlgoAndValue);
        signature.setPolicy(policy);
        signature.setSigningCertificate(createMockedSigningCertificate());

        XmlStructuralValidation xmlStructuralValidation = new XmlStructuralValidation();
        xmlStructuralValidation.setMessage("MESSAGE");
        xmlStructuralValidation.setValid(true);
        signature.setStructuralValidation(xmlStructuralValidation);

        XmlSignatureProductionPlace xmlSignatureProductionPlace = new XmlSignatureProductionPlace();
        xmlSignatureProductionPlace.setAddress("ADDRESS");
        xmlSignatureProductionPlace.setCity("CITY");
        xmlSignatureProductionPlace.setCountryName("ESTONIA");
        xmlSignatureProductionPlace.setPostalCode("POSTAL_CODE");
        xmlSignatureProductionPlace.setStateOrProvince("STATE");
        signature.setSignatureProductionPlace(xmlSignatureProductionPlace);

        signature.setCertificateChain(Arrays.asList(createMockedXmlChainItem("1"), createMockedXmlChainItem("2")));
        signature.setClaimedRoles(Arrays.asList("role1", "role2"));
        signature.setCommitmentTypeIndication(Arrays.asList("1", "2"));
        signature.setDateTime(new Date());

        signature.setTimestamps(Arrays.asList(createMockedXmlTimestamp("1"), createMockedXmlTimestamp("2")));
        return signature;
    }

    private XmlBasicSignature createMockedBasicSignature() {
        XmlBasicSignature basicSignature = new XmlBasicSignature();
        basicSignature.setDigestAlgoUsedToSignThisToken("A");
        basicSignature.setEncryptionAlgoUsedToSignThisToken("B");
        basicSignature.setKeyLengthUsedToSignThisToken("C");
        basicSignature.setMaskGenerationFunctionUsedToSignThisToken("D");
        basicSignature.setSignatureIntact(true);
        basicSignature.setSignatureValid(true);
        return basicSignature;
    }

    private XmlSigningCertificate createMockedSigningCertificate() {
        XmlSigningCertificate xmlSigningCertificate = new XmlSigningCertificate();
        xmlSigningCertificate.setId("ID");
        xmlSigningCertificate.setIssuerSerialMatch(true);
        xmlSigningCertificate.setDigestValuePresent(true);
        xmlSigningCertificate.setAttributePresent(true);
        xmlSigningCertificate.setDigestValueMatch(true);
        return xmlSigningCertificate;
    }

    private XmlChainItem createMockedXmlChainItem(String id) {
        XmlChainItem chainItem = new XmlChainItem();
        chainItem.setId(id);
        chainItem.setSource("SOURCE");
        return chainItem;
    }

    private XmlTimestamp createMockedXmlTimestamp(String id) {
        XmlTimestamp timestamp = new XmlTimestamp();
        timestamp.setId(id);
        timestamp.setBasicSignature(createMockedBasicSignature());
        timestamp.setProductionTime(new Date());
        timestamp.setSigningCertificate(createMockedSigningCertificate());
        timestamp.setType("TYPE");
        timestamp.setCertificateChain(Arrays.asList(createMockedXmlChainItem("33"), createMockedXmlChainItem("44")));

        XmlDigestMatcher digestMatcher = new XmlDigestMatcher();
        digestMatcher.setName("name");
        digestMatcher.setDataFound(true);
        digestMatcher.setDataIntact(true);
        digestMatcher.setType(DigestMatcherType.MANIFEST);
        digestMatcher.setDigestMethod("method");
        digestMatcher.setDigestValue("value");
        timestamp.setDigestMatcher(digestMatcher);
        return timestamp;
    }
}
