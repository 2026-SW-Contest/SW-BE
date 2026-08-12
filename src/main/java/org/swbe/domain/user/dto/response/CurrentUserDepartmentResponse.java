package org.swbe.domain.user.dto.response;

public record CurrentUserDepartmentResponse(
    Long departmentId,
    String departmentName
) {
}
