$(document).ready(function() {
	$('#editCategoryModal').on('hidden.bs.modal', function() {
		if (window.location.pathname.includes('/edit')) {
			window.location.href = '/admin/categories';
		}
	});
	$('#deleteCategoryModal').on('hidden.bs.modal', function() {
	});

});

function handleCategoryDelete(element) {

	var categoryName = element.getAttribute('data-category-name');
	var deleteUrl = element.getAttribute('data-delete-url');
	var confirmMessage = 'Bạn có chắc chắn muốn xóa danh mục [' + categoryName + '] không? (Nếu danh mục này có danh mục con, bạn sẽ được yêu cầu chọn danh mục thay thế.)';

	if (confirm(confirmMessage)) {
		window.location.href = deleteUrl;
		return true;
	}
	return false;
}