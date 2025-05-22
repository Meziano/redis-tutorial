package de.meziane.redis.service;

import de.meziane.redis.dto.ProductDto;
import de.meziane.redis.entity.Product;
import de.meziane.redis.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    public static final String PRODUCT_CACHE = "products";

    @CachePut(value = PRODUCT_CACHE, key = "#result.id()")
    public ProductDto createProduct(@Valid ProductDto productDto) {
        var product = new Product();
        product.setName(productDto.name());
        product.setPrice(productDto.price());
        Product savedProduct =productRepository.save(product);
        ProductDto pDto = new ProductDto(savedProduct.getId(), savedProduct.getName(), savedProduct.getPrice());;
        return pDto;
    }

    @CachePut(value = PRODUCT_CACHE, key = "#result.id()")
    public ProductDto updateProduct(@Valid ProductDto productDto) {
        Product product = productRepository.findById(productDto.id())
                .orElseThrow(() -> new IllegalArgumentException(String.format("No Product with id %d found.", productDto.id())));
        product.setName(productDto.name());
        product.setPrice(productDto.price());
        Product updatedProduct = productRepository.save(product);
        ProductDto  pDto= new ProductDto(updatedProduct.getId(),updatedProduct.getName(), updatedProduct.getPrice());
        return pDto;
    }

    @CacheEvict(value = PRODUCT_CACHE, key = "#productId")
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    @Cacheable(value = PRODUCT_CACHE, key = "#productId")
    public ProductDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("No Product with id %d found.", productId)));
        return new ProductDto(product.getId(), product.getName(),
                product.getPrice());
    }
}
