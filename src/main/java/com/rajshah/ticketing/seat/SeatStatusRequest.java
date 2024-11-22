package com.rajshah.ticketing.seat;

public record SeatStatusRequest(
        Integer seatId,
        String status,
        String sender
) {
}
