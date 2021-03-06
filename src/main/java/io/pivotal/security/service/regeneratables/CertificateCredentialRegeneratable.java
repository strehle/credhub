package io.pivotal.security.service.regeneratables;

import io.pivotal.security.audit.EventAuditRecordParameters;
import io.pivotal.security.domain.CertificateCredentialVersion;
import io.pivotal.security.domain.CertificateGenerationParameters;
import io.pivotal.security.domain.CredentialVersion;
import io.pivotal.security.exceptions.ParameterizedValidationException;
import io.pivotal.security.request.BaseCredentialGenerateRequest;
import io.pivotal.security.request.CertificateGenerateRequest;
import io.pivotal.security.util.CertificateReader;

import java.util.List;

import static io.pivotal.security.audit.AuditingOperationCode.CREDENTIAL_UPDATE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CertificateCredentialRegeneratable implements Regeneratable {

  @Override
  public BaseCredentialGenerateRequest createGenerateRequest(CredentialVersion credentialVersion, List<EventAuditRecordParameters> auditRecordParameters) {
    CertificateCredentialVersion certificateCredential = (CertificateCredentialVersion) credentialVersion;
    CertificateReader reader = certificateCredential.getParsedCertificate();

    if (!reader.isValid() || (isEmpty(certificateCredential.getCaName()) && !reader.isSelfSigned())) {
      auditRecordParameters.add(new EventAuditRecordParameters(CREDENTIAL_UPDATE, credentialVersion.getName()));
      throw new ParameterizedValidationException(
          "error.cannot_regenerate_non_generated_certificate");
    }

    CertificateGenerationParameters certificateGenerationParameters = new CertificateGenerationParameters(reader,
        certificateCredential.getCaName());

    CertificateGenerateRequest generateRequest = new CertificateGenerateRequest();
    generateRequest.setName(certificateCredential.getName());
    generateRequest.setType(certificateCredential.getCredentialType());
    generateRequest.setCertificateGenerationParameters(certificateGenerationParameters);
    generateRequest.setOverwrite(true);
    return generateRequest;
  }
}
