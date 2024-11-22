package com.rajshah.ticketing.config;

import com.rajshah.ticketing.seat.Seat;
import com.rajshah.ticketing.seat.SeatRepository;
import com.rajshah.ticketing.seat.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SeatRepository seatRepository;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            log.info("user disconnected: {}", username);
            List<Seat> seats = seatRepository.findAll();
            for (Seat seat : seats){
                if (Objects.equals(seat.getLastModifiedBy(), username) && !seat.getStatus().equals(SeatStatus.BOOKED)){
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setLastModifiedBy(null);
                    seatRepository.save(seat);
                }
            }
        }
    }

}