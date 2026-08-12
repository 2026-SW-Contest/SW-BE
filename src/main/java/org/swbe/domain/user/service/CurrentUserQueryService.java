package org.swbe.domain.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.campus.entity.Department;
import org.swbe.domain.user.dto.response.CurrentUserDataResponse;
import org.swbe.domain.user.dto.response.CurrentUserDepartmentResponse;
import org.swbe.domain.user.dto.response.CurrentUserResponse;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.UserErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserQueryService {

  private final AppUserRepository appUserRepository;
  private final UserRoleRepository userRoleRepository;

  // 로그인한 사용자의 계정 정보와 현재 활성화된 역할을 조회한다.
  public CurrentUserResponse getCurrentUser(Long userId) {
    AppUser user = appUserRepository.findByIdWithDepartment(userId)
        .orElseThrow(() -> new BusinessException(
            UserErrorCode.NOT_FOUND
        ));
    List<String> roles =
        userRoleRepository.findActiveRoleCodesByUserId(userId);
    CurrentUserDepartmentResponse department =
        createDepartmentResponse(user.getDepartment());
    CurrentUserDataResponse data = new CurrentUserDataResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getStudentNumber(),
        department,
        roles
    );

    return new CurrentUserResponse(data);
  }

  // 소속 부서가 있는 사용자만 부서 응답 정보를 생성한다.
  private CurrentUserDepartmentResponse createDepartmentResponse(
      Department department
  ) {
    if (department == null) {
      return null;
    }

    return new CurrentUserDepartmentResponse(
        department.getId(),
        department.getName()
    );
  }
}
