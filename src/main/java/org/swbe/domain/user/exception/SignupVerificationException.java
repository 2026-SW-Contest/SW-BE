package org.swbe.domain.user.exception;

import org.swbe.global.error.BusinessException;

public class SignupVerificationException extends BusinessException {

  private SignupVerificationException(AuthErrorCode errorCode) {
    super(errorCode);
  }

  public static SignupVerificationException invalidToken() {
    return new SignupVerificationException(
        AuthErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN
    );
  }

  public static SignupVerificationException expired() {
    return new SignupVerificationException(
        AuthErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED
    );
  }

  public static SignupVerificationException alreadyConsumed() {
    return new SignupVerificationException(
        AuthErrorCode.EMAIL_VERIFICATION_TOKEN_CONSUMED
    );
  }

  public static SignupVerificationException emailMismatch() {
    return new SignupVerificationException(
        AuthErrorCode.EMAIL_VERIFICATION_EMAIL_MISMATCH
    );
  }
}
