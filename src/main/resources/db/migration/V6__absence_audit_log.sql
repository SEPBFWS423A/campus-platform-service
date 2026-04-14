-- V6: Audit-Log Tabelle für Abwesenheitsstatus-Übergänge (Issue #10)
CREATE TABLE absence_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    absence_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    performed_by VARCHAR(100),
    performed_at DATETIME NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    reason VARCHAR(500)
);
