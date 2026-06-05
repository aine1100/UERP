package com.national.utility.billing.validation;

import com.national.utility.billing.model.enums.MeterType;

/** Common shape for class-level {@link ValidMeterNumber} validation. */
public interface MeterNumberValidatable {

    String getMeterNumber();

    MeterType getMeterType();
}
