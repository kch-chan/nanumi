package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.entity.TermsAgreement.Type;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("약관 동의 기록 엔티티")
class TermsAgreementTest {

  private static final LocalDateTime AGREED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

  private User newUser() {
    return User.builder().nickname("나눔이").aptName("행복아파트").build();
  }

  private TermsAgreement agreement(String key, boolean agreed) {
    return TermsAgreement.builder()
        .user(newUser())
        .termsKey(key)
        .termsVersion("시행일자 2026년 8월")
        .agreed(agreed)
        .agreedAt(AGREED_AT)
        .build();
  }

  @Test
  @DisplayName("만든 값이 그대로 담김")
  void 기본값() {
    TermsAgreement saved = agreement("service", true);

    assertThat(saved.getTermsKey()).isEqualTo("service");
    assertThat(saved.getTermsVersion()).isEqualTo("시행일자 2026년 8월");
    assertThat(saved.isAgreed()).isTrue();
    assertThat(saved.getAgreedAt()).isEqualTo(AGREED_AT);
    assertThat(saved.getUser().getNickname()).isEqualTo("나눔이");
  }

  // 선택 약관을 거부한 것도 한 줄로 남김
  // "물어봤고 거부했다" 는 사실 자체가 마케팅을 보내지 않았음을 증명하는 기록임
  @Test
  @DisplayName("거부한 기록도 남길 수 있음")
  void 거부_기록() {
    assertThat(agreement("marketing", false).isAgreed()).isFalse();
  }

  // 칸 길이가 줄어들면 저장할 때 값이 잘림
  // terms_version 에는 "시행일자 2026년 8월" 같은 문장이 들어가므로 20자로는 모자람
  @Test
  @DisplayName("칸 길이 상수는 20 과 40 임")
  void 길이_상수() {
    assertThat(TermsAgreement.TERMS_KEY_LENGTH).isEqualTo(20);
    assertThat(TermsAgreement.TERMS_VERSION_LENGTH).isEqualTo(40);
  }

  @Test
  @DisplayName("저장하는 값은 칸 길이 안에 들어감")
  void 값이_칸에_들어감() {
    TermsAgreement saved = agreement("marketing", true);

    assertThat(saved.getTermsKey().length()).isLessThanOrEqualTo(TermsAgreement.TERMS_KEY_LENGTH);
    assertThat(saved.getTermsVersion().length())
        .isLessThanOrEqualTo(TermsAgreement.TERMS_VERSION_LENGTH);
  }

  @Nested
  @DisplayName("약관 종류 enum")
  class TypeTest {

    // 이 값은 화면 상수(frontend/src/constants/terms.ts 의 TermsKey)와 글자까지 같아야 함
    // 서버가 'SERVICE' 를 기대하는데 화면이 'service' 를 보내면 모르는 key 로 버려지고,
    // 필수 약관이 빠진 것으로 판정되어 가입이 전부 막힘
    @Test
    @DisplayName("key 는 화면 상수와 같은 소문자임")
    void 소문자_key() {
      assertThat(Type.SERVICE.getKey()).isEqualTo("service");
      assertThat(Type.PRIVACY.getKey()).isEqualTo("privacy");
      assertThat(Type.MARKETING.getKey()).isEqualTo("marketing");
    }

    @Test
    @DisplayName("key 로 종류를 찾음")
    void key_로_찾기() {
      assertThat(Type.fromKey("privacy")).contains(Type.PRIVACY);
    }

    // 모르는 key 가 와도 예외를 던지지 않고 빈 값을 돌려줌
    // AuthService 가 이걸 그냥 버리기 때문에, 화면이 먼저 배포되어 서버가 모르는 약관을
    // 보내더라도 가입 전체가 멈추지 않음
    @Test
    @DisplayName("모르는 key 는 빈 값을 돌려줌")
    void 모르는_key() {
      assertThat(Type.fromKey("unknown")).isEmpty();
      assertThat(Type.fromKey("SERVICE")).isEmpty();
      assertThat(Type.fromKey(null)).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("필수 약관은 서비스 이용약관과 개인정보 수집·이용 동의 둘뿐임")
    void 필수_목록() {
      assertThat(Type.required()).containsExactlyInAnyOrder(Type.SERVICE, Type.PRIVACY);
    }

    // 마케팅을 필수로 바꾸면 개인정보 보호법 제22조 제5항 위반임
    // (선택 동의를 거부했다는 이유로 서비스 제공을 거부할 수 없음)
    // required=true 로 바꾸는 순간 이 테스트가 깨지도록 해 둠
    @Test
    @DisplayName("마케팅 수신 동의는 선택임")
    void 마케팅은_선택() {
      assertThat(Type.MARKETING.isRequired()).isFalse();
      assertThat(Type.required()).doesNotContain(Type.MARKETING);
    }
  }
}
