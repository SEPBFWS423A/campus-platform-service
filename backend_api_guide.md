# API-Spezifikation für das CampusPlatform-Backend (V4.1)

Diese Dokumentation bietet eine vollständige Referenz für alle Endpunkte des CampusPlatform-Backends. Jeder Endpunkt wird detailliert mit seinem Zweck, der erforderlichen Logik sowie den exakten Request- und Response-Strukturen erläutert.

---

## 1. Authentifizierung & Onboarding (`/api/auth`)

Diese Endpunkte dienen dem Zugangsschutz und der initialen Einrichtung von Benutzerkonten. Alle Endpunkte in dieser Sektion sind **öffentlich** zugänglich.

### **POST** `/api/auth/login`
- **Zweck**: Authentifizierung eines bestehenden Benutzers.
- **Backend-Logik**: Überprüfung der E-Mail und des Passworts; Prüfung des `enabled`-Status.
- **Request (`json`)**:
  ```json
  {
    "email": "user@beispiel.de",
    "password": "beispielPasswort123"
  }
  ```
- **Response (`json`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1..."
  }
  ```

### **POST** `/api/auth/complete-registration`
- **Zweck**: Abschluss der Registrierung nach einer Einladung.
- **Backend-Logik**: Validiert das Einladungs-Token; Speichert Namen und Passwort; Setzt den Benutzer auf `enabled: true`.
- **Request (`json`)**:
  ```json
  {
    "token": "d7b2e3f4-...",
    "firstName": "Max",
    "lastName": "Mustermann",
    "password": "meinNeuesPasswort123"
  }
  ```
- **Response**: `200 OK` (Leer)

### **POST** `/api/auth/forgot-password`
- **Zweck**: Anforderung eines Links zum Zurücksetzen des Passworts.
- **Backend-Logik**: Prüft Existenz der E-Mail; Generiert Reset-Token; Versendet E-Mail.
- **Request (`json`)**:
  ```json
  {
    "email": "user@beispiel.de"
  }
  ```
- **Response**: `200 OK` (Leer)

### **POST** `/api/auth/reset-password`
- **Zweck**: Vergabe eines neuen Passworts über ein gültiges Reset-Token.
- **Parameter**: `token` (Query-Parameter aus dem E-Mail-Link)
- **Request (`json`)**:
  ```json
  {
    "newPassword": "ganzNeuesPasswort456"
  }
  ```
- **Response**: `200 OK` (Leer)

---

## 2. Benutzerverwaltung & Profil (`/api/users`)

Diese Endpunkte erfordern eine Authentifizierung via JWT. Sie gelten für alle Rollen (`ADMIN`, `LECTURER`, `STUDENT`).

### **GET** `/api/users/me`
- **Zweck**: Lädt die Profildaten und UI-Präferenzen des angemeldeten Benutzers.
- **Response (`json`)**:
  ```json
  {
    "id": 1,
    "email": "max@mustermann.de",
    "firstName": "Max",
    "lastName": "Mustermann",
    "role": "STUDENT",
    "theme": "primary-blue",
    "brightness": "dark",
    "language": "de",
    "studentNumber": "123456",
    "startYear": 2024,
    "specializationId": 5,
    "specializationName": "Software Engineering",
    "courseOfStudyName": "Bachelor Informatik"
  }
  ```

### **PUT** `/api/users/profile/details`
- **Zweck**: Erlaubt Benutzern, ihren eigenen Vornamen und Nachnamen zu aktualisieren.
- **Request (`json`)**:
  ```json
  {
    "firstName": "Maximilian",
    "lastName": "Mustermann"
  }
  ```
- **Response**: `200 OK` (Leer)

### **PUT** `/api/users/profile/preferences`
- **Zweck**: Speichert UI-Einstellungen wie Farbschema, Darkmode-Status und Sprache.
- **Request (`json`)**:
  ```json
  {
    "theme": "nebula-purple",
    "brightness": "light",
    "language": "en"
  }
  ```
- **Response**: `200 OK` (Leer)

### **POST** `/api/users/change-password`
- **Zweck**: Passwortänderung innerhalb der App (erfordert das alte Passwort zur Sicherheit).
- **Request (`json`)**:
  ```json
  {
    "oldPassword": "aktuellesPasswort123",
    "newPassword": "superSicheresNeuesPasswort789"
  }
  ```
- **Response**: `200 OK` (Leer)

### **GET** `/api/users/institution`
- **Zweck**: Abruf der globalen Campus-Informationen für alle Benutzer.
- **Response (`json`)**:
  ```json
  {
    "universityName": "Campus University",
    "city": "Musterstadt",
    "sekretariatEmail": "sek@campus.edu",
    "sekretariatPhone": "+49 30 123456",
    "sekretariatOpeningTimes": "Mo-Fr: 09:00 - 15:00",
    "websiteEmail": "it-support@campus.edu",
    "bibliothekUrl": "https://lib.campus.edu",
    "mensaUrl": "https://mensa.campus.edu",
    "impressum": "Angaben gemäß § 5 TMG..."
  }
  ```

### **GET** `/api/users/rooms`
- **Zweck**: Übersicht aller Räume am Campus (z.B. für die Stundenplan-Ansicht).
- **Response (`json`)**:
  ```json
  [
    {
      "id": 1,
      "name": "Auditiorium A",
      "seats": 120,
      "examSeats": 60
    }
  ]
  ```

---

## 3. Administration (`/api/admin`)

Diese Endpunkte sind exklusiv für Benutzer mit der Rolle `ADMIN`.

### **Benutzer-Management**

#### **GET** `/api/admin/users`
- **Zweck**: Detaillierte Liste aller Benutzer für die Verwaltungsebene.
- **Response (`json`)**:
  ```json
  [
    {
      "id": 1,
      "salutation": "Herr",
      "title": "Prof. Dr.",
      "firstName": "Max",
      "lastName": "Mustermann",
      "email": "max@fh-beispiel.de",
      "role": "LECTURER",
      "enabled": true,
      "studentNumber": null,
      "startYear": null,
      "specializationId": null,
      "specializationName": null,
      "courseOfStudyName": null
    }
  ]
  ```

#### **GET** `/api/admin/users/stats`
- **Zweck**: Liefert Kernzahlen für das Admin-Dashboard.
- **Response (`json`)**:
  ```json
  {
    "total": 540,
    "staff": 40,
    "students": 500
  }
  ```

#### **POST** `/api/admin/invitations`
- **Request (`json`)**:
  ```json
  {
    "email": "new-student@beispiel.de",
    "role": "STUDENT",
    "studentNumber": "882299"
  }
  ```

#### **POST** `/api/admin/invitations/bulk`
- **Request (`json`)**:
  ```json
  {
    "invitations": [
      { "email": "s1@ex.de", "role": "STUDENT", "studentNumber": "101" },
      { "email": "d1@ex.de", "role": "LECTURER" }
    ]
  }
  ```

#### **PUT** `/api/admin/users/{id}`
- **Request (`json`)**:
  ```json
  {
    "firstName": "Max",
    "lastName": "Mustermann",
    "email": "max.neu@fh.de",
    "role": "ADMIN",
    "studentNumber": "123",
    "startYear": 2024,
    "specializationId": 2
  }
  ```

---

### **Studiengruppen**

#### **GET** `/api/admin/groups`
- **Response (`json`)**:
  ```json
  [
    {
      "id": 1,
      "name": "Winf 2024 Q3",
      "courseOfStudyName": "Bachelor Wirtschaftsinformatik",
      "specialization": "Digital Transformation",
      "memberCount": 15,
      "members": [
        {
          "id": 10,
          "studentNumber": "123456",
          "title": null,
          "firstName": "Max",
          "lastName": "Mustermann"
        }
      ]
    }
  ]
  ```

#### **POST** `/api/admin/groups`
- **Request (`json`)**:
  ```json
  {
    "name": "Gruppe B",
    "specializationId": 5
  }
  ```

---

### **Akademischer Katalog & Struktur**

#### **Studiengänge (GET)** `/api/admin/courses`
- **Response (`json`)**:
  ```json
  [
    { "id": 1, "name": "Informatik", "degreeType": "BACHELOR" }
  ]
  ```

#### **Studiengänge (POST)** `/api/admin/courses`
- **Request (`json`)**:
  ```json
  { "name": "Management", "degreeType": "MASTER" }
  ```

#### **Vertiefungen (GET)** `/api/admin/specializations`
- **Response (`json`)**:
  ```json
  [
    { "id": 5, "name": "Cyber Security", "courseId": 1 }
  ]
  ```

#### **Vertiefungen (POST)** `/api/admin/specializations`
- **Request (`json`)**:
  ```json
  { "name": "Artificial Intelligence", "courseId": 1 }
  ```

---

### **Module & Prüfungsformen**

#### **Modulkatalog (GET)** `/api/admin/modules`
- **Response (`json`)**:
  ```json
  [
    {
      "id": 1,
      "name": "Algorithmen & Datenstrukturen",
      "semester": 2,
      "requiredTotalHours": 180,
      "possibleExamTypes": [
        { "id": 1, "type": "KLAUSUR", "nameDe": "Klausur", "nameEn": "Exam", "shortDe": "KL", "shortEn": "EX" }
      ],
      "preferredExamTypeId": 1,
      "lecturers": [
        { "id": 4, "title": "Prof.", "firstName": "Alice", "lastName": "Schmidt" }
      ],
      "courseOfStudyId": 1,
      "specializationId": null
    }
  ]
  ```

#### **Modulkatalog (POST / PUT)** `/api/admin/modules`
- **Request (`json`)**:
  ```json
  {
    "name": "Web-Entwicklung",
    "semester": 3,
    "requiredTotalHours": 150,
    "examTypeIds": [1, 5],
    "lecturerIds": [4, 8],
    "courseOfStudyId": 1,
    "specializationId": 10,
    "preferredExamTypeId": 5
  }
  ```

#### **Prüfungsformen (GET / POST / PUT)** `/api/admin/exam-types`
- **Request (für POST/PUT) (`json`)**:
  ```json
  {
    "type": "HAUSARBEIT",
    "nameDe": "Hausarbeit",
    "nameEn": "Term Paper",
    "shortDe": "HA",
    "shortEn": "TP"
  }
  ```

---

### **Institutions-Management**

#### **Institution Update (PUT)** `/api/admin/institution`
- **Zweck**: Aktualisierung der Campus-Stammdaten.
- **Request (`json`)**: (Schema identisch zu `GET /api/users/institution`)

---

## 4. Fehlerbehandlung

Das Backend liefert im Fehlerfall standardisierte Antworten, die einen Key für die Lokalisierung enthalten.

**Beispiel-Fehler (`json`)**:
```json
{
  "message": "error.permission_denied"
}
```

**Häufige Keys**:
- `error.invalid_credentials`: E-Mail oder Passwort falsch.
- `error.token_expired`: Das Token für Passwort-Reset oder Registrierung ist abgelaufen.
- `error.unauthorized`: Kein Zugriff (JWT ungültig oder abgelaufen).
- `error.permission_denied`: Die Benutzerrolle ist für diesen Endpunkt nicht ausreichend.
