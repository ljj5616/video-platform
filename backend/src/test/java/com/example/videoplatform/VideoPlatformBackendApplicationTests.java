package com.example.videoplatform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long")
class VideoPlatformBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
