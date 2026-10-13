package com.fiscal.service;

import com.fiscal.dao.TaxTypeDAO;
import com.fiscal.model.TaxType;
import com.fiscal.util.ValidationUtil;

import java.util.List;

/**
 * Service managing tax category rules, percentage rates, and referential constraints.
 */
public class TaxTypeService {

    private final TaxTypeDAO taxTypeDAO = new TaxTypeDAO();

    public List<TaxType> listTaxTypes(String search) {
        return taxTypeDAO.findAll(search);
    }

    public TaxType getTaxType(Long id) {
        return taxTypeDAO.findById(id);
    }

    public TaxType createTaxType(TaxType taxType) {
        validateTaxType(taxType, true);
        boolean created = taxTypeDAO.create(taxType);
        if (!created) {
            throw new IllegalStateException("Failed to persist tax category in database");
        }
        return taxType;
    }

    public TaxType updateTaxType(TaxType taxType) {
        if (taxType.getTaxId() == null || taxType.getTaxId() <= 0) {
            throw new IllegalArgumentException("Tax ID is required for update");
        }
        validateTaxType(taxType, false);
        boolean updated = taxTypeDAO.update(taxType);
        if (!updated) {
            throw new IllegalStateException("Failed to update tax category record");
        }
        return taxType;
    }

    public boolean deleteTaxType(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid Tax Type ID");
        }
        if (taxTypeDAO.hasAssociatedPayments(id)) {
            throw new IllegalStateException("Cannot delete tax category: Existing payment records reference this tax type. Preserving historical ledger integrity.");
        }
        return taxTypeDAO.delete(id);
    }

    private void validateTaxType(TaxType t, boolean checkDuplicateName) {
        if (t == null) {
            throw new IllegalArgumentException("Tax type data cannot be null");
        }
        if (!ValidationUtil.isNonEmpty(t.getTaxName())) {
            throw new IllegalArgumentException("Tax category name is required");
        }
        if (!ValidationUtil.isValidTaxRate(t.getTaxRate())) {
            throw new IllegalArgumentException("Configured tax rate must be between 0.00% and 100.00%");
        }
        if (checkDuplicateName) {
            TaxType existing = taxTypeDAO.findByName(t.getTaxName());
            if (existing != null) {
                throw new IllegalArgumentException("A tax category named '" + t.getTaxName() + "' already exists");
            }
        }
    }
}
