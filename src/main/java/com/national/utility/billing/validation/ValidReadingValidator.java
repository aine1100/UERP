package com.national.utility.billing.validation;

import com.national.utility.billing.dto.request.ReadingRequest;
import com.national.utility.billing.model.Meter;
import com.national.utility.billing.model.Reading;
import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.repository.MeterRepository;
import com.national.utility.billing.repository.ReadingRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

        if (request.getReadingDate() != null && meter.getInstallationDate() != null
                && request.getReadingDate().isBefore(meter.getInstallationDate())) {
            context.buildConstraintViolationWithTemplate(
                    "Reading date cannot be before the meter installation date ("
                            + meter.getInstallationDate() + ")")
                    .addPropertyNode("readingDate")
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

        if (request.getPreviousReading() != null && request.getMonth() != null && request.getYear() != null) {
            Optional<Reading> latestReading = readingRepository
                    .findTopByMeterIdOrderByYearDescMonthDesc(request.getMeterId());

            if (latestReading.isPresent()) {
                Reading last = latestReading.get();

                if (!isAfter(request.getMonth(), request.getYear(), last.getMonth(), last.getYear())) {
                    context.buildConstraintViolationWithTemplate(
                            "Billing month/year must be after the most recent reading ("
                                    + last.getMonth() + "/" + last.getYear() + ")")
                            .addPropertyNode("month")
                            .addConstraintViolation();
                    valid = false;
                }

                if (request.getPreviousReading().compareTo(last.getCurrentReading()) != 0) {
                    context.buildConstraintViolationWithTemplate(
                            "Previous reading must match the last recorded current reading ("
                                    + last.getCurrentReading() + ")")
                            .addPropertyNode("previousReading")
                            .addConstraintViolation();
                    valid = false;
                }
            }
        }

        return valid;
    }

    private boolean isAfter(int month, int year, int priorMonth, int priorYear) {
        return year > priorYear || (year == priorYear && month > priorMonth);
    }
}
