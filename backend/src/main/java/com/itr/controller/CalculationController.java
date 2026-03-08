package com.itr.controller;

import com.itr.dto.CalculationDtos;
import com.itr.dto.CalculationDtos.*;
import com.itr.mapper.ITR1JsonBuilder;
import com.itr.mapper.PrefillMapper;
import com.itr.model.ITR1Result;
import com.itr.model.PreFillData;
import com.itr.service.ITR1CalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the ITR-1 tax calculation engine.
 *
 * POST /api/clients/{clientId}/prefill/{year}/calculate
 *
 * This endpoint:
 *  1. Accepts raw prefill JSON + optional user deduction overrides
 *  2. Maps prefill JSON to PreFillData
 *  3. Applies deduction overrides
 *  4. Runs the calculation engine
 *  5. Validates against CBDT rules
 *  6. Returns computed result + ITR-1 submission JSON
 */
@RestController
@RequestMapping("/api/clients/{clientId}/prefill/{year}")
public class CalculationController {

    private final PrefillMapper         prefillMapper;
    private final ITR1CalculatorService calculatorService;
    private final ITR1JsonBuilder       jsonBuilder;

    public CalculationController(PrefillMapper prefillMapper,
                                  ITR1CalculatorService calculatorService,
                                  ITR1JsonBuilder jsonBuilder) {
        this.prefillMapper     = prefillMapper;
        this.calculatorService = calculatorService;
        this.jsonBuilder       = jsonBuilder;
    }

    /**
     * Calculate ITR-1 from stored or submitted prefill JSON.
     *
     * @param clientId  Client ID (ownership enforced by Spring Security / Repository layer)
     * @param year      Assessment year, e.g. "AY2025-26"
     * @param request   Contains rawPrefillJson and optional overrides
     */
    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(
            @PathVariable Long clientId,
            @PathVariable String year,
            @RequestBody CalculationRequest request) {

        // 1. Map prefill JSON
        PreFillData prefill = prefillMapper.map(request.rawPrefillJson);

        // 2. Apply user overrides (e.g., from frontend form inputs)
        applyOverrides(prefill, request.deductionOverrides);

        // 3. Compute
        String filingDate = request.filingDate;
        ITR1Result result = calculatorService.compute(prefill, filingDate);

        // 4. Build ITR-1 submission JSON
        String itr1Json = jsonBuilder.build(prefill, result);

        // 5. Return response
        return ResponseEntity.ok(CalculationDtos.toResponse(result, itr1Json));
    }

    /**
     * Apply user-supplied deduction overrides on top of prefill values.
     * Null overrides are ignored — prefill values are preserved.
     */
    private void applyOverrides(PreFillData prefill, UserDeductionOverrides ov) {
        if (ov == null) return;

        if (ov.section80C              != null) prefill.section80C              = ov.section80C;
        if (ov.section80CCC            != null) prefill.section80CCC            = ov.section80CCC;
        if (ov.section80CCD_Employee   != null) prefill.section80CCD_Employee   = ov.section80CCD_Employee;
        if (ov.section80CCD_1B         != null) prefill.section80CCD_1B         = ov.section80CCD_1B;
        if (ov.section80CCD_Employer   != null) prefill.section80CCD_Employer   = ov.section80CCD_Employer;
        if (ov.section80D              != null) prefill.section80D              = ov.section80D;
        if (ov.section80DD             != null) prefill.section80DD             = ov.section80DD;
        if (ov.section80DDB            != null) prefill.section80DDB            = ov.section80DDB;
        if (ov.section80E              != null) prefill.section80E              = ov.section80E;
        if (ov.section80EE             != null) prefill.section80EE             = ov.section80EE;
        if (ov.section80EEA            != null) prefill.section80EEA            = ov.section80EEA;
        if (ov.section80EEB            != null) prefill.section80EEB            = ov.section80EEB;
        if (ov.section80G              != null) prefill.section80G              = ov.section80G;
        if (ov.section80GG             != null) prefill.section80GG             = ov.section80GG;
        if (ov.section80GGA            != null) prefill.section80GGA            = ov.section80GGA;
        if (ov.section80GGC            != null) prefill.section80GGC            = ov.section80GGC;
        if (ov.section80TTA            != null) prefill.section80TTA            = ov.section80TTA;
        if (ov.section80TTB            != null) prefill.section80TTB            = ov.section80TTB;
        if (ov.section80U              != null) prefill.section80U              = ov.section80U;
        if (ov.reliefUs89              != null) prefill.reliefUs89              = ov.reliefUs89;

        // House property overrides
        if (ov.typeOfHP                != null) prefill.typeOfHP                = ov.typeOfHP;
        if (ov.grossRentReceived       != null) prefill.grossRentReceived       = ov.grossRentReceived;
        if (ov.taxPaidLocalAuth        != null) prefill.taxPaidLocalAuth        = ov.taxPaidLocalAuth;
        if (ov.interestOnBorrowedCapital != null) prefill.interestOnBorrowedCapital = ov.interestOnBorrowedCapital;
    }
}
