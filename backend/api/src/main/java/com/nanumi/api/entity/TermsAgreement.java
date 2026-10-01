package com.nanumi.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 회원 한 명이 약관 한 건에 대해 남긴 동의 기록임
//
// 동의를 받았다는 사실의 입증 책임은 사업자에게 있음(개인정보 보호법 제22조).
// 화면의 체크박스만으로는 아무것도 증명하지 못하므로 어느 판에 언제 동의했는지를 남김
//
// 선택 약관은 거부한 것도 한 줄로 남김. agreed = false 가 "물어봤고 거부했다" 는 기록임
@Entity
@Table(
    name = "terms_agreements",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_terms_agreements_user_key",
          columnNames = {"user_id", "terms_key"})
    },
    indexes = {@Index(name = "ix_terms_agreements_user_id", columnList = "user_id")})
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsAgreement {

  // 약관 종류 칸의 길이임. 프런트 constants/terms.ts 의 TermsKey 와 같은 값이 들어옴
  public static final int TERMS_KEY_LENGTH = 20;

  // 시행일자를 그대로 담는 칸의 길이임 (예: "시행일자 2026년 8월")
  public static final int TERMS_VERSION_LENGTH = 40;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "terms_key", nullable = false, length = TERMS_KEY_LENGTH)
  private String termsKey;

  // 동의한 약관의 판. 개정 전 판에 동의한 회원을 구분하는 데 씀
  @Column(name = "terms_version", nullable = false, length = TERMS_VERSION_LENGTH)
  private String termsVersion;

  @Column(nullable = false)
  private boolean agreed;

  @Column(name = "agreed_at", nullable = false)
  private LocalDateTime agreedAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public TermsAgreement(
      User user, String termsKey, String termsVersion, boolean agreed, LocalDateTime agreedAt) {
    this.user = user;
    this.termsKey = termsKey;
    this.termsVersion = termsVersion;
    this.agreed = agreed;
    this.agreedAt = agreedAt;
  }

  // 서버가 아는 약관 목록임. 프런트 constants/terms.ts 의 TermsKey 와 같은 값이어야 함
  //
  // 칼럼은 VARCHAR 로 두고(ENUM 타입이 아님) 이 enum 으로 검사만 함.
  // 약관이 늘어날 때 DB 타입을 고치지 않아도 되고, 옛 판에 동의한 기록이 남아 있는 상태에서
  // 약관을 접더라도 이미 저장된 문자열을 읽지 못하게 되는 일이 없음
  public enum Type {
    SERVICE("service", true),
    PRIVACY("privacy", true),
    // 마케팅 동의를 필수로 하면 개인정보 보호법 제22조 제5항 위반임
    // (선택 동의를 거부한 이유로 서비스 제공을 거부할 수 없음)
    MARKETING("marketing", false);

    private final String key;
    private final boolean required;

    Type(String key, boolean required) {
      this.key = key;
      this.required = required;
    }

    public String getKey() {
      return key;
    }

    public boolean isRequired() {
      return required;
    }

    public static Optional<Type> fromKey(String key) {
      return Arrays.stream(values()).filter(type -> type.key.equals(key)).findFirst();
    }

    public static Set<Type> required() {
      return Arrays.stream(values())
          .filter(Type::isRequired)
          .collect(Collectors.toUnmodifiableSet());
    }
  }
}
