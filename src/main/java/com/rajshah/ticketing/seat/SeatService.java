package com.rajshah.ticketing.seat;

import com.rajshah.ticketing.exception.OperationNotPermittedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {
    private final SeatRepository seatRepository;
    private final SimpMessagingTemplate messagingTemplate;


    public List<Seat> updateSeatStatus(SeatStatusRequest request) {
        Seat seat = seatRepository.findById(request.seatId())
                .orElseThrow(() -> new EntityNotFoundException("No Seat found with ID:: " + request.seatId()));
        if(Objects.equals(request.status(), "AVAILABLE")){
            seat.setLastModifiedBy(null);
            seat.setStatus(SeatStatus.AVAILABLE);
        }  else if(Objects.equals(request.status(), "RESERVED") && seat.getStatus() == SeatStatus.AVAILABLE){
            seat.setLastModifiedBy(request.sender());
            seat.setStatus(SeatStatus.RESERVED);
        } else if(Objects.equals(request.status(), "BOOKED") && seat.getStatus() == SeatStatus.RESERVED){
            seat.setLastModifiedBy(request.sender());
            seat.setStatus(SeatStatus.BOOKED);
        } else {
            throw new OperationNotPermittedException("INCORRECT STATUS PROVIDED");
        }
        seat.setLastModifiedDate(LocalDateTime.now());
        seatRepository.save(seat);
        return seatRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Seat::getId)
                .thenComparingInt(Seat::getId))
                .collect(Collectors.toList());
    }

    public List<Seat> getAllSeats () {
        return seatRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Seat::getId)
                .thenComparingInt(Seat::getId))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 30000) // Run every 1 minute
    public void releaseExpiredReservations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(2);
        List<Seat> seatsToUpdate = seatRepository.findAll().stream()
                .filter(seat -> seat.getStatus() == SeatStatus.RESERVED &&
                        seat.getLastModifiedDate().isBefore(cutoffTime))
                .collect(Collectors.toList());

        if (!seatsToUpdate.isEmpty()) {
            for (Seat seat : seatsToUpdate) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLastModifiedBy(null);
                seatRepository.save(seat);
            }

            // Notify WebSocket clients of the updated seat statuses
            List<Seat> updatedSeats = getAllSeats();
            messagingTemplate.convertAndSend("/topic/public", updatedSeats);
        }    }
}
