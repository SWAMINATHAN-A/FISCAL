package com.fiscal.service;

import com.fiscal.dao.TaxPaymentDAO;
import com.fiscal.dao.TaxTypeDAO;
import com.fiscal.dao.TaxpayerDAO;
import com.fiscal.model.TaxPayment;
import com.fiscal.model.TaxType;
import com.fiscal.model.Taxpayer;
import com.fiscal.util.ValidationUtil;

import java.sql.Date;
import java.util.List;

/**
 * Service managing payment recording, statutory verification, and status updates.
 */
public class TaxPaymentService {

    private final TaxPaymentDAO paymentDAO = new TaxPaymentDAO();
    private final TaxpayerDAO taxpayerDAO = new TaxpayerDAO();
    private final TaxTypeDAO taxTypeDAO = new TaxTypeDAO();

    public List<TaxPayment> listPayments(String search, Long taxpayerId, Long taxId, String status, String sortBy) {
        return paymentDAO.findAll(search, taxpayerId, taxId, status, sortBy);
    }

    public TaxPayment getPayment(Long id) {
        return paymentDAO.findById(id);
    }

    public List<TaxPayment> getPaymentsForTaxpayer(Long taxpayerId) {
        return paymentDAO.findByTaxpayerId(taxpayerId);
    }

    public TaxPayment recordPayment(TaxPayment payment) {
        validatePayment(payment);
        boolean created = paymentDAO.create(payment);
        if (!created) {
            throw new IllegalStateException("Failed to commit tax payment record into the ledger");
        }
        return paymentDAO.findById(payment.getPaymentId());
    }

    public TaxPayment updatePaymentStatus(Long paymentId, String status, String remarks) {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("Valid Payment ID is required");
        }
        if (!ValidationUtil.isValidPaymentStatus(status)) {
            throw new IllegalArgumentException("Invalid status token. Allowed values: 'PAID', 'PENDING', 'REVERSED'");
        }
        boolean updated = paymentDAO.updateStatus(paymentId, status, remarks);
        if (!updated) {
            throw new IllegalStateException("Failed to update status for payment #" + paymentId);
        }
        return paymentDAO.findById(paymentId);
    }

    public boolean deletePayment(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Valid Payment ID is required");
        }
        return paymentDAO.delete(id);
    }

    private void validatePayment(TaxPayment p) {
        if (p == null) {
            throw new IllegalArgumentException("Payment record payload cannot be null");
        }
        if (p.getTaxpayerId() == null || p.getTaxpayerId() <= 0) {
            throw new IllegalArgumentException("Valid Taxpayer ID is required");
        }
        Taxpayer taxpayer = taxpayerDAO.findById(p.getTaxpayerId());
        if (taxpayer == null) {
            throw new IllegalArgumentException("Referenced taxpayer (ID: " + p.getTaxpayerId() + ") does not exist");
        }

        if (p.getTaxId() == null || p.getTaxId() <= 0) {
            throw new IllegalArgumentException("Valid Tax Type ID is required");
        }
        TaxType taxType = taxTypeDAO.findById(p.getTaxId());
        if (taxType == null) {
            throw new IllegalArgumentException("Referenced tax category (ID: " + p.getTaxId() + ") does not exist");
        }

        if (!ValidationUtil.isPositiveAmount(p.getAmount())) {
            throw new IllegalArgumentException("Payment remittance amount must be strictly greater than zero");
        }

        if (p.getStatus() == null || p.getStatus().trim().isEmpty()) {
            p.setStatus("PAID");
        } else if (!ValidationUtil.isValidPaymentStatus(p.getStatus())) {
            throw new IllegalArgumentException("Invalid payment status. Allowed values: 'PAID', 'PENDING', 'REVERSED'");
        }

        if (p.getPaymentDate() == null) {
            p.setPaymentDate(new Date(System.currentTimeMillis()));
        }
    }
}
