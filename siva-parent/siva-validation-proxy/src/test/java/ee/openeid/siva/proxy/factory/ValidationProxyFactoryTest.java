package ee.openeid.siva.proxy.factory;


import ee.openeid.siva.proxy.ValidationProxy;
import ee.openeid.siva.proxy.factory.ValidationProxyFactory;
import ee.openeid.siva.proxy.impl.PdfValidationProxy;
import eu.europa.esig.dss.MimeType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValidationProxyFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    ValidationProxyFactory validationProxyFactory = new ValidationProxyFactory();

    @Test
    public void getPdfValidationProxy() {
        ValidationProxy validationProxy = validationProxyFactory.getValidationProxy(MimeType.PDF);
        Assert.assertTrue(validationProxy instanceof PdfValidationProxy);
    }

    @Test
    public void expectExceptionWhenMimeTypeUnsupported() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("type = application/octet-stream is unsupported");

        validationProxyFactory.getValidationProxy(MimeType.BINARY);
    }

}