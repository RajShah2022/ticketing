package com.rajshah.ticketing.seat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SeatController {

    private final SeatService service;

    @MessageMapping("/seat.updateStatus")
    @SendTo("/topic/public")
    public List<Seat> updateStatus(
            @Payload SeatStatusRequest request
    ) {
        return service.updateSeatStatus(request);
    }

    @MessageMapping("/seat.addUser")
    @SendTo("/topic/public")
    public List<Seat> addUser(
            @Payload SeatStatusRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", request.sender());
        return service.getAllSeats();
    }
}
