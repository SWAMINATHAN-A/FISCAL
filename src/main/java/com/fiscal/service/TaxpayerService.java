package com.fiscal.service;

import com.fiscal.dao.TaxpayerDAO;
import com.fiscal.model.Taxpayer;
import com.fiscal.util.ValidationUtil;

import java.util.List;

/**
 * Service managing taxpayer validation, CRUD logic, and constraint enforcement.
 */
public class TaxpayerService {

    private final TaxpayerDAO taxpayerDAO = new TaxpayerDAO();

    public List<Taxpayer> listTaxpayers(String search, String city, String sortBy) {
        return taxpayerDAO.findAll(search, city, sortBy);
    }

    public Taxpayer getTaxpayer(Long id) {
        return taxpayerDAO.findById(id);
    }

    public List<String> listCities() {
        return taxpayerDAO.getDistinctCities();
    }

    public Taxpayer createTaxpayer(Taxpayer taxpayer) {
        validateTaxpayer(taxpayer, true);
        boolean created = taxpayerDAO.create(taxpayer);
        if (!created) {
            throw new IllegalStateException("Failed to persist taxpayer in database");
        }
        return taxpayer;
    }

    public Taxpayer updateTaxpayer(Taxpayer taxpayer) {
        if (taxpayer.getTaxpayerId() == null || taxpayer.getTaxpayerId() <= 0) {
            throw new IllegalArgumentException("Taxpayer ID is required for update");
        }
        validateTaxpayer(taxpayer, false);
        boolean updated = taxpayerDAO.update(taxpayer);
        if (!updated) {
            throw new IllegalStateException("Failed to update taxpayer record");
        }
        return taxpayer;
    }

    public boolean deleteTaxpayer(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid Taxpayer ID");
        }
        return taxpayerDAO.delete(id);
    }

    private void validateTaxpayer(Taxpayer t, boolean checkDuplicatePan) {
        if (t == null) {
            throw new IllegalArgumentException("Taxpayer data cannot be null");
        }
        if (!ValidationUtil.isNonEmpty(t.getName())) {
            throw new IllegalArgumentException("Taxpayer full name is required");
        }
        if (!ValidationUtil.isValidPan(t.getPanNo())) {
            throw new IllegalArgumentException("Invalid statutory PAN format. Must be 5 uppercase letters, 4 digits, and 1 letter (e.g. ABCDE1234F)");
        }
        t.setPanNo(ValidationUtil.normalizePan(t.getPanNo()));

        if (checkDuplicatePan) {
            Taxpayer existing = taxpayerDAO.findByPan(t.getPanNo());
            if (existing != null) {
                throw new IllegalArgumentException("A taxpayer with PAN " + t.getPanNo() + " already exists in the system");
            }
        }

        if (!ValidationUtil.isValidAge(t.getAge())) {
            throw new IllegalArgumentException("Age must be between 18 and 120 years");
        }
        if (!ValidationUtil.isNonEmpty(t.getCity())) {
            throw new IllegalArgumentException("City is required");
        }
        if (!ValidationUtil.isNonNegativeAmount(t.getIncome())) {
            throw new IllegalArgumentException("Annual declared income must be non-negative");
        }
    }
}
