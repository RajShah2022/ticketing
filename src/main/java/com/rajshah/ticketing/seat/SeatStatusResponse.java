package com.rajshah.ticketing.seat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SeatStatusResponse {
    private List<Seat> seats;
}
