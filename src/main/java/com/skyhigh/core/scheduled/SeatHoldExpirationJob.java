package com.skyhigh.core.scheduled;

import com.skyhigh.core.model.entity.Seat;
import com.skyhigh.core.model.entity.SeatHold;
import com.skyhigh.core.model.enums.HoldStatus;
import com.skyhigh.core.model.enums.SeatStatus;
import com.skyhigh.core.repository.SeatHoldRepository;
import com.skyhigh.core.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job to automatically expire seat holds after 120 seconds
 * Runs every 10 seconds with distributed locking (ShedLock)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeatHoldExpirationJob {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatRepository seatRepository;

    /**
     * Find and expire all holds that have passed their expiration time
     * Uses ShedLock to ensure only one instance runs this job in a distributed environment
     */
    @Scheduled(fixedRateString = "${app.seat-hold.expiration-job-rate:10000}")
    @SchedulerLock(
        name = "expireSeatHolds",
        lockAtLeastFor = "5s",
        lockAtMostFor = "15s"
    )
    @CacheEvict(value = "seatMap", allEntries = true)
    public void expireExpiredHolds() {
        log.debug("Running seat hold expiration job");

        try {
            List<SeatHold> expiredHolds = seatHoldRepository.findExpiredHolds(HoldStatus.ACTIVE, Instant.now());

            if (expiredHolds.isEmpty()) {
                log.debug("No expired holds found");
                return;
            }

            log.info("Found {} expired holds to process", expiredHolds.size());

            int successCount = 0;
            int failureCount = 0;

            for (SeatHold hold : expiredHolds) {
                try {
                    expireHold(hold);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to expire hold {}: {}", hold.getHoldId(), e.getMessage(), e);
                    failureCount++;
                }
            }

            log.info("Seat hold expiration job completed: {} succeeded, {} failed",
                     successCount, failureCount);

        } catch (Exception e) {
            log.error("Seat hold expiration job failed", e);
        }
    }

    /**
     * Expire a single hold and release the seat
     */
    @Transactional
    protected void expireHold(SeatHold hold) {
        log.debug("Expiring hold {} for seat {}", hold.getHoldId(), hold.getSeatId());

        // Update hold status to EXPIRED
        hold.setStatus(HoldStatus.EXPIRED);
        seatHoldRepository.save(hold);

        // Release seat back to AVAILABLE
        Seat seat = seatRepository.findById(hold.getSeatId())
            .orElse(null);

        if (seat != null && seat.getStatus() == SeatStatus.HELD) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            log.info("Expired hold {} for seat {}", hold.getHoldId(), seat.getSeatNumber());
        } else if (seat == null) {
            log.warn("Seat {} not found for expired hold {}", hold.getSeatId(), hold.getHoldId());
        } else {
            log.debug("Seat {} already in status {}, not changing",
                     seat.getSeatNumber(), seat.getStatus());
        }
    }
}

