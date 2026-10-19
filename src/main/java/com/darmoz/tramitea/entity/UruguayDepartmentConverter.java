package com.darmoz.tramitea.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UruguayDepartmentConverter implements AttributeConverter<UruguayDepartment, String> {

    @Override
    public String convertToDatabaseColumn(UruguayDepartment department) {
        return department == null ? null : department.getCode();
    }

    @Override
    public UruguayDepartment convertToEntityAttribute(String code) {
        return UruguayDepartment.fromCode(code);
    }
}
