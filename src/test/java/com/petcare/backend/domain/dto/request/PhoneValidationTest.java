package com.petcare.backend.domain.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsExactlyNineDigitsForDuenioVeterinarioAndAsistentePhones() {
		assertThat(validator.validate(duenioRequest("999888777"))).isEmpty();
		assertThat(validator.validate(veterinarioRequest("999777666"))).isEmpty();
		assertThat(validator.validate(asistenteRequest("999666555"))).isEmpty();
	}

	@Test
	void rejectsPhonesThatAreNotExactlyNineDigits() {
		List<Object> invalidRequests = List.of(
				duenioRequest("99988877"),
				duenioRequest("9998887771"),
				duenioRequest("+51999888"),
				veterinarioRequest("99977766"),
				veterinarioRequest("9997776661"),
				veterinarioRequest("999-77766"),
				asistenteRequest("99966655"),
				asistenteRequest("9996665551"),
				asistenteRequest("999 66655")
		);

		assertThat(invalidRequests)
				.allSatisfy(request -> assertThat(validator.validate(request)).isNotEmpty());
	}

	private DuenioRequest duenioRequest(String telefono) {
		return new DuenioRequest(
				null,
				"Daniel",
				"Torres",
				"DNI",
				"12345678",
				telefono,
				"daniel@test.com",
				"Av. Siempre Viva 123"
		);
	}

	private VeterinarioRequest veterinarioRequest(String telefono) {
		return new VeterinarioRequest(
				null,
				"Ana",
				"Salas",
				"CMVP-001",
				"Medicina general",
				telefono,
				"ana.vet@test.com",
				List.of()
		);
	}

	private AsistenteRequest asistenteRequest(String telefono) {
		return new AsistenteRequest(
				null,
				"Maria",
				"Lopez",
				"DNI",
				"70500001",
				telefono,
				"maria.asistente@test.com",
				"Agenda de citas",
				"secret123"
		);
	}
}
