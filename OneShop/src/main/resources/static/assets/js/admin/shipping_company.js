
$(document).ready(function() {

	if (typeof openEditModal !== 'undefined' && openEditModal) {
		const editModal = new bootstrap.Modal(document.getElementById('editShippingCompanyModal'));
		editModal.show();
	}
	function reIndexRules(containerSelector, entryClass, idPrefix) {
		$(containerSelector).children('.' + entryClass).each(function(newIndex) {
			const $ruleEntry = $(this);
			$ruleEntry.attr('data-index', newIndex);
			$ruleEntry.find('h4').text('Quy tắc #' + newIndex);

			$ruleEntry.find('[name]').each(function() {
				const $input = $(this);
				const oldName = $input.attr('name');
				if (oldName) {
					const newName = oldName.replace(/rules\[\d+\]/g, 'rules[' + newIndex + ']');
					$input.attr('name', newName);
				}
				if ($input.is(':checkbox')) {
					const newId = idPrefix + newIndex;
					$input.attr('id', newId);
					$ruleEntry.find('label[for^="' + idPrefix + '"]').attr('for', newId);
				}
			});
		});
	}
	const $editRulesContainer = $('#editRulesContainer');
	const editRuleTemplate = $("#ruleTemplate").html().replace(/rule-entry/g, 'rule-entry-edit').replace(/isExpress_/g, 'isExpressEdit_');
	$('#btnAddRuleEdit').on('click', function() {
		const index = $editRulesContainer.children('.rule-entry-edit').length;
		let template = editRuleTemplate.replace(/{INDEX}/g, index);
		$editRulesContainer.append(template);
		$editRulesContainer.find('.rule-entry-edit').last().find('input[name*="ruleId"]').val('');

		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});
	$editRulesContainer.on('click', '.btnRemoveRuleEdit', function() {
		$(this).closest('.rule-entry-edit').remove();
		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});
	const $rulesContainer = $('#rulesContainer');
	$('#btnAddRule').on('click', function() {
		const index = $rulesContainer.children('.rule-entry').length;
		let template = $('#ruleTemplate').html().replace(/{INDEX}/g, index);
		$rulesContainer.append(template);

		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_');
	});
	$rulesContainer.on('click', '.btnRemoveRule', function() {
		$(this).closest('.rule-entry').remove();
		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_'); 
	});
	$('.traditional-status-toggle').on('change', function() {
		const switchElement = $(this);
		const companyId = switchElement.data('id');
		const currentActive = switchElement.data('current-active');
		const action = currentActive ? 'VÔ HIỆU HÓA' : 'KÍCH HOẠT'; 
		const confirmMessage = `Bạn có chắc chắn muốn ${action} nhà vận chuyển ID: ${companyId}?`;
		if (confirm(confirmMessage)) {
			const formId = '#form-' + companyId;
			$(formId).submit();

		} else {
			switchElement.prop('checked', currentActive);
		}
	});
});