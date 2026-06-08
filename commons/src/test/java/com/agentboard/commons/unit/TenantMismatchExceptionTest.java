package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.exceptions.TenantMismatchException;
import org.junit.jupiter.api.Test;

class TenantMismatchExceptionTest {

  @Test
  void shouldSetMessage_whenConstructedWithMessage() {
    TenantMismatchException ex = new TenantMismatchException("Tenant mismatch detected");

    assertThat(ex.getMessage()).isEqualTo("Tenant mismatch detected");
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
