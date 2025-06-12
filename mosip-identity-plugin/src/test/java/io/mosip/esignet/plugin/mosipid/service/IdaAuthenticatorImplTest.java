/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.esignet.plugin.mosipid.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.KycSigningCertificateException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.esignet.plugin.mosipid.dto.GetAllCertificatesResponse;
import io.mosip.esignet.plugin.mosipid.dto.IdaKycAuthRequest;
import io.mosip.esignet.plugin.mosipid.dto.IdaKycAuthResponse;
import io.mosip.esignet.plugin.mosipid.dto.IdaKycExchangeResponse;
import io.mosip.esignet.plugin.mosipid.dto.IdaResponseWrapper;
import io.mosip.esignet.plugin.mosipid.helper.AuthTransactionHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.esignet.api.dto.AuthChallenge;
import io.mosip.esignet.api.dto.KycAuthDto;
import io.mosip.esignet.api.dto.KycAuthResult;
import io.mosip.esignet.api.dto.KycExchangeDto;
import io.mosip.esignet.api.dto.KycExchangeResult;
import io.mosip.esignet.api.dto.KycSigningCertificateData;
import io.mosip.esignet.api.dto.SendOtpDto;
import io.mosip.esignet.api.dto.SendOtpResult;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.KycSigningCertificateException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class IdaAuthenticatorImplTest {

	@InjectMocks
	IdaAuthenticatorImpl idaAuthenticatorImpl;


	ObjectMapper mapper = new ObjectMapper();;

	@Mock
	RestTemplate restTemplate;

	@Mock
	HelperService helperService;

	@Mock
    AuthTransactionHelper authTransactionHelper;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		ReflectionTestUtils.setField(helperService, "sendOtpUrl", "https:/");
		ReflectionTestUtils.setField(helperService, "idaPartnerCertificateUrl", "https://test");
		ReflectionTestUtils.setField(helperService, "symmetricAlgorithm", "AES");
		ReflectionTestUtils.setField(helperService, "symmetricKeyLength", 256);

		ReflectionTestUtils.setField(idaAuthenticatorImpl, "kycExchangeUrl", "https://dev.mosip.net");
		ReflectionTestUtils.setField(idaAuthenticatorImpl, "idaVersion", "VersionIDA");
		ReflectionTestUtils.setField(idaAuthenticatorImpl, "kycAuthUrl", "https://testkycAuthUrl");
		ReflectionTestUtils.setField(idaAuthenticatorImpl, "getCertsUrl", "https://testGetCertsUrl");
		ReflectionTestUtils.setField(idaAuthenticatorImpl, "otpChannels", Arrays.asList("otp", "pin", "bio"));
		ReflectionTestUtils.setField(idaAuthenticatorImpl, "objectMapper", mapper);
	}

	@Test
	public void doKycAuth_withInvalidDetails_throwsException() throws Exception {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("PIN");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycAuthResponse>>>any())).thenReturn(null);

		Assert.assertThrows(KycAuthException.class,
				() -> idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto));
	}

	@Test
	public void doKycAuth_withValidDetails_thenPass() throws Exception {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("OTP");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);


		IdaKycAuthResponse idaKycAuthResponse = new IdaKycAuthResponse();
		idaKycAuthResponse.setAuthToken("authToken1234");
		idaKycAuthResponse.setKycToken("kycToken1234");
		idaKycAuthResponse.setKycStatus(true);

		IdaResponseWrapper<IdaKycAuthResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycAuthResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycAuthResponse>>>any()))
				.thenReturn(responseEntity);

		KycAuthResult kycAuthResult = idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto);

		Assert.assertEquals(kycAuthResult.getKycToken(), kycAuthResult.getKycToken());
	}

	@Test
	public void doKycAuth_withInValidResponseDetails_thenFail() throws Exception {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("OTP");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);


		IdaResponseWrapper<IdaKycAuthResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
						Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycAuthResponse>>>any()))
				.thenReturn(responseEntity);
		try{
			idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto);
		}catch (KycAuthException e){
			Assert.assertEquals(e.getErrorCode(), ErrorConstants.AUTH_FAILED);
		}
	}

	@Test
	public void doKycAuth_withInvalidRequest_thenFail() throws Exception {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("OTP");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);


		IdaResponseWrapper<IdaKycAuthResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>>(
				idaResponseWrapper, HttpStatus.BAD_REQUEST);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
						Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycAuthResponse>>>any()))
				.thenReturn(responseEntity);
		try{
			idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto);
		}catch (KycAuthException e){
			Assert.assertEquals(e.getErrorCode(), ErrorConstants.AUTH_FAILED);
		}
	}

	@Test
	public void doKycAuth_withAuthChallengeNull_thenFail()  {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		kycAuthDto.setChallengeList(null);

		Assert.assertThrows(KycAuthException.class,
				() -> idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto));
	}

	@Test
	public void doKycAuth_withInvalidAuthChallenge_thenFail()  {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("Test");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);

		Assert.assertThrows(KycAuthException.class,
				() -> idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto));
	}

	@Test
	public void doKycAuth_withBIOAuthChallenge_thenPass() throws Exception {
		KycAuthDto kycAuthDto = new KycAuthDto();
		kycAuthDto.setIndividualId("IND1234");
		kycAuthDto.setTransactionId("TRAN1234");
		AuthChallenge authChallenge = new AuthChallenge();
		authChallenge.setAuthFactorType("BIO");
		authChallenge.setChallenge("111111");
		List<AuthChallenge> authChallengeList = new ArrayList<>();
		authChallengeList.add(authChallenge);
		kycAuthDto.setChallengeList(authChallengeList);

		IdaKycAuthRequest.Biometric b = new IdaKycAuthRequest.Biometric();
		b.setData(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
		b.setHash("Hash");
		b.setSessionKey("SessionKey");
		b.setSpecVersion("SepecV");
		b.setThumbprint("Thumbprint");
		List<IdaKycAuthRequest.Biometric> bioList = new ArrayList<>();
		bioList.add(b);
		IdaKycAuthResponse idaKycAuthResponse = new IdaKycAuthResponse();
		idaKycAuthResponse.setAuthToken("authToken1234");
		idaKycAuthResponse.setKycToken("kycToken1234");
		idaKycAuthResponse.setKycStatus(true);

		IdaResponseWrapper<IdaKycAuthResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycAuthResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycAuthResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycAuthResponse>>>any()))
				.thenReturn(responseEntity);

		KycAuthResult kycAuthResult = idaAuthenticatorImpl.doKycAuth("relyingId", "clientId", kycAuthDto);

		Assert.assertEquals(kycAuthResult.getKycToken(), kycAuthResult.getKycToken());
	}

	@Test
	public void doKycExchange_withValidDetails_thenPass() throws Exception {
		KycExchangeDto kycExchangeDto = new KycExchangeDto();
		kycExchangeDto.setIndividualId("IND1234");
		kycExchangeDto.setKycToken("KYCT123");
		kycExchangeDto.setTransactionId("TRAN123");
		List<String> acceptedClaims = new ArrayList<>();
		acceptedClaims.add("claims");
		kycExchangeDto.setAcceptedClaims(acceptedClaims);
		String[] claimsLacales = new String[] { "claims", "locales" };
		kycExchangeDto.setClaimsLocales(claimsLacales);


		IdaKycExchangeResponse idaKycExchangeResponse = new IdaKycExchangeResponse();
		idaKycExchangeResponse.setEncryptedKyc("ENCRKYC123");

		IdaResponseWrapper<IdaKycExchangeResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycExchangeResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(responseEntity);

		KycExchangeResult kycExchangeResult = idaAuthenticatorImpl.doKycExchange("relyingPartyId", "clientId",
				kycExchangeDto);

		Assert.assertEquals(idaKycExchangeResponse.getEncryptedKyc(), kycExchangeResult.getEncryptedKyc());
	}


	@Test
	public void doKycExchange_withValidDetailsEmptyAcceptedClaims_thenPass() throws Exception {
		KycExchangeDto kycExchangeDto = new KycExchangeDto();
		kycExchangeDto.setIndividualId("IND1234");
		kycExchangeDto.setKycToken("KYCT123");
		kycExchangeDto.setTransactionId("TRAN123");
		List<String> acceptedClaims = List.of();
		kycExchangeDto.setAcceptedClaims(acceptedClaims);
		String[] claimsLacales = new String[] { "claims", "locales" };
		kycExchangeDto.setClaimsLocales(claimsLacales);


		IdaKycExchangeResponse idaKycExchangeResponse = new IdaKycExchangeResponse();
		idaKycExchangeResponse.setEncryptedKyc("ENCRKYC123");

		IdaResponseWrapper<IdaKycExchangeResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycExchangeResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
						Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(responseEntity);

		KycExchangeResult kycExchangeResult = idaAuthenticatorImpl.doKycExchange("relyingPartyId", "clientId",
				kycExchangeDto);

		Assert.assertEquals(idaKycExchangeResponse.getEncryptedKyc(), kycExchangeResult.getEncryptedKyc());
	}

	@Test
	public void doKycExchange_withConsentedVerifiedClaims_thenPass() throws Exception {
		VerifiedKycExchangeDto verifiedDto = new VerifiedKycExchangeDto();
		verifiedDto.setIndividualId("IND1234");
		verifiedDto.setKycToken("KYCT123");
		verifiedDto.setTransactionId("TRAN123");
		verifiedDto.setAcceptedClaims(List.of( "name", "dob","email"));
		verifiedDto.setClaimsLocales(new String[]{"en"});

		ObjectMapper mapper = new ObjectMapper();
		JsonNode verifiedClaimsNode = mapper.readTree("[{\"email\":\"test@gmail.com\"}]");

		JsonNode nameNode = mapper.readTree("\"John Doe\"");
		JsonNode dobNode = mapper.readTree("\"1990-01-01\"");

		Map<String, JsonNode> claimDetails = new HashMap<>();
		claimDetails.put("verified_claims", verifiedClaimsNode);
		claimDetails.put("name", nameNode);
		claimDetails.put("dob", dobNode);

		verifiedDto.setAcceptedClaimDetails(claimDetails);

		IdaKycExchangeResponse idaKycExchangeResponse = new IdaKycExchangeResponse();
		idaKycExchangeResponse.setEncryptedKyc("encryptedKyc");

		IdaResponseWrapper<IdaKycExchangeResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycExchangeResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER2");

		ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>> responseEntity =
				new ResponseEntity<>(idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<String>>any(),
						Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(responseEntity);

		KycExchangeResult result = idaAuthenticatorImpl.doKycExchange("relyingPartyId", "clientId", verifiedDto);

		Assert.assertEquals("encryptedKyc", result.getEncryptedKyc());
	}

	@Test
	public void doKycExchange_withConsentedUnVerifiedClaims_thenPass() throws Exception {
		VerifiedKycExchangeDto verifiedDto = new VerifiedKycExchangeDto();
		verifiedDto.setIndividualId("IND1234");
		verifiedDto.setKycToken("KYCT123");
		verifiedDto.setTransactionId("TRAN123");
		verifiedDto.setAcceptedClaims(List.of( "gender"));
		verifiedDto.setClaimsLocales(new String[]{"en"});

		ObjectMapper mapper = new ObjectMapper();
		JsonNode verifiedClaimsNode = mapper.readTree("[{\"email\":\"test@gmail.com\"}]");

		JsonNode nameNode = mapper.readTree("\"John Doe\"");
		JsonNode genderNode = mapper.readTree("\"Male\"");

		Map<String, JsonNode> claimDetails = new HashMap<>();
		claimDetails.put("verified_claims", verifiedClaimsNode);
		claimDetails.put("name", nameNode);
		claimDetails.put("dob", genderNode);

		verifiedDto.setAcceptedClaimDetails(claimDetails);

		IdaKycExchangeResponse idaKycExchangeResponse = new IdaKycExchangeResponse();
		idaKycExchangeResponse.setEncryptedKyc("encryptedKyc");

		IdaResponseWrapper<IdaKycExchangeResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(idaKycExchangeResponse);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER2");

		ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>> responseEntity =
				new ResponseEntity<>(idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<String>>any(),
						Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(responseEntity);

		KycExchangeResult result = idaAuthenticatorImpl.doKycExchange("relyingPartyId", "clientId", verifiedDto);

		Assert.assertEquals("encryptedKyc", result.getEncryptedKyc());
	}

	@Test
	public void doKycExchange_withInvalidDetails_thenFail() throws Exception {
		KycExchangeDto kycExchangeDto = new KycExchangeDto();
		kycExchangeDto.setIndividualId(null);
		kycExchangeDto.setKycToken("KYCT123");
		kycExchangeDto.setTransactionId("TRAN123");
		List<String> acceptedClaims = new ArrayList<>();
		acceptedClaims.add("claims");
		kycExchangeDto.setAcceptedClaims(acceptedClaims);
		String[] claimsLacales = new String[] { "claims", "locales" };
		kycExchangeDto.setClaimsLocales(claimsLacales);


		IdaKycExchangeResponse idaKycExchangeResponse = new IdaKycExchangeResponse();
		idaKycExchangeResponse.setEncryptedKyc("ENCRKYC123");

		IdaResponseWrapper<IdaKycExchangeResponse> idaResponseWrapper = new IdaResponseWrapper<>();
		idaResponseWrapper.setResponse(null);
		idaResponseWrapper.setTransactionID("TRAN123");
		idaResponseWrapper.setVersion("VER1");

		ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>> responseEntity = new ResponseEntity<IdaResponseWrapper<IdaKycExchangeResponse>>(
				idaResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(responseEntity);

		Assert.assertThrows(KycExchangeException.class,
				() -> idaAuthenticatorImpl.doKycExchange("test-relyingPartyId", "test-clientId", kycExchangeDto));
	}

	@Test
	public void doKycExchange_withInvalidIndividualId_throwsException() throws  Exception {
		KycExchangeDto kycExchangeDto = new KycExchangeDto();
		kycExchangeDto.setIndividualId("IND1234");
		kycExchangeDto.setKycToken("KYCT123");
		kycExchangeDto.setTransactionId("TRAN123");
		List<String> acceptedClaims = new ArrayList<>();
		acceptedClaims.add("claims");
		kycExchangeDto.setAcceptedClaims(acceptedClaims);
		String[] claimsLacales = new String[] { "claims", "locales" };
		kycExchangeDto.setClaimsLocales(claimsLacales);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<IdaResponseWrapper<IdaKycExchangeResponse>>>any()))
				.thenReturn(null);

		Assert.assertThrows(KycExchangeException.class,
				() -> idaAuthenticatorImpl.doKycExchange("relyingId", "clientId", kycExchangeDto));
	}

	@Test
	public void sendOtp_withValidDetails_thenPass() throws Exception {
		SendOtpDto sendOtpDto = new SendOtpDto();
		sendOtpDto.setIndividualId("1234");
		sendOtpDto.setTransactionId("4567");
		List<String> otpChannelsList = new ArrayList<>();
		otpChannelsList.add("channel");
		sendOtpDto.setOtpChannels(otpChannelsList);

		Mockito.when(helperService.sendOTP(any(),any(),any())).thenReturn(new SendOtpResult(sendOtpDto.getTransactionId(), "", ""));

		SendOtpResult sendOtpResult = idaAuthenticatorImpl.sendOtp("rly123", "cli123", sendOtpDto);

		Assert.assertEquals(sendOtpDto.getTransactionId(), sendOtpResult.getTransactionId());
	}

	@Test
	public void sendOtp_withErrorResponse_throwsException() throws Exception {
		SendOtpDto sendOtpDto = new SendOtpDto();
		sendOtpDto.setIndividualId(null);
		sendOtpDto.setTransactionId("4567");
		List<String> otpChannelsList = new ArrayList<>();
		otpChannelsList.add("channel");
		sendOtpDto.setOtpChannels(otpChannelsList);

		Mockito.when(helperService.sendOTP(any(),any(),any())).thenThrow(new SendOtpException("error-100"));

		try {
			idaAuthenticatorImpl.sendOtp("rly123", "cli123", sendOtpDto);
			Assert.fail();
		} catch (SendOtpException e) {
			Assert.assertEquals("error-100", e.getErrorCode());
		}
	}

	@Test
	public void isSupportedOtpChannel_withInvalidChannel_thenFail() {
		Assert.assertFalse(idaAuthenticatorImpl.isSupportedOtpChannel("test"));
	}
	
	@Test
	public void isSupportedOtpChannel_withValidChannel_thenPass() {
		Assert.assertTrue(idaAuthenticatorImpl.isSupportedOtpChannel("OTP"));
	}
	
	@Test
	public void getAllKycSigningCertificates_withValidDetails_thenPass() throws Exception {
		Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("test-token");

		GetAllCertificatesResponse getAllCertificatesResponse = new GetAllCertificatesResponse();
		getAllCertificatesResponse.setAllCertificates(new ArrayList<KycSigningCertificateData>());

		ResponseWrapper<GetAllCertificatesResponse> certsResponseWrapper = new ResponseWrapper<GetAllCertificatesResponse>();
		certsResponseWrapper.setId("test-id");
		certsResponseWrapper.setResponse(getAllCertificatesResponse);

		ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>> certsResponseEntity = new ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>>(
				certsResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<ResponseWrapper<GetAllCertificatesResponse>>>any()))
				.thenReturn(certsResponseEntity);

		List<KycSigningCertificateData> signingCertificates = new ArrayList<>();

		signingCertificates = idaAuthenticatorImpl.getAllKycSigningCertificates();

		Assert.assertSame(signingCertificates, getAllCertificatesResponse.getAllCertificates());
	}

	@Test
	public void getAllKycSigningCertificates_withInvalidResponse_throwsException() throws Exception {
		Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("test-token");

		ResponseWrapper<GetAllCertificatesResponse> certsResponseWrapper = new ResponseWrapper<GetAllCertificatesResponse>();
		certsResponseWrapper.setId("test-id");
		List<ServiceError> errors = new ArrayList<>();
		ServiceError error = new ServiceError("ERR-001", "Certificates not found");
		errors.add(error);
		certsResponseWrapper.setErrors(errors);

		ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>> certsResponseEntity = new ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>>(
				certsResponseWrapper, HttpStatus.OK);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<ResponseWrapper<GetAllCertificatesResponse>>>any()))
				.thenReturn(certsResponseEntity);

		Assert.assertThrows(KycSigningCertificateException.class,
				() -> idaAuthenticatorImpl.getAllKycSigningCertificates());
	}
	
	@Test
	public void getAllKycSigningCertificates_withErrorResponse_throwsException() throws Exception {
		Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("test-token");

		ResponseWrapper<GetAllCertificatesResponse> certsResponseWrapper = new ResponseWrapper<GetAllCertificatesResponse>();
		certsResponseWrapper.setId("test-id");
		List<ServiceError> errors = new ArrayList<>();
		ServiceError error = new ServiceError("ERR-001", "Certificates not found");
		errors.add(error);
		certsResponseWrapper.setErrors(errors);

		ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>> certsResponseEntity = new ResponseEntity<ResponseWrapper<GetAllCertificatesResponse>>(
				certsResponseWrapper, HttpStatus.FORBIDDEN);

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<ResponseWrapper<GetAllCertificatesResponse>>>any()))
				.thenReturn(certsResponseEntity);

		Assert.assertThrows(KycSigningCertificateException.class,
				() -> idaAuthenticatorImpl.getAllKycSigningCertificates());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void getAllKycSigningCertificates_withInvalidToken_thenFail() throws Exception {
		Mockito.when(authTransactionHelper.getAuthToken()).thenReturn("test-token");

		Mockito.when(restTemplate.exchange(Mockito.<RequestEntity<Void>>any(),
				Mockito.<ParameterizedTypeReference<ResponseWrapper>>any())).thenThrow(RuntimeException.class);

		Assert.assertThrows(KycSigningCertificateException.class,
				() -> idaAuthenticatorImpl.getAllKycSigningCertificates());
	}

}
