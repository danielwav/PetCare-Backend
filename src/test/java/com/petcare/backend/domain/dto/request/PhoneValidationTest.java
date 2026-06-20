package com.petcare.backend.domain.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsValidInternationalPhones() {
		assertThat(validator.validate(duenioRequest("+51999888777"))).isEmpty();
		assertThat(validator.validate(duenioRequest("+57 3001234567"))).isEmpty();
		assertThat(validator.validate(duenioRequest("+1 5551234567"))).isEmpty();
		assertThat(validator.validate(veterinarioRequest("999777666"))).isEmpty();
		assertThat(validator.validate(asistenteRequest("999-666-555"))).isEmpty();
	}

	@Test
	void rejectsPhonesThatAreTooShort() {
		List<Object> invalidRequests = List.of(
				duenioRequest("1234567"),
				veterinarioRequest("1234567"),
				asistenteRequest("1234567")
		);

		assertThat(invalidRequests)
				.allSatisfy(request -> assertThat(validator.validate(request)).isNotEmpty());
	}

	@Test
	void rejectsPhonesWithInvalidCharacters() {
		List<Object> invalidRequests = List.of(
				duenioRequest("abc12345"),
				veterinarioRequest("999 777 66a"),
				asistenteRequest("555-666-7f7")
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
