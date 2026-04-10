package de.campusplatform.campus_platform_service;

import de.campusplatform.campus_platform_service.enums.*;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.model.Module;
import de.campusplatform.campus_platform_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ExamTypeRepository examTypeRepository;
    private final CourseOfStudyRepository courseOfStudyRepository;
    private final SpecializationRepository specializationRepository;
    private final AppUserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMembershipRepository membershipRepository;
    private final ModuleRepository moduleRepository;
    private final InstitutionRepository institutionRepository;
    private final FaqRepository faqRepository;  
    private final CourseSeriesRepository courseSeriesRepository;
    private final RoomRepository roomRepository;
    private final EventRepository eventRepository;
    private final StudentCourseSubmissionRepository submissionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (examTypeRepository.count() == 0) {
            // --- Institution Identity ---
            InstitutionInfo glCampus = institutionRepository.save(InstitutionInfo.builder()
                    .universityName("FDSE")
                    .city("Bergisch Gladbach")
                    .sekretariatEmail("office@gl.campusplatform.de")
                    .sekretariatPhone("+49 2202 123456")
                    .sekretariatOpeningTimes("Mon-Fri: 09:00 - 15:00")
                    .websiteEmail("admin@campusplatform.de")
                    .bibliothekUrl("https://bib.gl.campusplatform.de")
                    .mensaUrl("https://mensa.gl.campusplatform.de")
                    .impressum("FDSE Bergisch Gladbach - Campus Platform Management")
                    .build());

            // Base Exam Types
            ExamType kl = examTypeRepository.save(ExamType.builder().type("KL").category(ExamCategory.WRITTEN).nameDe("Klausur").nameEn("Written Exam")
                    .shortDe("KL").shortEn("WE").build());
            ExamType rf = examTypeRepository.save(ExamType.builder().type("RF").category(ExamCategory.SUBMISSION).nameDe("Referat").nameEn("Presentation")
                    .shortDe("RF").shortEn("PRS").build());
            ExamType sa = examTypeRepository.save(ExamType.builder().type("SA").category(ExamCategory.SUBMISSION).nameDe("Studienarbeit")
                    .nameEn("Term Paper").shortDe("SA").shortEn("TP").build());
            Set<ExamType> allTypes = Set.of(kl, rf, sa);

            // --- Courses & Specializations ---
            CourseOfStudy bwBachelor = courseOfStudyRepository
                    .save(CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy bwMaster = courseOfStudyRepository
                    .save(CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.MASTER).build());
            createSpecializations(bwBachelor,
                    List.of("Automotive Management", "Banking and Finance", "Business Management",
                            "International Business", "International Business Management", "Logistikmanagement",
                            "Steuerrecht"));
            createSpecializations(bwMaster,
                    List.of("Automotive Management", "Business Management", "Controlling",
                            "Einkauf und Logistikmanagement", "Human Resource Management",
                            "Management und Führung im Finanzvertrieb", "Marketing und Vertrieb"));

            CourseOfStudy wiBachelor = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy wiMaster = courseOfStudyRepository
                    .save(CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.MASTER).build());
            createSpecializations(wiBachelor, List.of("Business Process Management", "Cyber Security", "IT-Consulting",
                    "Künstliche Intelligenz & Data Science", "Software Engineering"));
            createSpecializations(wiMaster, List.of("Angewandte Künstliche Intelligenz", "Cyber Security",
                    "IT-Management", "Künstliche Intelligenz & Data Science", "Nachhaltige IT"));

            CourseOfStudy aiBachelor = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Angewandte Informatik").degreeType(DegreeType.BACHELOR).build());
            createSpecializations(aiBachelor,
                    List.of("Smart Systems", "Software-Entwicklung und -Management", "Virtual Worlds"));

            AppUser instructor = userRepository.findByEmail("lecturer@campusplatform.de")
                    .orElseThrow(() -> new IllegalStateException("Initial lecturer not found!"));
            AppUser p = createLecturer(Salutation.MS, AcademicTitle.PROF_DR, "Patrice", "Admin",
                    "p.admin@campusplatform.de");
            AppUser a = createLecturer(Salutation.MR, AcademicTitle.DR, "Adam", "Smart", "a.smart@campusplatform.de");
            AppUser m = createLecturer(Salutation.MR, null, "Marc", "Code", "m.code@campusplatform.de");
            List<AppUser> lecturers = List.of(instructor, p, a, m);

            // --- Mockup Modules (Wirtschaftsinformatik) ---
            Specialization seSpec = specializationRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Software Engineering")
                            && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                    .findFirst().orElseThrow();

            Module prog1 = moduleRepository.save(Module.builder()
                    .name("Programmierung 1")
                    .semester(1)
                    .requiredTotalHours(40)
                    .courseOfStudy(wiBachelor)
                    .specialization(null)
                    .possibleExamTypes(allTypes)
                    .preferredExamType(kl)
                    .build());

            Module prog2 = moduleRepository.save(Module.builder()
                    .name("Programmierung 2")
                    .semester(2)
                    .requiredTotalHours(40)
                    .courseOfStudy(wiBachelor)
                    .specialization(null)
                    .possibleExamTypes(allTypes)
                    .preferredExamType(kl)
                    .build());

            Module seProject = moduleRepository.save(Module.builder()
                    .name("Software Engineering Project")
                    .semester(6)
                    .requiredTotalHours(40)
                    .courseOfStudy(wiBachelor)
                    .specialization(seSpec)
                    .possibleExamTypes(allTypes)
                    .preferredExamType(rf)
                    .build());

            assignLecturersToModule(prog1, lecturers);
            assignLecturersToModule(prog2, lecturers);
            assignLecturersToModule(seProject, lecturers);

            // --- Mockup Students and Groups ---
            if (studyGroupRepository.count() == 0) {
                Specialization csSpec = specializationRepository.findAll().stream()
                        .filter(s -> s.getName().equals("Cyber Security")
                                && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                        .findFirst().orElseThrow();
                Specialization iteSpec = specializationRepository.findAll().stream()
                        .filter(s -> s.getName().equals("IT-Consulting")
                                && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                        .findFirst().orElseThrow();

                // Group Naming Logic based on Frontend:
                // {City}{Uni}{Course}{Spec}{Quartal}{YearYY}{Degree}
                // Bergisch Gladbach (B), FDSE (F), Wirtschaftsinformatik (W), SE/CS/ITC
                // (S/C/I), Q4 (4), 2024 (24), Bachelor (A)
                StudyGroup g1 = studyGroupRepository
                        .save(StudyGroup.builder().name("BFWS424A").specialization(seSpec).startYear(2024).startQuartal(4).build());
                StudyGroup g2 = studyGroupRepository
                        .save(StudyGroup.builder().name("BFWC424A").specialization(csSpec).startYear(2024).startQuartal(4).build());
                StudyGroup g3 = studyGroupRepository
                        .save(StudyGroup.builder().name("BFWI424A").specialization(iteSpec).startYear(2024).startQuartal(4).build());

                createStudentsForGroup(g1, 20, 1000);
                createStudentsForGroup(g2, 10, 2000);
                createStudentsForGroup(g3, 10, 3000);
            }

            createFaqs();
  
            // --- Course Series Demo Data ---
            if (courseSeriesRepository.count() == 0) {
                java.util.List<StudyGroup> allGroups = studyGroupRepository.findAll();
                StudyGroup mockG1 = allGroups.stream().filter(g -> g.getName().equals("BFWS424A")).findFirst()
                        .orElse(null);
                StudyGroup mockG2 = allGroups.stream().filter(g -> g.getName().equals("BFWI424A")).findFirst()
                        .orElse(null);

                if (mockG1 == null || mockG2 == null) {
                    throw new IllegalStateException("Required study groups for demo data are missing!");
                }

                CourseSeries cs1 = CourseSeries.builder()
                        .module(prog1)
                        .assignedLecturer(instructor)
                        .status(CourseStatus.ACTIVE)
                        .selectedExamType(kl)
                        .submissionStartDate(LocalDateTime.now().minusDays(10))
                        .submissionDeadline(LocalDateTime.now().plusDays(20))
                        .studyGroups(java.util.Set.of(mockG1))
                        .build();

                CourseSeries cs2 = CourseSeries.builder()
                        .module(seProject)
                        .assignedLecturer(instructor)
                        .status(CourseStatus.PLANNED)
                        .selectedExamType(rf)
                        .submissionStartDate(LocalDateTime.now().plusDays(30))
                        .submissionDeadline(LocalDateTime.now().plusDays(60))
                        .studyGroups(java.util.Set.of(mockG2))
                        .build();

                CourseSeries cs3 = CourseSeries.builder()
                        .module(prog1)
                        .assignedLecturer(instructor)
                        .status(CourseStatus.GRADING)
                        .examStatus(ExamStatus.GRADING)
                        .selectedExamType(kl)
                        .submissionStartDate(LocalDateTime.now().minusDays(90))
                        .submissionDeadline(LocalDateTime.now().minusDays(30))
                        .studyGroups(java.util.Set.of(mockG1, mockG2))
                        .build();

                List<CourseSeries> savedSeries = courseSeriesRepository.saveAll(List.of(cs1, cs2, cs3));

                // Create Demo Events using existing rooms
                List<Room> rooms = roomRepository.findAll();
                if (!rooms.isEmpty()) {
                    for (CourseSeries series : savedSeries) {
                        for (int i = 0; i < 10; i++) {
                            Room room = rooms.get(i % rooms.size());
                            EventType type = EventType.LEHRVERANSTALTUNG;
                            LocalDateTime startTime = series.getSubmissionStartDate().plusWeeks(i)
                                    .withHour(Math.min(8 + i, 18)).withMinute(0);

                            // Special case for cs3: set last event as KLAUSUR yesterday
                            if (series == cs3 && i == 9) {
                                type = EventType.KLAUSUR;
                                startTime = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0);
                            }

                            eventRepository.save(Event.builder()
                                    .courseSeries(series)
                                    .room(room)
                                    .name(series.getModule().getName() + " (" + (i + 1) + ")")
                                    .eventType(type)
                                    .startTime(startTime)
                                    .durationMinutes(90)
                                    .build());
                        }
                    }
                }

                // Create some mock submissions for cs3 to test grading
                java.util.List<AppUser> allStudents = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.STUDENT)
                        .limit(5)
                        .collect(Collectors.toList());
                
                for (AppUser student : allStudents) {
                    submissionRepository.save(StudentCourseSubmission.builder()
                            .courseSeries(cs3)
                            .student(student)
                            .status(SubmissionStatus.SUBMITTED)
                            .submissionDate(LocalDateTime.now().minusDays(2))
                            .documentUrl("https://example.com/submission.pdf")
                            .build());
                }
            }

            System.out.println("=================================================================");
            System.out.println("   ✓ CAMPUS PLATFORM DATA INITIALIZED");
            System.out.println("   Institution:     " + glCampus.getUniversityName() + " (" + glCampus.getCity() + ")");
            System.out.println("   Website:         https://gl.campusplatform.de");
            System.out.println("   Exam Types:      " + examTypeRepository.count());
            System.out.println("   Courses:         " + courseOfStudyRepository.count());
            System.out.println("   Course Series:   " + courseSeriesRepository.count());
            System.out.println("   Mock Students:   40");
            System.out.println("   Study Groups:    " + studyGroupRepository.count() + " (Aligned with Frontend Logic)");
            System.out.println("   FAQs:            " + faqRepository.count());
            System.out.println("=================================================================");
        }
    }

    private AppUser createLecturer(Salutation salutation, AcademicTitle title, String first, String last,
            String email) {
        return userRepository.save(AppUser.builder()
                .salutation(salutation)
                .title(title)
                .firstName(first)
                .lastName(last)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .role(Role.LECTURER)
                .enabled(true)
                .startYear(2024)
                .startQuartal(4)
                .build());
    }

    private void assignLecturersToModule(Module module, List<AppUser> lecturers) {
        Set<ModuleLecturer> quals = new java.util.HashSet<>();
        for (AppUser l : lecturers) {
            quals.add(ModuleLecturer.builder().module(module).lecturer(l).grantedAt(LocalDateTime.now()).build());
        }
        module.setQualifiedLecturers(quals);
        moduleRepository.save(module);
    }

    private void createStudentsForGroup(StudyGroup group, int count, int studentNumStart) {
        for (int i = 1; i <= count; i++) {
            String firstName = "Student" + (studentNumStart + i);
            String lastName = group.getSpecialization().getName().split(" ")[0] + i;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@campusplatform.de";
            Salutation salutation = (i % 2 == 0) ? Salutation.MR : Salutation.MS;

            AppUser user = AppUser.builder()
                    .salutation(salutation)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(passwordEncoder.encode("password"))
                    .role(Role.STUDENT)
                    .enabled(true)
                    .startYear(2024)
                    .startQuartal(4)
                    .build();
            user = userRepository.save(user);

            StudentProfile profile = StudentProfile.builder()
                    .appUser(user)
                    .studentNumber("S-" + (studentNumStart + i))
                    .startYear(2024)
                    .startQuartal(4)
                    .specialization(group.getSpecialization())
                    .build();
            profile = profileRepository.save(profile);

            user.setStudentProfile(profile);
            userRepository.save(user);

            membershipRepository.save(StudyGroupMembership.builder().student(profile).studyGroup(group).build());
        }
    }

    private void createSpecializations(CourseOfStudy course, List<String> names) {
        for (String name : names) {
            specializationRepository.save(Specialization.builder().name(name).courseOfStudy(course).build());
        }
    }

    private void createFaqs() {
        if (faqRepository.count() > 0) {
            return;
        }

        Faq faq1 = Faq.builder()
                .sortOrder(1)
                .published(true)
                .build();
        faq1.getTranslations().add(FaqTranslation.builder()
                .faq(faq1)
                .languageCode("de")
                .question("Wie kann ich mein Passwort zurücksetzen?")
                .answer("Nutzen Sie auf der Login-Seite die Funktion \"Passwort vergessen\". Anschließend erhalten Sie per E-Mail einen Link zum Zurücksetzen Ihres Passworts.")
                .category("Login & Sicherheit")
                .build());
        faq1.getTranslations().add(FaqTranslation.builder()
                .faq(faq1)
                .languageCode("en")
                .question("How can I reset my password?")
                .answer("Use the \"Forgot password\" function on the login page. You will then receive an email with a link to reset your password.")
                .category("Login & Security")
                .build());
        faqRepository.save(faq1);

        Faq faq2 = Faq.builder()
                .sortOrder(2)
                .published(true)
                .build();
        faq2.getTranslations().add(FaqTranslation.builder()
                .faq(faq2)
                .languageCode("de")
                .question("Wo finde ich meinen Prüfungsplan?")
                .answer("Ihren Prüfungsplan finden Sie im Bereich \"Prüfungen\". Dort werden alle freigegebenen Klausur- und Prüfungstermine angezeigt.")
                .category("Prüfungen")
                .build());
        faq2.getTranslations().add(FaqTranslation.builder()
                .faq(faq2)
                .languageCode("en")
                .question("Where can I find my exam schedule?")
                .answer("You can find your exam schedule in the \"Exams\" section. All released exam dates are displayed there.")
                .category("Exams")
                .build());
        faqRepository.save(faq2);

        Faq faq3 = Faq.builder()
                .sortOrder(3)
                .published(true)
                .build();
        faq3.getTranslations().add(FaqTranslation.builder()
                .faq(faq3)
                .languageCode("de")
                .question("Wo finde ich Dokumente und Formulare?")
                .answer("Alle freigegebenen Dokumente und Formulare finden Sie im Bereich \"Downloads\".")
                .category("Downloads")
                .build());
        faq3.getTranslations().add(FaqTranslation.builder()
                .faq(faq3)
                .languageCode("en")
                .question("Where can I find documents and forms?")
                .answer("All released documents and forms can be found in the \"Downloads\" section.")
                .category("Downloads")
                .build());
        faqRepository.save(faq3);

        Faq faq4 = Faq.builder()
                .sortOrder(4)
                .published(true)
                .build();
        faq4.getTranslations().add(FaqTranslation.builder()
                .faq(faq4)
                .languageCode("de")
                .question("Wie kann ich die Hochschule kontaktieren?")
                .answer("Die Kontaktinformationen der Hochschule und des Sekretariats finden Sie im Bereich \"Info\".")
                .category("Kontakt")
                .build());
        faq4.getTranslations().add(FaqTranslation.builder()
                .faq(faq4)
                .languageCode("en")
                .question("How can I contact the university?")
                .answer("You can find the university and secretariat contact details in the \"Info\" section.")
                .category("Contact")
                .build());
        faqRepository.save(faq4);

        Faq faq5 = Faq.builder()
                .sortOrder(5)
                .published(true)
                .build();
        faq5.getTranslations().add(FaqTranslation.builder()
                .faq(faq5)
                .languageCode("de")
                .question("Kann ich meine Profildaten selbst ändern?")
                .answer("Einige persönliche Daten können im Profilbereich geändert werden. Offizielle Stammdaten werden durch die Verwaltung gepflegt.")
                .category("Benutzerkonto")
                .build());
        faq5.getTranslations().add(FaqTranslation.builder()
                .faq(faq5)
                .languageCode("en")
                .question("Can I change my profile data myself?")
                .answer("Some personal data can be changed in the profile section. Official master data is maintained by the administration.")
                .category("User Account")
                .build());
        faqRepository.save(faq5);

        Faq faq6 = Faq.builder()
                .sortOrder(6)
                .published(false)
                .build();
        faq6.getTranslations().add(FaqTranslation.builder()
                .faq(faq6)
                .languageCode("de")
                .question("Wann erscheinen neue FAQ-Einträge?")
                .answer("Neue FAQ-Einträge werden nach redaktioneller Prüfung durch Administratoren veröffentlicht.")
                .category("FAQ")
                .build());
        faq6.getTranslations().add(FaqTranslation.builder()
                .faq(faq6)
                .languageCode("en")
                .question("When are new FAQ entries published?")
                .answer("New FAQ entries are published after editorial review by administrators.")
                .category("FAQ")
                .build());
        faqRepository.save(faq6);

        Faq faq7 = Faq.builder()
                .sortOrder(7)
                .published(false)
                .build();
        faq7.getTranslations().add(FaqTranslation.builder()
                .faq(faq7)
                .languageCode("de")
                .question("Werden künftig weitere Self-Service-Funktionen ergänzt?")
                .answer("Weitere Funktionen sind geplant, aber noch nicht für alle Nutzer freigeschaltet.")
                .category("System")
                .build());
        faq7.getTranslations().add(FaqTranslation.builder()
                .faq(faq7)
                .languageCode("en")
                .question("Will additional self-service features be added in the future?")
                .answer("Additional features are planned, but they have not yet been enabled for all users.")
                .category("System")
                .build());
        faqRepository.save(faq7);
    }
}
