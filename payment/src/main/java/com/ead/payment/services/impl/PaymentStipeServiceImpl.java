package com.ead.payment.services.impl;

import com.ead.payment.enums.PaymentControl;
import com.ead.payment.models.CreditCardModel;
import com.ead.payment.models.PaymentModel;
import com.ead.payment.services.PaymentStipeService;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentStipeServiceImpl implements PaymentStipeService {

    @Value(value = "${ead.stipe.secretKey}")
    private String secretKeyStripe;

    @Override
    public PaymentModel processStripePayment(PaymentModel paymentModel, CreditCardModel creditCardModel) {
        Stripe.apiKey = secretKeyStripe;
        String paymentIntendId = null;

        try {
            List<Object> paymentMethodTypes = new ArrayList<>();
            paymentMethodTypes.add("card");

            Map<String, Object> paramsPaymentIntent = new HashMap<>();
            paramsPaymentIntent.put("amount", paymentModel.getValuePaid().multiply(new BigDecimal("100")).longValue());
            paramsPaymentIntent.put("currency", "brl");
            paramsPaymentIntent.put("payment_method_types", paymentMethodTypes);

            PaymentIntent paymentIntent = PaymentIntent.create(paramsPaymentIntent);
            paymentIntendId = paymentIntent.getId();

            Map<String, Object> card = new HashMap<>();
            card.put("number", creditCardModel.getCreditCardNumber().replaceAll(" ",""));
            card.put("exp_month", creditCardModel.getExpirationDate().split("/")[0]);
            card.put("exp_year", creditCardModel.getExpirationDate().split("/")[1]);
            card.put("cvc", creditCardModel.getCvvCode());
            Map<String, Object> paramsPaymentMethod = new HashMap<>();
            paramsPaymentMethod.put("type", "card");
            paramsPaymentMethod.put("card", card);
            PaymentMethod paymentMethod = PaymentMethod.create(paramsPaymentMethod);

            Map<String, Object> paramsPaymentConfirm = new HashMap<>();
            paramsPaymentConfirm.put("payment_method", paymentMethod.getId());
            PaymentIntent confirmPaymentIntent = paymentIntent.confirm(paramsPaymentConfirm);


            if(confirmPaymentIntent.getStatus().equals("succeeded")){
                paymentModel.setPaymentControl(PaymentControl.EFFECTED);
                paymentModel.setPaymentMessage("payment effected - paymentIntent: " + paymentIntendId);
                paymentModel.setPaymentCompletionDate(LocalDateTime.now(ZoneId.of("UTC")));
            } else{
                paymentModel.setPaymentControl(PaymentControl.ERROR);
                paymentModel.setPaymentMessage("payment error v1 - paymentIntent: " + paymentIntendId);
            }

        } catch (CardException e){
            System.out.println("A payment error occurred: {}");
            try {
                paymentModel.setPaymentControl(PaymentControl.REFUSED);
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntendId);
                paymentModel.setPaymentMessage("payment refused v1 - paymentIntent: " + paymentIntendId);
            } catch (Exception e1){
                paymentModel.setPaymentMessage("payment refused v2 - paymentIntent: " + paymentIntendId);
            }
        } catch (Exception e) {
            paymentModel.setPaymentControl(PaymentControl.ERROR);
            paymentModel.setPaymentMessage("payment error v2 - paymentIntent: " + paymentIntendId);
            System.out.println("Another problem occurred, maybe unrelated to Stipe.");
        }

        return paymentModel;
    }
}
