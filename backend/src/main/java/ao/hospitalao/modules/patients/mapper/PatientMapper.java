package ao.hospitalao.modules.patients.mapper;

import ao.hospitalao.modules.patients.dto.PatientResponse;
import ao.hospitalao.modules.patients.entity.Patient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class PatientMapper {

    public PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
            .id(p.getId())
            .fullName(p.getFullName())
            .birthDate(p.getBirthDate())
            .age(calculateAge(p.getBirthDate()))
            .gender(p.getGender())
            .nationalId(p.getNationalId())
            .healthCardNumber(p.getHealthCardNumber())
            .phone(p.getPhone())
            .email(p.getEmail())
            .address(p.getAddress())
            .province(p.getProvince())
            .municipality(p.getMunicipality())
            .emergencyContactName(p.getEmergencyContactName())
            .emergencyContactPhone(p.getEmergencyContactPhone())
            .emergencyContactRelationship(p.getEmergencyContactRelationship())
            .bloodType(p.getBloodType())
            .allergies(p.getAllergies())
            .chronicConditions(p.getChronicConditions())
            .notes(p.getNotes())
            .active(p.isActive())
            .createdAt(p.getCreatedAt())
            .build();
    }

    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}