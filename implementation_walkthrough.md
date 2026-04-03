# Architektur-Dokumentation: Campus Plattform JPA-Entitäten

Diese Dokumentation beschreibt die Struktur und das Zusammenspiel der JPA-Entitäten der Campus-Plattform. Sie dient als Leitfaden für Entwickler, um die Beziehungen zwischen Studenten, Dozenten, Modulen und Veranstaltungen zu verstehen.

## 1. Kern-Domain: Benutzer & Gruppen

Die Plattform unterscheidet zwischen administrativen Daten, Lehrinhalten und der konkreten Ausführung von Kursen.

### AppUser (Tabelle: `app_user`)
Zentrale Entität für alle Personen im System.
- **Eigenschaften**: `id`, `salutation`, `title`, `lastName`, `firstName`, `email`, `password`, `role` (Enum `Role`), `enabled`.
- **UI-Präferenzen**: `theme`, `brightness`, `language`.
- **Beziehungen**:
    - `studentProfile`: `@OneToOne` zu `StudentProfile` (mapped by `appUser`).
    - `teachingQualifications`: `@OneToMany` zu `ModuleLecturer` (Lehrbefähigungen).
    - `courseSubmissions`: `@OneToMany` zu `StudentCourseSubmission` (Prüfungsleistungen).
    - `absences`: `@OneToMany` zu `LecturerAbsence` (individuelle Fehlzeiten).

### StudentProfile (Tabelle: `student_profile`)
Zusatzdaten für Studenten, verknüpft via Shared Primary Key (`user_id`).
- **Eigenschaften**: `userId` (PK), `studentNumber` (unique), `startYear`.
- **Beziehungen**:
    - `appUser`: `@OneToOne` zurück zu `AppUser`.
    - `specialization`: `@ManyToOne` zur fachlichen Vertiefung.
    - `memberships`: `@OneToMany` zu `StudyGroupMembership`.

### StudyGroup (Tabelle: `study_group`)
Repräsentiert studentische Kohorten (z.B. "Informatik 2023").
- **Eigenschaften**: `id`, `name`.
- **Beziehungen**:
    - `specialization`: `@ManyToOne` (Pflicht) zur fachlichen Vertiefung.
    - `memberships`: `@OneToMany` zu `StudyGroupMembership`.
    - `courseSeries`: `@ManyToMany` (via `course_series_study_group`) zu den belegten Kursen.

### StudyGroupMembership (Tabelle: `study_group_membership`)
Join-Entität zwischen `StudentProfile` und `StudyGroup`.
- **Eigenschaften**: `id`, `joinedAt` (Zeitpunkt des Beitritts).
- **Beziehungen**:
    - `student`: `@ManyToOne` zu `StudentProfile`.
    - `studyGroup`: `@ManyToOne` zu `StudyGroup`.

## 2. Akademische Struktur (Studiengänge, Vertiefungen & Module)

### CourseOfStudy (Tabelle: `course_of_study`)
Repräsentiert einen gesamten Studiengang.
- **Eigenschaften**: `id`, `name`, `degreeType` (Enum `DegreeType`: BACHELOR, MASTER).
- **Beziehungen**:
    - `specializations`: `@OneToMany` zu den Vertiefungsrichtungen.
    - `modules`: `@OneToMany` zu den enthaltenen Modulen.

### Specialization (Tabelle: `specialization`)
Eine fachliche Vertiefung innerhalb eines Studiengangs.
- **Eigenschaften**: `id`, `name`.
- **Beziehungen**:
    - `courseOfStudy`: `@ManyToOne` zum Studiengang.
    - `modules`: `@OneToMany` zu den spezialisierungsspezifischen Modulen.

### Module (Tabelle: `module`)
Die fachliche Definition einer Lehrveranstaltung.
- **Eigenschaften**: `id`, `name`, `semester`, `requiredTotalHours`.
- **Beziehungen**:
    - `possibleExamTypes`: `@ManyToMany` (via `module_exam_types`) zu `ExamType`.
    - `preferredExamType`: `@ManyToOne` zu `ExamType` (Standard-Empfehlung).
    - `courseOfStudy`: `@ManyToOne` (Pflicht) zum zugehörigen Studiengang.
    - `specialization`: `@ManyToOne` zur optionalen Vertiefung.
    - `qualifiedLecturers`: `@OneToMany` zu `ModuleLecturer`.
    - `courseSeries`: `@OneToMany` zu den Durchläufen.

### ModuleLecturer (Tabelle: `module_lecturer`)
Join-Entität für die Lehrbefähigung der Dozenten.
- **Eigenschaften**: `id`, `grantedAt` (Zeitpunkt der Erteilung).
- **Beziehungen**:
    - `module`: `@ManyToOne` zum Modul.
    - `lecturer`: `@ManyToOne` zum Dozenten (`AppUser`).

