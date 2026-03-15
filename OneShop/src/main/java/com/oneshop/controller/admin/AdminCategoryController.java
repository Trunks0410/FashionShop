package com.oneshop.controller.admin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oneshop.entity.Category;
import com.oneshop.service.CategoryService;
import com.oneshop.service.ProductService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminCategoryController {

	private static final Logger logger = LoggerFactory.getLogger(AdminCategoryController.class);

	private static final Long UNCATEGORIZED_CATEGORY_ID = 1L;

	public static class CategoryNode {
		private final Category category;
		private final int level;
		public CategoryNode(Category category, int level) {
			this.category = category;
			this.level = level;
		}
		public Category getCategory() { return category; }
		public int getLevel() { return level; }
	}

	@Autowired
	private CategoryService categoryService;
	@Autowired
	private ProductService productService;

	@GetMapping("/categories")
	public String listCategories(Model model) {
		model.addAttribute("currentPage", "admin-categories");
		logger.debug("Accessing categories management page.");
		try {
			List<Category> allCategories = categoryService.findAll();
			List<CategoryNode> hierarchicalCategories = buildCategoryTree(allCategories);
			model.addAttribute("categoryNodes", hierarchicalCategories);
			model.addAttribute("allCategories", allCategories);

			if (!model.containsAttribute("newCategory")) {
				model.addAttribute("newCategory", new Category());
			}
			if (!model.containsAttribute("editCategory")) {
				model.addAttribute("editCategory", new Category());
			}
			if (!model.containsAttribute("categoryToDelete")) {
				model.addAttribute("categoryToDelete", new Category());
			}
			if (!model.containsAttribute("replacementCategories")) {
				model.addAttribute("replacementCategories", new ArrayList<Category>());
			}
		} catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách danh mục.");
            model.addAttribute("categoryNodes", List.of());
            model.addAttribute("allCategories", List.of());
        }
		return "admin/categories";
	}

	private List<CategoryNode> buildCategoryTree(List<Category> allCategories) {
		List<CategoryNode> result = new ArrayList<>();
		List<Category> rootCategories = allCategories.stream().filter(cat -> cat.getParentCategory() == null)
				.sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
				.collect(Collectors.toList());
		for (Category root : rootCategories) {
			traverseCategoryTree(root, allCategories, result, 0);
		}
		return result;
	}

	private void traverseCategoryTree(Category current, List<Category> allCategories, List<CategoryNode> result,
			int level) {
		result.add(new CategoryNode(current, level));
		List<Category> children = allCategories.stream()
				.filter(cat -> cat.getParentCategory() != null
						&& cat.getParentCategory().getId().equals(current.getId()))
				.sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
				.collect(Collectors.toList());
		for (Category child : children) {
			traverseCategoryTree(child, allCategories, result, level + 1);
		}
	}

	@GetMapping("/categories/{id}/edit")
	public String editCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		logger.debug("Preparing to edit category ID: {}", id);
		try {
			Category categoryToEdit = categoryService.findById(id).orElse(null);
			if (categoryToEdit == null) {
				logger.warn("Category ID {} not found for editing.", id);
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để sửa.");
				return "redirect:/admin/categories";
			}

			List<Category> allCategories = categoryService.findAll();
			List<Category> eligibleParentCategories = getEligibleParentCategories(categoryToEdit, allCategories);

			redirectAttributes.addFlashAttribute("editCategory", categoryToEdit);
			redirectAttributes.addFlashAttribute("eligibleParentCategories", eligibleParentCategories);
			redirectAttributes.addFlashAttribute("isEditing", true);

		} catch (Exception e) {
			logger.error("Error preparing edit category form for ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị sửa danh mục.");
		}
		return "redirect:/admin/categories";
	}

	private List<Category> getEligibleParentCategories(Category categoryToExclude, List<Category> allCategories) {
		List<Category> descendants = getDescendants(categoryToExclude, allCategories);
		List<Long> excludedIds = descendants.stream().map(Category::getId).collect(Collectors.toList());
		excludedIds.add(categoryToExclude.getId());
		return allCategories.stream().filter(cat -> !excludedIds.contains(cat.getId()))
				.collect(Collectors.toList());
	}

	private List<Category> getDescendants(Category category, List<Category> allCategories) {
		List<Category> descendants = new ArrayList<>();
		List<Category> directChildren = allCategories.stream()
				.filter(cat -> cat.getParentCategory() != null
						&& cat.getParentCategory().getId().equals(category.getId()))
				.collect(Collectors.toList());
		for (Category child : directChildren) {
			descendants.add(child);
			descendants.addAll(getDescendants(child, allCategories));
		}
		return descendants;
	}

	@GetMapping("/categories/{id}/delete")
	public String prepareDeleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		logger.warn("Preparing to delete category ID: {}", id);
		try {
			List<Category> allCategories = categoryService.findAll();
			Category categoryToDelete = categoryService.findById(id).orElse(null);

			if (categoryToDelete == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để xóa.");
				return "redirect:/admin/categories";
			}

			long childCount = countDirectChildren(categoryToDelete, allCategories);
            long productCount = productService.countProductsByCategory(id);

			if (childCount > 0 || productCount > 0) {
				List<Category> replacementCategories = getEligibleParentCategories(categoryToDelete, allCategories);
				redirectAttributes.addFlashAttribute("categoryToDelete", categoryToDelete);
				redirectAttributes.addFlashAttribute("replacementCategories", replacementCategories);
				redirectAttributes.addFlashAttribute("isDeleting", true);
				logger.debug("Category {} has {} children or {} products. Showing replacement modal.", id, childCount, productCount);
				return "redirect:/admin/categories";
			} else {
				logger.debug("Category {} has no children or products. Deleting directly.", id);
				categoryService.deleteById(id);
				redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục **" + categoryToDelete.getName() + "** thành công!");
			}
		} catch (Exception e) {
			logger.error("Error preparing delete category for ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị xóa danh mục: " + e.getMessage());
		}
		return "redirect:/admin/categories";
	}

	private long countDirectChildren(Category category, List<Category> allCategories) {
		return allCategories.stream().filter(cat -> cat.getParentCategory() != null
				&& cat.getParentCategory().getId().equals(category.getId())).count();
	}

	@PostMapping("/categories/delete")
	public String performDeleteCategory(@RequestParam("categoryId") Long categoryId,
			@RequestParam(value = "replacementId", required = true) Long replacementId,
			RedirectAttributes redirectAttributes) {
		logger.warn("Performing delete for category ID: {}, moving children/products to {}", categoryId, replacementId);

		Category categoryToDelete = categoryService.findById(categoryId).orElse(null);
		if (categoryToDelete == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để xóa.");
			return "redirect:/admin/categories";
		}

		Category replacementCategory = null;
		String replacementName;
		Long finalReplacementId = replacementId;

        if (!replacementId.equals(UNCATEGORIZED_CATEGORY_ID)) {
             replacementCategory = categoryService.findById(replacementId).orElse(null);
             if (replacementCategory == null) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Danh mục thay thế không hợp lệ.");
                 return "redirect:/admin/categories";
             }
             replacementName = replacementCategory.getName();
        } else {
            replacementName = "Chưa Phân Loại";
        }

		try {
			List<Category> allCategories = categoryService.findAll();

			List<Category> directChildren = allCategories.stream()
					.filter(cat -> cat.getParentCategory() != null
							&& cat.getParentCategory().getId().equals(categoryId))
					.collect(Collectors.toList());

			if (!directChildren.isEmpty()) {
				for (Category child : directChildren) {
					child.setParentCategory(replacementId.equals(UNCATEGORIZED_CATEGORY_ID) ? null : replacementCategory);
					categoryService.saveCategory(child);
				}
				redirectAttributes.addFlashAttribute("infoMessage",
						"Đã chuyển **" + directChildren.size() + "** danh mục con sang **" + replacementName + "**.");
			}

			int productsMoved = productService.updateCategoryForProducts(categoryId, finalReplacementId);
			if (productsMoved > 0) {
				redirectAttributes.addFlashAttribute("infoMessage2",
						"Đã chuyển **" + productsMoved + "** sản phẩm sang **" + replacementName + "**.");
			}

			categoryService.deleteById(categoryId);
			redirectAttributes.addFlashAttribute("successMessage",
					"Xóa danh mục **" + categoryToDelete.getName() + "** và chuyển mục con/sản phẩm thành công!");

		} catch (Exception e) {
			logger.error("Error performing delete for category ID {}: {}", categoryId, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Lỗi khi xóa danh mục **" + categoryToDelete.getName() + "**: " + e.getMessage());
		}

		return "redirect:/admin/categories";
	}

	@PostMapping("/categories/save")
	public String saveCategory(@Valid @ModelAttribute("newCategory") Category category,
                               BindingResult bindingResult,
			                   RedirectAttributes redirectAttributes) {
		logger.info("Attempting to save category. ID: {}, Name: {}", category.getId(), category.getName());
		boolean isNew = category.getId() == null;

        Long parentId = null;
		if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
            if (category.getId() != null && category.getId().equals(category.getParentCategory().getId())) {
                 bindingResult.rejectValue("parentCategory.id", "SelfParent", "Không thể chọn chính nó làm danh mục cha.");
            } else {
			    parentId = category.getParentCategory().getId();
            }
		}

        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors saving category: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult." + (isNew ? "newCategory" : "editCategory"), bindingResult);
            redirectAttributes.addFlashAttribute(isNew ? "newCategory" : "editCategory", category);
            redirectAttributes.addFlashAttribute(isNew ? "isAddingWithError" : "isEditingWithError", true);
            if (!isNew) {
                try {
                    List<Category> all = categoryService.findAll();
                    redirectAttributes.addFlashAttribute("eligibleParentCategories", getEligibleParentCategories(category, all));
                } catch (Exception e) { logger.error("Error reloading eligible parents on save error: {}", e.getMessage()); }
            }
            return "redirect:/admin/categories";
        }

		try {
			if (parentId != null) {
				Category parent = categoryService.findById(parentId)
                        .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại."));
				category.setParentCategory(parent);
			} else {
				category.setParentCategory(null);
			}

			categoryService.saveCategory(category);

			String message = isNew ? "Thêm danh mục **" + category.getName() + "** thành công!"
					: "Cập nhật danh mục **" + category.getName() + "** thành công!";
			redirectAttributes.addFlashAttribute("successMessage", message);

		} catch (IllegalArgumentException e) {
            logger.warn("Error saving category: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute(isNew ? "newCategory" : "editCategory", category);
            redirectAttributes.addFlashAttribute(isNew ? "isAddingWithError" : "isEditingWithError", true);
             if (!isNew) {
                try {
                    List<Category> all = categoryService.findAll();
                    redirectAttributes.addFlashAttribute("eligibleParentCategories", getEligibleParentCategories(category, all));
                } catch (Exception ex) { logger.error("Error reloading eligible parents on save error: {}", ex.getMessage()); }
            }
        } catch (Exception e) {
			logger.error("Unexpected error saving category: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu danh mục: " + e.getMessage());
            redirectAttributes.addFlashAttribute(isNew ? "newCategory" : "editCategory", category);
             redirectAttributes.addFlashAttribute(isNew ? "isAddingWithError" : "isEditingWithError", true);
             if (!isNew) {
                try {
                    List<Category> all = categoryService.findAll();
                    redirectAttributes.addFlashAttribute("eligibleParentCategories", getEligibleParentCategories(category, all));
                } catch (Exception ex) { logger.error("Error reloading eligible parents on save error: {}", ex.getMessage()); }
            }
		}
		return "redirect:/admin/categories";
	}
}