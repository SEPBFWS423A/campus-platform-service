package de.campusplatform.campus_platform_service;

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
            ExamType kl = examTypeRepository.save(ExamType.builder().type("KL").nameDe("Klausur").nameEn("Written Exam").shortDe("KL").shortEn("WE").build());
            ExamType rf = examTypeRepository.save(ExamType.builder().type("RF").nameDe("Referat").nameEn("Presentation").shortDe("RF").shortEn("PRS").build());
            ExamType sa = examTypeRepository.save(ExamType.builder().type("SA").nameDe("Studienarbeit").nameEn("Term Paper").shortDe("SA").shortEn("TP").build());
            Set<ExamType> allTypes = Set.of(kl, rf, sa);

            // --- Courses & Specializations ---
            CourseOfStudy bwBachelor = courseOfStudyRepository.save(CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy bwMaster = courseOfStudyRepository.save(CourseOfStudy.builder().name("Betriebswirtschaft").degreeType(DegreeType.MASTER).build());
            createSpecializations(bwBachelor, List.of("Automotive Management", "Banking and Finance", "Business Management", "International Business", "International Business Management", "Logistikmanagement", "Steuerrecht"));
            createSpecializations(bwMaster, List.of("Automotive Management", "Business Management", "Controlling", "Einkauf und Logistikmanagement", "Human Resource Management", "Management und Führung im Finanzvertrieb", "Marketing und Vertrieb"));

            CourseOfStudy wiBachelor = courseOfStudyRepository.save(CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.BACHELOR).build());
            CourseOfStudy wiMaster = courseOfStudyRepository.save(CourseOfStudy.builder().name("Wirtschaftsinformatik").degreeType(DegreeType.MASTER).build());
            createSpecializations(wiBachelor, List.of("Business Process Management", "Cyber Security", "IT-Consulting", "Künstliche Intelligenz & Data Science", "Software Engineering"));
            createSpecializations(wiMaster, List.of("Angewandte Künstliche Intelligenz", "Cyber Security", "IT-Management", "Künstliche Intelligenz & Data Science", "Nachhaltige IT"));

            CourseOfStudy aiBachelor = courseOfStudyRepository.save(CourseOfStudy.builder().name("Angewandte Informatik").degreeType(DegreeType.BACHELOR).build());
            createSpecializations(aiBachelor, List.of("Smart Systems", "Software-Entwicklung und -Management", "Virtual Worlds"));

            // --- Mockup Lecturers ---
            AppUser p = createLecturer("Ms.", "Prof. Dr.", "Patrice", "Admin", "p.admin@campusplatform.de");
            AppUser a = createLecturer("Mr.", "Dr.", "Adam", "Smart", "a.smart@campusplatform.de");
            AppUser m = createLecturer("Mr.", null, "Marc", "Code", "m.code@campusplatform.de");
            List<AppUser> lecturers = List.of(p, a, m);

            // --- Mockup Modules (Wirtschaftsinformatik) ---
            Specialization seSpec = specializationRepository.findAll().stream().filter(s -> s.getName().equals("Software Engineering") && s.getCourseOfStudy().getId().equals(wiBachelor.getId())).findFirst().orElseThrow();

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
                Specialization csSpec = specializationRepository.findAll().stream().filter(s -> s.getName().equals("Cyber Security") && s.getCourseOfStudy().getId().equals(wiBachelor.getId())).findFirst().orElseThrow();
                Specialization iteSpec = specializationRepository.findAll().stream().filter(s -> s.getName().equals("IT-Consulting") && s.getCourseOfStudy().getId().equals(wiBachelor.getId())).findFirst().orElseThrow();

                // Group Naming Logic based on Frontend: {City}{Uni}{Course}{Spec}{Quartal}{YearYY}{Degree}
                // Bergisch Gladbach (B), FDSE (F), Wirtschaftsinformatik (W), SE/CS/ITC (S/C/I), Q4 (4), 2024 (24), Bachelor (A)
                StudyGroup g1 = studyGroupRepository.save(StudyGroup.builder().name("BFWS424A").specialization(seSpec).build());
                StudyGroup g2 = studyGroupRepository.save(StudyGroup.builder().name("BFWC424A").specialization(csSpec).build());
                StudyGroup g3 = studyGroupRepository.save(StudyGroup.builder().name("BFWI424A").specialization(iteSpec).build());

                createStudentsForGroup(g1, 20, 1000);
                createStudentsForGroup(g2, 10, 2000);
                createStudentsForGroup(g3, 10, 3000);
            }

            createFaqs();

            System.out.println("=================================================================");
            System.out.println("   ✓ CAMPUS PLATFORM DATA INITIALIZED");
            System.out.println("   Institution:     " + glCampus.getUniversityName() + " (" + glCampus.getCity() + ")");
            System.out.println("   Website:         https://gl.campusplatform.de");
            System.out.println("   Exam Types:      " + examTypeRepository.count());
            System.out.println("   Courses:         " + courseOfStudyRepository.count());
            System.out.println("   Mock Students:   40");
            System.out.println("   Study Groups:    " + studyGroupRepository.count() + " (Aligned with Frontend Logic)");
            System.out.println("   FAQs:            " + faqRepository.count());
            System.out.println("=================================================================");
        }
    }

    private AppUser createLecturer(String salutation, String title, String first, String last, String email) {
        return userRepository.save(AppUser.builder()
            .salutation(salutation)
            .title(title)
            .firstName(first)
            .lastName(last)
            .email(email)
            .password("password")
            .role(Role.LECTURER)
            .enabled(true)
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
            String salutation = (i % 2 == 0) ? "Mr." : "Ms.";
            
            AppUser user = AppUser.builder()
                .salutation(salutation)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("password")
                .role(Role.STUDENT)
                .enabled(true)
                .build();
            user = userRepository.save(user);

            StudentProfile profile = StudentProfile.builder().appUser(user).studentNumber("S-" + (studentNumStart + i)).startYear(2024).specialization(group.getSpecialization()).build();
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

        faqRepository.save(Faq.builder()
                .question("Wie kann ich mein Passwort zurücksetzen?")
                .answer("Nutzen Sie auf der Login-Seite die Funktion \"Passwort vergessen\". Anschließend erhalten Sie per E-Mail einen Link zum Zurücksetzen Ihres Passworts.")
                .category("Login & Sicherheit")
                .sortOrder(1)
                .published(true)
                .build());

        faqRepository.save(Faq.builder()
                .question("Wo finde ich meinen Prüfungsplan?")
                .answer("Ihren Prüfungsplan finden Sie im Bereich \"Prüfungen\". Dort werden alle freigegebenen Klausur- und Prüfungstermine angezeigt.")
                .category("Prüfungen")
                .sortOrder(2)
                .published(true)
                .build());

        faqRepository.save(Faq.builder()
                .question("Wo finde ich Dokumente und Formulare?")
                .answer("Alle freigegebenen Dokumente und Formulare finden Sie im Bereich \"Downloads\".")
                .category("Downloads")
                .sortOrder(3)
                .published(true)
                .build());

        faqRepository.save(Faq.builder()
                .question("Wie kann ich die Hochschule kontaktieren?")
                .answer("Die Kontaktinformationen der Hochschule und des Sekretariats finden Sie im Bereich \"Info\".")
                .category("Kontakt")
                .sortOrder(4)
                .published(true)
                .build());

        faqRepository.save(Faq.builder()
                .question("Kann ich meine Profildaten selbst ändern?")
                .answer("Einige persönliche Daten können im Profilbereich geändert werden. Offizielle Stammdaten werden durch die Verwaltung gepflegt.")
                .category("Benutzerkonto")
                .sortOrder(5)
                .published(true)
                .build());

        faqRepository.save(Faq.builder()
                .question("Wann erscheinen neue FAQ-Einträge?")
                .answer("Neue FAQ-Einträge werden nach redaktioneller Prüfung durch Administratoren veröffentlicht.")
                .category("FAQ")
                .sortOrder(6)
                .published(false)
                .build());

        faqRepository.save(Faq.builder()
                .question("Werden künftig weitere Self-Service-Funktionen ergänzt?")
                .answer("Weitere Funktionen sind geplant, aber noch nicht für alle Nutzer freigeschaltet.")
                .category("System")
                .sortOrder(7)
                .published(false)
                .build());
    }
}
