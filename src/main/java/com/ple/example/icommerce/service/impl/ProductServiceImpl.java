package com.ple.example.icommerce.service.impl;

import com.ple.example.icommerce.dao.PriceHistoryRepository;
import com.ple.example.icommerce.dao.ProductRepository;
import com.ple.example.icommerce.dto.ProductFilter;
import com.ple.example.icommerce.dto.ProductRequest;
import com.ple.example.icommerce.entity.PriceHistory;
import com.ple.example.icommerce.entity.Product;
import com.ple.example.icommerce.exp.CommerceBadRequestException;
import com.ple.example.icommerce.service.ProductService;
import com.ple.example.icommerce.spec.ProductSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;


    @Override
    public Product create(ProductRequest productRequest) {
        validateSku(productRequest.getSku());

        Product product = new Product();
        BeanUtils.copyProperties(productRequest, product);
        product = productRepository.save(product);

        keepTrackPriceChange(product);

        return product;
    }

    @Override
    public Optional<Product> get(Long key) {
        return productRepository.findById(key);
    }

    @Override
    public Optional<Product> update(Long key, ProductRequest productRequest) {
        Optional<Product> productOpt = get(key);
        if (!productOpt.isPresent()) {
            log.debug("Product is not found: #key: {}", key);
            return productOpt;
        }

        Product product = productOpt.get();
        if (!productRequest.getSku().equalsIgnoreCase(product.getSku())) {
            validateSku(productRequest.getSku());
        }

        boolean hasChangePrice = productRequest.getPrice().compareTo(product.getPrice()) != 0;

        BeanUtils.copyProperties(productRequest, product);
        product = productRepository.save(product);

        if (hasChangePrice) {
            keepTrackPriceChange(product);
        }

        return Optional.of(product);
    }

    @Override
    public Page<Product> search(ProductFilter productFilter) {
        Specification specs = Specification
                .where(ProductSpecifications.skuLike(productFilter.getSku()))
                .and(ProductSpecifications.nameLike(productFilter.getName()))
                .and(ProductSpecifications.quantityInRange(productFilter.getMinQuantity(), productFilter.getMaxQuantity()))
                .and(ProductSpecifications.priceInRange(productFilter.getMinPrice(), productFilter.getMaxPrice()));

        return productRepository.findAll(specs, productFilter.getPagingSort());
    }

    private void keepTrackPriceChange(Product product) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setPrice(product.getPrice());
        priceHistory.setProduct(product);
        priceHistoryRepository.save(priceHistory);
    }

    private void validateSku(String sku) {
        Product foundBySku = productRepository.findBySku(sku);
        if (foundBySku != null) {
            throw new CommerceBadRequestException(CommerceBadRequestException.PRODUCT_SKU_IS_EXISTING);
        }
    }

}
