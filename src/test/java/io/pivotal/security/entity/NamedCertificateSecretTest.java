package io.pivotal.security.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.view.CertificateSecret;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@ActiveProfiles("unit-test")
public class NamedCertificateSecretTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void canCreateModelFromEntity() throws Exception {
    NamedCertificateSecret subject = io.pivotal.security.entity.NamedCertificateSecret.make("Foo", "my-ca", "my-cert", "my-priv");
    Object actual = subject.generateView();
    assertThat(objectMapper.writer().writeValueAsString(actual), equalTo("{\"type\":\"certificate\",\"updated_at\":null,\"value\":{\"root\":\"my-ca\",\"certificate\":\"my-cert\",\"private_key\":\"my-priv\"}}"));
  }

  @Test
  public void convertToModel_setsUpdatedAtFromEntity() {
    Instant now = Instant.now();
    NamedCertificateSecret subject = new NamedCertificateSecret("Foo").setUpdatedAt(now);
    CertificateSecret actual = subject.generateView();
    assertThat(actual.getUpdatedAt(), equalTo(now));
  }
}