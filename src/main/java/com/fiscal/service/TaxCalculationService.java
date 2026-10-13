package com.fiscal.service;

import com.fiscal.dao.TaxTypeDAO;
import com.fiscal.dao.TaxpayerDAO;
import com.fiscal.model.TaxCalculationRequest;
import com.fiscal.model.TaxCalculationResponse;
import com.fiscal.model.TaxType;
import com.fiscal.model.Taxpayer;
import com.fiscal.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Authoritative Server-Side Tax Calculation and Assessment Service.
 * Implements strict numeric precision with RoundingMode.HALF_UP.
 */
public class TaxCalculationService {

    private final TaxpayerDAO taxpayerDAO = new TaxpayerDAO();
    private final TaxTypeDAO taxTypeDAO = new TaxTypeDAO();

    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Calculation request payload cannot be null");
        }
        if (request.getTaxpayerId() == null || request.getTaxpayerId() <= 0) {
            throw new IllegalArgumentException("A valid Taxpayer ID is required");
        }
        if (request.getTaxId() == null || request.getTaxId() <= 0) {
            throw new IllegalArgumentException("A valid Tax Type ID is required");
        }

        Taxpayer taxpayer = taxpayerDAO.findById(request.getTaxpayerId());
        if (taxpayer == null) {
            throw new IllegalArgumentException("Taxpayer with ID " + request.getTaxpayerId() + " not found");
        }

        TaxType taxType = taxTypeDAO.findById(request.getTaxId());
        if (taxType == null) {
            throw new IllegalArgumentException("Tax category with ID " + request.getTaxId() + " not found");
        }

        BigDecimal applicableAmount;
        if (request.getTaxableAmount() != null) {
            if (!ValidationUtil.isNonNegativeAmount(request.getTaxableAmount())) {
                throw new IllegalArgumentException("Applicable taxable amount cannot be negative");
            }
            applicableAmount = request.getTaxableAmount();
        } else {
            applicableAmount = taxpayer.getIncome();
        }

        // Authoritative statutory formula:
        // Tax Amount = (Applicable Amount * Tax Rate) / 100
        BigDecimal rate = taxType.getTaxRate();
        BigDecimal taxAmount = applicableAmount.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        TaxCalculationResponse response = new TaxCalculationResponse();
        response.setTaxpayerId(taxpayer.getTaxpayerId());
        response.setTaxpayerName(taxpayer.getName());
        response.setPanNo(taxpayer.getPanNo());
        response.setTaxId(taxType.getTaxId());
        response.setTaxName(taxType.getTaxName());
        response.setTaxRate(rate);
        response.setApplicableAmount(applicableAmount.setScale(2, RoundingMode.HALF_UP));
        response.setCalculatedTax(taxAmount);
        response.setCalculationFormula(applicableAmount + " × " + rate + "% ÷ 100 = " + taxAmount);
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return response;
    }
}
