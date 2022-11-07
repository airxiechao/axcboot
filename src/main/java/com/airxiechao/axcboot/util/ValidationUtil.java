package com.airxiechao.axcboot.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValidationUtil {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static <T> void validate(T obj) throws Exception{
        Set<ConstraintViolation<T>> violations = validator.validate(obj);
        if(violations.size() == 0){
            return;
        }

        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<T> violation : violations) {
            errors.add(violation.getMessage());
        }

        throw new Exception("validation error: " + String.join("; ", errors));
    }
}
