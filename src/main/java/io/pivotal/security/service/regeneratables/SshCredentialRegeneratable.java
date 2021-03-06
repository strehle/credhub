package io.pivotal.security.service.regeneratables;

import io.pivotal.security.audit.EventAuditRecordParameters;
import io.pivotal.security.domain.CredentialVersion;
import io.pivotal.security.domain.SshCredentialVersion;
import io.pivotal.security.request.BaseCredentialGenerateRequest;
import io.pivotal.security.request.SshGenerateRequest;
import io.pivotal.security.request.SshGenerationParameters;

import java.util.List;

public class SshCredentialRegeneratable implements Regeneratable {

  @Override
  public BaseCredentialGenerateRequest createGenerateRequest(CredentialVersion credentialVersion, List<EventAuditRecordParameters> auditRecordParameters) {
    SshCredentialVersion sshCredential = (SshCredentialVersion) credentialVersion;
    SshGenerateRequest generateRequest = new SshGenerateRequest();

    generateRequest.setName(sshCredential.getName());
    generateRequest.setType(sshCredential.getCredentialType());
    SshGenerationParameters generationParameters = new SshGenerationParameters();
    generationParameters.setSshComment(sshCredential.getComment());
    generateRequest.setGenerationParameters(generationParameters);
    generateRequest.setOverwrite(true);
    return generateRequest;
  }
}
