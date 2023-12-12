package com.ead.notification.dtos;

import com.ead.notification.enums.NotificationStatus;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class NotificationDto {

    @NotNull
    private NotificationStatus notificationStatus;

}
