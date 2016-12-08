package io.pivotal.security.entity;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import static io.pivotal.security.constants.EncryptionConstants.ENCRYPTED_BYTES;
import static io.pivotal.security.constants.EncryptionConstants.GCM_NONCE_BYTES;
import static io.pivotal.security.constants.UuidConstants.UUID_BYTES;

@Entity
@Table(name = "NamedCertificateAuthority")
@EntityListeners(AuditingEntityListener.class)
public class NamedCertificateAuthority implements EncryptedValueContainer {
  @Id
  @Column(length = UUID_BYTES, columnDefinition = "VARBINARY")
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID uuid;

  @Column(unique = true, nullable = false)
  private String name;

  @Column()
  private String type;

  @Column(length = 7000)
  private String certificate;

  @Column(length = ENCRYPTED_BYTES + GCM_NONCE_BYTES, name = "encrypted_value")
  private byte[] encryptedValue;

  @Column(length = GCM_NONCE_BYTES)
  private byte[] nonce;

  @Convert(converter = InstantMillisecondsConverter.class)
  @Column(nullable = false, columnDefinition = "BIGINT NOT NULL")
  @CreatedDate
  @LastModifiedDate
  private Instant updatedAt;

  @SuppressWarnings("unused")
  public NamedCertificateAuthority() {
  }

  public NamedCertificateAuthority(String name) {
    this.name = name;
  }

  public UUID getUuid() {
    return uuid;
  }

  public NamedCertificateAuthority setUuid(UUID uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getName() {
    return name;
  }

  public NamedCertificateAuthority setName(String name) {
    this.name = name;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public NamedCertificateAuthority setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getCertificate() {
    return certificate;
  }

  public NamedCertificateAuthority setCertificate(String certificate) {
    this.certificate = certificate;
    return this;
  }

  public String getType() {
    return type;
  }

  public NamedCertificateAuthority setType(String type) {
    this.type = type;
    return this;
  }

  public byte[] getNonce() {
    return nonce;
  }

  public void setNonce(byte[] nonce) {
    this.nonce = nonce;
  }

  public byte[] getEncryptedValue() {
    return encryptedValue;
  }

  public void setEncryptedValue(byte[] encryptedValue) {
    this.encryptedValue = encryptedValue;
  }

  public void copyInto(NamedCertificateAuthority copy) {
    copy.setCertificate(certificate);
    copy.setEncryptedValue(encryptedValue);
    copy.setNonce(nonce);
  }

  public String getPrivateKey() {
    return SecretEncryptionHelperProvider.getInstance().retrieveClearTextValue(this);
  }

  public NamedCertificateAuthority setPrivateKey(String privateKey) {
    SecretEncryptionHelperProvider.getInstance().refreshEncryptedValue(this, privateKey);
    return this;
  }
}
