package com.national.utility.billing.service;

import com.national.utility.billing.dto.response.FinanceSummaryResponse;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.Payment;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.PaymentStatus;
import com.national.utility.billing.repository.BillRepository;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.PaymentRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public FinanceSummaryResponse getFinanceSummary() {
        return FinanceSummaryResponse.builder()
                .totalBills(billRepository.count())
                .totalBilledAmount(defaultZero(billRepository.sumTotalAmount()))
                .totalOutstanding(defaultZero(billRepository.sumOutstandingBalance()))
                .unpaidBills(billRepository.countByStatus(BillStatus.UNPAID))
                .partialBills(billRepository.countByStatus(BillStatus.PARTIAL))
                .paidBills(billRepository.countByStatus(BillStatus.PAID))
                .pendingApprovalBills(billRepository.countByStatus(BillStatus.PENDING_APPROVAL))
                .pendingApprovalPayments(paymentRepository.countByStatus(PaymentStatus.PENDING_APPROVAL))
                .totalPayments(paymentRepository.countByStatus(PaymentStatus.APPROVED))
                .totalPaymentsAmount(defaultZero(paymentRepository.sumApprovedAmountPaid()))
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportBillsCsv(Pageable pageable) throws IOException {
        Page<Bill> bills = billRepository.findAll(pageable);
        return writeCsv(new String[]{
                "ID", "Reference", "Customer", "Consumption", "Amount", "Tax",
                "Penalty", "Total", "Outstanding", "Status", "Month", "Year"
        }, bills.getContent().stream().map(bill -> new String[]{
                String.valueOf(bill.getId()),
                bill.getBillReference(),
                bill.getCustomer().getFullNames(),
                bill.getConsumption().toPlainString(),
                bill.getAmount().toPlainString(),
                bill.getTax().toPlainString(),
                bill.getPenalty().toPlainString(),
                bill.getTotalAmount().toPlainString(),
                bill.getOutstandingBalance().toPlainString(),
                bill.getStatus().name(),
                String.valueOf(bill.getMonth()),
                String.valueOf(bill.getYear())
        }).toList());
    }

    @Transactional(readOnly = true)
    public byte[] exportBillsExcel(Pageable pageable) throws IOException {
        Page<Bill> bills = billRepository.findAll(pageable);
        return writeExcel("Bills", new String[]{
                "ID", "Reference", "Customer", "Consumption", "Amount", "Tax",
                "Penalty", "Total", "Outstanding", "Status", "Month", "Year"
        }, bills.getContent(), (row, bill) -> {
            row.createCell(0).setCellValue(bill.getId().toString());
            row.createCell(1).setCellValue(bill.getBillReference());
            row.createCell(2).setCellValue(bill.getCustomer().getFullNames());
            row.createCell(3).setCellValue(bill.getConsumption().doubleValue());
            row.createCell(4).setCellValue(bill.getAmount().doubleValue());
            row.createCell(5).setCellValue(bill.getTax().doubleValue());
            row.createCell(6).setCellValue(bill.getPenalty().doubleValue());
            row.createCell(7).setCellValue(bill.getTotalAmount().doubleValue());
            row.createCell(8).setCellValue(bill.getOutstandingBalance().doubleValue());
            row.createCell(9).setCellValue(bill.getStatus().name());
            row.createCell(10).setCellValue(bill.getMonth());
            row.createCell(11).setCellValue(bill.getYear());
        });
    }

    @Transactional(readOnly = true)
    public byte[] exportPaymentsCsv(Pageable pageable) throws IOException {
        Page<Payment> payments = paymentRepository.findAll(pageable);
        return writeCsv(new String[]{
                "ID", "Bill Reference", "Amount Paid", "Method", "Payment Date", "Customer"
        }, payments.getContent().stream().map(payment -> new String[]{
                String.valueOf(payment.getId()),
                payment.getBill().getBillReference(),
                payment.getAmountPaid().toPlainString(),
                payment.getPaymentMethod().name(),
                payment.getPaymentDate().toString(),
                payment.getBill().getCustomer().getFullNames()
        }).toList());
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomersCsv(Pageable pageable) throws IOException {
        Page<Customer> customers = customerRepository.findAll(pageable);
        return writeCsv(new String[]{
                "ID", "Full Names", "National ID", "Email", "Phone",
                "Province", "District", "Sector", "Cell", "Village", "Status"
        }, customers.getContent().stream().map(customer -> new String[]{
                String.valueOf(customer.getId()),
                customer.getFullNames(),
                customer.getNationalId(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress() != null ? customer.getAddress().getProvince() : "",
                customer.getAddress() != null ? customer.getAddress().getDistrict() : "",
                customer.getAddress() != null ? customer.getAddress().getSector() : "",
                customer.getAddress() != null ? customer.getAddress().getCell() : "",
                customer.getAddress() != null ? customer.getAddress().getVillage() : "",
                customer.getStatus().name()
        }).toList());
    }

    private byte[] writeCsv(String[] headers, List<String[]> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(headers);
            writer.writeAll(rows);
            writer.flush();
            return out.toByteArray();
        }
    }

    private <T> byte[] writeExcel(String sheetName, String[] headers, List<T> items,
                                  ExcelRowWriter<T> rowWriter) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (T item : items) {
                Row row = sheet.createRow(rowNum++);
                rowWriter.write(row, item);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @FunctionalInterface
    private interface ExcelRowWriter<T> {
        void write(Row row, T item);
    }
}
