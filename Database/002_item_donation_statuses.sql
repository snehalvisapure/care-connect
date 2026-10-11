-- Adds the ACCEPTED and REJECTED statuses for item donations.
-- Run once on your local MySQL (and on Railway if you use it).
ALTER TABLE item_donations
    MODIFY COLUMN delivery_status
    ENUM('PENDING','ACCEPTED','REJECTED','PICKED_UP','DELIVERED','CANCELLED')
    DEFAULT 'PENDING';