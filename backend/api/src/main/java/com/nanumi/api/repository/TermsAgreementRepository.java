package com.nanumi.api.repository;

import com.nanumi.api.entity.TermsAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Integer> {

  // 회원이 어떤 약관에 동의했는지 확인할 때 씀
  List<TermsAgreement> findByUser_Id(Integer userId);

  // 보유 기한이 지난 회원의 동의 기록을 파기할 때 씀
  // users 에 ON DELETE CASCADE 가 걸려 있지만, JPA 가 아는 순서로 지워야
  // 영속성 컨텍스트와 DB 가 어긋나지 않음
  void deleteByUser_Id(Integer userId);
}
