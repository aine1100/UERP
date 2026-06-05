package com.national.utility.billing.service.mapper;

import com.national.utility.billing.dto.common.LocationAddressDto;
import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.dto.response.*;
import com.national.utility.billing.model.*;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullNames(user.getFullNames())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullNames(customer.getFullNames())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .location(LocationSelectionDto.fromAddressDto(
                        LocationAddressDto.fromEntity(customer.getAddress())))
                .status(customer.getStatus())
                .userId(customer.getUser() != null ? customer.getUser().getId() : null)
                .createdAt(customer.getCreatedAt())
                .build();
    }

    public static MeterResponse toMeterResponse(Meter meter) {
        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .meterType(meter.getMeterType())
                .installationDate(meter.getInstallationDate())
                .status(meter.getStatus())
                .customerId(meter.getCustomer().getId())
                .customerName(meter.getCustomer().getFullNames())
                .createdAt(meter.getCreatedAt())
                .build();
    }

    public static ReadingResponse toReadingResponse(Reading reading) {
        return ReadingResponse.builder()
                .id(reading.getId())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .readingDate(reading.getReadingDate())
                .month(reading.getMonth())
                .year(reading.getYear())
                .meterId(reading.getMeter().getId())
                .meterNumber(reading.getMeter().getMeterNumber())
                .billId(reading.getBill() != null ? reading.getBill().getId() : null)
                .createdAt(reading.getCreatedAt())
                .build();
    }

    public static BillResponse toBillResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billReference(bill.getBillReference())
                .consumption(bill.getConsumption())
                .amount(bill.getAmount())
                .tax(bill.getTax())
                .penalty(bill.getPenalty())
                .totalAmount(bill.getTotalAmount())
                .outstandingBalance(bill.getOutstandingBalance())
                .status(bill.getStatus())
                .month(bill.getMonth())
                .year(bill.getYear())
                .customerId(bill.getCustomer().getId())
                .customerName(bill.getCustomer().getFullNames())
                .readingId(bill.getReading().getId())
                .utilityType(bill.getReading().getMeter().getMeterType())
                .createdAt(bill.getCreatedAt())
                .build();
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .billId(payment.getBill().getId())
                .billReference(payment.getBill().getBillReference())
                .remainingBalance(payment.getBill().getOutstandingBalance())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public static TariffResponse toTariffResponse(Tariff tariff) {
        return TariffResponse.builder()
                .id(tariff.getId())
                .utilityType(tariff.getUtilityType())
                .ratePerUnit(tariff.getRatePerUnit())
                .fixedServiceCharge(tariff.getFixedServiceCharge())
                .vatPercentage(tariff.getVatPercentage())
                .latePenaltyFee(tariff.getLatePenaltyFee())
                .version(tariff.getVersion())
                .effectiveFrom(tariff.getEffectiveFrom())
                .status(tariff.getStatus())
                .createdAt(tariff.getCreatedAt())
                .build();
    }
}
