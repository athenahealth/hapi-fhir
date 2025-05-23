package ca.uhn.hapi.fhir.cdshooks.svc;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.hapi.fhir.cdshooks.api.CDSHooksVersion;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsServiceMethod;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceFeedbackJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.serializer.CdsServiceRequestJsonDeserializer;
import ca.uhn.hapi.fhir.cdshooks.svc.prefetch.CdsPrefetchSvc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsServiceRegistryImplTest {
	private static final String SERVICE_ID = "service-id";
	@Mock
	private CdsHooksContextBooter myCdsHooksContextBooter;
	@Mock
	private CdsPrefetchSvc myCdsPrefetchSvc;
	@Mock
	private CdsServiceCache myCdsServiceCache;
	@Mock
	private CdsServiceRequestJsonDeserializer myCdsServiceRequestJsonDeserializer;
	private final ObjectMapper myObjectMapper = new ObjectMapper();
	private CdsServiceRegistryImpl myFixture;

	@BeforeEach()
	void setup() {
		myFixture = new CdsServiceRegistryImpl(myCdsHooksContextBooter, myCdsPrefetchSvc, myObjectMapper, myCdsServiceRequestJsonDeserializer);
	}

	@Test
	void encodeFeedbackResponseWhenResponseIsString() {
		// setup
		final String expectedCardText = "some-card";
		final String input = """
   			{
   				"card": "some-card"
   			}
			""";
		// execute
		final CdsServiceFeedbackJson actual = myFixture.encodeFeedbackResponse(SERVICE_ID, input);
		// validate
		assertThat(actual.getCard()).isEqualTo(expectedCardText);
	}

	@Test
	void encodeFeedbackResponseWhenResponseIsCdsServiceFeedbackJson() {
		// setup
		final CdsServiceFeedbackJson expected = new CdsServiceFeedbackJson();
		expected.setCard("some-card");
		// execute
		final CdsServiceFeedbackJson actual = myFixture.encodeFeedbackResponse(SERVICE_ID, expected);
		// validate
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void encodeFeedbackResponseWhenResponseIsInvalidString() {
		// setup
		final String invalidString = "some-invalid-feedback";
		// execute & validate
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			myFixture.encodeFeedbackResponse(SERVICE_ID, invalidString);
		}).withMessageContaining("HAPI-2538: Failed to serialize json Cds Feedback response for service service-id.");
	}

	@Test
	void encodeFeedbackResponseWhenResponseIsInvalidObject() {
		// setup
		final CdsServiceResponseJson invalidObject = new CdsServiceResponseJson();
		// execute & validate
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> {
			myFixture.encodeFeedbackResponse(SERVICE_ID, invalidObject);
		}).withMessageContaining("HAPI-2537: Failed to cast feedback response CdsServiceFeedbackJson for service service-id.");
	}

	@Test
	void encodeServiceResponseWhenResponseIsString() throws JsonProcessingException {
		// setup
		final String input = """
   			{
   				"cards": [
   					{
   						"summary": "some-summary",
   						"indicator": "info",
   						"source": {
   						 	"label": "some-label"
   						 }
   					}
   				] 
   			}
			""";
		// execute
		final CdsServiceResponseJson actual = myFixture.encodeServiceResponse(SERVICE_ID, input);
		// validate
		assertThat(actual).usingRecursiveComparison().isEqualTo(myObjectMapper.readValue(input, CdsServiceResponseJson.class));
	}

	@Test
	void encodeServiceResponseWhenResponseCdsServiceResponseJson() throws JsonProcessingException {
		// setup
		final String input = """
   			{
   				"cards": [
   					{
   						"summary": "some-summary",
   						"indicator": "info",
   						"source": {
   						 	"label": "some-label"
   						 }
   					}
   				] 
   			}
			""";
		// execute
		final CdsServiceResponseJson actual = myFixture.encodeServiceResponse(SERVICE_ID, myObjectMapper.readValue(input, CdsServiceResponseJson.class));
		// validate
		assertThat(actual).usingRecursiveComparison().isEqualTo(actual);
	}

	@Test
	void encodeServiceResponseWhenResponseIsInvalidString() {
		// setup
		final String invalidString = "some-string";
		// execute & validate
		assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> {
			myFixture.encodeServiceResponse(SERVICE_ID, invalidString);
		}).withMessageContaining("Failed to json deserialize Cds service response of type java.lang.String when calling CDS Hook Service service-id.");
	}

	@Test
	void encodeServiceResponseWhenResponseIsInvalidObject() {
		// setup
		final CdsServiceFeedbackJson invalidObject = new CdsServiceFeedbackJson();
		// execute & validate
		assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> {
			myFixture.encodeServiceResponse(SERVICE_ID, invalidObject);
		}).withMessageContaining("Failed to cast Cds service response to CdsServiceResponseJson when calling CDS Hook Service service-id. The type ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceFeedbackJson cannot be casted to CdsServiceResponseJson");
	}

	@Test
	void getCdsServiceJsonWhenServicePresent() {
		// setup
		final CdsServiceJson cdsService = new CdsServiceJson();
		cdsService.setId(SERVICE_ID);
		myFixture.setServiceCache(myCdsServiceCache);
		doReturn(cdsService).when(myCdsServiceCache).getCdsServiceJson(SERVICE_ID);
		// execute
		final CdsServiceJson actual = myFixture.getCdsServiceJson(SERVICE_ID);
		// validate
		assertThat(actual).isNotNull();
	}

	@Test
	void getCdsServiceJsonWhenServiceIsNotPresent() {
		// setup
		final String serviceId = "non-existent-serviceid";
		myFixture.setServiceCache(myCdsServiceCache);
		doReturn(null).when(myCdsServiceCache).getCdsServiceJson(serviceId);
		// execute & validate
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			myFixture.getCdsServiceJson(serviceId);
		}).withMessage("HAPI-2536: No service with " + serviceId +  " is registered.");
	}

	@Test
	void testCallService_doesNotEnforceFhirServerIsHttpsForCDSHooksV1() {
		String fhirServer = "http://localhost:8000/remote_fhir";
		String response = "{\"cards\": []}";
		CdsServiceRegistryImpl serviceRegistryImpl = createAndSetupCdsServiceRegistryImplForCallService(CDSHooksVersion.V_1_1, fhirServer, response);
		CdsServiceResponseJson responseJson = serviceRegistryImpl.callService(SERVICE_ID, "");
		assertThat(responseJson).isNotNull();
	}

	@Test
	void testCallService_noValidationIfFhirServerIsNullForCDSHooksV2() {
		String response = "{\"cards\": []}";
		CdsServiceRegistryImpl serviceRegistryImpl = createAndSetupCdsServiceRegistryImplForCallService(CDSHooksVersion.V_2_0, null, response);
		CdsServiceResponseJson responseJson = serviceRegistryImpl.callService(SERVICE_ID, "");
		assertThat(responseJson).isNotNull();
	}

	@Test
	void testCallService_enforcesFhirServerIsHttpsForCDSHooksV2_InvalidInput() {
		String fhirServer = "http://localhost:8000/remote_fhir";
		CdsServiceRegistryImpl serviceRegistryImpl = createAndSetupCdsServiceRegistryImplForCallService(CDSHooksVersion.V_2_0, fhirServer, null);
		InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> serviceRegistryImpl.callService(SERVICE_ID, ""));
		assertThat(ex.getMessage()).isEqualTo("HAPI-2632: The scheme for the fhirServer must be https");
	}

	@Test
	void testCallService_enforcesFhirServerIsHttpsForCDSHooksV2_ValidInput() {
		String fhirServer = "https://localhost:8000/remote_fhir";
		String response = "{\"cards\": []}";
		CdsServiceRegistryImpl serviceRegistryImpl = createAndSetupCdsServiceRegistryImplForCallService(CDSHooksVersion.V_2_0, fhirServer, response);
		CdsServiceResponseJson responseJson = serviceRegistryImpl.callService(SERVICE_ID, "");
		assertThat(responseJson).isNotNull();
	}

	private CdsServiceRegistryImpl createAndSetupCdsServiceRegistryImplForCallService(CDSHooksVersion theCdsHooksVersion, @Nullable String theFhirServer, @Nullable String theSuccessResponse) {
		CdsServiceRegistryImpl serviceRegistryImpl = new CdsServiceRegistryImpl(
			myCdsHooksContextBooter,
			myCdsPrefetchSvc,
			myObjectMapper,
			myCdsServiceRequestJsonDeserializer,
			theCdsHooksVersion);

		final CdsServiceJson cdsService = new CdsServiceJson();
		cdsService.setId(SERVICE_ID);
		serviceRegistryImpl.setServiceCache(myCdsServiceCache);
		doReturn(cdsService).when(myCdsServiceCache).getCdsServiceJson(SERVICE_ID);

		CdsServiceRequestJson requestJson = new CdsServiceRequestJson();
		requestJson.setFhirServer(theFhirServer);
		doReturn(requestJson).when(myCdsServiceRequestJsonDeserializer).deserialize(eq(cdsService), anyString());

		if (theSuccessResponse != null) {
			ICdsServiceMethod theMethodMock= mock(ICdsServiceMethod.class);
			when(theMethodMock.invoke(myObjectMapper, requestJson, SERVICE_ID)).thenReturn(theSuccessResponse);
			when(myCdsServiceCache.getServiceMethod(SERVICE_ID)).thenReturn(theMethodMock);
		}


		return serviceRegistryImpl;
	}

}
