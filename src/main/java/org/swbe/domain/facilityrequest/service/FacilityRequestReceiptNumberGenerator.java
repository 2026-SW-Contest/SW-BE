package org.swbe.domain.facilityrequest.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FacilityRequestReceiptNumberGenerator {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.BASIC_ISO_DATE;

  private final JdbcTemplate jdbcTemplate;

  public FacilityRequestReceiptNumberGenerator(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String next(LocalDate receiptDate) {
    jdbcTemplate.update(
        """
            INSERT INTO facility_request_receipt_sequence (
                receipt_date,
                current_value
            ) VALUES (?, LAST_INSERT_ID(1))
            ON DUPLICATE KEY UPDATE
                current_value = LAST_INSERT_ID(current_value + 1)
            """,
        Date.valueOf(receiptDate)
    );

    Long sequence = jdbcTemplate.queryForObject(
        "SELECT LAST_INSERT_ID()",
        Long.class
    );
    if (sequence == null) {
      throw new IllegalStateException("Receipt sequence was not generated");
    }

    return "FR-%s-%04d".formatted(
        receiptDate.format(DATE_FORMATTER),
        sequence
    );
  }
}
