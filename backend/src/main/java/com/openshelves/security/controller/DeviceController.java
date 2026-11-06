package com.openshelves.security.controller;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.model.DeviceResponse;
import com.openshelves.security.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/// REST controller for the devices a user is signed in on.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/devices")
public class DeviceController {

    private final DeviceService deviceService;

    /// Lists the devices the caller is signed in on.
    @GetMapping
    public List<DeviceResponse> listDevices(@AuthenticationPrincipal SessionPrincipal principal) {
        return deviceService.listDevices(principal);
    }

    /// Signs one device out.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signOutDevice(@AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        deviceService.signOutDevice(principal, id);
    }

    /// Signs out every device except this one.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signOutOtherDevices(@AuthenticationPrincipal SessionPrincipal principal) {
        deviceService.signOutOtherDevices(principal);
    }
}
