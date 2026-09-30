package com.nanumi.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanumi.api.entity.User.Role;
import com.nanumi.api.entity.User.Status;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("회원 엔티티")
class UserTest {

  private User newUser() {
    return User.builder().nickname("나눔이").aptName("행복아파트").dong("101").ho("1502").build();
  }

  @Test
  @DisplayName("새로 만들면 일반 회원이고 활동 중임")
  void 기본값() {
    User user = newUser();

    assertThat(user.getRole()).isEqualTo(Role.USER);
    assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(user.isWithdrawn()).isFalse();
    assertThat(user.getWithdrawnAt()).isNull();
  }

  @Test
  @DisplayName("탈퇴하면 상태·시각·사유가 함께 남음")
  void 탈퇴() {
    User user = newUser();
    LocalDateTime before = LocalDateTime.now();

    user.withdraw("이사 갑니다");

    assertThat(user.isWithdrawn()).isTrue();
    assertThat(user.getStatus()).isEqualTo(Status.WITHDRAWN);
    assertThat(user.getWithdrawalReason()).isEqualTo("이사 갑니다");
    assertThat(user.getWithdrawnAt()).isNotNull().isAfterOrEqualTo(before);
  }

  // 사유는 개인정보 수집·이용 동의에서 선택 항목이라 안 적어도 탈퇴는 되어야 함
  @Test
  @DisplayName("사유를 안 적어도 탈퇴는 됨")
  void 사유_없이_탈퇴() {
    User user = newUser();

    user.withdraw(null);

    assertThat(user.isWithdrawn()).isTrue();
    assertThat(user.getWithdrawalReason()).isNull();
  }

  @Test
  @DisplayName("닉네임·아파트·동·호를 바꿀 수 있음")
  void 정보_변경() {
    User user = newUser();

    user.changeNickname("새이름");
    user.changeApt("행복2차");
    user.changeDong("202");
    user.changeHo("301");

    assertThat(user.getNickname()).isEqualTo("새이름");
    assertThat(user.getAptName()).isEqualTo("행복2차");
    assertThat(user.getDong()).isEqualTo("202");
    assertThat(user.getHo()).isEqualTo("301");
  }

  // 탈퇴 사유 칸 길이는 WithdrawalRequest 의 @Size 와 같아야 함
  // 어긋나면 검증은 통과했는데 DB 에서 잘리는 일이 생김
  @Test
  @DisplayName("탈퇴 사유 칸 길이는 255 임")
  void 사유_길이_상수() {
    assertThat(User.WITHDRAWAL_REASON_LENGTH).isEqualTo(255);
  }
}
