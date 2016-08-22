package io.pivotal.security.mapper;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.view.CertificateAuthority;
import org.exparity.hamcrest.BeanMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.validation.ValidationException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@ActiveProfiles("unit-test")
public class CertificateAuthoritySetterRequestTranslatorTest {

  @Autowired
  private Configuration jsonConfiguration;

  @Test
  public void doValidScenarios() {
    doTestValid(new CertificateAuthority("root", "a", "b"), "a", "b");
  }

  @Test
  public void doInvalidScenarios() throws ValidationException {
    doTestInvalid("root", "", "a", "error.missing_ca_credentials");
    doTestInvalid("root", "b", "", "error.missing_ca_credentials");
    doTestInvalid("root", "", "", "error.missing_ca_credentials");
    doTestInvalid("root", "", "a", "error.missing_ca_credentials");
    doTestInvalid("root", "b", "", "error.missing_ca_credentials");
    doTestInvalid("invalid_ca_type", "b", "a", "error.type_invalid");
  }

  private void doTestValid(CertificateAuthority expected, String certificate, String privateKey) {
    String requestJson = "{\"type\":\"root\",\"value\":{\"certificate\":\"" + certificate + "\",\"private_key\":\"" + privateKey + "\"}}";

    DocumentContext parsed = JsonPath.using(jsonConfiguration).parse(requestJson);
    CertificateAuthority actual = new CertificateAuthoritySetterRequestTranslator().createAuthorityFromJson(parsed);
    assertThat(actual, BeanMatchers.theSameAs(expected));
  }

  private void doTestInvalid(String type, String certificate, String privateKey, String expectedErrorMessage) throws ValidationException {
    String requestJson = "{\"type\":" + type + ",\"value\":{\"certificate\":\"" + certificate + "\",\"private_key\":\"" + privateKey + "\"}}";

    DocumentContext parsed = JsonPath.using(jsonConfiguration).parse(requestJson);
    try {
      new CertificateAuthoritySetterRequestTranslator().createAuthorityFromJson(parsed);
      fail();
    } catch (ValidationException ve) {
      assertThat(ve.getMessage(), equalTo(expectedErrorMessage));
    }
  }
}