-- Migration: Raumerweiterung (Stammdaten & Status)

ALTER TABLE room
    ADD COLUMN building VARCHAR(50),
    ADD COLUMN floor INT,
    ADD COLUMN room_type VARCHAR(50) DEFAULT 'SEMINARRAUM',
    ADD COLUMN operational_status VARCHAR(50) DEFAULT 'AKTIV',
    ADD COLUMN barrierefreiheit BOOLEAN DEFAULT FALSE,
    ADD COLUMN description TEXT;

-- Notiz: 'features' werden als M-N Zuordnung verarbeitet (Tabelle room_features in Hibernate)
CREATE TABLE room_features (
    room_id BIGINT NOT NULL,
    feature VARCHAR(255)
);

ALTER TABLE room_features 
    ADD CONSTRAINT fk_room_features_room 
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE;
