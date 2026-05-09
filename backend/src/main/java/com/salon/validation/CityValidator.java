package com.salon.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class CityValidator implements ConstraintValidator<ValidCity, String> {

    private static final Set<String> VALID_CITIES = Set.of(
            "Visakhapatnam", "Vijayawada", "Hyderabad", "Ananthapur", "Khammam"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && VALID_CITIES.contains(value);
    }
}