### ExamType (Tabelle: `exam_type`)
Definiert die Arten der Prüfungen (Klausur, Hausarbeit, etc.).
- **Eigenschaften**: `id`, `type`, `nameDe`, `nameEn`, `shortDe`, `shortEn`.

## 3. Kursplanung & Ausführung (CourseSeries)

### CourseSeries (Tabelle: `course_series`)
Die konkrete Instanziierung eines Moduls in einem Semester.
- **Eigenschaften**: `id`, `status` (Enum `CourseStatus`), `examFileUrl`, `submissionStartDate`, `submissionDeadline`.
- **Beziehungen**:
    - `module`: `@ManyToOne` (Pflicht) zum übergeordneten Modul.
    - `assignedLecturer`: `@ManyToOne` (Pflicht) zum leitenden Dozenten (`AppUser`).
    - `selectedExamType`: `@ManyToOne` zur gewählten Prüfungsform.
    - `events`: `@OneToMany` zu Einzelterminen.
    - `studentSubmissions`: `@OneToMany` zu den studentischen Leistungen.
    - `studyGroups`: `@ManyToMany` (via `course_series_study_group`) zur Zielgruppe/Kohorte.

### Event (Tabelle: `event`)
Ein konkreter Kalender- oder Stundenplaneintrag.
- **Eigenschaften**: `id`, `name`, `eventType` (Enum `EventType`), `startTime`, `durationMinutes`.
- **Beziehungen**:
    - `courseSeries`: `@ManyToOne` zum Kursdurchlauf.
    - `room`: `@ManyToOne` zum Veranstaltungsort.

## 4. Prüfungs- und Leistungserfassung (Submissions)

### StudentCourseSubmission (Tabelle: `student_course_submission`)
Die Verknüpfung von Studenten-Leistungen mit einem Kursdurchlauf.
- **Eigenschaften**: `id`, `status` (Enum `SubmissionStatus`), `grade`, `documentUrl`, `submissionDate`.
- **Beziehungen**:
    - `student`: `@ManyToOne` zum leistenden Studenten (`AppUser`).
    - `courseSeries`: `@ManyToOne` zum Kursdurchlauf.

## 5. Abwesenheiten, Räume & Infrastruktur

### Room (Tabelle: `room`)
Physische Räumlichkeiten der Universität.
- **Eigenschaften**: `id`, `name`, `seats`, `examSeats`.

### GlobalAbsence (Tabelle: `global_absence`)
Systemweite Sperrzeiten (z.B. Feiertage).
- **Eigenschaften**: `id`, `name`, `startDate`, `endDate`.

### LecturerAbsence (Tabelle: `lecturer_absence`)
Individuelle Abwesenheiten von Lehrenden.
- **Eigenschaften**: `id`, `reason`, `startDate`, `endDate`.
- **Beziehungen**:
    - `lecturer`: `@ManyToOne` zum entsprechenden `AppUser`.

### InstitutionInfo (Tabelle: `institution_info`)
Stammdaten der Universität.
- **Eigenschaften**: `id`, `universityName`, `city`, `sekretariatEmail`, `sekretariatPhone`, `sekretariatOpeningTimes`, `websiteEmail`, `bibliothekUrl`, `mensaUrl`, `impressum`.

### Invitation (Tabelle: `invitation`)
Einladungs-Tokens für neue Benutzer.
- **Eigenschaften**: `id`, `email`, `role`, `studentNumber`, `token`, `status` (Enum `InvitationStatus`).

### VerificationToken (Tabelle: `verification_token`)
Sicherheits-Tokens.
- **Eigenschaften**: `id`, `token`, `expiryDate` (Date).
- **Beziehungen**:
    - `user`: `@OneToOne` (Pflicht) zum betroffenen `AppUser`.

## 6. Datenmodell-Übersicht (UML)

