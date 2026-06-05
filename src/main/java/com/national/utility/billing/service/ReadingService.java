package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.ReadingRequest;
import com.national.utility.billing.dto.response.ReadingResponse;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Meter;
import com.national.utility.billing.model.Reading;
import com.national.utility.billing.repository.MeterRepository;
import com.national.utility.billing.repository.ReadingRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Operator submits readings; {@link BillService#generateBillFromReading} persists the bill,
 * which fires the PostgreSQL bill-generation trigger (notification insert).
 */
@Service
@RequiredArgsConstructor
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final MeterRepository meterRepository;
    private final BillService billService;

    @Transactional
    public ReadingResponse submitReading(ReadingRequest request) {
        Meter meter = meterRepository.findById(request.getMeterId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));

        Reading reading = Reading.builder()
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .month(request.getMonth())
                .year(request.getYear())
                .meter(meter)
                .build();

        reading = readingRepository.save(reading);

        Bill bill = billService.generateBillFromReading(reading);
        reading.setBill(bill);
        reading = readingRepository.save(reading);

        return EntityMapper.toReadingResponse(reading);
    }

    @Transactional(readOnly = true)
    public Page<ReadingResponse> getAllReadings(Pageable pageable) {
        return readingRepository.findAll(pageable).map(EntityMapper::toReadingResponse);
    }

    @Transactional(readOnly = true)
    public ReadingResponse getReadingById(UUID id) {
        return EntityMapper.toReadingResponse(findReading(id));
    }

    private Reading findReading(UUID id) {
        return readingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found with id: " + id));
    }
}
