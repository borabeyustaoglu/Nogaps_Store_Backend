package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.category.CategoryListDto;
import org.example.common.dto.category.CategorySpecDefinitionDto;
import org.example.common.dto.category.CategorySpecFieldDto;
import org.example.common.dto.category.CategoryRequest;
import org.example.common.entity.ProductCategory;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.mapper.CategoryMapper;
import org.example.common.spec.ProductSpecDefinitions;
import org.example.data.repository.ProductCategoryRepository;
import org.example.data.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<CategoryListDto> listAll() {
        return productCategoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(categoryMapper::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySpecDefinitionDto> listCategorySpecs() {
        return ProductSpecDefinitions.all().entrySet().stream()
                .map(entry -> new CategorySpecDefinitionDto(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(field -> new CategorySpecFieldDto(field.key(), field.label(), field.options()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public MessageResponse create(CategoryRequest request) {
        ProductCategory category = new ProductCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        productCategoryRepository.save(category);
        auditLogService.log("CATEGORY_CREATE", "CATEGORY", category.getId(), "Category created: " + category.getName());
        return new MessageResponse("Kategori oluşturuldu.");
    }

    @Transactional
    public MessageResponse update(Integer categoryId, CategoryRequest request) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        productCategoryRepository.save(category);
        auditLogService.log("CATEGORY_UPDATE", "CATEGORY", category.getId(), "Category updated: " + category.getName());
        return new MessageResponse("Kategori güncellendi.");
    }

    @Transactional
    public MessageResponse delete(Integer categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if (productRepository.existsByCategoryId(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        productCategoryRepository.delete(category);
        auditLogService.log("CATEGORY_DELETE", "CATEGORY", categoryId, "Category hard deleted: " + category.getName());
        return new MessageResponse("Kategori silindi.");
    }
}