```mermaid
classDiagram
    direction TB
    
    class AppUser {
        +Long id
        +String salutation
        +String title
        +String lastName
        +String firstName
        +String email
        +String password
        +Role role
        +Boolean enabled
        +String theme
        +String brightness
        +String language
        +StudentProfile studentProfile
        +Set~ModuleLecturer~ teachingQualifications
        +Set~StudentCourseSubmission~ courseSubmissions
        +Set~LecturerAbsence~ absences
    }

    class StudentProfile {
        +Long userId
        +AppUser appUser
        +String studentNumber
        +Integer startYear
        +Specialization specialization
        +Set~StudyGroupMembership~ memberships
    }

    class CourseOfStudy {
        +Long id
        +String name
        +DegreeType degreeType
        +Set~Specialization~ specializations
        +Set~Module~ modules
    }

    class Specialization {
        +Long id
        +String name
        +CourseOfStudy courseOfStudy
        +Set~Module~ modules
    }

    class StudyGroup {
        +Long id
        +String name
        +Specialization specialization
        +Set~StudyGroupMembership~ memberships
    }

    class StudyGroupMembership {
        +Long id
        +StudentProfile student
        +StudyGroup studyGroup
        +LocalDateTime joinedAt
    }

    class Module {
        +Long id
        +String name
        +Integer semester
        +Integer requiredTotalHours
        +Set~ExamType~ possibleExamTypes
        +ExamType preferredExamType
        +Set~ModuleLecturer~ qualifiedLecturers
        +CourseOfStudy courseOfStudy
        +Specialization specialization
        +Set~CourseSeries~ courseSeries
    }

    class CourseSeries {
        +Long id
        +Module module
        +AppUser assignedLecturer
        +CourseStatus status
        +ExamType selectedExamType
        +String examFileUrl
        +LocalDateTime submissionStartDate
        +LocalDateTime submissionDeadline
        +Set~Event~ events
        +Set~StudentCourseSubmission~ studentSubmissions
        +Set~StudyGroup~ studyGroups
    }

    class StudentCourseSubmission {
        +Long id
        +AppUser student
        +CourseSeries courseSeries
        +SubmissionStatus status
        +String documentUrl
        +LocalDateTime submissionDate
        +Double grade
    }

    class Event {
        +Long id
        +CourseSeries courseSeries
        +Room room
        +String name
        +EventType eventType
        +LocalDateTime startTime
        +Integer durationMinutes
    }

    class ModuleLecturer {
        +Long id
        +Module module
        +AppUser lecturer
        +LocalDateTime grantedAt
    }

    class ExamType {
        +Long id
        +String type
        +String nameDe
        +String nameEn
        +String shortDe
        +String shortEn
    }

    class Room {
        +Long id
        +String name
        +Integer seats
        +Integer examSeats
    }

    class GlobalAbsence {
        +Long id
        +String name
        +LocalDateTime startDate
        +LocalDateTime endDate
    }

    class LecturerAbsence {
        +Long id
        +String reason
        +LocalDateTime startDate
        +LocalDateTime endDate
        +AppUser lecturer
    }

    class InstitutionInfo {
        +Long id
        +String universityName
        +String city
        +String sekretariatEmail
        +String sekretariatPhone
        +String sekretariatOpeningTimes
        +String websiteEmail
        +String libraryUrl
        +String cafeteriaUrl
        +String imprint
    }

    class Invitation {
        +Long id
        +String email
        +Role role
        +String studentNumber
        +String token
        +InvitationStatus status
    }

    class VerificationToken {
        +Long id
        +String token
        +AppUser user
        +Date expiryDate
    }

    AppUser "1" *-- "0..1" StudentProfile : has
    StudentProfile "1" -- "*" StudyGroupMembership : has
    StudentProfile "1" -- "1" Specialization : follows
    
    CourseOfStudy "1" -- "*" Specialization : contains
    CourseOfStudy "1" -- "*" Module : includes
    Specialization "1" -- "*" Module : focuses on
    Specialization "1" -- "*" StudyGroup : organizes
    
    StudyGroup "1" -- "*" StudyGroupMembership : has
    
    AppUser "1" -- "*" ModuleLecturer : qualified as
    Module "1" -- "*" ModuleLecturer : accessible by

    Module "1" -- "*" CourseSeries : instantiates
    CourseSeries "1" -- "1" AppUser : managed by
    CourseSeries "1" -- "*" Event : consists of
    CourseSeries "*" -- "*" StudyGroup : targeted to
    Module "*" -- "*" ExamType : possibleExamTypes
    Module "*" -- "0..1" ExamType : prefers
    CourseSeries "1" -- "0..1" ExamType : uses
    
    Event "1" -- "1" Room : held in
    
    StudentCourseSubmission "*" -- "1" CourseSeries : for
    StudentCourseSubmission "*" -- "1" AppUser : from student
    
    AppUser "1" -- "*" LecturerAbsence : has
    AppUser "1" -- "0..1" VerificationToken : verified by
```

---

### Entwickler-Hinweise:
1. **Kein ManyToMany** (Ausnahme: `Module.possibleExamTypes` und `CourseSeries.studyGroups`): Standardmäßig sollten neue Beziehungen über explizite Join-Tabellen/Entitäten (wie `ModuleLecturer`) gelöst werden, um Metadaten hinzufügen zu können.
2. **Cascade-Rules**: Löschvorgänge bei `AppUser` oder `Module` kaskadieren standardmäßig (`CascadeType.ALL`) auf ihre Join-Tabellen-Einträge, um Datenleichen zu vermeiden.
3. **Naming**: Alle Tabellen nutzen den `snake_case` Standard (z.B. `app_user`), während Java-Klassen `CamelCase` nutzen.
