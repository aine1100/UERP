package com.national.utility.billing.validation;

import com.national.utility.billing.dto.request.ReadingRequest;
import com.national.utility.billing.model.Meter;
import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.repository.MeterRepository;
import com.national.utility.billing.repository.ReadingRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidReadingValidator implements ConstraintValidator<ValidReading, ReadingRequest> {

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;

    @Override
    public boolean isValid(ReadingRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (request.getCurrentReading() != null && request.getPreviousReading() != null
                && request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            context.buildConstraintViolationWithTemplate(
                    "Current reading must be greater than previous reading")
                    .addPropertyNode("currentReading")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.getMeterId() == null) {
            return valid;
        }

        Meter meter = meterRepository.findById(request.getMeterId()).orElse(null);
        if (meter == null) {
            context.buildConstraintViolationWithTemplate("Meter not found")
                    .addPropertyNode("meterId")
                    .addConstraintViolation();
            return false;
        }

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            context.buildConstraintViolationWithTemplate("Meter must be Active to submit a reading")
                    .addPropertyNode("meterId")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.getMonth() != null && request.getYear() != null
                && readingRepository.existsByMeterIdAndMonthAndYear(
                        request.getMeterId(), request.getMonth(), request.getYear())) {
            context.buildConstraintViolationWithTemplate(
                    "A reading already exists for this meter in the specified month and year")
                    .addPropertyNode("month")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
