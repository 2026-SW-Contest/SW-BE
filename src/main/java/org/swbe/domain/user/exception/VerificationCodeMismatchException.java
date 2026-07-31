package org.swbe.domain.user.exception;

import org.swbe.global.error.BusinessException;

public class VerificationCodeMismatchException extends BusinessException {

  public VerificationCodeMismatchException() {
    super(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
  }
}
