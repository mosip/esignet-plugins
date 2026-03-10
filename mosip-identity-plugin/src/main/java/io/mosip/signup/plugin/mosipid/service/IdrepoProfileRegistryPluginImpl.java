/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.signup.plugin.mosipid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.micrometer.core.annotation.Timed;
import io.mosip.esignet.core.util.IdentityProviderUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.signup.api.dto.ProfileDto;
import io.mosip.signup.api.dto.ProfileResult;
import io.mosip.signup.api.exception.InvalidProfileException;
import io.mosip.signup.api.exception.ProfileException;
import io.mosip.signup.api.spi.ProfileRegistryPlugin;
import io.mosip.signup.api.util.ProfileCreateUpdateStatus;
import io.mosip.signup.plugin.mosipid.dto.*;
import io.mosip.signup.plugin.mosipid.util.BiometricUtil;
import io.mosip.signup.plugin.mosipid.util.ErrorConstants;
import io.mosip.signup.plugin.mosipid.util.ProfileCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import javax.imageio.spi.IIORegistry;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.mosip.signup.api.util.ErrorConstants.SERVER_UNREACHABLE;
import static io.mosip.signup.plugin.mosipid.util.ErrorConstants.INVALID_INDIVIDUAL_BIOMETRICS;
import static io.mosip.signup.plugin.mosipid.util.ErrorConstants.REQUEST_FAILED;

@Slf4j
@Component
@ConditionalOnProperty(value = "mosip.signup.integration.profile-registry-plugin", havingValue = "MOSIPProfileRegistryPluginImpl")
public class IdrepoProfileRegistryPluginImpl implements ProfileRegistryPlugin {

    private static final String ID_SCHEMA_VERSION_FIELD_ID = "IDSchemaVersion";
    private static final String UIN = "UIN";
    private static final String VID = "VID";
    private static final String SELECTED_HANDLES_FIELD_ID = "selectedHandles";
    private static final String UTC_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final Map<Double, SchemaResponse> schemaMap = new HashMap<>();
    private static final List<String> ACTIONS = Arrays.asList("CREATE", "UPDATE");
    private final String HANDLE_SEPARATOR = "@";

    @Value("#{'${mosip.signup.idrepo.default.selected-handles:phone}'.split(',')}")
    private List<String> defaultSelectedHandles;
    
    @Value("${mosip.signup.idrepo.identifier-field:phone}")
    private String identifierField;

    @Value("${mosip.signup.idrepo.schema-url}")
    private String schemaUrl;

    @Value("#{'${mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.new-registration:}'.split(',')}")
    private List<String> requiredFields;

    @Value("#{'${mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.update-uin:}'.split(',')}")
    private List<String> requiredUpdateFields;

    @Value("${mosip.signup.idrepo.add-identity.request.id}")
    private String addIdentityRequestID;

    @Value("${mosip.signup.idrepo.update-identity.request.id}")
    private String updateIdentityRequestID;

    @Value("${mosip.signup.idrepo.identity.request.version}")
    private String identityRequestVersion;

    @Value("${mosip.signup.idrepo.identity.endpoint}")
    private String identityEndpoint;

    @Value("${mosip.signup.idrepo.get-identity.endpoint}")
    private String getIdentityEndpoint;

    @Value("${mosip.signup.idrepo.generate-hash.endpoint}")
    private String generateHashEndpoint;

    @Value("${mosip.signup.idrepo.get-uin.endpoint}")
    private String getUinEndpoint;

    @Value("${mosip.signup.idrepo.get-status.endpoint}")
    private String getStatusEndpoint;

    @Value("#{'${mosip.signup.idrepo.mandatory-language:}'.split(',')}")
    private List<String> mandatoryLanguages;

    @Value("#{'${mosip.signup.idrepo.optional-language:}'.split(',')}")
    private List<String> optionalLanguages;

    @Value("${mosip.signup.idrepo.get-identity-method:POST}")
    private String getIdentityEndpointMethod;

    @Value("${mosip.signup.idrepo.get-identity-fallback-path}")
    private String getIdentityEndpointFallbackPath;

    @Value("${mosip.signup.idrepo.biometric.field-name:individualBiometrics}")
    private String biometricDataFieldName;

    @Value("${mosip.signup.idrepo.uin.length:10}")
    private int uinLength;

