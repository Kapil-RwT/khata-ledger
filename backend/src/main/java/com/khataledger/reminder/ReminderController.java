package com.khataledger.reminder;

import com.khataledger.config.MerchantPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderRepository reminders;
    private final ReminderService reminderService;

    @GetMapping
    public List<ReminderResponse> list(@AuthenticationPrincipal MerchantPrincipal me) {
        return reminders.findAllByMerchantIdOrderByCreatedAtDesc(me.merchantId())
                .stream().map(ReminderResponse::of).toList();
    }

    /** Manual trigger for the overdue scan; handy for demos. */
    @PostMapping("/scan")
    public String triggerScan() {
        reminderService.scanOverdueLedgers();
        return "scan dispatched";
    }
}
