package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.dto.UsageResponse;
import com.sgagestudio.warm_follow_backend.model.UsageLedger;
import com.sgagestudio.warm_follow_backend.repository.UsageLedgerRepository;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsageService {
    private final UsageLedgerRepository usageLedgerRepository;

    public UsageService(UsageLedgerRepository usageLedgerRepository) {
        this.usageLedgerRepository = usageLedgerRepository;
    }

    public UsageResponse getCurrentUsage(UUID workspaceId) {
        String period = YearMonth.now(ZoneOffset.UTC).toString();
        UsageLedger ledger = usageLedgerRepository.findByWorkspaceIdAndPeriod(workspaceId, period).orElse(null);
        if (ledger == null) {
            return new UsageResponse(period, 0, 0, 0, 0);
        }
        return new UsageResponse(
                period,
                ledger.getEmailsSent(),
                ledger.getSmsSent(),
                ledger.getSmsCreditsBalance(),
                ledger.getOverageCostCents()
        );
    }
}
