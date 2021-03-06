package io.pivotal.security.handler;

import io.pivotal.security.audit.EventAuditRecordParameters;
import io.pivotal.security.auth.UserContext;
import io.pivotal.security.data.CredentialDataService;
import io.pivotal.security.domain.CredentialVersion;
import io.pivotal.security.domain.PasswordCredentialVersion;
import io.pivotal.security.entity.Credential;
import io.pivotal.security.entity.PasswordCredentialVersionData;
import io.pivotal.security.exceptions.EntryNotFoundException;
import io.pivotal.security.exceptions.InvalidPermissionOperationException;
import io.pivotal.security.request.PermissionEntry;
import io.pivotal.security.request.PermissionOperation;
import io.pivotal.security.request.PermissionsRequest;
import io.pivotal.security.service.PermissionCheckingService;
import io.pivotal.security.service.PermissionService;
import io.pivotal.security.service.PermissionedCredentialService;
import io.pivotal.security.view.PermissionsView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.pivotal.security.request.PermissionOperation.READ;
import static io.pivotal.security.request.PermissionOperation.READ_ACL;
import static io.pivotal.security.request.PermissionOperation.WRITE;
import static io.pivotal.security.request.PermissionOperation.WRITE_ACL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class PermissionsHandlerTest {
  private static final String CREDENTIAL_NAME = "/test-credential";
  private static final String ACTOR_NAME = "test-actor";
  private static final String ACTOR_NAME2 = "someone-else";
  private static final String USER = "test-user";

  private PermissionsHandler subject;

  private PermissionService permissionService;
  private PermissionCheckingService permissionCheckingService;
  private CredentialDataService credentialDataService;
  private PermissionedCredentialService permissionedCredentialService;

  private final Credential credential = new Credential(CREDENTIAL_NAME);
  private final CredentialVersion credentialVersion = new PasswordCredentialVersion(new PasswordCredentialVersionData(CREDENTIAL_NAME));
  private final UserContext userContext = mock(UserContext.class);
  private List<EventAuditRecordParameters> auditRecordParameters;
  private PermissionsRequest permissionsRequest;

  @Before
  public void beforeEach() {
    permissionService = mock(PermissionService.class);
    permissionCheckingService = mock(PermissionCheckingService.class);
    credentialDataService = mock(CredentialDataService.class);
    permissionedCredentialService = mock(PermissionedCredentialService.class);
    subject = new PermissionsHandler(
        permissionService,
        permissionedCredentialService);

    permissionsRequest = mock(PermissionsRequest.class);
    auditRecordParameters = new ArrayList<>();

    when(permissionedCredentialService.findMostRecent(CREDENTIAL_NAME)).thenReturn(credentialVersion);
    when(credentialDataService.find(any(String.class))).thenReturn(credential);
  }

  @Test
  public void getPermissions_whenTheNameDoesntStartWithASlash_fixesTheName() {
    List<PermissionEntry> accessControlList = newArrayList();
    when(permissionService.getPermissions(any(CredentialVersion.class), eq(auditRecordParameters), eq(CREDENTIAL_NAME)))
        .thenReturn(accessControlList);
    when(permissionCheckingService
        .hasPermission(any(String.class), eq(CREDENTIAL_NAME), eq(READ_ACL)))
        .thenReturn(true);

    PermissionsView response = subject.getPermissions(
        CREDENTIAL_NAME,
        auditRecordParameters);
    assertThat(response.getCredentialName(), equalTo(CREDENTIAL_NAME));

  }

  @Test
  public void getPermissions_verifiesTheUserHasPermissionToReadTheAcl_andReturnsTheAclResponse() {
    ArrayList<PermissionOperation> operations = newArrayList(
        READ,
        WRITE
    );
    when(permissionCheckingService
        .hasPermission(any(String.class), eq(CREDENTIAL_NAME), eq(READ_ACL)))
        .thenReturn(true);
    PermissionEntry permissionEntry = new PermissionEntry(
        ACTOR_NAME,
        operations
    );
    List<PermissionEntry> accessControlList = newArrayList(permissionEntry);
    when(permissionService.getPermissions(credentialVersion, auditRecordParameters, CREDENTIAL_NAME))
        .thenReturn(accessControlList);

    PermissionsView response = subject.getPermissions(
        CREDENTIAL_NAME,
        auditRecordParameters);

    List<PermissionEntry> accessControlEntries = response.getPermissions();

    assertThat(response.getCredentialName(), equalTo(CREDENTIAL_NAME));
    assertThat(accessControlEntries, hasSize(1));

    PermissionEntry entry = accessControlEntries.get(0);
    assertThat(entry.getActor(), equalTo(ACTOR_NAME));

    List<PermissionOperation> allowedOperations = entry.getAllowedOperations();
    assertThat(allowedOperations, contains(
        equalTo(READ),
        equalTo(WRITE)
    ));
  }

  @Test
  public void setPermissions_setsAndReturnsThePermissions() {
    when(permissionCheckingService
        .hasPermission(any(String.class), eq(CREDENTIAL_NAME), eq(WRITE_ACL)))
        .thenReturn(true);
    when(permissionCheckingService
        .userAllowedToOperateOnActor(ACTOR_NAME))
        .thenReturn(true);

    ArrayList<PermissionOperation> operations = newArrayList(
        READ,
        WRITE
    );
    PermissionEntry permissionEntry = new PermissionEntry(ACTOR_NAME, operations);
    List<PermissionEntry> accessControlList = newArrayList(permissionEntry);

    PermissionEntry preexistingPermissionEntry = new PermissionEntry(
        ACTOR_NAME2,
        newArrayList(READ)
    );
    List<PermissionEntry> expectedControlList = newArrayList(permissionEntry,
        preexistingPermissionEntry);

    when(permissionService.getPermissions(credentialVersion, auditRecordParameters, CREDENTIAL_NAME))
        .thenReturn(expectedControlList);

    when(permissionsRequest.getCredentialName()).thenReturn(CREDENTIAL_NAME);
    when(permissionsRequest.getPermissions()).thenReturn(accessControlList);

    subject.setPermissions(permissionsRequest, auditRecordParameters);

    ArgumentCaptor<List> permissionsListCaptor = ArgumentCaptor.forClass(List.class);
    verify(permissionService).savePermissions(eq(credentialVersion), permissionsListCaptor.capture(), eq(auditRecordParameters), eq(false), eq(CREDENTIAL_NAME));

    List<PermissionEntry> accessControlEntries = permissionsListCaptor.getValue();

    PermissionEntry entry = accessControlEntries.get(0);
    assertThat(entry.getActor(), equalTo(ACTOR_NAME));
    assertThat(entry.getAllowedOperations(), contains(
        equalTo(READ),
        equalTo(WRITE)
    ));
  }

  @Test
  public void setPermissions_whenUserUpdatesOwnPermission_throwsException() {
    when(permissionCheckingService
        .hasPermission(any(String.class), eq(CREDENTIAL_NAME), eq(WRITE_ACL)))
        .thenReturn(true);
    when(permissionCheckingService
        .userAllowedToOperateOnActor(ACTOR_NAME))
        .thenReturn(false);

    List<PermissionEntry> accessControlList = Arrays.asList(new PermissionEntry(ACTOR_NAME, Arrays.asList(READ)));
    when(permissionsRequest.getCredentialName()).thenReturn(CREDENTIAL_NAME);
    when(permissionsRequest.getPermissions()).thenReturn(accessControlList);

    try {
      subject.setPermissions(permissionsRequest, auditRecordParameters);
    } catch (InvalidPermissionOperationException e) {
      assertThat(e.getMessage(), equalTo("error.permission.invalid_update_operation"));
      verify(permissionService, times(0)).savePermissions(any(), any(), eq(auditRecordParameters), eq(false), eq(CREDENTIAL_NAME));
    }
  }

  @Test
  public void deletePermissions_whenTheUserHasPermission_deletesTheAce() {
    when(permissionCheckingService
        .hasPermission(any(String.class), eq(CREDENTIAL_NAME), eq(WRITE_ACL)))
        .thenReturn(true);
    when(permissionService.deletePermissions(CREDENTIAL_NAME, ACTOR_NAME, auditRecordParameters))
        .thenReturn(true);
    when(permissionCheckingService
        .userAllowedToOperateOnActor(ACTOR_NAME))
        .thenReturn(true);

    subject.deletePermissionEntry(CREDENTIAL_NAME, ACTOR_NAME,
        auditRecordParameters);

    verify(permissionService, times(1)).deletePermissions(
        CREDENTIAL_NAME, ACTOR_NAME, auditRecordParameters);

  }

  @Test
  public void deletePermissions_whenNothingIsDeleted_throwsAnException() {
    when(permissionService.deletePermissions(CREDENTIAL_NAME, ACTOR_NAME, auditRecordParameters))
        .thenReturn(false);

    try {
      subject.deletePermissionEntry(CREDENTIAL_NAME, ACTOR_NAME,
          auditRecordParameters);
      fail("should throw");
    } catch (EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo("error.credential.invalid_access"));
    }
  }
}
