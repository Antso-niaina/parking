package com.projet.parking.controller;

import com.projet.parking.config.SecuriteService;
import com.projet.parking.dto.*;
import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.*;
import com.projet.parking.mapper.*;
import com.projet.parking.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice(basePackages = "com.projet.parking.controller")
public class PageModelAdvice {

    @ModelAttribute("moi")
    public Clients moi(@AuthenticationPrincipal Clients moi) {
        return moi;   // null si personne n'est connecté
    }

    @ModelAttribute("estAdmin")
    public boolean estAdmin(@AuthenticationPrincipal Clients moi) {
        return moi != null && moi.getType_client() == ClientsEnum.ADMIN;
    }
}