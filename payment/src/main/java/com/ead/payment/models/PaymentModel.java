package com.ead.payment.models;

import com.ead.payment.dtos.PaymentEventDto;
import com.ead.payment.enums.PaymentControl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "TB_PAYMENTS")
public class PaymentModel implements Serializable {
    private static final long serialVersionUID = 1l;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID paymentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentControl paymentControl;

    @Column(nullable = false)
    private LocalDateTime paymentRequestDate;

    @Column
    private LocalDateTime paymentCompletionDate;

    @Column(nullable = false)
    private LocalDateTime paymentExpirationDate;

    @Column(nullable = false, length = 4)
    private String lastDigitsCreditCard;

    @Column(nullable = false)
    private BigDecimal valuePaid;

    @Column(length = 150)
    private String paymentMessage;

    @Column
    private boolean recurrence;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private  UserModel user;

    public PaymentEventDto convertPaymentEventDto(){
        PaymentEventDto paymentEventDto = new PaymentEventDto();
        BeanUtils.copyProperties(this, paymentEventDto);
        paymentEventDto.setPaymentControl(this.getPaymentControl().toString());
        paymentEventDto.setUserId(this.getUser().getUserId());
        paymentEventDto.setUserId(this.getUser().getUserId());
        return paymentEventDto;
    }

}
