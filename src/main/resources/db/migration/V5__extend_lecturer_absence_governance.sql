-- V5: Governance-Felder für Abwesenheitsverwaltung (Issue #10)
ALTER TABLE lecturer_absence ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'BEANTRAGT';
ALTER TABLE lecturer_absence ADD COLUMN priority VARCHAR(20) DEFAULT 'MEDIUM';
ALTER TABLE lecturer_absence ADD COLUMN notice_days INT;
ALTER TABLE lecturer_absence ADD COLUMN document_required BOOLEAN DEFAULT FALSE;
ALTER TABLE lecturer_absence ADD COLUMN approved_by VARCHAR(100);
ALTER TABLE lecturer_absence ADD COLUMN approved_at DATETIME;
ALTER TABLE lecturer_absence ADD COLUMN rejection_reason VARCHAR(500);
