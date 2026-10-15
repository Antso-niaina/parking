package com.projet.parking.controller;

import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(basePackages = "com.projet.parking.controller")
public class PageModelAdvice {

    @ModelAttribute("moi")
    public Clients moi(@AuthenticationPrincipal Clients moi) {
        return moi;
    }

    @ModelAttribute("estAdmin")
    public boolean estAdmin(@AuthenticationPrincipal Clients moi) {
        return moi != null && moi.getType_client() == ClientsEnum.ADMIN;
    }
}