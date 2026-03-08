package com.itr.service;

import com.itr.dto.Itr1FormData;
import com.itr.dto.Itr1FormData.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class Itr1ReportService {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public byte[] generateExcelReport(Itr1FormData formData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Create sheets
            createPersonalInfoSheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle);
            createSalarySheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);
            createHousePropertySheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);
            createOtherSourcesSheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);
            createDeductionsSheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);
            createTaxComputationSheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);
            createTaxesPaidSheet(workbook, formData, headerStyle, subHeaderStyle, dataStyle, currencyStyle);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createPersonalInfoSheet(Workbook workbook, Itr1FormData formData, 
                                         CellStyle headerStyle, CellStyle subHeaderStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Personal Information");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);

        int rowNum = 0;
        PartA_GeneralInfo partA = formData.getPartA();

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ITR-1 FORM - PERSONAL INFORMATION");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row

        // Personal details
        addDataRow(sheet, rowNum++, "Name", partA.getAssesseeName(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "PAN", partA.getPan(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Aadhaar", partA.getAadhaar(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Date of Birth", partA.getDob(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Email", partA.getEmail(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Mobile", partA.getMobile(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Assessment Year", partA.getAssessmentYear(), subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Tax Regime", partA.isNewTaxRegime() ? "New Regime" : "Old Regime", subHeaderStyle, dataStyle);
        addDataRow(sheet, rowNum++, "Residential Status", partA.getResidentialStatus(), subHeaderStyle, dataStyle);
    }

    private void createSalarySheet(Workbook workbook, Itr1FormData formData,
                                   CellStyle headerStyle, CellStyle subHeaderStyle, 
                                   CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Schedule Salary");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        ScheduleSalary sal = formData.getScheduleSalary();

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("SCHEDULE SALARY - Income chargeable under head 'Salaries'");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row

        // Salary components
        addCurrencyRow(sheet, rowNum++, "(ia) Salary as per sec 17(1)", sal.getSalaryU17_1(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(ib) Perquisites u/s 17(2)", sal.getPerquisitesU17_2(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(ic) Profits in lieu of salary u/s 17(3)", sal.getProfitsU17_3(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(id) Income u/s 89A (notified)", sal.getIncomeU89A_notified(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(ie) Income u/s 89A (other)", sal.getIncomeU89A_other(), subHeaderStyle, currencyStyle);
        
        rowNum++; // Empty row
        addCurrencyRow(sheet, rowNum++, "(ii) Gross Salary [Rule 59]", sal.getGrossSalary(), headerStyle, currencyStyle);
        
        rowNum++; // Empty row
        addCurrencyRow(sheet, rowNum++, "(iia) Allowances exempt u/s 10", sal.getAllowancesExemptU10(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(iib) Relief u/s 89A", sal.getReliefU89A(), subHeaderStyle, currencyStyle);
        
        rowNum++; // Empty row
        addCurrencyRow(sheet, rowNum++, "(iii) Net Salary [Rule 60]", sal.getNetSalary(), headerStyle, currencyStyle);
        
        rowNum++; // Empty row
        Row deductionsHeader = sheet.createRow(rowNum++);
        Cell dedCell = deductionsHeader.createCell(0);
        dedCell.setCellValue("Deductions u/s 16");
        dedCell.setCellStyle(subHeaderStyle);
        
        addCurrencyRow(sheet, rowNum++, "(iva) Standard deduction u/s 16(ia)", sal.getStandardDeduction(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(ivb) Entertainment allowance u/s 16(ii)", sal.getEntertainmentAllowance(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "(ivc) Professional tax u/s 16(iii)", sal.getProfessionalTax(), subHeaderStyle, currencyStyle);
        
        rowNum++; // Empty row
        addCurrencyRow(sheet, rowNum++, "(iv) Total deductions u/s 16 [Rule 61]", sal.getTotalDeductionsU16(), headerStyle, currencyStyle);
        
        rowNum++; // Empty row
        addCurrencyRow(sheet, rowNum++, "(v) Income chargeable under Salaries [Rule 62]", sal.getIncomeFromSalary(), headerStyle, currencyStyle);
    }

    private void createHousePropertySheet(Workbook workbook, Itr1FormData formData,
                                         CellStyle headerStyle, CellStyle subHeaderStyle,
                                         CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("House Property");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        ScheduleHouseProperty hp = formData.getScheduleHP();

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("SCHEDULE HP - Income from House Property");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++;
        addDataRow(sheet, rowNum++, "Property Type", hp.getPropertyType(), subHeaderStyle, dataStyle);
        addCurrencyRow(sheet, rowNum++, "Gross Rent Received", hp.getGrossRent(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Municipal Tax Paid", hp.getMunicipalTaxPaid(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Annual Value", hp.getAnnualValue(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Standard Deduction (30%)", hp.getStandardDeduction30Pct(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Interest on Loan u/s 24(b)", hp.getInterestOnLoanU24b(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Arrears/Unrealized Rent", hp.getArrearsUnrealizedRent(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Income from House Property", hp.getIncomeFromHP(), headerStyle, currencyStyle);
    }

    private void createOtherSourcesSheet(Workbook workbook, Itr1FormData formData,
                                        CellStyle headerStyle, CellStyle subHeaderStyle,
                                        CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Other Sources");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        ScheduleOtherSources os = formData.getScheduleOS();

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("SCHEDULE OS - Income from Other Sources");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Savings Account Interest", os.getSavingsInterest(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Fixed Deposit Interest", os.getDepositInterest(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Income Tax Refund Interest", os.getIncomeTaxRefundInterest(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Family Pension", os.getFamilyPension(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Dividend Income", os.getDividendIncome(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Other Income", os.getOtherIncome(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Gross Income from Other Sources", os.getGrossOtherSources(), headerStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Deduction u/s 57(iia)", os.getDeductionU57iia(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Net Income from Other Sources", os.getIncomeFromOtherSources(), headerStyle, currencyStyle);
    }

    private void createDeductionsSheet(Workbook workbook, Itr1FormData formData,
                                      CellStyle headerStyle, CellStyle subHeaderStyle,
                                      CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Deductions VI-A");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        DeductionsVIA ded = formData.getDeductionsVIA();

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DEDUCTIONS UNDER CHAPTER VI-A");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Section 80C", ded.getSection80C(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80CCC", ded.getSection80CCC(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80CCD(1)", ded.getSection80CCD1(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Total 80C+CCC+CCD(1) [Max â‚¹1,50,000]", ded.getTotal80C_CCC_CCD1(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80CCD(1B) [Max â‚¹50,000]", ded.getSection80CCD1B(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80CCD(2) - Employer NPS", ded.getSection80CCD2(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80D - Health Insurance", ded.getSection80D(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80DD - Disability", ded.getSection80DD(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80DDB - Medical Treatment", ded.getSection80DDB(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80E - Education Loan", ded.getSection80E(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80EE - Home Loan Interest", ded.getSection80EE(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80EEA - Affordable Housing", ded.getSection80EEA(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80EEB - Electric Vehicle Loan", ded.getSection80EEB(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80G - Donations", ded.getSection80G(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80GG - Rent Paid", ded.getSection80GG(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80GGA - Scientific Research", ded.getSection80GGA(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80GGC - Political Contributions", ded.getSection80GGC(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80TTA - Savings Interest", ded.getSection80TTA(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80TTB - Senior Citizen Interest", ded.getSection80TTB(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Section 80U - Self Disability", ded.getSection80U(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "TOTAL DEDUCTIONS", ded.getTotalDeductions(), headerStyle, currencyStyle);
    }

    private void createTaxComputationSheet(Workbook workbook, Itr1FormData formData,
                                          CellStyle headerStyle, CellStyle subHeaderStyle,
                                          CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Tax Computation");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        TaxComputation comp = formData.getComputation();

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TAX COMPUTATION");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Gross Total Income", comp.getGrossTotalIncome(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Total Deductions", comp.getTotalDeductions(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Total Taxable Income", comp.getTotalTaxableIncome(), headerStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Tax on Total Income", comp.getTaxOnIncome(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Rebate u/s 87A", comp.getRebateU87A(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Tax After Rebate", comp.getTaxAfterRebate(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Surcharge", comp.getSurcharge(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Health & Education Cess (4%)", comp.getCessAt4Pct(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Total Tax Liability", comp.getTotalTaxLiability(), headerStyle, currencyStyle);
        
        addCurrencyRow(sheet, rowNum++, "Relief u/s 89", comp.getReliefU89(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Total Tax After Relief", comp.getTotalTaxAfterRelief(), headerStyle, currencyStyle);
        
        addCurrencyRow(sheet, rowNum++, "Total Taxes Paid", comp.getTotalTaxesPaid(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Interest Payable", comp.getInterestPayable(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "Balance Tax Payable", comp.getBalanceTaxPayable(), headerStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Refund Due", comp.getRefundDue(), headerStyle, currencyStyle);
    }

    private void createTaxesPaidSheet(Workbook workbook, Itr1FormData formData,
                                     CellStyle headerStyle, CellStyle subHeaderStyle,
                                     CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Taxes Paid");
        sheet.setColumnWidth(0, 12000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;
        ScheduleTaxesPaid taxes = formData.getTaxesPaid();

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TAXES PAID");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++;
        addCurrencyRow(sheet, rowNum++, "TDS on Salary", taxes.getTdsOnSalary(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "TDS Other Than Salary", taxes.getTdsOtherThanSalary(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "TDS u/s 194-IA/IB/M", taxes.getTds3(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "TCS", taxes.getTcs(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Advance Tax", taxes.getAdvanceTax(), subHeaderStyle, currencyStyle);
        addCurrencyRow(sheet, rowNum++, "Self Assessment Tax", taxes.getSelfAssessmentTax(), subHeaderStyle, currencyStyle);
        
        rowNum++;
        addCurrencyRow(sheet, rowNum++, "TOTAL TAXES PAID", taxes.getTotalTaxesPaid(), headerStyle, currencyStyle);
    }

    // Helper methods for styling
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("â‚¹#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void addDataRow(Sheet sheet, int rowNum, String label, String value,
                           CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(valueStyle);
    }

    private void addCurrencyRow(Sheet sheet, int rowNum, String label, double value,
                               CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // PDF GENERATION â€” CA-style Statement of Income
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    public byte[] generatePdfReport(Itr1FormData formData) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(outputStream);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf,
                com.itextpdf.kernel.geom.PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            com.itextpdf.kernel.font.PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            com.itextpdf.kernel.font.PdfFont normal = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            PartA_GeneralInfo a = formData.getPartA();
            ScheduleSalary sal = formData.getScheduleSalary();
            ScheduleOtherSources os = formData.getScheduleOS();
            TaxComputation comp = formData.getComputation();
            ScheduleTaxesPaid taxes = formData.getTaxesPaid();

            String ayYear = a.getAssessmentYear() != null ? a.getAssessmentYear() : "AY2025-26";
            // Derive previous year from AY (e.g. AY2025-26 -> 2024-2025)
            String prevYear = derivePreviousYear(ayYear);

            // â”€â”€ PAGE 1: Header + Statement of Income â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

            // AY header (right-aligned)
            document.add(new com.itextpdf.layout.element.Paragraph("A.Y. " + ayYear.replace("AY", ""))
                .setFont(bold).setFontSize(11)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                .setMarginBottom(4));

            // Name / Previous Year row
            addHeaderRow(document, normal, bold,
                "Name : " + nvl(a.getAssesseeName()),
                "Previous Year : " + prevYear);
            addHeaderRow(document, normal, bold,
                "PAN : " + nvl(a.getPan()),
                "Aadhaar No. : " + formatAadhaar(nvl(a.getAadhaar())));
            addHeaderRow(document, normal, bold,
                "Date of Birth : " + formatDob(nvl(a.getDob())),
                "Status : Individual\n" + resolveStatus(a.getResidentialStatus()));

            document.add(new com.itextpdf.layout.element.Paragraph(" ")
                .setFontSize(4).setMarginBottom(2));

            // Divider line
            addHorizontalRule(document);

            // "Statement of Income" title
            document.add(new com.itextpdf.layout.element.Paragraph("Statement of Income")
                .setFont(bold).setFontSize(13)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginTop(6).setMarginBottom(2));

            String regimeLabel = a.isNewTaxRegime()
                ? "Tax u/s 115BAC (New Regime)"
                : "Tax as per Old Regime";
            document.add(new com.itextpdf.layout.element.Paragraph(regimeLabel)
                .setFont(normal).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(8));

            // Main 4-column income table: Description | Sch | Rs (sub) | Rs (total)
            com.itextpdf.layout.element.Table incTable = newStatementTable();

            // Column headers
            addStatementHeader(incTable, bold, "Particulars", "Sch.", "Rs.", "Rs.");

            // â”€â”€ Income from Salaries â”€â”€
            String empLabel = sal != null && sal.getGrossSalary() > 0 ? "n  Income from Salaries" : "";
            if (sal != null && sal.getGrossSalary() > 0) {
                addStatementRow(incTable, normal, bold, "n  Income from Salaries", "", "", "");
                String empName = nvl(a.getEmployerName());
                if (!empName.isEmpty()) {
                    addStatementRow(incTable, normal, normal,
                        "   Employer: " + empName, "1", "", "");
                }
                addStatementRow(incTable, normal, normal,
                    "   Salaries, allowances and perquisites", "2",
                    formatAmt(sal.getGrossSalary()), "");
                addStatementRow(incTable, normal, normal,
                    "   Standard deduction u/s 16(ia)", "",
                    formatAmt(sal.getStandardDeduction()), "");
                addStatementRow(incTable, normal, bold,
                    "   Income chargeable under the head \"Salaries\"", "", "",
                    formatAmt(sal.getIncomeFromSalary()));
            }

            // â”€â”€ Income from House Property â”€â”€
            ScheduleHouseProperty hp = formData.getScheduleHP();
            if (hp != null && hp.getIncomeFromHP() != 0) {
                addStatementRow(incTable, normal, normal, "", "", "", "");
                addStatementRow(incTable, normal, bold, "n  Income from House Property", "", "", "");
                addStatementRow(incTable, normal, bold,
                    "   Income chargeable under the head \"House Property\"", "", "",
                    formatAmt(hp.getIncomeFromHP()));
            }

            // â”€â”€ Income from Other Sources â”€â”€
            if (os != null && os.getGrossOtherSources() > 0) {
                addStatementRow(incTable, normal, normal, "", "", "", "");
                addStatementRow(incTable, normal, bold, "n  Income from Other Sources", "", "", "");
                if (os.getDividendIncome() > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Dividends", "3", formatAmt(os.getDividendIncome()), "");
                }
                double interest = os.getSavingsInterest() + os.getDepositInterest()
                    + os.getIncomeTaxRefundInterest();
                if (interest > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Interest income", "4", formatAmt(interest), "");
                }
                if (os.getFamilyPension() > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Family Pension", "", formatAmt(os.getFamilyPension()), "");
                }
                if (os.getOtherIncome() > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Other income", "", formatAmt(os.getOtherIncome()), "");
                }
                addStatementRow(incTable, normal, bold,
                    "   Income chargeable under the head \"Other Sources\"", "", "",
                    formatAmt(os.getIncomeFromOtherSources()));
            }

            // â”€â”€ Deductions (if old regime) â”€â”€
            DeductionsVIA ded = formData.getDeductionsVIA();
            if (!a.isNewTaxRegime() && ded != null && ded.getTotalDeductions() > 0) {
                addStatementRow(incTable, normal, normal, "", "", "", "");
                addStatementRow(incTable, normal, bold, "n  Deductions (Chapter VI-A)", "", "", "");
                addStatementRow(incTable, normal, bold,
                    "   Total Deductions", "", "", "(-) " + formatAmt(ded.getTotalDeductions()));
            }

            // â”€â”€ Total Income â”€â”€
            addStatementRow(incTable, normal, normal, "", "", "", "");
            addStatementRow(incTable, normal, bold,
                "n  Total Income", "", "", formatAmt(comp.getGrossTotalIncome()));

            long roundedIncome = Math.round(comp.getTotalTaxableIncome() / 10.0) * 10;
            addStatementRow(incTable, normal, normal,
                "   Total income rounded off u/s 288A", "", "", formatAmt(roundedIncome));

            // â”€â”€ Tax Computation â”€â”€
            addStatementRow(incTable, normal, normal, "", "", "", "");
            addStatementRow(incTable, normal, normal,
                "   Tax on total income", "", formatAmt(comp.getTaxOnIncome()), "");
            if (comp.getRebateU87A() > 0) {
                addStatementRow(incTable, normal, normal,
                    "   Less: Rebate u/s 87A", "", formatAmt(comp.getRebateU87A()), "");
            }
            addStatementRow(incTable, normal, normal,
                "   Add: Cess (4%)", "", formatAmt(comp.getCessAt4Pct()), "");
            addStatementRow(incTable, normal, bold,
                "   Tax with cess", "", "", formatAmt(comp.getTotalTaxLiability()));

            // â”€â”€ Taxes Paid â”€â”€
            if (taxes != null) {
                double tdsTotal = taxes.getTdsOnSalary() + taxes.getTdsOtherThanSalary() + taxes.getTcs();
                if (tdsTotal > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   TDS / TCS", "5", formatAmt(tdsTotal), "");
                }
                if (taxes.getAdvanceTax() > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Advance tax paid", "", formatAmt(taxes.getAdvanceTax()), "");
                }
                if (taxes.getSelfAssessmentTax() > 0) {
                    addStatementRow(incTable, normal, normal,
                        "   Self-assessment tax paid", "6", formatAmt(taxes.getSelfAssessmentTax()), "");
                }
            }

            addStatementRow(incTable, normal, normal, "", "", "", "");
            // Balance payable or refund
            if (comp.getBalanceTaxPayable() > 0) {
                addStatementRow(incTable, normal, bold,
                    "n  Balance Tax Payable", "", "",
                    formatAmt(comp.getBalanceTaxPayable()));
            } else if (comp.getRefundDue() > 0) {
                addStatementRow(incTable, normal, bold,
                    "n  Refund Due", "", "",
                    formatAmt(comp.getRefundDue()));
            } else {
                addStatementRow(incTable, normal, bold,
                    "n  Balance Tax Payable", "", "", "Nil");
            }

            document.add(incTable);

            // â”€â”€ PAGE 2: Schedules â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            document.add(new com.itextpdf.layout.element.AreaBreak(
                com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));

            // Page 2 header
            addPage2Header(document, normal, bold, nvl(a.getAssesseeName()), ayYear);

            // Schedule 1 â€” Employer Details
            if (sal != null && sal.getGrossSalary() > 0) {
                addScheduleTitle(document, bold, "Schedule 1");
                addScheduleSubTitle(document, bold, "Employer Details");
                com.itextpdf.layout.element.Table sch1 = newScheduleTable();
                String empDisplayName = nvl(a.getEmployerName());
                if (empDisplayName.isEmpty()) empDisplayName = "â€”";
                addScheduleRow(sch1, normal, "Name", empDisplayName);
                addScheduleRow(sch1, normal, "TAN", nvl(a.getEmployerTAN()).isEmpty() ? "â€”" : nvl(a.getEmployerTAN()));
                addScheduleRow(sch1, normal, "Nature of Employment",
                    resolveEmployment(a.getNatureOfEmployment()));
                document.add(sch1);
                document.add(spacer());
            }

            // Schedule 2 â€” Salary Breakdown
            if (sal != null && sal.getGrossSalary() > 0) {
                addScheduleTitle(document, bold, "Schedule 2");
                addScheduleSubTitle(document, bold, "Salary Income");
                com.itextpdf.layout.element.Table sch2 = newScheduleTable3();
                addSch2Header(sch2, bold);
                if (sal.getSalaryU17_1() > 0) {
                    addSch2Row(sch2, normal, "Salary u/s 17(1)", "", formatAmt(sal.getSalaryU17_1()));
                }
                if (sal.getPerquisitesU17_2() > 0) {
                    addSch2Row(sch2, normal, "Perquisites u/s 17(2)", "", formatAmt(sal.getPerquisitesU17_2()));
                }
                if (sal.getProfitsU17_3() > 0) {
                    addSch2Row(sch2, normal, "Profits in lieu u/s 17(3)", "", formatAmt(sal.getProfitsU17_3()));
                }
                addSch2Row(sch2, normal, "Total", "", formatAmt(sal.getGrossSalary()));
                addSch2Row(sch2, normal, "Summary of Salary", "Gross", "Taxable");
                addSch2Row(sch2, normal, "Salary income", formatAmt(sal.getGrossSalary()),
                    formatAmt(sal.getIncomeFromSalary()));
                document.add(sch2);
                document.add(spacer());
            }

            // Schedule 3 â€” Dividends
            if (os != null && os.getDividendIncome() > 0) {
                addScheduleTitle(document, bold, "Schedule 3");
                addScheduleSubTitle(document, bold, "Dividends taxable at Normal rate");
                com.itextpdf.layout.element.Table sch3 = newScheduleTable();
                addScheduleRow(sch3, normal, "Domestic Company Dividend", formatAmt(os.getDividendIncome()));
                addScheduleRow(sch3, normal, "Total Dividends", formatAmt(os.getDividendIncome()));
                document.add(sch3);
                document.add(spacer());
            }

            // Schedule 4 â€” Interest Income
            double totalInterest = os != null
                ? os.getSavingsInterest() + os.getDepositInterest() + os.getIncomeTaxRefundInterest()
                : 0;
            if (totalInterest > 0) {
                addScheduleTitle(document, bold, "Schedule 4");
                addScheduleSubTitle(document, bold, "Interest Income");
                com.itextpdf.layout.element.Table sch4 = newScheduleTable();
                if (os.getSavingsInterest() > 0)
                    addScheduleRow(sch4, normal, "Saving Bank Interest", formatAmt(os.getSavingsInterest()));
                if (os.getDepositInterest() > 0)
                    addScheduleRow(sch4, normal, "Fixed Deposit Interest", formatAmt(os.getDepositInterest()));
                if (os.getIncomeTaxRefundInterest() > 0)
                    addScheduleRow(sch4, normal, "Income Tax Refund Interest", formatAmt(os.getIncomeTaxRefundInterest()));
                document.add(sch4);
                document.add(spacer());
            }

            // Schedule 5 â€” TDS
            if (taxes != null && (taxes.getTdsOnSalary() > 0 || taxes.getTdsOtherThanSalary() > 0 || taxes.getTcs() > 0)) {
                addScheduleTitle(document, bold, "Schedule 5");
                addScheduleSubTitle(document, bold, "TDS Details");
                com.itextpdf.layout.element.Table sch5 = newScheduleTable();
                if (taxes.getTdsOnSalary() > 0) {
                    addScheduleRow(sch5, normal,
                        "TDS on Salary" + (!nvl(a.getEmployerTAN()).isEmpty()
                            ? " (TAN: " + a.getEmployerTAN() + ")" : ""),
                        formatAmt(taxes.getTdsOnSalary()));
                }
                if (taxes.getTdsOtherThanSalary() > 0) {
                    addScheduleRow(sch5, normal, "TDS on Other Payments",
                        formatAmt(taxes.getTdsOtherThanSalary()));
                }
                if (taxes.getTcs() > 0) {
                    addScheduleRow(sch5, normal, "TCS", formatAmt(taxes.getTcs()));
                }
                document.add(sch5);
                document.add(spacer());
            }

            // Schedule 6 â€” Self Assessment Tax
            if (taxes != null && (taxes.getSelfAssessmentTax() > 0 || taxes.getAdvanceTax() > 0)) {
                addScheduleTitle(document, bold, "Schedule 6");
                addScheduleSubTitle(document, bold, "Self Assessment / Advance Tax Paid");
                com.itextpdf.layout.element.Table sch6 = newScheduleTable();
                if (taxes.getAdvanceTax() > 0)
                    addScheduleRow(sch6, normal, "Advance Tax", formatAmt(taxes.getAdvanceTax()));
                if (taxes.getSelfAssessmentTax() > 0)
                    addScheduleRow(sch6, normal, "Self Assessment Tax", formatAmt(taxes.getSelfAssessmentTax()));
                document.add(sch6);
                document.add(spacer());
            }

            // Footer signature block
            document.add(spacer());
            addHorizontalRule(document);
            String dateStr = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            com.itextpdf.layout.element.Table footerTable = new com.itextpdf.layout.element.Table(
                new float[]{1, 1})
                .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            footerTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph("Date : " + dateStr)
                    .setFont(normal).setFontSize(9))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footerTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(
                        "(" + nvl(a.getAssesseeName()) + ")")
                    .setFont(bold).setFontSize(9)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            document.add(footerTable);

            document.close();
            return outputStream.toByteArray();
        }
    }

    // â”€â”€ Layout helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void addHeaderRow(com.itextpdf.layout.Document doc,
                               com.itextpdf.kernel.font.PdfFont normal,
                               com.itextpdf.kernel.font.PdfFont bold,
                               String left, String right) {
        com.itextpdf.layout.element.Table t = new com.itextpdf.layout.element.Table(new float[]{1, 1})
            .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setMarginBottom(2);
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(left)
                .setFont(normal).setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(right)
                .setFont(normal).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(t);
    }

    private void addHorizontalRule(com.itextpdf.layout.Document doc) {
        doc.add(new com.itextpdf.layout.element.LineSeparator(
            new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
            .setMarginTop(2).setMarginBottom(4));
    }

    private com.itextpdf.layout.element.Table newStatementTable() {
        // 4 cols: Description | Sch | Amt | Total
        com.itextpdf.layout.element.Table t = new com.itextpdf.layout.element.Table(
            new float[]{260, 30, 80, 80})
            .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        return t;
    }

    private void addStatementHeader(com.itextpdf.layout.element.Table t,
                                    com.itextpdf.kernel.font.PdfFont bold,
                                    String c1, String c2, String c3, String c4) {
        com.itextpdf.kernel.colors.Color underline = new com.itextpdf.kernel.colors.DeviceRgb(0, 0, 0);
        addStatCell(t, bold, c1, com.itextpdf.layout.properties.TextAlignment.LEFT, true);
        addStatCell(t, bold, c2, com.itextpdf.layout.properties.TextAlignment.CENTER, true);
        addStatCell(t, bold, c3, com.itextpdf.layout.properties.TextAlignment.RIGHT, true);
        addStatCell(t, bold, c4, com.itextpdf.layout.properties.TextAlignment.RIGHT, true);
    }

    private void addStatementRow(com.itextpdf.layout.element.Table t,
                                  com.itextpdf.kernel.font.PdfFont normalFont,
                                  com.itextpdf.kernel.font.PdfFont cellFont,
                                  String c1, String c2, String c3, String c4) {
        addStatCell(t, cellFont, c1, com.itextpdf.layout.properties.TextAlignment.LEFT, false);
        addStatCell(t, normalFont, c2, com.itextpdf.layout.properties.TextAlignment.CENTER, false);
        addStatCell(t, normalFont, c3, com.itextpdf.layout.properties.TextAlignment.RIGHT, false);
        addStatCell(t, normalFont, c4, com.itextpdf.layout.properties.TextAlignment.RIGHT, false);
    }

    private void addStatCell(com.itextpdf.layout.element.Table t,
                              com.itextpdf.kernel.font.PdfFont font, String text,
                              com.itextpdf.layout.properties.TextAlignment align, boolean underline) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(text != null ? text : "")
                .setFont(font).setFontSize(9))
            .setTextAlignment(align)
            .setPaddingTop(1).setPaddingBottom(1)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        if (underline) {
            cell.setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(0.5f));
        }
        t.addCell(cell);
    }

    private void addPage2Header(com.itextpdf.layout.Document doc,
                                 com.itextpdf.kernel.font.PdfFont normal,
                                 com.itextpdf.kernel.font.PdfFont bold,
                                 String name, String ay) {
        com.itextpdf.layout.element.Table t = new com.itextpdf.layout.element.Table(new float[]{1, 1})
            .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(name).setFont(bold).setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph("Asst year: " + ay.replace("AY", ""))
                .setFont(normal).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(t);
        addHorizontalRule(doc);
    }

    private void addScheduleTitle(com.itextpdf.layout.Document doc,
                                   com.itextpdf.kernel.font.PdfFont bold, String title) {
        doc.add(new com.itextpdf.layout.element.Paragraph(title)
            .setFont(bold).setFontSize(10)
            .setMarginTop(8).setMarginBottom(0));
    }

    private void addScheduleSubTitle(com.itextpdf.layout.Document doc,
                                      com.itextpdf.kernel.font.PdfFont bold, String sub) {
        doc.add(new com.itextpdf.layout.element.Paragraph(sub)
            .setFont(bold).setFontSize(9)
            .setMarginBottom(3));
    }

    private com.itextpdf.layout.element.Table newScheduleTable() {
        return new com.itextpdf.layout.element.Table(new float[]{3, 1})
            .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(90))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
    }

    private com.itextpdf.layout.element.Table newScheduleTable3() {
        return new com.itextpdf.layout.element.Table(new float[]{3, 1, 1})
            .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(90))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
    }

    private void addScheduleRow(com.itextpdf.layout.element.Table t,
                                 com.itextpdf.kernel.font.PdfFont font,
                                 String label, String value) {
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(label).setFont(font).setFontSize(9))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setPaddingTop(1).setPaddingBottom(1));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(value != null ? value : "")
                .setFont(font).setFontSize(9))
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setPaddingTop(1).setPaddingBottom(1));
    }

    private void addSch2Header(com.itextpdf.layout.element.Table t,
                                com.itextpdf.kernel.font.PdfFont bold) {
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph("Particulars")
                .setFont(bold).setFontSize(9))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(0.5f)));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph("Exempt")
                .setFont(bold).setFontSize(9))
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(0.5f)));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph("Taxable")
                .setFont(bold).setFontSize(9))
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(0.5f)));
    }

    private void addSch2Row(com.itextpdf.layout.element.Table t,
                             com.itextpdf.kernel.font.PdfFont font,
                             String label, String exempt, String taxable) {
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(label).setFont(font).setFontSize(9))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPaddingTop(1));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(exempt != null ? exempt : "")
                .setFont(font).setFontSize(9))
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPaddingTop(1));
        t.addCell(new com.itextpdf.layout.element.Cell()
            .add(new com.itextpdf.layout.element.Paragraph(taxable != null ? taxable : "")
                .setFont(font).setFontSize(9))
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPaddingTop(1));
    }

    private com.itextpdf.layout.element.Paragraph spacer() {
        return new com.itextpdf.layout.element.Paragraph(" ").setFontSize(4).setMarginBottom(2);
    }

    // â”€â”€ Utility methods â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String formatAmt(double amount) {
        if (amount == 0) return "";
        NumberFormat nf = NumberFormat.getInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format((long) Math.abs(amount));
    }

    private String formatAmt(long amount) {
        if (amount == 0) return "";
        NumberFormat nf = NumberFormat.getInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(Math.abs(amount));
    }

    private String formatAadhaar(String s) {
        if (s == null || s.length() < 4) return s != null ? s : "";
        // Format as XXXX XXXX XXXX
        String digits = s.replaceAll("\\D", "");
        if (digits.length() == 12) {
            return digits.substring(0, 4) + " " + digits.substring(4, 8) + " " + digits.substring(8);
        }
        return s;
    }

    private String formatDob(String dob) {
        if (dob == null || dob.isEmpty()) return "";
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(dob);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        } catch (Exception e) {
            return dob;
        }
    }

    private String derivePreviousYear(String ay) {
        // AY2025-26 -> 2024-2025
        try {
            String stripped = ay.replace("AY", "").trim();
            String[] parts = stripped.split("-");
            int start = Integer.parseInt(parts[0]) - 1;
            return start + "-" + parts[0];
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveStatus(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "RES": return "Resident";
            case "NOR": return "Resident but Not Ordinarily Resident";
            case "NRI": return "Non-Resident";
            default: return status;
        }
    }

    private String resolveEmployment(String code) {
        if (code == null) return "Others";
        switch (code.toUpperCase()) {
            case "CG": return "Central Government";
            case "SG": return "State Government";
            case "PSU": return "Public Sector Undertaking";
            case "PE":
            case "PEN": return "Pensioner";
            default: return "Others";
        }
    }
}
