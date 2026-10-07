package ao.hospitalao.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Extensão do UserDetails do Spring Security que expõe o UUID
 * do utilizador autenticado — necessário para o NotificationController
 * e qualquer outro componente que precise do UUID além do username/email.
 */
public class JwtUserDetails extends User {

    private final UUID userId;
    private final UUID hospitalId;

    public JwtUserDetails(
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        UUID userId,
        UUID hospitalId
    ) {
        super(username, password, authorities);
        this.userId     = userId;
        this.hospitalId = hospitalId;
    }

    public UUID getUserId()     { return userId;     }
    public UUID getHospitalId() { return hospitalId; }
}