-- V3: Sample data for testing

-- Sample passengers
INSERT INTO passengers (passenger_id, first_name, last_name, email, phone) VALUES
('P001', 'John', 'Doe', 'john.doe@email.com', '+1234567890'),
('P002', 'Jane', 'Smith', 'jane.smith@email.com', '+1234567891'),
('P003', 'Bob', 'Johnson', 'bob.johnson@email.com', '+1234567892'),
('P004', 'Alice', 'Williams', 'alice.williams@email.com', '+1234567893'),
('P005', 'Charlie', 'Brown', 'charlie.brown@email.com', '+1234567894');

-- Sample flights
INSERT INTO flights (flight_id, flight_number, origin, destination, departure_time, arrival_time, aircraft_type, status, check_in_opens_at, check_in_closes_at) VALUES
('SK101', 'SK101', 'LAX', 'JFK', CURRENT_TIMESTAMP + INTERVAL '24 hours', CURRENT_TIMESTAMP + INTERVAL '30 hours', 'Boeing 737', 'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '23 hours'),
('SK102', 'SK102', 'JFK', 'LAX', CURRENT_TIMESTAMP + INTERVAL '48 hours', CURRENT_TIMESTAMP + INTERVAL '54 hours', 'Airbus A320', 'SCHEDULED', CURRENT_TIMESTAMP + INTERVAL '24 hours', CURRENT_TIMESTAMP + INTERVAL '47 hours');

-- Sample seats for SK101 (Boeing 737 - 30 seats)
-- Economy seats (rows 1-5, columns A-F)
INSERT INTO seats (seat_id, flight_id, seat_number, row_number, column_letter, seat_class, position, status, price) VALUES
-- Row 1
(gen_random_uuid(), 'SK101', '1A', 1, 'A', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '1B', 1, 'B', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '1C', 1, 'C', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '1D', 1, 'D', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '1E', 1, 'E', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '1F', 1, 'F', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
-- Row 2
(gen_random_uuid(), 'SK101', '2A', 2, 'A', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '2B', 2, 'B', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '2C', 2, 'C', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '2D', 2, 'D', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '2E', 2, 'E', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '2F', 2, 'F', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
-- Row 3
(gen_random_uuid(), 'SK101', '3A', 3, 'A', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '3B', 3, 'B', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '3C', 3, 'C', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '3D', 3, 'D', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '3E', 3, 'E', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '3F', 3, 'F', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
-- Row 4
(gen_random_uuid(), 'SK101', '4A', 4, 'A', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '4B', 4, 'B', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '4C', 4, 'C', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '4D', 4, 'D', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '4E', 4, 'E', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '4F', 4, 'F', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
-- Row 5
(gen_random_uuid(), 'SK101', '5A', 5, 'A', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '5B', 5, 'B', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '5C', 5, 'C', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '5D', 5, 'D', 'ECONOMY', 'AISLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '5E', 5, 'E', 'ECONOMY', 'MIDDLE', 'AVAILABLE', 150.00),
(gen_random_uuid(), 'SK101', '5F', 5, 'F', 'ECONOMY', 'WINDOW', 'AVAILABLE', 150.00);
