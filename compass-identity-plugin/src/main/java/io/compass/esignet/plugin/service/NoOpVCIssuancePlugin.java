package io.compass.esignet.plugin.service;

import foundation.identity.jsonld.JsonLDObject;
import io.mosip.esignet.api.dto.VCRequestDto;
import io.mosip.esignet.api.dto.VCResult;
import io.mosip.esignet.api.exception.VCIExchangeException;
import io.mosip.esignet.api.spi.VCIssuancePlugin;
import io.mosip.esignet.api.util.ErrorConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Component;

import java.util.*;

@ConditionalOnProperty(value = "mosip.esignet.integration.vci-plugin", havingValue = "NoOpVCIssuancePlugin")
@Component
@Slf4j
public class NoOpVCIssuancePlugin implements VCIssuancePlugin {

	@Override
	public VCResult<JsonLDObject> getVerifiableCredentialWithLinkedDataProof(VCRequestDto vcRequestDto, String holderId, Map<String, Object> identityDetails) throws VCIExchangeException {
		throw new VCIExchangeException(ErrorConstants.NOT_IMPLEMENTED);
	}

	@Override
	public VCResult<String> getVerifiableCredential(VCRequestDto vcRequestDto, String holderId, Map<String, Object> identityDetails) throws VCIExchangeException {
		throw new VCIExchangeException(ErrorConstants.NOT_IMPLEMENTED);
	}

}
