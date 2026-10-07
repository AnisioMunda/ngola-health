package ao.hospitalao.modules.laboratory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import ao.hospitalao.modules.laboratory.dto.LabDtos.CreateLabRequestRequest;
import ao.hospitalao.modules.laboratory.dto.LabDtos.CreateLabTestRequest;
import ao.hospitalao.modules.laboratory.dto.LabDtos.SubmitResultRequest;
import jakarta.validation.Validation;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LabDtosValidationTest {

  @Test
  void createLabTestRequiresCodeNameAndCategory() {
    var request = new CreateLabTestRequest();

    Set<String> invalidFields = validateFields(request);

    assertThat(invalidFields).contains("code", "name", "category");
  }

  @Test
  void createLabRequestRequiresPatientAndAtLeastOneTest() {
    var request = new CreateLabRequestRequest();

    Set<String> invalidFields = validateFields(request);

    assertThat(invalidFields).contains("patientId", "labTestIds");
  }

  @Test
  void submitResultRequiresAValueAndBoundsUnitAndReferenceRange() {
    var request = new SubmitResultRequest();
    request.setResultValue(" ");
    request.setResultUnit("u".repeat(31));
    request.setReferenceRange("r".repeat(101));

    Set<String> invalidFields = validateFields(request);

    assertThat(invalidFields).contains("resultValue", "resultUnit", "referenceRange");
  }

  private Set<String> validateFields(Object request) {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      return validatorFactory.getValidator().validate(request).stream()
          .map(violation -> violation.getPropertyPath().toString())
          .collect(Collectors.toSet());
    }
  }
}
