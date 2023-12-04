package com.ead.payment.services.impl;

import com.ead.payment.dtos.PaymentCommandDto;
import com.ead.payment.dtos.PaymentRequestDto;
import com.ead.payment.enums.PaymentControl;
import com.ead.payment.enums.PaymentStatus;
import com.ead.payment.models.CreditCardModel;
import com.ead.payment.models.PaymentModel;
import com.ead.payment.models.UserModel;
import com.ead.payment.publishers.PaymentCommandPublisher;
import com.ead.payment.publishers.PaymentEventPublisher;
import com.ead.payment.repositories.CreditCardRepository;
import com.ead.payment.repositories.PaymentRepository;
import com.ead.payment.repositories.UserRepository;
import com.ead.payment.services.PaymentService;
import com.ead.payment.services.PaymentStipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {


    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private PaymentCommandPublisher paymentCommandPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentStipeService paymentStipeService;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public PaymentModel requestPayment(PaymentRequestDto paymentRequestDto, UserModel userModel) {
        CreditCardModel creditCardModel = new CreditCardModel();

        Optional<CreditCardModel> creditCardModelOptional = creditCardRepository.findByUser(userModel);
        if(creditCardModelOptional.isPresent()) {
            creditCardModel = creditCardModelOptional.get();
        }

        BeanUtils.copyProperties(paymentRequestDto, creditCardModel);
        creditCardModel.setUserModel(userModel);
        creditCardRepository.save(creditCardModel);

        PaymentModel paymentModel = new PaymentModel();
        paymentModel.setPaymentControl(PaymentControl.REQUESTED);
        paymentModel.setPaymentRequestDate(LocalDateTime.now(ZoneId.of("UTC")));
        paymentModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")).plusDays(30));
        paymentModel.setLastDigitsCreditCard(paymentRequestDto.getCreditCardNumber().substring(paymentRequestDto.getCreditCardNumber().length()-4));
        paymentModel.setValuePaid(paymentRequestDto.getValuePaid());
        paymentModel.setUser(userModel);
        paymentRepository.save(paymentModel);

        try{
            PaymentCommandDto paymentCommandDto = new PaymentCommandDto();
            paymentCommandDto.setUserId(userModel.getUserId());
            paymentCommandDto.setPaymentId(paymentModel.getPaymentId());
            paymentCommandDto.setCardId(creditCardModel.getCardId());
            paymentCommandPublisher.publishPaymentCommand(paymentCommandDto);
        } catch (Exception e){
            log.warn("Error sending payment command!");
        }

        return paymentModel;
    }

    @Override
    public Optional<PaymentModel> findLastPaymentByUser(UserModel userModel) {
        return paymentRepository.findTopByUserOrderByPaymentRequestDateDesc(userModel);
    }

    @Override
    public Page<PaymentModel> findAllByUser(Specification<PaymentModel> spec, Pageable pageable) {
        return paymentRepository.findAll(spec, pageable);
    }

    @Override
    public Optional<PaymentModel> findPaymentByUser(UUID userId, UUID paymentId) {
        return paymentRepository.findPaymentByUser(userId, paymentId);
    }

    @Override
    @Transactional
    public void makePayment(PaymentCommandDto paymentCommandDto) {
        PaymentModel paymentModel = paymentRepository.findById(paymentCommandDto.getPaymentId()).get();
        UserModel userModel = userRepository.findById(paymentCommandDto.getUserId()).get();
        CreditCardModel creditCardModel = creditCardRepository.findById(paymentCommandDto.getCardId()).get();

        paymentModel = paymentStipeService.processStripePayment(paymentModel, creditCardModel);
        paymentRepository.save(paymentModel);


        if(paymentModel.getPaymentControl().equals(PaymentControl.EFFECTED)){
            userModel.setPaymentStatus(PaymentStatus.PAYING);
            userModel.setLastPaymentDate(LocalDateTime.now(ZoneId.of("UTC")));
            userModel.setPaymentExpirationDate(LocalDateTime.now(ZoneId.of("UTC")).plusDays(30));
            if(userModel.getFirstPaymentDate() == null){
                userModel.setFirstPaymentDate(LocalDateTime.now(ZoneId.of("UTC")));
            }
        } else {
            userModel.setPaymentStatus(PaymentStatus.DEBTOR);
        }
        userRepository.save(userModel);

        if(paymentModel.getPaymentControl().equals(PaymentControl.EFFECTED) ||
            paymentModel.getPaymentControl().equals(PaymentControl.REFUSED)){
            paymentEventPublisher.publishPaymentEvent(paymentModel.convertPaymentEventDto());
        } else if (paymentModel.getPaymentControl().equals(PaymentControl.ERROR)) {
            
        }


    }
}
