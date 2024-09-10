package io.mosip.signup.plugin.mock.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockIdentityRequest {
	String individualId;

	String pin;

	List<LanguageValue> name;
	
	List<LanguageValue> fullName;

	String preferredLang;

	List<LanguageValue> givenName;

	List<LanguageValue> familyName;

	List<LanguageValue> middleName;

	List<LanguageValue> nickName;

	List<LanguageValue> preferredUsername;

	List<LanguageValue> gender;

	String dateOfBirth;

	List<LanguageValue> streetAddress;

	List<LanguageValue> locality;

	List<LanguageValue> region;

	String postalCode;

	List<LanguageValue> country;

	String encodedPhoto;

	BiometricData individualBiometrics;

	String email;

	String phone;

	String zoneInfo;
	
	String locale;
	
	String password;

}
