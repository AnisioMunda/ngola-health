package ao.hospitalao.modules.users.mapper;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.users.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .especiality(user.getEspeciality())
            .professionalCard(user.getProfessionalCard())
            .registerStatus(user.getRegisterStatus())
            .mustChangePassword(user.isMustChangePassword())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .roles(user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet()))
            .hospitalId(user.getHospital() != null ? user.getHospital().getId() : null)
            .build();
    }
}