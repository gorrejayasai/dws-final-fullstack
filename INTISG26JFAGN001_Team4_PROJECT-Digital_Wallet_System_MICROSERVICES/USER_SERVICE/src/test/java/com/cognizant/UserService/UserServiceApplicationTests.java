package com.cognizant.UserService;

import com.cognizant.UserService.config.AdminSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class UserServiceApplicationTests {

	@MockitoBean
	private AdminSeeder adminSeeder;

	@Test
	void contextLoads() {
	}

}
