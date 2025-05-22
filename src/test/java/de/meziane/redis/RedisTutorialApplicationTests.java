package de.meziane.redis;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.meziane.redis.dto.ProductDto;
import de.meziane.redis.entity.Product;
import de.meziane.redis.repository.ProductRepository;
import de.meziane.redis.service.ProductService;
import org.junit.jupiter.api.Assertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;


import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

@Slf4j
//@RequiredArgsConstructor
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisTutorialApplicationTests {

//	@Container
//	private static final RedisContainer REDIS_CONTAINER =
//			new RedisContainer( new DockerImageName("redis:alpine"));
//			new RedisContainer( DockerImageName.parse("redis:alpine")).withExposedPorts(6379);

//	static {
//		GenericContainer<?> redis =
//				new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
//		redis.start();
//		log.info("spring.redis.host:{}", redis.getHost());
//		log.info("spring.redis.port:{}", redis.getMappedPort(6379).toString());
//	}


//	@Container
//	public  GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
//			.withExposedPorts(6379);

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private ProductRepository productRepositorySpy;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CacheManager cacheManager;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static GenericContainer<?> redis;

	@BeforeAll
	static void beforeAll() {
		redis = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
		redis.start();
		/*System.setProperty("spring.data.redis.host", redis.getHost());
		System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());*/
	}

	@BeforeEach
	public void setForEach() {
		productRepository.deleteAll();
	}

	@Test
	void givenRedisContainerConfiguredWithDynamicProperties_whenCheckingRunningStatus_thenStatusIsRunning() {
		assertTrue(redis.isRunning());
	}

	@DynamicPropertySource
	private static void registerRedisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
	}



	@Test
	void testCreateProductAndCacheIt() throws Exception {
		ProductDto productDto = new ProductDto(null, "Laptop", BigDecimal.valueOf(1200L));

		// Step 1: Create a Product
		MvcResult result = mockMvc.perform(post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(productDto)))
				.andExpect(status().isCreated())
				.andReturn();

		ProductDto createdProduct = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
		Long productId = createdProduct.id();
		// Step 2: Check Product Exists in DB
		assertTrue(productRepository.findById(productId).isPresent());
		// Step 3: Check Cache
		Cache cache = cacheManager.getCache(ProductService.PRODUCT_CACHE);
		assertNotNull(cache);
		assertNotNull(cache.get(productId, ProductDto.class));
	}

	@Test
	void testUpdateProductAndCacheIt() throws Exception {
		// Step 1: Create a Product
		Product product = new Product();
		product.setName("Tablet");
		product.setPrice(BigDecimal.valueOf(500L));
		product = productRepository.save(product);

		ProductDto updatedProductDto = new ProductDto(product.getId(), "Updated Tablet", BigDecimal.valueOf(550L));

		// Step 2: Update Product
		mockMvc.perform(MockMvcRequestBuilders.put("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedProductDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Tablet"))
				.andExpect(jsonPath("$.price").value(550.0));

		// Step 3: Verify Cache is Updated
		Cache cache = cacheManager.getCache(ProductService.PRODUCT_CACHE);
		assertNotNull(cache);
		ProductDto cachedProduct = cache.get(product.getId(), ProductDto.class);
		assertNotNull(cachedProduct);
		Assertions.assertEquals("Updated Tablet", cachedProduct.name());
	}

	@Test
	void testGetProductAndVerifyCache() throws Exception {
		// Step 1: Save product in DB
		Product product = new Product();
		product.setName("Phone");
		product.setPrice(BigDecimal.valueOf(800L));
		productRepository.save(product);

		// Step 2: Fetch product
		mockMvc.perform(MockMvcRequestBuilders.get("/api/product/" + product.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Phone"));

		Mockito.verify(productRepositorySpy, Mockito.times(1)).findById(product.getId());

		Mockito.clearInvocations(productRepositorySpy);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/product/" + product.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Phone"));

		Mockito.verify(productRepositorySpy, Mockito.times(0)).findById(product.getId());
	}

	@Test
	void testDeleteProductAndVerifyCache() throws Exception {
		// Step 1: Save product in DB
		Product product = new Product();
		product.setName("Smartwatch");
		product.setPrice(BigDecimal.valueOf(250L));
		productRepository.save(product);

		// Step 2: Fetch product
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/product/" + product.getId()))
				.andExpect(status().isNoContent());

		// Step 3: Check that Product is Deleted from DB
		Assertions.assertFalse(productRepository.findById(product.getId()).isPresent());

		// Step 4: Check Cache Eviction
		Cache cache = cacheManager.getCache(ProductService.PRODUCT_CACHE);
		assertNotNull(cache);
		Assertions.assertNull(cache.get(product.getId()));
	}


}
