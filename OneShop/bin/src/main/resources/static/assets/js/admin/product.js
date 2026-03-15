
document.addEventListener('DOMContentLoaded', function() {
	var editModalElement = document.getElementById('editProductModal');

	if (editModalElement) {
		var modalInstance = new bootstrap.Modal(editModalElement);

		editModalElement.addEventListener('show.bs.modal', function(event) {

			try {
				var button = event.relatedTarget;
				var productId = button.getAttribute('data-product-id');
				var productName = button.getAttribute('data-product-name');
				var productCategory = button.getAttribute('data-product-category');
				var productDescription = button.getAttribute('data-product-description');
				var modalTitle = editModalElement.querySelector('.modal-title');
				var modalProductId = editModalElement.querySelector('#modalProductId');
				var modalProductName = editModalElement.querySelector('#modalProductName');
				var modalProductCategory = editModalElement.querySelector('#modalProductCategory');
				var modalProductDescription = editModalElement.querySelector('#modalProductDescription');

				if (modalTitle) modalTitle.textContent = 'Chỉnh Sửa Sản Phẩm ID: ' + productId;
				if (modalProductId) modalProductId.value = productId;
				if (modalProductName) modalProductName.value = productName;
				if (modalProductCategory) modalProductCategory.value = productCategory;
				if (modalProductDescription) modalProductDescription.value = productDescription;
				setTimeout(function() {
					var currentModalInstance = bootstrap.Modal.getInstance(editModalElement);
					if (currentModalInstance) {
						currentModalInstance.handleUpdate();
					} else {
						modalInstance.handleUpdate();
					}
				}, 100);

			} catch (error) {
				console.error("LỖI XỬ LÝ DỮ LIỆU MODAL:", error);
			}
		});
	}
});