    @Autowired
    @Qualifier("selfTokenRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileCacheService profileCacheService;

    @Autowired
    private BiometricUtil biometricUtil;

    @Value("${mosip.signup.mosipid.get-ui-spec.endpoint}")
    private String uiSpecUrl;

    @Value("${mosip.signup.mosipid.uispec.schema-jsonpath:$[0].jsonSpec[0].spec.schema}")
    private String schemaJsonpath;

    @Value("${mosip.signup.mosipid.uispec.allowedvalues-jsonpath:$[0].jsonSpec[0].spec.allowedValues}")
    private String allowedValuesJsonpath;

    @Value("${mosip.signup.mosipid.uispec.i18values-jsonpath:$[0].jsonSpec[0].spec.i18nValues}")
    private String i18nValuesJsonpath;

    @Value("${mosip.signup.mosipid.uispec.i18values-errors-jsonpath:$[0].jsonSpec[0].spec.i18nValues.errors}")
    private String i18nValuesErrorJsonpath;

    @Value("${mosip.signup.mosipid.uispec.maxuploadfilesize-jsonpath:$[0].jsonSpec[0].spec.maxUploadFileSize}")
    private String maxUploadFileSizeJsonpath;

    @Value("${mosip.signup.mosipid.uispec.resetPasswordChallengeFields-jsonpath:$[0].jsonSpec[0].spec.resetPasswordChallengeFields}")
    private String resetPasswordChallengeFieldsJsonpath;

    @Value("${mosip.signup.mosipid.uispec.errors-jsonpath:$[0].jsonSpec[0].spec.errors}")
    private String errorsJsonpath;

    @Value("#{${mosip.signup.mosipid.uispec.errors:null}}")
    private Map<String, Object> errorsFromConfig = new HashMap<>();

    @Value("${mosip.signup.mosipid.dynamic-fields.endpoint}")
    private String dynamicFieldsBaseUrl;

    @Value("${mosip.signup.mosipid.doc-types-category.endpoint}")
    private String docTypesAndCategoryBaseUrl;


    @PostConstruct
    public void init() {

        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new J2KImageReaderSpi());
    }

    /**
     * Read the i18nValues from the UI-spec schema
     * @param responseJson the response json from ${mosip.signup.mosipid.get-ui-spec.endpoint}
     * @return i18nValues
     */
    private ObjectNode readI18nValues(String responseJson) {
        Object i18nValueResponse;
        ObjectNode  i18nValues;
        try {
            i18nValueResponse = JsonPath.read(responseJson, i18nValuesJsonpath);
            i18nValues = objectMapper.convertValue(i18nValueResponse, ObjectNode.class);
            i18nValues.set("errors", objectMapper.valueToTree(readErrors(responseJson, i18nValuesErrorJsonpath)));
        } catch (PathNotFoundException e) {
            log.error("i18nValues not found in schema");
            i18nValues = objectMapper.createObjectNode();
        }
        return i18nValues;
    }

    /**
     * Read the allowed values from master UI-spec and if not found read it from master data.
     * @param responseJson the response json from ${mosip.signup.mosipid.get-ui-spec.endpoint}
     * @return allowedValues
     */
    private JsonNode readAllowedValues(String responseJson) {
        JsonNode allowedValues;
        ObjectNode allowedValuesFromSpec = objectMapper.convertValue(JsonPath.read(responseJson, allowedValuesJsonpath), ObjectNode.class);
        if (allowedValuesFromSpec != null && !allowedValuesFromSpec.isEmpty()) {
            allowedValues = allowedValuesFromSpec; //allowed values from UI-Spec
        } else {
            allowedValues = generateAllowedValues(); //allowed values from master-data
        }
        return allowedValues;
    }

    /**
     * Read the errors from UI-spec if not present, from config
     * @param responseJson the response json from ${mosip.signup.mosipid.get-ui-spec.endpoint}
     * @param jsonpath errors path inside schema
     * @return errors
     */
    private Object readErrors(String responseJson, String jsonpath) {
        Object errors;
        try {
            errors = JsonPath.read(responseJson, jsonpath);
        } catch (PathNotFoundException e) {
            errors = errorsFromConfig;
        }
        return errors;
    }

    /**
     * Reads max upload file size from the UI-spec
     * @param responseJson the response json from ${mosip.signup.mosipid.get-ui-spec.endpoint}
     * @return maxUploadFileSize
     */
    private Object readMaxUploadFileSize(String responseJson) {
        Object maxUploadFileSize;
        try {
            maxUploadFileSize = JsonPath.read(responseJson, maxUploadFileSizeJsonpath);
        } catch (PathNotFoundException e) {
            log.error("maxUploadFileSize not found in schema, setting to default");
            maxUploadFileSize = 5242880;
        }
        return maxUploadFileSize;
    }

