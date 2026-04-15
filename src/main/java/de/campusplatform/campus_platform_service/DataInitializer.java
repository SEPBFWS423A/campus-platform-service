package de.campusplatform.campus_platform_service;

import de.campusplatform.campus_platform_service.enums.*;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.model.Module;
import de.campusplatform.campus_platform_service.repository.*;
import de.campusplatform.campus_platform_service.service.StudentSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Value("${app.initial.student.email:}")
    private String demoStudent1Email;

    @Value("${app.initial.student2.email:}")
    private String demoStudent2Email;

    @Value("${app.initial.lecturer.email:}")
    private String initialLecturerEmail;

    private static final byte[] DEMO_PDF_BYTES =
            "%PDF-1.4\nDemo PDF\n".getBytes(StandardCharsets.UTF_8);
    private static final String DEMO_PDF_BASE64 =
            java.util.Base64.getEncoder().encodeToString(DEMO_PDF_BYTES);

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
    private final SubmissionDocumentRepository submissionDocumentRepository;
    private final StudentSubmissionService studentSubmissionService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JobPostingRepository jobPostingRepository;
    private final StudyProgramRepository studyProgramRepository;

    @Override
    @Transactional
    public void run(String... args) {
        examTypeRepository.findAll().forEach(et -> {
            if (("RF".equals(et.getType()) || "SA".equals(et.getType())) && !et.isSubmission()) {
                et.setSubmission(true);
                examTypeRepository.save(et);
            }
        });

        if (examTypeRepository.count() == 0) {
            InstitutionInfo glCampus = institutionRepository.save(InstitutionInfo.builder()
                    .universityName("CampusPlatform")
                    .city("Bergisch Gladbach")
                    .sekretariatEmail("office@gl.campusplatform.de")
                    .sekretariatPhone("+49 2202 123456")
                    .sekretariatOpeningTimes("Mon-Fri: 09:00 - 15:00")
                    .websiteEmail("admin@campusplatform.de")
                    .bibliothekUrl("https://bib.gl.campusplatform.de")
                    .mensaUrl("https://mensa.gl.campusplatform.de")
                    .impressum("CampusPlatform Bergisch Gladbach - Campus Platform Management")
                    .build());

            ExamType kl = examTypeRepository.save(ExamType.builder()
                    .type("KL")
                    .submission(false)
                    .nameDe("Klausur")
                    .nameEn("Written Exam")
                    .shortDe("KL")
                    .shortEn("WE")
                    .build());

            ExamType rf = examTypeRepository.save(ExamType.builder()
                    .type("RF")
                    .submission(true)
                    .nameDe("Referat")
                    .nameEn("Presentation")
                    .shortDe("RF")
                    .shortEn("PRS")
                    .build());

            ExamType sa = examTypeRepository.save(ExamType.builder()
                    .type("SA")
                    .submission(true)
                    .nameDe("Studienarbeit")
                    .nameEn("Term Paper")
                    .shortDe("SA")
                    .shortEn("TP")
                    .build());

            Set<ExamType> allTypes = Set.of(kl, rf, sa);

            CourseOfStudy bwBachelor = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy bwMaster = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.MASTER).build());

            createSpecializations(bwBachelor, List.of(
                    "Automotive Management",
                    "Banking and Finance",
                    "Business Management",
                    "International Business",
                    "International Business Management",
                    "Logistikmanagement",
                    "Steuerrecht"
            ));

            createSpecializations(bwMaster, List.of(
                    "Automotive Management",
                    "Business Management",
                    "Controlling",
                    "Einkauf und Logistikmanagement",
                    "Human Resource Management",
                    "Management und Führung im Finanzvertrieb",
                    "Marketing und Vertrieb"
            ));

            CourseOfStudy wiBachelor = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy wiMaster = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.MASTER).build());

            createSpecializations(wiBachelor, List.of(
                    "Business Process Management",
                    "Cyber Security",
                    "IT-Consulting",
                    "Künstliche Intelligenz & Data Science",
                    "Software Engineering"
            ));

            createSpecializations(wiMaster, List.of(
                    "Angewandte Künstliche Intelligenz",
                    "Cyber Security",
                    "IT-Management",
                    "Künstliche Intelligenz & Data Science",
                    "Nachhaltige IT"
            ));

            CourseOfStudy aiBachelor = courseOfStudyRepository.save(
                    CourseOfStudy.builder().name("Angewandte Informatik").degreeType(DegreeType.BACHELOR).build());

            createSpecializations(aiBachelor, List.of(
                    "Smart Systems",
                    "Software-Entwicklung und -Management",
                    "Virtual Worlds"
            ));

            AppUser instructor = userRepository.findByEmail(initialLecturerEmail)
                    .orElseThrow(() -> new IllegalStateException("Initial lecturer not found!"));

            AppUser p = createLecturer(Salutation.MS, AcademicTitle.PROF_DR, "Patrice", "Admin",
                    "p.admin@campusplatform.de");
            AppUser a = createLecturer(Salutation.MR, AcademicTitle.DR, "Adam", "Smart",
                    "a.smart@campusplatform.de");
            AppUser m = createLecturer(Salutation.MR, null, "Marc", "Code",
                    "m.code@campusplatform.de");

            List<AppUser> lecturers = List.of(instructor, p, a, m);

            Specialization seSpec = specializationRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Software Engineering")
                            && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                    .findFirst()
                    .orElseThrow();

            Specialization csSpec = specializationRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Cyber Security")
                            && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                    .findFirst()
                    .orElseThrow();

            Specialization iteSpec = specializationRepository.findAll().stream()
                    .filter(s -> s.getName().equals("IT-Consulting")
                            && s.getCourseOfStudy().getId().equals(wiBachelor.getId()))
                    .findFirst()
                    .orElseThrow();

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

            Module softArch = moduleRepository.save(Module.builder()
                    .name("Software Modelling and Architecture")
                    .semester(6)
                    .requiredTotalHours(40)
                    .courseOfStudy(wiBachelor)
                    .specialization(seSpec)
                    .possibleExamTypes(allTypes)
                    .preferredExamType(kl)
                    .build());

            Module projMgmt = moduleRepository.save(Module.builder()
                    .name("Projektmanagement")
                    .semester(6)
                    .requiredTotalHours(40)
                    .courseOfStudy(wiBachelor)
                    .specialization(null)
                    .possibleExamTypes(allTypes)
                    .preferredExamType(sa)
                    .build());

            assignLecturersToModule(prog1, lecturers);
            assignLecturersToModule(prog2, lecturers);
            assignLecturersToModule(seProject, lecturers);
            assignLecturersToModule(softArch, lecturers);
            assignLecturersToModule(projMgmt, lecturers);

            createDemoStudyPrograms();

            if (studyGroupRepository.count() == 0) {
                StudyGroup g1 = studyGroupRepository.save(
                        StudyGroup.builder().name("BFWS424A").specialization(seSpec).startYear(2024).startQuartal(4).build());
                StudyGroup g2 = studyGroupRepository.save(
                        StudyGroup.builder().name("BFWC424A").specialization(csSpec).startYear(2024).startQuartal(4).build());
                StudyGroup g3 = studyGroupRepository.save(
                        StudyGroup.builder().name("BFWI424A").specialization(iteSpec).startYear(2024).startQuartal(4).build());
                StudyGroup g4 = studyGroupRepository.save(
                        StudyGroup.builder().name("BFWS423A").specialization(seSpec).startYear(2023).startQuartal(4).build());

                createStudentsForGroup(g1, 20, 1000);
                createStudentsForGroup(g2, 10, 2000);
                createStudentsForGroup(g3, 10, 3000);
                createStudentsForGroup(g4, 3, 4000);

                userRepository.findByEmail(demoStudent1Email).ifPresent(s1 ->
                        createMembership(s1, g4)
                );
                userRepository.findByEmail(demoStudent2Email).ifPresent(s2 ->
                        createMembership(s2, g4)
                );
            }

            createFaqs();
            createDemoJobPostings();

            if (courseSeriesRepository.count() == 0) {
                List<StudyGroup> allGroups = studyGroupRepository.findAll();
                StudyGroup mockG1 = allGroups.stream().filter(g -> g.getName().equals("BFWS424A")).findFirst().orElseThrow();
                StudyGroup mockG2 = allGroups.stream().filter(g -> g.getName().equals("BFWC424A")).findFirst().orElseThrow();
                StudyGroup mockG3 = allGroups.stream().filter(g -> g.getName().equals("BFWI424A")).findFirst().orElseThrow();
                StudyGroup mockG4 = allGroups.stream().filter(g -> g.getName().equals("BFWS423A")).findFirst().orElseThrow();

                List<Room> rooms = roomRepository.findAll();
                Room roomA = rooms.isEmpty() ? null : rooms.get(0);
                Room roomB = rooms.size() > 1 ? rooms.get(1) : roomA;

                CourseSeries seriesG4_SE = courseSeriesRepository.save(CourseSeries.builder()
                        .module(seProject).assignedLecturer(instructor).status(CourseStatus.ACTIVE)
                        .selectedExamType(rf).studyGroups(Set.of(mockG4))
                        .submissionStartDate(LocalDateTime.now().minusDays(10)).submissionDeadline(LocalDateTime.now().plusDays(20))
                        .build());

                CourseSeries seriesG4_SoftArch = courseSeriesRepository.save(CourseSeries.builder()
                        .module(softArch).assignedLecturer(instructor).status(CourseStatus.ACTIVE)
                        .selectedExamType(kl).studyGroups(Set.of(mockG4))
                        .submissionStartDate(LocalDateTime.now().minusDays(5)).submissionDeadline(LocalDateTime.now().plusDays(30))
                        .build());

                CourseSeries seriesG4_ProjMgmt = courseSeriesRepository.save(CourseSeries.builder()
                        .module(projMgmt).assignedLecturer(instructor).status(CourseStatus.GRADING)
                        .examStatus(ExamStatus.GRADING).selectedExamType(sa).studyGroups(Set.of(mockG4))
                        .submissionStartDate(LocalDateTime.now().minusDays(60)).submissionDeadline(LocalDateTime.now().minusDays(10))
                        .build());

                CourseSeries seriesG1_ProjMgmt = courseSeriesRepository.save(CourseSeries.builder()
                        .module(projMgmt).assignedLecturer(instructor).status(CourseStatus.ACTIVE)
                        .selectedExamType(sa).studyGroups(Set.of(mockG1))
                        .submissionStartDate(LocalDateTime.now().minusDays(20)).submissionDeadline(LocalDateTime.now().plusDays(15))
                        .build());

                CourseSeries seriesG2_Prog2 = courseSeriesRepository.save(CourseSeries.builder()
                        .module(prog2).assignedLecturer(instructor).status(CourseStatus.PLANNED)
                        .selectedExamType(kl).studyGroups(Set.of(mockG4))
                        .submissionStartDate(LocalDateTime.now().plusWeeks(8)).submissionDeadline(LocalDateTime.now().plusWeeks(12))
                        .build());

                LocalDateTime lastG4_SE = createSchedule(seriesG4_SE, roomA, 1, 9, 45, 12);
                LocalDateTime lastG4_SoftArch = createSchedule(seriesG4_SoftArch, roomB, 2, 13, 45, 12);
                LocalDateTime lastG4_ProjMgmt = createSchedule(seriesG4_ProjMgmt, roomA, 1, 13, 45, 12);
                LocalDateTime lastG1_ProjMgmt = createSchedule(seriesG1_ProjMgmt, roomB, 3, 9, 45, 12);

                attemptAddExam(seriesG4_SE, roomA, lastG4_SE);
                attemptAddExam(seriesG4_SoftArch, roomB, lastG4_SoftArch);
                attemptAddExam(seriesG1_ProjMgmt, roomB, lastG1_ProjMgmt);
                attemptAddExam(seriesG2_Prog2, roomA, LocalDateTime.now().plusWeeks(12));

                List.of(seriesG4_SE, seriesG4_SoftArch, seriesG4_ProjMgmt, seriesG1_ProjMgmt).forEach(s -> {
                    studentSubmissionService.initializeSubmissionsForCourseSeries(s.getId());
                });

                AppUser student1 = userRepository.findByEmail(demoStudent1Email).orElse(null);
                AppUser student2 = userRepository.findByEmail(demoStudent2Email).orElse(null);

                if (student1 != null) {
                    createSubmittedSubmissionWithDocument(student1, seriesG4_SE, "se-entwurf.pdf", LocalDateTime.now().minusDays(2));
                    createGradedSubmissionWithDocument(student1, seriesG4_ProjMgmt, "management-plan.pdf",
                            LocalDateTime.now().minusDays(15), 1.3, 92.0, "Hervorragende Analyse!");
                    createPendingSubmission(student1, seriesG4_SoftArch);
                }

                if (student2 != null) {
                    createPendingSubmissionWithDocument(student2, seriesG4_SE, "draft.pdf", LocalDateTime.now().minusDays(1));
                    createGradedSubmissionWithDocument(student2, seriesG4_ProjMgmt, "mgmt.pdf",
                            LocalDateTime.now().minusDays(12), 2.7, 75.0, "Gute Ansätze, aber lückenhaft.");
                    createPendingSubmission(student2, seriesG4_SoftArch);
                }

                System.out.println("=================================================================");
                System.out.println("   ✓ CAMPUS PLATFORM DATA INITIALIZED");
                System.out.println("   Institution:     " + glCampus.getUniversityName() + " (" + glCampus.getCity() + ")");
                System.out.println("   Website:         https://gl.campusplatform.de");
                System.out.println("   Exam Types:      " + examTypeRepository.count());
                System.out.println("   Courses:         " + courseOfStudyRepository.count());
                System.out.println("   Course Series:   " + courseSeriesRepository.count());
                System.out.println("   Mock Students:   40");
                System.out.println("   Study Groups:    " + studyGroupRepository.count());
                System.out.println("   FAQs:            " + faqRepository.count());
                System.out.println("   Study Programs:  " + studyProgramRepository.count());
                System.out.println("=================================================================");
            }
        }
    }

    private void createDemoStudyPrograms() {
        if (studyProgramRepository.count() > 0) return;

        studyProgramRepository.saveAll(List.of(
                createStudyProgram("Wirtschaftsinformatik B.Sc.", "Kombination aus BWL und IT"),
                createStudyProgram("Informatik B.Sc.", "Grundstudium der Informatik"),
                createStudyProgram("Data Science M.Sc.", "Masterstudiengang für Datenwissenschaft"),
                createStudyProgram("Medieninformatik B.Sc.", "Informatik mit Medienschwerpunkt"),
                createStudyProgram("Betriebswirtschaft B.Sc.", "Klassisches BWL-Studium")
        ));
    }

    private StudyProgram createStudyProgram(String name, String description) {
        StudyProgram p = new StudyProgram();
        p.setName(name);
        p.setDescription(description);
        p.setActive(true);
        return p;
    }

    private LocalDateTime createSchedule(CourseSeries series, Room room, int dayOfWeek, int hour, int minute, int weeks) {
        LocalDateTime base = LocalDateTime.of(2026, 4, 1, hour, minute, 0, 0);
        while (base.getDayOfWeek().getValue() != dayOfWeek) {
            base = base.plusDays(1);
        }

        LocalDateTime lastTime = null;
        for (int i = 0; i < weeks; i++) {
            LocalDateTime startTime = base.plusWeeks(i);
            eventRepository.save(Event.builder()
                    .courseSeries(series)
                    .rooms(room != null ? Set.of(room) : Set.of())
                    .name(series.getModule().getName())
                    .eventType(EventType.LEHRVERANSTALTUNG)
                    .startTime(startTime)
                    .durationMinutes(195)
                    .build());
            lastTime = startTime;
        }
        return lastTime;
    }

    private void attemptAddExam(CourseSeries series, Room room, LocalDateTime lastLectureTime) {
        if (series.getSelectedExamType() == null || lastLectureTime == null || series.getStatus() == CourseStatus.PLANNED) return;
        String type = series.getSelectedExamType().getType();
        if ("KL".equals(type) || "RF".equals(type)) {
            addExamEvent(series, room, lastLectureTime.plusWeeks(1));
        }
    }

    private void addExamEvent(CourseSeries series, Room room, LocalDateTime dateTime) {
        eventRepository.save(Event.builder()
                .courseSeries(series)
                .rooms(room != null ? Set.of(room) : Set.of())
                .name("Prüfung: " + series.getModule().getName())
                .eventType(EventType.KLAUSUR)
                .startTime(dateTime.withSecond(0).withNano(0))
                .durationMinutes(90)
                .build());
    }

    private AppUser createLecturer(Salutation salutation, AcademicTitle title, String first, String last, String email) {
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
            quals.add(ModuleLecturer.builder()
                    .module(module)
                    .lecturer(l)
                    .grantedAt(LocalDateTime.now())
                    .build());
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

            membershipRepository.save(StudyGroupMembership.builder()
                    .student(profile)
                    .studyGroup(group)
                    .build());
        }
    }

    private void createMembership(AppUser user, StudyGroup group) {
        StudentProfile profile = profileRepository.findByAppUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile p = StudentProfile.builder()
                            .appUser(user)
                            .studentNumber("S-" + user.getId())
                            .startYear(group.getStartYear())
                            .specialization(group.getSpecialization())
                            .build();
                    return profileRepository.save(p);
                });

        if (membershipRepository.findByStudentUserIdAndStudyGroupId(user.getId(), group.getId()).isEmpty()) {
            membershipRepository.save(StudyGroupMembership.builder()
                    .student(profile)
                    .studyGroup(group)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void createSpecializations(CourseOfStudy course, List<String> names) {
        for (String name : names) {
            specializationRepository.save(Specialization.builder()
                    .name(name)
                    .courseOfStudy(course)
                    .build());
        }
    }

    private void createPendingSubmission(AppUser student, CourseSeries courseSeries) {
        StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(courseSeries.getId(), student.getId())
                .orElse(StudentCourseSubmission.builder()
                        .courseSeries(courseSeries)
                        .student(student)
                        .build());

        submission.setStatus(SubmissionStatus.PENDING);
        submissionRepository.save(submission);
    }

    private void createPendingSubmissionWithDocument(AppUser student,
                                                     CourseSeries courseSeries,
                                                     String fileName,
                                                     LocalDateTime uploadedAt) {
        StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(courseSeries.getId(), student.getId())
                .orElse(StudentCourseSubmission.builder()
                        .courseSeries(courseSeries)
                        .student(student)
                        .build());

        submission.setStatus(SubmissionStatus.PENDING);
        StudentCourseSubmission saved = submissionRepository.save(submission);

        submissionDocumentRepository.save(SubmissionDocument.builder()
                .submission(saved)
                .fileName(fileName)
                .mimeType("application/pdf")
                .fileSize((long) DEMO_PDF_BYTES.length)
                .contentBase64(DEMO_PDF_BASE64)
                .uploadedAt(uploadedAt)
                .build());
    }

    private void createSubmittedSubmissionWithDocument(AppUser student,
                                                       CourseSeries courseSeries,
                                                       String fileName,
                                                       LocalDateTime submissionDate) {
        StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(courseSeries.getId(), student.getId())
                .orElse(StudentCourseSubmission.builder()
                        .courseSeries(courseSeries)
                        .student(student)
                        .build());

        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmissionDate(submissionDate);
        StudentCourseSubmission saved = submissionRepository.save(submission);

        submissionDocumentRepository.save(SubmissionDocument.builder()
                .submission(saved)
                .fileName(fileName)
                .mimeType("application/pdf")
                .fileSize((long) DEMO_PDF_BYTES.length)
                .contentBase64(DEMO_PDF_BASE64)
                .uploadedAt(submissionDate)
                .build());
    }

    private void createGradedSubmissionWithDocument(AppUser student,
                                                    CourseSeries courseSeries,
                                                    String fileName,
                                                    LocalDateTime submissionDate,
                                                    Double grade,
                                                    Double points,
                                                    String feedback) {
        StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(courseSeries.getId(), student.getId())
                .orElse(StudentCourseSubmission.builder()
                        .courseSeries(courseSeries)
                        .student(student)
                        .build());

        submission.setStatus(SubmissionStatus.GRADED);
        submission.setSubmissionDate(submissionDate);
        submission.setGrade(grade);
        submission.setPoints(points);
        submission.setFeedback(feedback);
        StudentCourseSubmission saved = submissionRepository.save(submission);

        submissionDocumentRepository.save(SubmissionDocument.builder()
                .submission(saved)
                .fileName(fileName)
                .mimeType("application/pdf")
                .fileSize((long) DEMO_PDF_BYTES.length)
                .contentBase64(DEMO_PDF_BASE64)
                .uploadedAt(submissionDate)
                .build());
    }

    private void createFaqs() {
        if (faqRepository.count() > 0) return;

        Faq faq1 = Faq.builder().sortOrder(1).published(true).build();
        faq1.getTranslations().add(FaqTranslation.builder().faq(faq1).languageCode("de")
                .question("Wie kann ich mein Passwort zurücksetzen?")
                .answer("Nutzen Sie auf der Login-Seite die Funktion \"Passwort vergessen\". Anschließend erhalten Sie per E-Mail einen Link zum Zurücksetzen Ihres Passworts.")
                .category("Login & Sicherheit").build());
        faq1.getTranslations().add(FaqTranslation.builder().faq(faq1).languageCode("en")
                .question("How can I reset my password?")
                .answer("Use the \"Forgot password\" function on the login page. You will then receive an email with a link to reset your password.")
                .category("Login & Security").build());
        faqRepository.save(faq1);

        Faq faq2 = Faq.builder().sortOrder(2).published(true).build();
        faq2.getTranslations().add(FaqTranslation.builder().faq(faq2).languageCode("de")
                .question("Wo finde ich meinen Prüfungsplan?")
                .answer("Ihren Prüfungsplan finden Sie im Bereich \"Prüfungen\". Dort werden alle freigegebenen Klausur- und Prüfungstermine angezeigt.")
                .category("Prüfungen").build());
        faq2.getTranslations().add(FaqTranslation.builder().faq(faq2).languageCode("en")
                .question("Where can I find my exam schedule?")
                .answer("You can find your exam schedule in the \"Exams\" section. All released exam dates are displayed there.")
                .category("Exams").build());
        faqRepository.save(faq2);

        Faq faq3 = Faq.builder().sortOrder(3).published(true).build();
        faq3.getTranslations().add(FaqTranslation.builder().faq(faq3).languageCode("de")
                .question("Wo finde ich Dokumente und Formulare?")
                .answer("Alle freigegebenen Dokumente und Formulare finden Sie im Bereich \"Downloads\".")
                .category("Downloads").build());
        faq3.getTranslations().add(FaqTranslation.builder().faq(faq3).languageCode("en")
                .question("Where can I find documents and forms?")
                .answer("All released documents and forms can be found in the \"Downloads\" section.")
                .category("Downloads").build());
        faqRepository.save(faq3);

        Faq faq4 = Faq.builder().sortOrder(4).published(true).build();
        faq4.getTranslations().add(FaqTranslation.builder().faq(faq4).languageCode("de")
                .question("Wie kann ich die Hochschule kontaktieren?")
                .answer("Die Kontaktinformationen der Hochschule und des Sekretariats finden Sie im Bereich \"Info\".")
                .category("Kontakt").build());
        faq4.getTranslations().add(FaqTranslation.builder().faq(faq4).languageCode("en")
                .question("How can I contact the university?")
                .answer("You can find the university and secretariat contact details in the \"Info\" section.")
                .category("Contact").build());
        faqRepository.save(faq4);

        Faq faq5 = Faq.builder().sortOrder(5).published(true).build();
        faq5.getTranslations().add(FaqTranslation.builder().faq(faq5).languageCode("de")
                .question("Kann ich meine Profildaten selbst ändern?")
                .answer("Einige persönliche Daten können im Profilbereich geändert werden. Offizielle Stammdaten werden durch die Verwaltung gepflegt.")
                .category("Benutzerkonto").build());
        faq5.getTranslations().add(FaqTranslation.builder().faq(faq5).languageCode("en")
                .question("Can I change my profile data myself?")
                .answer("Some personal data can be changed in the profile section. Official master data is maintained by the administration.")
                .category("User Account").build());
        faqRepository.save(faq5);
    }

    private void createDemoJobPostings() {
        if (jobPostingRepository.count() > 0) return;

        jobPostingRepository.save(JobPosting.builder()
                .title("Dozent für Wirtschaftsinformatik (m/w/d)")
                .department("Informatik")
                .type(de.campusplatform.campus_platform_service.enums.JobType.LEHRAUFTRAG)
                .status(de.campusplatform.campus_platform_service.enums.JobStatus.AKTIV)
                .description("Wir suchen einen erfahrenen Dozenten zur Verstärkung unseres Lehrteams " +
                        "im Bereich Wirtschaftsinformatik. Schwerpunkte: Datenbanken, ERP-Systeme, " +
                        "und agile Softwareentwicklung.")
                .requirements("Abgeschlossenes Studium der Informatik oder Wirtschaftsinformatik (Master oder Promotion), " +
                        "Lehrerfahrung im Hochschulbereich von Vorteil, Praxiserfahrung in der IT-Branche erwünscht.")
                .deadline(LocalDate.of(2026, 5, 31))
                .autoPublish(true)
                .createdBy("admin@campusplatform.de")
                .build());

        jobPostingRepository.save(JobPosting.builder()
                .title("Studentische Hilfskraft Bibliothek (m/w/d)")
                .department("Bibliothek & Informationsdienste")
                .type(de.campusplatform.campus_platform_service.enums.JobType.STUDENTISCHE_HILFSKRAFT)
                .status(de.campusplatform.campus_platform_service.enums.JobStatus.AKTIV)
                .description("Unterstützung des Bibliothekspersonals bei der Katalogisierung, " +
                        "Benutzerbetreuung sowie dem Aufbau digitaler Ressourcen.")
                .requirements("Immatrikuliert an der Hochschule, sorgfältige und selbstständige Arbeitsweise, " +
                        "gute Deutschkenntnisse in Wort und Schrift.")
                .deadline(LocalDate.of(2026, 5, 15))
                .autoPublish(true)
                .createdBy("admin@campusplatform.de")
                .build());
    }
}