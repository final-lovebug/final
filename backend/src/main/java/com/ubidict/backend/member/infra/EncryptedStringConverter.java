package com.ubidict.backend.member.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link com.ubidict.backend.member.domain.Member}의 {@code email}/{@code displayName}에
 * 붙는 JPA 컨버터. 실제 암·복호화는 {@link MemberFieldEncryptor}에 위임한다 — JPA가 이 클래스를
 * 리플렉션으로 직접 생성해 Spring 빈 주입을 못 받기 때문에, 암호화 로직 자체는 여기 두지 않는다.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : MemberFieldEncryptor.instance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MemberFieldEncryptor.instance().decrypt(dbData);
    }
}
