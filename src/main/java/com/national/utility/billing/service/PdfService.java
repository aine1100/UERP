package com.national.utility.billing.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);

    public byte[] generatePaymentReceipt(Bill bill, Payment payment) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("NATIONAL UTILITY BILLING SYSTEM", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Payment Receipt", HEADER_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20f);
            document.add(subtitle);

            document.add(new Paragraph("Bill Reference: " + bill.getBillReference(), NORMAL_FONT));
            document.add(new Paragraph("Customer: " + bill.getCustomer().getFullNames(), NORMAL_FONT));
            document.add(new Paragraph("Period: " + bill.getMonth() + "/" + bill.getYear(), NORMAL_FONT));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addTableRow(table, "Amount Paid", "RWF " + payment.getAmountPaid());
            addTableRow(table, "Payment Method", payment.getPaymentMethod().name());
            addTableRow(table, "Payment Date", payment.getPaymentDate().toString());
            addTableRow(table, "Bill Total", "RWF " + bill.getTotalAmount());
            addTableRow(table, "Remaining Balance", "RWF " + bill.getOutstandingBalance());
            addTableRow(table, "Bill Status", bill.getStatus().name());
            document.add(table);

            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                    "Generated on: " + java.time.LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    NORMAL_FONT);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate PDF receipt: {}", ex.getMessage());
            throw new RuntimeException("Failed to generate payment receipt PDF", ex);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
