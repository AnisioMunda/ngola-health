package ao.hospitalao.modules.financial.agt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da integração com a API AGT Angola.
 * Configurar em application.yml:
 *
 * agt:
 *   api-url: https://sandbox.portaldoparceiro.minfin.gov.ao
 *   client-id: SEU_CLIENT_ID
 *   client-secret: SEU_CLIENT_SECRET
 *   nif: 5000413178            # NIF de teste
 *   software-id: HospitalAO
 *   software-version: 1.0.0
 *   software-validation-number: SEU_NUMERO_VALIDACAO
 *   private-key-path: /etc/hospitalao/agt-private.pem
 *   sandbox: true
 */
@Data
@Component
@ConfigurationProperties(prefix = "agt")
public class AgtProperties {

    /** URL base da API AGT (sandbox ou produção) */
    private String apiUrl = "https://sandbox.portaldoparceiro.minfin.gov.ao";

    /** Client ID para autenticação OAuth2 AGT */
    private String clientId;

    /** Client Secret para autenticação OAuth2 AGT */
    private String clientSecret;

    /** NIF do hospital/contribuinte */
    private String nif;

    /** Nome do software de facturação */
    private String softwareId = "HospitalAO";

    /** Versão do software */
    private String softwareVersion = "1.0.0";

    /** Número de validação do software atribuído pela AGT */
    private String softwareValidationNumber;

    /** Caminho para a chave privada RSA PEM */
    private String privateKeyPath;

    /** Conteúdo da chave privada RSA (alternativa ao path) */
    private String privateKeyContent;

    /** true = sandbox/teste, false = produção */
    private boolean sandbox = true;
}