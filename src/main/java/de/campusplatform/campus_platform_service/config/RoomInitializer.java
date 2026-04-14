package de.campusplatform.campus_platform_service.config;

import de.campusplatform.campus_platform_service.model.OperationalStatus;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.model.RoomType;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class RoomInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    public RoomInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (roomRepository.count() == 0) {
            roomRepository.saveAll(List.of(
                Room.builder().name("B-D01").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D02").seats(30).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D03").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D04").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D05").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D06").seats(30).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D07").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D08").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D09").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build(),
                Room.builder().name("B-D10").seats(40).examSeats(25).building("B").roomType(RoomType.HOERSAAL).operationalStatus(OperationalStatus.AKTIV).build()
            ));
            System.out.println("=================================================================");
            System.out.println("INITIAL ROOMS CREATED (HOERSAAL)");
            System.out.println("=================================================================");
        } else {
            // Update existing rooms that might have null values after migration
            List<Room> allRooms = roomRepository.findAll();
            boolean updated = false;
            for (Room room : allRooms) {
                if (room.getBuilding() == null) {
                    room.setBuilding("B");
                    updated = true;
                }
                if (room.getRoomType() == null) {
                    room.setRoomType(RoomType.HOERSAAL);
                    updated = true;
                }
                if (room.getOperationalStatus() == null) {
                    room.setOperationalStatus(OperationalStatus.AKTIV);
                    updated = true;
                }
            }
            if (updated) {
                roomRepository.saveAll(allRooms);
                System.out.println("=================================================================");
                System.out.println("EXISTING ROOMS UPDATED TO HOERSAAL");
                System.out.println("=================================================================");
            }
        }
    }
}