    private Object readResetPasswordChallengeFields(String responseJson) {
        Object resetPasswordChallengeFields;
        try {
            resetPasswordChallengeFields = JsonPath.read(responseJson, resetPasswordChallengeFieldsJsonpath);
        } catch (PathNotFoundException e) {
            log.error("resetPasswordChallengeFields not found in schema");
            return Collections.emptyList();
        }
        return resetPasswordChallengeFields;
    }

    /**
     * Generate combined JsonNode from List<JsonNode> dynamicFields and List<JsonNode> documentCategories
     * @return JsonNode containing the allowed values.
     */
    public JsonNode generateAllowedValues() {
        ObjectNode result = objectMapper.createObjectNode();
        fetchAndProcessDynamicFields(result);
        fetchAndProcessDocTypesAndCategories(result);
        return result;
    }

    private String buildDynamicFieldsUrl(int pageNumber, int pageSize) {
        return String.format(dynamicFieldsBaseUrl, pageNumber, pageSize);
    }

    /**
     * Fetch and process document types and categories
     */
    private void fetchAndProcessDocTypesAndCategories(ObjectNode result) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(docTypesAndCategoryBaseUrl, JsonNode.class);
        JsonNode responseBody = response.getBody();

        if (responseBody != null && responseBody.has("response")) {
            JsonNode data = responseBody.get("response").get("documentCategories");
            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    if (!item.has("isActive") || !item.get("isActive").asBoolean()) continue;

                    String categoryCode = item.hasNonNull("code") ? item.get("code").asText() : null;
                    String langCode = item.hasNonNull("langCode") ? item.get("langCode").asText() : null;
                    JsonNode documentTypes = item.get("documentTypes");

                    if (categoryCode == null || langCode == null || documentTypes == null || !documentTypes.isArray())
                        continue;

                    ObjectNode docTypeMap = (ObjectNode) result.get(categoryCode);
                    if (docTypeMap == null) {
                        docTypeMap = objectMapper.createObjectNode();
                        result.set(categoryCode, docTypeMap);
                    }

                    for (JsonNode docType : documentTypes) {
                        String docTypeCode = docType.hasNonNull("code") ? docType.get("code").asText() : null;
                        String docTypeName = docType.hasNonNull("name") ? docType.get("name").asText() : null;
                        if (docTypeCode == null || docTypeName == null) continue;

                        ObjectNode langMap = (ObjectNode) docTypeMap.get(docTypeCode);
                        if (langMap == null) {
                            langMap = objectMapper.createObjectNode();
                            docTypeMap.set(docTypeCode, langMap);
                        }
                        langMap.put(langCode, docTypeName);
                    }
                }
            }
        }
    }

    /**
     * Fetch and processes the dynamic fields JSON list and adds their structured data into the provided ObjectNode.
     * @param result The ObjectNode where data is accumulated
     */
    private void fetchAndProcessDynamicFields(ObjectNode result) {
        int pageNumber = 0;
        int pageSize = 10;
        int totalPages = 1;
        int totalItems = 0;

        while (pageNumber < totalPages) {
            String url = buildDynamicFieldsUrl(pageNumber, pageSize);
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("response")) {
                JsonNode responseNode = responseBody.get("response");
                if (pageNumber == 0) {
                    totalPages = objectMapper.convertValue(responseNode.get("totalPages"), Integer.class);
                    totalItems = objectMapper.convertValue(responseNode.get("totalItems"), Integer.class);
                }
                JsonNode data = responseNode.get("data");
                if (data != null && data.isArray()) {
                    for (JsonNode item : data) {
                        if (!item.has("isActive") || !item.get("isActive").asBoolean()) continue;

                        String name = item.hasNonNull("name") ? item.get("name").asText() : null;
                        String lang = item.hasNonNull("langCode") ? item.get("langCode").asText() : null;
                        JsonNode fieldValues = item.get("fieldVal");

                        if (name == null || lang == null || fieldValues == null || !fieldValues.isArray()) continue;

                        ObjectNode nameNode = (ObjectNode) result.get(name);
                        if (nameNode == null) {
                            nameNode = objectMapper.createObjectNode();
                            result.set(name, nameNode);
                        }

                        for (JsonNode fv : fieldValues) {
                            String code = fv.hasNonNull("code") ? fv.get("code").asText() : null;
                            String value = fv.hasNonNull("value") ? fv.get("value").asText() : null;
                            if (code == null || value == null) continue;

                            ObjectNode langMap = (ObjectNode) nameNode.get(code);
                            if (langMap == null) {
                                langMap = objectMapper.createObjectNode();
                                nameNode.set(code, langMap);
                            }
                            langMap.put(lang, value);
                        }
                    }
                }
            }
            pageNumber++;
            int remainingItems = totalItems - (pageNumber * pageSize);
            if (remainingItems < pageSize) {
                pageSize = remainingItems;
            }
        }
    }


    @Override
    public void validate(String action, ProfileDto profileDto) throws InvalidProfileException {
        if (!ACTIONS.contains(action)) {
            throw new InvalidProfileException(ErrorConstants.INVALID_ACTION);
        }

        JsonNode inputJson = profileDto.getIdentity();
        double version = inputJson.has(ID_SCHEMA_VERSION_FIELD_ID) ? inputJson.get(ID_SCHEMA_VERSION_FIELD_ID).asDouble() : 0;
        SchemaResponse schemaResponse = getSchemaJson(version);
        ((ObjectNode) inputJson).set(ID_SCHEMA_VERSION_FIELD_ID, objectMapper.valueToTree(schemaResponse.getIdVersion()));

        // check if any required field is missing during the "create" action.
        JsonNode requiredFieldIds = schemaResponse.getParsedSchemaJson().at("/properties/identity/required");
        if (action.equals("CREATE")) {
            Iterator itr = requiredFieldIds.iterator();
            while (itr.hasNext()) {
                String fieldName = ((TextNode)itr.next()).textValue();
                if (inputJson.get(fieldName) == null) {
                    log.error("Null/Empty value found in the required field of {}, required: {}", fieldName, requiredFieldIds);
                    throw new InvalidProfileException("invalid_".concat(fieldName.toLowerCase()));
                }
            }
        }

        // validate each entry field with schemaResponse
        Iterator<Map.Entry<String, JsonNode>> itr = inputJson.fields();
        JsonNode fields = schemaResponse.getParsedSchemaJson().at("/properties/identity/properties");
        validateEntryFields(itr, fields);
    }

    @Override
    public ProfileResult createProfile(String requestId, ProfileDto profileDto) throws ProfileException {
    	if(identifierField != null && !profileDto.getIndividualId().equalsIgnoreCase(profileDto.getIdentity().get(identifierField).asText())) {
            log.error("IndividualId and {} mismatch", identifierField);
            throw new InvalidProfileException(ErrorConstants.IDENTIFIER_MISMATCH);
        }

        JsonNode inputJson = profileDto.getIdentity();
        //set UIN
        ((ObjectNode) inputJson).set(UIN, objectMapper.valueToTree(getUniqueIdentifier()));
        //Build identity request
        IdentityRequest identityRequest = buildIdentityRequest(inputJson, false);
        identityRequest.setRegistrationId(requestId);

        if(!inputJson.has(SELECTED_HANDLES_FIELD_ID) && !CollectionUtils.isEmpty(defaultSelectedHandles)){
            ((ObjectNode) inputJson).set(SELECTED_HANDLES_FIELD_ID, objectMapper.valueToTree(defaultSelectedHandles));
        }

        List<String> requestIdsToTrack = new ArrayList<>();
        if(inputJson.has(SELECTED_HANDLES_FIELD_ID)) {
            Iterator itr = inputJson.get(SELECTED_HANDLES_FIELD_ID).iterator();
            while(itr.hasNext()) {
                String selectedHandle = ((TextNode)itr.next()).textValue();
                if(!inputJson.get(selectedHandle).isArray()) {
                    String value = inputJson.get(selectedHandle).textValue();
                    requestIdsToTrack.add(getHandleRequestId(requestId, selectedHandle, value));
                }
            }
        }
        profileCacheService.setHandleRequestIds(requestId, requestIdsToTrack);
        IdentityResponse identityResponse = addIdentity(identityRequest);
        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileResult updateProfile(String requestId, ProfileDto profileDto) throws ProfileException {
        JsonNode inputJson = profileDto.getIdentity();

        if(profileDto.getIndividualId().contains(HANDLE_SEPARATOR) || profileDto.getIndividualId().length() > uinLength) {
            ((ObjectNode) inputJson).set(UIN, objectMapper.valueToTree(getProfile(profileDto.getIndividualId()).getIndividualId()));
        } else {
            ((ObjectNode) inputJson).set(UIN, objectMapper.valueToTree(profileDto.getIndividualId()));
        }

        //Build identity request
        IdentityRequest identityRequest = buildIdentityRequest(inputJson, true);
        identityRequest.setRegistrationId(requestId);

        IdentityResponse identityResponse = updateIdentity(identityRequest);
        log.info("Received IdentityResponse for requestId {}: {}", requestId, identityResponse);
        profileCacheService.setHandleRequestIds(requestId, Arrays.asList(requestId));

        ProfileResult profileResult = new ProfileResult();
        profileResult.setStatus(identityResponse.getStatus());
        return profileResult;
    }

    @Override
    public ProfileCreateUpdateStatus getProfileCreateUpdateStatus(String requestId) throws ProfileException {
        List<String> handleRequestIds = profileCacheService.getHandleRequestIds(requestId);
        if(handleRequestIds == null || handleRequestIds.isEmpty())
            return getRequestStatusFromServer(requestId);

        //TODO - Need to support returning multiple handles status
        //TODO - Also we should cache the handle create/update status
        return getRequestStatusFromServer(handleRequestIds.get(0));
    }

    @Override
    public ProfileDto getProfile(String individualId) throws ProfileException {
        try {
            boolean isHandle = individualId.contains(HANDLE_SEPARATOR);
            ResponseWrapper<IdentityResponse> responseWrapper = null;
            switch (getIdentityEndpointMethod.toLowerCase()) {
                case "post" :
                    IdRequestByIdDTO requestByIdDTO = new IdRequestByIdDTO();
                    RequestWrapper<IdRequestByIdDTO> idDTORequestWrapper=new RequestWrapper<>();
                    requestByIdDTO.setId(individualId);
                    requestByIdDTO.setType("demo");
                    if (isHandle) {
                        requestByIdDTO.setIdType("HANDLE");
                    } else {
                        requestByIdDTO.setIdType(individualId.length() > uinLength ? VID : UIN);
                    }
                    idDTORequestWrapper.setRequest(requestByIdDTO);
                    idDTORequestWrapper.setRequesttime(getUTCDateTime());
                    responseWrapper = request(getIdentityEndpoint, HttpMethod.POST, idDTORequestWrapper,
                            new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {});
                    break;
                case "get":
                    String path = String.format(getIdentityEndpointFallbackPath, individualId);
                    if(isHandle) path += "&idType=HANDLE";
                    responseWrapper = request(getIdentityEndpoint+path, HttpMethod.GET, null,
                            new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {});
                    break;
            }
            if(responseWrapper==null || responseWrapper.getResponse() == null || responseWrapper.getResponse().getIdentity() == null){
                throw new ProfileException(REQUEST_FAILED);
            }
            ProfileDto profileDto = new ProfileDto();
            profileDto.setIndividualId(responseWrapper.getResponse().getIdentity().get(UIN).textValue());
            profileDto.setIdentity(responseWrapper.getResponse().getIdentity());
            profileDto.setActive("ACTIVATED".equals(responseWrapper.getResponse().getStatus()));
            return profileDto;
        } catch (ProfileException e) {
            if (e.getErrorCode().equals("IDR-IDC-007")) {
                ProfileDto profileDto = new ProfileDto();
                profileDto.setIndividualId(individualId);
                profileDto.setActive(false);
                return profileDto;
            }
            throw e;
        }
    }

    @Override
    public boolean isMatch(JsonNode identity, JsonNode inputChallenge) {
        int matchCount = 0;
        Iterator itr = inputChallenge.fieldNames();
        while(itr.hasNext()) {
            String fieldName = (String) itr.next();
            if(!identity.has(fieldName))
                break;

            if(identity.get(fieldName).isArray()) {
                for (JsonNode jsonNode : identity.get(fieldName)) {
                    //As of now assumption is we take user input only in single language
                    matchCount = matchCount + ((jsonNode.equals(inputChallenge.get(fieldName).get(0))) ? 1 : 0);
                }
            }
            else {
                matchCount = matchCount + ((identity.get(fieldName).equals(inputChallenge.get(fieldName))) ? 1 : 0);
            }
        }
        return !inputChallenge.isEmpty() && matchCount >= inputChallenge.size();
    }

    @Override
    public JsonNode getUISpecification() {
        String responseJson = request(uiSpecUrl, HttpMethod.GET, null, new ParameterizedTypeReference<ResponseWrapper<JsonNode>>() {
        })
                .getResponse()
                .toString();
        Object schema = JsonPath.read(responseJson, schemaJsonpath);
        Object errors = readErrors(responseJson, errorsJsonpath);
        ObjectNode i18nValues = readI18nValues(responseJson);
        JsonNode allowedValues = readAllowedValues(responseJson);
        Object maxUploadFileSize = readMaxUploadFileSize(responseJson);
        Object resetPasswordChallengeFields = readResetPasswordChallengeFields(responseJson);

        return objectMapper.valueToTree(
                Map.ofEntries(
                        Map.entry("schema", schema),
                        Map.entry("errors", errors),
                        Map.entry("i18nValues", i18nValues),
                        Map.entry("language", Map.of("mandatory", mandatoryLanguages, "optional", optionalLanguages)),
                        Map.entry("allowedValues", allowedValues),
                        Map.entry("maxUploadFileSize", maxUploadFileSize),
                        Map.entry("resetPasswordChallengeFields",resetPasswordChallengeFields)
                )
        );
    }

    private SchemaResponse getSchemaJson(double version) throws ProfileException {
        if(schemaMap.containsKey(version))
            return schemaMap.get(version);

        ResponseWrapper<SchemaResponse> responseWrapper = request(schemaUrl+version,
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseWrapper<SchemaResponse>>() {});
        if (responseWrapper.getResponse().getSchemaJson()!=null) {
            SchemaResponse schemaResponse = new SchemaResponse();
            try {
                schemaResponse.setParsedSchemaJson(objectMapper.readValue(responseWrapper.getResponse().getSchemaJson(), JsonNode.class));
                schemaResponse.setIdVersion(responseWrapper.getResponse().getIdVersion());
                schemaMap.put(version, schemaResponse);
                return schemaMap.get(version);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse schemaResponse", e);
            }
        }
        log.error("Failed to fetch the latest schema json due to {}", responseWrapper);
        throw new ProfileException(REQUEST_FAILED);
    }

    @Timed(value = "getuin.api.timer", percentiles = {0.9})
    private String getUniqueIdentifier() throws ProfileException {
        ResponseWrapper<UINResponse> responseWrapper = request(getUinEndpoint, HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseWrapper<UINResponse>>() {});
        if (!StringUtils.isEmpty(responseWrapper.getResponse().getUIN()) ) {
            return responseWrapper.getResponse().getUIN();
        }
        log.error("Failed to generate UIN {}", responseWrapper.getResponse());
        throw new ProfileException(REQUEST_FAILED);
    }

    @Timed(value = "pwdhash.api.timer", percentiles = {0.9})
    private Password generateSaltedHash(String password) throws ProfileException {
        RequestWrapper<Password.PasswordPlaintext> requestWrapper = new RequestWrapper<>();
        requestWrapper.setRequesttime(getUTCDateTime());
        requestWrapper.setRequest(new Password.PasswordPlaintext(password));
        ResponseWrapper<Password.PasswordHash> responseWrapper = request(generateHashEndpoint, HttpMethod.POST, requestWrapper,
                new ParameterizedTypeReference<ResponseWrapper<Password.PasswordHash>>() {});
        if (!StringUtils.isEmpty(responseWrapper.getResponse().getHashValue()) &&
                !StringUtils.isEmpty(responseWrapper.getResponse().getSalt())) {
            return new Password(responseWrapper.getResponse().getHashValue(),
                    responseWrapper.getResponse().getSalt());
        }
        log.error("Failed to generate salted hash {}", responseWrapper.getResponse());
        throw new ProfileException(REQUEST_FAILED);
    }

    @Timed(value = "addidentity.api.timer", percentiles = {0.9})
    private IdentityResponse addIdentity(IdentityRequest identityRequest) throws ProfileException{
        RequestWrapper<IdentityRequest> restRequest = new RequestWrapper<>();
        restRequest.setId(addIdentityRequestID);
        restRequest.setVersion(identityRequestVersion);
        restRequest.setRequesttime(getUTCDateTime());
        restRequest.setRequest(identityRequest);
        ResponseWrapper<IdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.POST, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {});
        return responseWrapper.getResponse();
    }

    @Timed(value = "updateidentity.api.timer", percentiles = {0.9})
    private IdentityResponse updateIdentity(IdentityRequest identityRequest) throws ProfileException{
        RequestWrapper<IdentityRequest> restRequest = new RequestWrapper<>();
        restRequest.setId(updateIdentityRequestID);
        restRequest.setVersion(identityRequestVersion);
        restRequest.setRequesttime(getUTCDateTime());
        restRequest.setRequest(identityRequest);

        log.debug("update request {} with request ID {}", restRequest, updateIdentityRequestID);

        ResponseWrapper<IdentityResponse> responseWrapper = request(identityEndpoint, HttpMethod.PATCH, restRequest,
                new ParameterizedTypeReference<ResponseWrapper<IdentityResponse>>() {});
        return responseWrapper.getResponse();
    }

    @Timed(value = "getstatus.api.timer", percentiles = {0.9})
    private ProfileCreateUpdateStatus getRequestStatusFromServer(String applicationId) {
        ResponseWrapper<IdentityStatusResponse> responseWrapper = request(getStatusEndpoint+applicationId,
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseWrapper<IdentityStatusResponse>>() {});
        log.info("Received registration status response for applicationId {}: {}", applicationId, responseWrapper);
        if (responseWrapper != null && responseWrapper.getResponse() != null &&
                !StringUtils.isEmpty(responseWrapper.getResponse().getStatusCode())) {
            switch (responseWrapper.getResponse().getStatusCode()) {
                case "STORED":
                    return ProfileCreateUpdateStatus.COMPLETED;
                case "FAILED":
                    return ProfileCreateUpdateStatus.FAILED;
                case "ISSUED":
                default:
                    return ProfileCreateUpdateStatus.PENDING;
            }
        }
        log.error("Get registration status failed with response {} -> {}", applicationId, responseWrapper);
        throw new ProfileException( CollectionUtils.isEmpty(responseWrapper.getErrors()) ?  REQUEST_FAILED :
                responseWrapper.getErrors().get(0).getErrorCode() );
    }

    private <T> ResponseWrapper<T> request(String url, HttpMethod method, Object request,
                                           ParameterizedTypeReference<ResponseWrapper<T>> responseType) {
        try {
            HttpEntity<?> httpEntity = null;
            if(request != null) {
                httpEntity = new HttpEntity<>(request);
            }
            ResponseWrapper<T> responseWrapper = restTemplate.exchange(
                    url,
                    method,
                    httpEntity,
                    responseType).getBody();
            if (responseWrapper != null && responseWrapper.getResponse() != null) {
                return responseWrapper;
            }
            log.error("{} endpoint returned error response {} ", url, responseWrapper);
            throw new ProfileException(responseWrapper != null && !CollectionUtils.isEmpty(responseWrapper.getErrors()) ?
                    responseWrapper.getErrors().get(0).getErrorCode() : REQUEST_FAILED);
        } catch (RestClientException e) {
            log.error("{} endpoint is unreachable.", url, e);
            throw new ProfileException(SERVER_UNREACHABLE);
        }
    }

    private String getHandleRequestId(String requestId, String handleFieldId, String handle) {
        //TODO need to take the tag from configuration based on fieldId
        String handleWithTaggedHandleType = handle.concat("@").concat(handleFieldId).toLowerCase(Locale.ROOT);
        String handleRequestId = requestId.concat(handleWithTaggedHandleType);
        try {
            return HMACUtils2.digestAsPlainText(handleRequestId.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate handleRequestId", e);
        }
        return requestId;
    }

    private IdentityRequest buildIdentityRequest(JsonNode inputJson, boolean isUpdate) {
        double version = inputJson.has(ID_SCHEMA_VERSION_FIELD_ID) ? inputJson.get(ID_SCHEMA_VERSION_FIELD_ID).asDouble() : 0;
        SchemaResponse schemaResponse = getSchemaJson(version);
        ((ObjectNode) inputJson).set(ID_SCHEMA_VERSION_FIELD_ID, objectMapper.valueToTree(schemaResponse.getIdVersion()));

        //generate salted hash for password, if exists
        if(inputJson.has("password")) {
            Password password = generateSaltedHash(inputJson.get("password").asText());
            ((ObjectNode) inputJson).set("password", objectMapper.valueToTree(password));
        }

        IdentityRequest identityRequest = new IdentityRequest();

        //if verified claims exists then pass it in the request as "verifiedAttributes"
        if(inputJson.has("verified_claims")) {
            identityRequest.setVerifiedAttributes(buildVerifiedClaims(inputJson.get("verified_claims")));
            ((ObjectNode) inputJson).remove("verified_claims");
        }

        identityRequest.setDocuments(buildDocuments(inputJson));

        identityRequest.setIdentity(inputJson);
        return identityRequest;
    }

    /**
     * Method to build List<VerificationMetadata> from the verified claims
     * @param verifiedClaims {@link JsonNode}
     * @return List<VerificationMetadata> verifiedAttributes
     */
    private List<VerificationMetadata> buildVerifiedClaims(JsonNode verifiedClaims) {
        List<VerificationMetadata> verifiedAttributes = new ArrayList<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = verifiedClaims.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String claim = entry.getKey();
            JsonNode value = entry.getValue();

            VerificationMetadata metadata = new VerificationMetadata();
            metadata.setTrustFramework(value.get("trust_framework").asText());
            metadata.setVerificationProcess(value.get("verification_process").asText());
            metadata.setClaims(Collections.singletonList(claim));

            Map<String, Object> metaMap = new HashMap<>();

            // Add fields to metadata
            value.fields().forEachRemaining(field -> metaMap.put(field.getKey(), field.getValue()));

            metaMap.put("time", IdentityProviderUtil.getUTCDateTime());
            metadata.setMetadata(metaMap);

            verifiedAttributes.add(metadata);
        }

        return verifiedAttributes;
    }

    private String getUTCDateTime() {
        return ZonedDateTime
                .now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(UTC_DATETIME_PATTERN));
    }

    private void validateValue(String keyName, SchemaFieldValidator validator, String value) {
        log.info("Validate field : {} with value : {} using validator : {}", keyName, value, validator.getValidator());
        if(value == null || value.trim().isEmpty())
            throw new InvalidProfileException("invalid_".concat(keyName.toLowerCase()));

        if( validator != null && "regex".equalsIgnoreCase(validator.getType()) && !value.matches(validator.getValidator()) ) {
            log.error("Regex of {} does not match value of {}", validator.getValidator(), value);
            throw new InvalidProfileException("invalid_".concat(keyName.toLowerCase()));
        }
    }

    private void validateEntryFields(Iterator<Map.Entry<String, JsonNode>> input, JsonNode schemaFields) {
        while (input.hasNext()) {
            Map.Entry<String, JsonNode> entry = input.next();
            log.debug("started to validate field {}", entry.getKey());
            JsonNode schemaField = schemaFields.get(entry.getKey());

            if (schemaField == null) {
                log.error("No field found in the schema with this field name : {}", entry.getKey());
                throw new InvalidProfileException(ErrorConstants.UNKNOWN_FIELD);
            }

            if(!schemaField.hasNonNull("validators"))
                continue;

            SchemaFieldValidator[] validators = objectMapper.convertValue(schemaField.get("validators"), SchemaFieldValidator[].class);
            if(validators == null || validators.length == 0) continue;

            String datatype = schemaField.get("type") == null ? schemaField.get("$ref").textValue() : schemaField.get("type").textValue();
            switch (datatype) {
                case "string" :
                    validateValue(entry.getKey(), validators[0], entry.getValue().textValue());
                    break;
                case "#/definitions/simpleType":
                    SimpleType[] values = objectMapper.convertValue(entry.getValue(), SimpleType[].class);
                    Optional<SimpleType> mandatoryLangValue = Arrays.stream(values).filter( v -> mandatoryLanguages.contains(v.getLanguage())).findFirst();
                    if(mandatoryLangValue.isEmpty())
                        throw new InvalidProfileException(ErrorConstants.INVALID_LANGUAGE);

                    for(SimpleType value : values) {
                        validateLanguage(value.getLanguage());
                        Optional<SchemaFieldValidator> result = Arrays.stream(validators)
                                .filter(v-> value.getLanguage().equals(v.getLangCode())).findFirst();
                        if(result.isEmpty()) {
                            result = Arrays.stream(validators).filter(v-> v.getLangCode() == null).findFirst();
                        }
                        result.ifPresent(schemaFieldValidator -> validateValue(entry.getKey(), schemaFieldValidator, value.getValue()));
                    }
                    break;
                default:
                    log.error("Unhandled datatype found : {}", datatype);
            }
        }
    }

    private void validateLanguage(String language) {
        if(!mandatoryLanguages.contains(language) && (optionalLanguages != null && !optionalLanguages.contains(language)))
            throw new InvalidProfileException(ErrorConstants.INVALID_LANGUAGE);
    }

    private ArrayNode buildDocuments(JsonNode inputJson) {
        ArrayNode documents = objectMapper.createArrayNode();
        if (!inputJson.path(biometricDataFieldName).path("value").isMissingNode()) {
            String base64FaceImage = inputJson.path(biometricDataFieldName).path("value").textValue();
            String base64BirXmlEncoded = null;
            try {
                base64BirXmlEncoded = biometricUtil.convertBase64JpegToBase64BirXML(base64FaceImage);
            } catch (Exception e) {
                log.error("Failed to create cbeff from face image: ", e);
                throw new ProfileException(INVALID_INDIVIDUAL_BIOMETRICS);
            }
            ((ObjectNode) inputJson).set(biometricDataFieldName, objectMapper.valueToTree(Map.ofEntries(
                    Map.entry("format", "cbeff"),
                    Map.entry("version", 1),
                    Map.entry("value", "fileReferenceID")
            )));
            documents.add(objectMapper.createObjectNode()
                    .put("category", biometricDataFieldName)
                    .put("value", base64BirXmlEncoded)
            );
        }
        if(documents.isEmpty()) return null;
        return documents;
    }

}
