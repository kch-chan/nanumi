package com.nanumi.api.security.password;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 나누미 전용 비밀번호 인코더임
//
// 저장 형식: $nanumi${버전}${반복횟수}$Base64(salt)$Base64(hash)
// 예) $nanumi$1$210000$ES9dxu6QeMBGl8fA4kR6bw$Xo1r...  (기본값 기준 83자)
//
// - 비밀번호마다 salt 를 새로 뽑아서 같은 비밀번호라도 해시가 달라짐
// - 서버만 아는 pepper 를 덧붙여서 DB 만 털려도 대입 공격이 어려움
// - 버전과 반복 횟수를 해시에 같이 적어 두므로, 파라미터를 올려도 기존 계정이 그대로 로그인됨
//   (로그인 성공 시 AuthService 가 upgradeEncoding 을 보고 새 파라미터로 다시 해싱함)
// - 예전에 쓰던 BCrypt 해시도 검증만은 계속 지원함
@Component
@RequiredArgsConstructor
public class NanumiPasswordEncoder implements PasswordEncoder {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String PREFIX = "nanumi";
  private static final int VERSION = 1;
  private static final int SALT_BYTES = 16;
  private static final int HASH_BYTES = 32;
  private static final char SEPARATOR = '$';

  // BCrypt 해시가 쓰는 접두사임. 예전 계정을 알아보는 데만 씀
  private static final String[] BCRYPT_PREFIXES = {"$2a$", "$2b$", "$2y$"};

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final NanumiPasswordProperties properties;
  private final BCryptPasswordEncoder legacyEncoder = new BCryptPasswordEncoder();

  @Override
  public String encode(CharSequence rawPassword) {
    if (rawPassword == null) {
      throw new IllegalArgumentException("비밀번호가 비어 있음");
    }

    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);

    int iterations = resolveIterations();
    byte[] hash = pbkdf2(rawPassword, salt, iterations);

    return new StringBuilder()
        .append(SEPARATOR)
        .append(PREFIX)
        .append(SEPARATOR)
        .append(VERSION)
        .append(SEPARATOR)
        .append(iterations)
        .append(SEPARATOR)
        .append(ENCODER.encodeToString(salt))
        .append(SEPARATOR)
        .append(ENCODER.encodeToString(hash))
        .toString();
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
      return false;
    }

    if (isLegacyHash(encodedPassword)) {
      return legacyEncoder.matches(rawPassword, encodedPassword);
    }

    ParsedHash parsed = parse(encodedPassword);
    if (parsed == null) {
      return false;
    }

    byte[] actual = pbkdf2(rawPassword, parsed.salt(), parsed.iterations());

    // 앞자리부터 비교하다 중간에 끊기면 시간 차가 생기므로 상수 시간 비교를 씀
    return MessageDigest.isEqual(parsed.hash(), actual);
  }

  // 다시 해싱해야 하는 해시인지 알려 줌. 로그인에 성공한 순간에만 확인함
  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    if (encodedPassword == null || encodedPassword.isEmpty()) {
      return false;
    }

    // 예전 BCrypt 해시는 무조건 새 형식으로 갈아탐
    if (isLegacyHash(encodedPassword)) {
      return true;
    }

    ParsedHash parsed = parse(encodedPassword);
    if (parsed == null) {
      // 읽지 못하는 해시는 다시 해싱해도 의미가 없음
      return false;
    }

    return parsed.version() != VERSION || parsed.iterations() < resolveIterations();
  }

  // 예전에 쓰던 BCrypt 해시인지 확인함
  public boolean isLegacyHash(String encodedPassword) {
    if (encodedPassword == null) {
      return false;
    }
    for (String prefix : BCRYPT_PREFIXES) {
      if (encodedPassword.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private int resolveIterations() {
    int iterations = properties.getIterations();
    if (iterations < 1) {
      throw new IllegalStateException("PBKDF2 반복 횟수는 1 이상이어야 함: " + iterations);
    }
    return iterations;
  }

  private byte[] pbkdf2(CharSequence rawPassword, byte[] salt, int iterations) {
    // pepper 를 비밀번호 뒤에 붙여서 같이 늘림. pepper 는 DB 에 남지 않음
    char[] material = (rawPassword.toString() + properties.getPepper()).toCharArray();
    PBEKeySpec spec = new PBEKeySpec(material, salt, iterations, HASH_BYTES * 8);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("비밀번호 해시를 만들지 못함", e);
    } finally {
      spec.clearPassword();
      Arrays.fill(material, '\0');
    }
  }

  // 저장된 해시를 조각내서 읽음. 형식이 어긋나면 null 을 돌려줌
  private ParsedHash parse(String encodedPassword) {
    String[] parts = encodedPassword.split("\\" + SEPARATOR);

    // "$nanumi$1$210000$salt$hash" 를 나누면 맨 앞이 빈 문자열이라 6조각이 나옴
    if (parts.length != 6 || !parts[0].isEmpty() || !PREFIX.equals(parts[1])) {
      return null;
    }

    try {
      int version = Integer.parseInt(parts[2]);
      int iterations = Integer.parseInt(parts[3]);
      if (iterations < 1) {
        return null;
      }

      byte[] salt = DECODER.decode(parts[4]);
      byte[] hash = DECODER.decode(parts[5]);
      if (salt.length == 0 || hash.length == 0) {
        return null;
      }

      return new ParsedHash(version, iterations, salt, hash);
    } catch (IllegalArgumentException e) {
      // 숫자가 아니거나 Base64 가 깨진 해시임
      return null;
    }
  }

  private record ParsedHash(int version, int iterations, byte[] salt, byte[] hash) {}
}
