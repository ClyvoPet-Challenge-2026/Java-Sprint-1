package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.PaymentMethodRequest;
import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {
    private final PaymentMethodRepository paymentRepository;


    public List<PaymentMethod> findAllPaymentMethods() {
        return paymentRepository.findAll();
    }

    public PaymentMethod findPaymentMethodById(Long id) {
        return paymentRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("PaymentMethod with ID %d not found", id))
        );
    }

    public PaymentMethod createPaymentMethod(PaymentMethodRequest request) {
        return paymentRepository.save(request.toEntity());
    }

    public PaymentMethod updatePaymentMethod(Long id, PaymentMethodRequest request) {
        PaymentMethod existing = findPaymentMethodById(id);
        existing.setName(request.name());
        return paymentRepository.save(existing);
    }

    public void deletePaymentMethod(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("PaymentMethod with ID %d not found", id));
        }
        paymentRepository.deleteById(id);
    }

}
