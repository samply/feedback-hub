package com.samply.feedbackhub;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
import com.samply.feedbackhub.model.DoiData;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
class FeedbackHubApplicationTests {

	@Test
	void contextLoads() {
	}
	@Test
	void encryptAndDecryptDOI() {
		String doi = "sample.doi.com";
		Key key = Key.generateKey();
		String serkey = key.serialise();
		key = new Key(serkey);
		final Token token = Token.generate(key, doi);

		final Validator<String> validator = new StringValidator() {
		};
		assertEquals(token.validateAndDecrypt(key, validator), doi);
	}
	@Test
	void encryptDoiDataReference() {
		String doi = "sample.doi.com";
		final Key key = Key.generateKey();
		DoiData doiData = new DoiData("req1", doi, key.serialise(), UUID.randomUUID().toString());

		final Validator<String> validator = new StringValidator() {
		};
		Token token = Token.fromString(doiData.getPublicationReferenceToken());
		assertEquals(token.validateAndDecrypt(key, validator), doi);
	}
}
