package io.pivotal.security.domain;

import io.pivotal.security.entity.EncryptedValue;
import io.pivotal.security.service.BcEncryptionService;
import io.pivotal.security.service.BcNullConnection;
import io.pivotal.security.service.EncryptionKeyCanaryMapper;
import io.pivotal.security.service.RetryingEncryptionService;
import io.pivotal.security.util.PasswordKeyProxyFactoryTestImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.UUID;

import static io.pivotal.security.helper.TestHelper.getBouncyCastleProvider;
import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class EncryptorTest {

  private EncryptionKeyCanaryMapper keyMapper;
  private Encryptor subject;

  private byte[] encryptedValue;

  private byte[] nonce;
  private UUID oldUuid;
  private UUID newUuid;

  @Before
  public void beforeEach() throws Exception {
    oldUuid = UUID.randomUUID();
    newUuid = UUID.randomUUID();

    keyMapper = mock(EncryptionKeyCanaryMapper.class);
    BcEncryptionService bcEncryptionService;
    bcEncryptionService = new BcEncryptionService(getBouncyCastleProvider(), new PasswordKeyProxyFactoryTestImpl());

    Key newKey = new SecretKeySpec(parseHexBinary("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"), 0, 16,
        "AES");
    when(keyMapper.getActiveKey()).thenReturn(newKey);
    when(keyMapper.getActiveUuid()).thenReturn(newUuid);
    Key oldKey = new SecretKeySpec(parseHexBinary("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), 0, 16,
        "AES");
    when(keyMapper.getKeyForUuid(oldUuid)).thenReturn(oldKey);
    when(keyMapper.getKeyForUuid(newUuid)).thenReturn(newKey);

    RetryingEncryptionService encryptionService = new RetryingEncryptionService(
        bcEncryptionService, keyMapper, new BcNullConnection());
    subject = new Encryptor(encryptionService);
  }

  @Test
  public void encrypt_returnsNullForNullInput() {
    EncryptedValue encryption = subject.encrypt(null);

    assertThat(encryption.getEncryptedValue(), nullValue());
    assertThat(encryption.getNonce(), nullValue());
  }

  @Test
  public void encrypt_encryptsPlainTest() {
    EncryptedValue encryption = subject.encrypt("some value");

    assertThat(encryption.getEncryptedValue(), notNullValue());
    assertThat(encryption.getNonce(), notNullValue());
  }

  @Test(expected = RuntimeException.class)
  public void encrypt_wrapsExceptions() {
    when(keyMapper.getActiveUuid()).thenThrow(new IllegalArgumentException());

    subject.encrypt("some value");
  }

  @Test
  public void decrypt_decryptsEncryptedValues() {
    EncryptedValue encryption = subject.encrypt("the expected clear text");
    encryptedValue = encryption.getEncryptedValue();
    nonce = encryption.getNonce();

    assertThat(subject.decrypt(new EncryptedValue(newUuid, encryptedValue, nonce)), equalTo("the expected clear text"));
  }

  @Test(expected = RuntimeException.class)
  public void decrypt_failsToEncryptWhenGivenWrongKeyUuid() {
    EncryptedValue encryption = subject.encrypt("the expected clear text");
    encryptedValue = encryption.getEncryptedValue();
    nonce = encryption.getNonce();

    subject.decrypt(new EncryptedValue(oldUuid, encryptedValue, nonce));
  }
}
