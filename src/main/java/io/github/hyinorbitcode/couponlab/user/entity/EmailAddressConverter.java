package io.github.hyinorbitcode.couponlab.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** EmailAddress ↔ users.email 문자열 컬럼. 스키마는 그대로 문자열 하나다. */
@Converter(autoApply = true)
public class EmailAddressConverter implements AttributeConverter<EmailAddress, String> {

    @Override
    public String convertToDatabaseColumn(EmailAddress attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public EmailAddress convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new EmailAddress(dbData);
    }
}
