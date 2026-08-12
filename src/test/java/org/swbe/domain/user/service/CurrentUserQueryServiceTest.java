package org.swbe.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.campus.entity.Department;
import org.swbe.domain.user.dto.response.CurrentUserResponse;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.domain.user.exception.UserErrorCode;
import org.swbe.domain.user.repository.AppUserRepository;
import org.swbe.domain.user.repository.UserRoleRepository;
import org.swbe.global.error.BusinessException;

class CurrentUserQueryServiceTest {

  private static final Long USER_ID = 10L;

  private AppUserRepository appUserRepository;
  private UserRoleRepository userRoleRepository;
  private CurrentUserQueryService service;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    userRoleRepository = mock(UserRoleRepository.class);
    service = new CurrentUserQueryService(
        appUserRepository,
        userRoleRepository
    );
  }

  @Test
  void returnsCurrentUserWithDepartmentAndActiveRoles() {
    Department department = mock(Department.class);
    when(department.getId()).thenReturn(1L);
    when(department.getName()).thenReturn("시설관리팀");
    AppUser user = user(department);
    when(appUserRepository.findByIdWithDepartment(USER_ID))
        .thenReturn(Optional.of(user));
    when(userRoleRepository.findActiveRoleCodesByUserId(USER_ID))
        .thenReturn(List.of("ADMIN"));

    CurrentUserResponse response = service.getCurrentUser(USER_ID);

    assertThat(response.data().userId()).isEqualTo(USER_ID);
    assertThat(response.data().name()).isEqualTo("커넥띵관리자");
    assertThat(response.data().department().departmentId()).isEqualTo(1L);
    assertThat(response.data().department().departmentName())
        .isEqualTo("시설관리팀");
    assertThat(response.data().roles()).containsExactly("ADMIN");
  }

  @Test
  void returnsNullDepartmentWhenUserHasNoDepartment() {
    AppUser user = user(null);
    when(appUserRepository.findByIdWithDepartment(USER_ID))
        .thenReturn(Optional.of(user));
    when(userRoleRepository.findActiveRoleCodesByUserId(USER_ID))
        .thenReturn(List.of("STUDENT"));

    CurrentUserResponse response = service.getCurrentUser(USER_ID);

    assertThat(response.data().department()).isNull();
    assertThat(response.data().roles()).containsExactly("STUDENT");
  }

  @Test
  void missingUserReturnsBusinessError() {
    when(appUserRepository.findByIdWithDepartment(USER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCurrentUser(USER_ID))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.NOT_FOUND)
        );
  }

  private AppUser user(Department department) {
    AppUser user = mock(AppUser.class);
    when(user.getId()).thenReturn(USER_ID);
    when(user.getName()).thenReturn("커넥띵관리자");
    when(user.getEmail()).thenReturn("admin@mju.ac.kr");
    when(user.getStudentNumber()).thenReturn(null);
    when(user.getDepartment()).thenReturn(department);
    return user;
  }
}
