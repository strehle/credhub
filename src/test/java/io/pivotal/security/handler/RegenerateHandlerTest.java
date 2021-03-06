package io.pivotal.security.handler;

import io.pivotal.security.auth.UserContext;
import io.pivotal.security.domain.CredentialVersion;
import io.pivotal.security.domain.PasswordCredentialVersion;
import io.pivotal.security.request.PasswordGenerateRequest;
import io.pivotal.security.service.PermissionService;
import io.pivotal.security.service.PermissionedCredentialService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class RegenerateHandlerTest {
  private static final String SIGNER_NAME = "signer name";

  private RegenerateHandler subject;
  private PermissionedCredentialService credentialService;
  private PermissionService permissionService;
  private UniversalCredentialGenerator credentialGenerator;
  private GenerationRequestGenerator generationRequestGenerator;
  private UserContext userContext;

  @Before
  public void beforeEach() {
    credentialService = mock(PermissionedCredentialService.class);
    permissionService = mock(PermissionService.class);
    credentialGenerator = mock(UniversalCredentialGenerator.class);
    generationRequestGenerator = mock(GenerationRequestGenerator.class);
    userContext = mock(UserContext.class);
    subject = new RegenerateHandler(
        credentialService,
        credentialGenerator,
        generationRequestGenerator);
  }

  @Test
  public void handleBulkRegenerate_regeneratesEverythingInTheList() throws Exception {
    when(credentialService.findAllCertificateCredentialsByCaName(SIGNER_NAME))
        .thenReturn(newArrayList("firstExpectedName", "secondExpectedName"));
    when(credentialService.findMostRecent(anyString()))
        .thenReturn(mock(CredentialVersion.class));
    CredentialVersion credentialVersion = mock(PasswordCredentialVersion.class);
    when(credentialService.save(anyObject(), anyString(), anyString(), anyObject(), anyObject(), anyList(), anyString(), anyList())).thenReturn(credentialVersion);

    PasswordGenerateRequest generateRequest1 = new PasswordGenerateRequest();
    generateRequest1.setName("/firstExpectedName");
    PasswordGenerateRequest generateRequest2 = new PasswordGenerateRequest();
    generateRequest2.setName("/secondExpectedName");
    when(generationRequestGenerator.createGenerateRequest(any(CredentialVersion.class), any(String.class), any(List.class)))
        .thenReturn(generateRequest1)
        .thenReturn(generateRequest2);

    subject.handleBulkRegenerate(SIGNER_NAME, newArrayList());

    verify(credentialService).save(
        any(), eq("/firstExpectedName"),
        any(), any(), any(),
        any(), anyString(),
        any());

    verify(credentialService).save(
        any(), eq("/secondExpectedName"),
        any(), any(), any(),
        any(), anyString(),
        any());

  }

}
