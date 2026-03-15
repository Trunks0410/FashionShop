package com.oneshop.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.ShippingCompany;
import com.oneshop.entity.ShippingRule;
import com.oneshop.service.ShippingCompanyService;

@Controller
@RequestMapping("/admin")
public class AdminShippingCompanyController {

	@Autowired
	private ShippingCompanyService shippingCompanyService;

	@GetMapping("/shipping_company")
	public String listShippingCompanies(Model model) {
		List<ShippingCompany> shippingCompanies = shippingCompanyService.findAll();
		model.addAttribute("shippingCompanies", shippingCompanies);
		if (!model.containsAttribute("newShippingCompany")) {
			ShippingCompany newShippingCompany = new ShippingCompany();
			newShippingCompany.setShippingId(null); 
			newShippingCompany.getRules().add(new ShippingRule());
			model.addAttribute("newShippingCompany", newShippingCompany);
		}
		if (!model.containsAttribute("shippingCompanyToEdit")) {
			ShippingCompany emptyCompany = new ShippingCompany();
			emptyCompany.setShippingId(null);
			emptyCompany.getRules().add(new ShippingRule());
			model.addAttribute("shippingCompanyToEdit", emptyCompany);
			model.addAttribute("openEditModal", false);
		}

		return "admin/shipping_company";
	}

	// Phương thức xử lý việc Thêm mới Nhà vận chuyển và các Quy tắc đi kèm
	@PostMapping("/shipping-companies/save")
	public String saveShippingCompany(@ModelAttribute("newShippingCompany") ShippingCompany company,
			RedirectAttributes redirectAttributes) {
		try {
			List<ShippingRule> rules = company.getRules();
			rules.removeIf(rule -> rule.getRuleName() == null || rule.getRuleName().trim().isEmpty());
			for (ShippingRule rule : rules) {
				rule.setCompany(company);
			}

			shippingCompanyService.save(company);

			redirectAttributes.addFlashAttribute("successMessage", "Thêm nhà vận chuyển và quy tắc thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm nhà vận chuyển: " + e.getMessage());
		}

		return "redirect:/admin/shipping_company";
	}

	@PostMapping("/shipping-companies/{id}/toggle-status")
	public String toggleStatus(@PathVariable("id") Long companyId, @RequestParam("isActiveNew") Boolean isActiveNew,
			RedirectAttributes redirectAttributes) {
		try {
			ShippingCompany company = shippingCompanyService.findById(companyId);
			if (company == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhà vận chuyển ID: " + companyId);
				return "redirect:/admin/shipping_company";
			}

			company.setIsActive(isActiveNew);
			shippingCompanyService.save(company);

			String message = isActiveNew ? "Kích hoạt" : "Vô hiệu hóa";
			redirectAttributes.addFlashAttribute("successMessage", message + " nhà vận chuyển thành công!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
		}

		return "redirect:/admin/shipping_company";
	}

	// Phương thức xử lý việc Xem chi tiết Quy tắc của Nhà vận chuyển
	@GetMapping("/shipping-companies/{id}/rules")
	public String listShippingRules(@PathVariable("id") Long companyId, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			ShippingCompany company = shippingCompanyService.findById(companyId);
			if (company == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhà vận chuyển ID: " + companyId);
				return "redirect:/admin/shipping_company";
			}

			model.addAttribute("company", company);
			model.addAttribute("shippingRules", company.getRules());

			return "admin/shipping_rules_detail"; 
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xem chi tiết quy tắc: " + e.getMessage());
			return "redirect:/admin/shipping_company";
		}

	}

	// Xử lý hiển thị form Chỉnh sửa
	@GetMapping("/shipping-companies/{id}/edit")
	public String showEditForm(@PathVariable("id") Long companyId, RedirectAttributes redirectAttributes) {
		try {
			ShippingCompany companyToEdit = shippingCompanyService.findById(companyId);
			if (companyToEdit == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhà vận chuyển ID: " + companyId);
				return "redirect:/admin/shipping_company";
			}
			companyToEdit.getRules().size();
			redirectAttributes.addFlashAttribute("shippingCompanyToEdit", companyToEdit);
			redirectAttributes.addFlashAttribute("openEditModal", true); 

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chuẩn bị chỉnh sửa: " + e.getMessage());
		}

		return "redirect:/admin/shipping_company";
	}

	//Xử lý việc Cập nhật Nhà vận chuyển và các Quy tắc đi kèm
	@PostMapping("/shipping-companies/update")
	public String updateShippingCompany(@ModelAttribute("shippingCompanyToEdit") ShippingCompany company,
			RedirectAttributes redirectAttributes) {
		try {
			ShippingCompany existingCompany = shippingCompanyService.findById(company.getShippingId());
			if (existingCompany == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy nhà vận chuyển để cập nhật.");
				return "redirect:/admin/shipping_company";
			}
			existingCompany.setName(company.getName());
			existingCompany.setPhone(company.getPhone());
			existingCompany.getRules().clear();
			List<ShippingRule> newRules = company.getRules();
			newRules.removeIf(rule -> rule.getRuleName() == null || rule.getRuleName().trim().isEmpty());

			for (ShippingRule rule : newRules) {
				if (rule.getRuleId() != null && rule.getRuleId() == 0) {
					rule.setRuleId(null);
				}
				rule.setCompany(existingCompany);
				existingCompany.getRules().add(rule);
			}

			shippingCompanyService.save(existingCompany);

			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhà vận chuyển và quy tắc thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật nhà vận chuyển: " + e.getMessage());
		}

		return "redirect:/admin/shipping_company";
	}

}