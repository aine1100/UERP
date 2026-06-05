package com.national.utility.billing.validation;

import com.national.utility.billing.dto.request.PaymentRequest;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Reading;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.repository.BillRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ValidPaymentValidator implements ConstraintValidator<ValidPayment, PaymentRequest> {

    private final BillRepository billRepository;

    @Override
    public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (request.getBillId() == null) {
            return true;
        }

        Bill bill = billRepository.findByIdWithReading(request.getBillId()).orElse(null);
        if (bill == null) {
            context.buildConstraintViolationWithTemplate("Bill not found")
                    .addPropertyNode("billId")
                    .addConstraintViolation();
            return false;
        }

        if (bill.getStatus() != BillStatus.UNPAID && bill.getStatus() != BillStatus.PARTIAL) {
            context.buildConstraintViolationWithTemplate(
                    "Payments are only allowed on finance-approved bills that are unpaid or partially paid")
                    .addPropertyNode("billId")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.getPaymentDate() != null) {
            Reading reading = bill.getReading();
            LocalDate readingDate = reading != null ? reading.getReadingDate() : null;

            if (readingDate != null && request.getPaymentDate().isBefore(readingDate)) {
                context.buildConstraintViolationWithTemplate(
                        "Payment date cannot be before the meter reading date ("
                                + readingDate + ") for this bill")
                        .addPropertyNode("paymentDate")
                        .addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}
