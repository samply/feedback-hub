package com.samply.feedbackhub;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
import com.samply.feedbackhub.model.DoiData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
class FeedbackHubApplicationTests {

	@Test
	void contextLoads() {
	}
	@Test
	void encryptAndDecryptDOI() {
		String doi = "sample.doi.com";
		final Key key = Key.generateKey();
		final Token token = Token.generate(key, doi);

		final Validator<String> validator = new StringValidator() {
		};
		System.out.println(token.validateAndDecrypt(key, validator));
		assertEquals(token.validateAndDecrypt(key, validator), doi);
	}
}
