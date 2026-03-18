-- V1: Initial Schema for SkyHigh Check-In System

-- Passengers table
CREATE TABLE passengers (
    passenger_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20)
);

-- Flights table
CREATE TABLE flights (
    flight_id VARCHAR(20) PRIMARY KEY,
    flight_number VARCHAR(10) NOT NULL,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    aircraft_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    check_in_opens_at TIMESTAMP,
    check_in_closes_at TIMESTAMP
);

-- Seats table with optimistic locking
CREATE TABLE seats (
    seat_id UUID PRIMARY KEY,
    flight_id VARCHAR(20) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    row_number INTEGER NOT NULL,
    column_letter VARCHAR(2) NOT NULL,
    seat_class VARCHAR(20) NOT NULL,
    position VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_seat_flight FOREIGN KEY (flight_id) REFERENCES flights(flight_id)
);

CREATE INDEX idx_seat_flight_number ON seats(flight_id, seat_number);
CREATE INDEX idx_seat_status ON seats(status);

-- Check-ins table
CREATE TABLE check_ins (
    check_in_id UUID PRIMARY KEY,
    passenger_id VARCHAR(50) NOT NULL,
    flight_id VARCHAR(20) NOT NULL,
    booking_reference VARCHAR(10) NOT NULL,
    seat_number VARCHAR(10),
    status VARCHAR(20) NOT NULL,
    baggage_weight DECIMAL(10, 2),
    payment_required BOOLEAN,
    payment_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_checkin_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id),
    CONSTRAINT fk_checkin_flight FOREIGN KEY (flight_id) REFERENCES flights(flight_id)
);

CREATE UNIQUE INDEX idx_checkin_passenger_flight ON check_ins(passenger_id, flight_id);
CREATE INDEX idx_checkin_status ON check_ins(status);

-- Seat holds table (120-second reservations)
CREATE TABLE seat_holds (
    hold_id UUID PRIMARY KEY,
    seat_id UUID NOT NULL,
    check_in_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    held_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    CONSTRAINT fk_hold_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id),
    CONSTRAINT fk_hold_checkin FOREIGN KEY (check_in_id) REFERENCES check_ins(check_in_id)
);

CREATE INDEX idx_hold_seat ON seat_holds(seat_id);
CREATE INDEX idx_hold_checkin ON seat_holds(check_in_id);
CREATE INDEX idx_hold_status_expires ON seat_holds(status, expires_at);

-- Baggage table
CREATE TABLE baggage (
    baggage_id UUID PRIMARY KEY,
    check_in_id UUID NOT NULL,
    weight DECIMAL(10, 2) NOT NULL,
    pieces INTEGER NOT NULL,
    excess_weight DECIMAL(10, 2),
    excess_fee DECIMAL(10, 2),
    payment_transaction_id VARCHAR(100),
    validated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_baggage_checkin FOREIGN KEY (check_in_id) REFERENCES check_ins(check_in_id)
);

CREATE INDEX idx_baggage_checkin ON baggage(check_in_id);
