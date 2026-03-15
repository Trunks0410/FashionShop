// Đợi cho toàn bộ cây HTML được tải xong rồi mới chạy script
document.addEventListener('DOMContentLoaded', function() {

    // =========================================================================
    // SLIDER LOGIC - Chỉ chạy khi tìm thấy phần tử slider trên trang
    // =========================================================================
    const sliderContainer = document.querySelector('.main__slice');

    if (sliderContainer) {
        // Giả sử các slide của bạn có class là "slide-item"
        const slides = sliderContainer.querySelectorAll(".slide-item"); 
        const prev = sliderContainer.querySelector(".controls .prev");
        const next = sliderContainer.querySelector(".controls .next");
        const indicator = sliderContainer.querySelector(".indicator");
        let index = 0;
        let timer; // Khai báo timer ở đây

        // Chỉ tiếp tục nếu các thành phần cần thiết của slider tồn tại
        if (slides.length > 0 && prev && next && indicator) {

            prev.addEventListener("click", function() {
                prevSlide();
                updateCircleIndicator();
                resetTimer();
            });

            next.addEventListener("click", function() {
                nextSlide();
                updateCircleIndicator();
                resetTimer();
            });

            function circleIndicator() {
                for (let i = 0; i < slides.length; i++) {
                    const div = document.createElement("div");
                    // div.innerHTML = i + 1; // Thường không cần số
                    div.setAttribute("onclick", "indicateSlide(this)");
                    div.id = i;
                    if (i == 0) {
                        div.className = "active";
                    }
                    indicator.appendChild(div);
                }
            }
            circleIndicator();

            window.indicateSlide = function(element) { // Gán vào window để onclick có thể gọi được
                index = parseInt(element.id);
                changeSlide();
                updateCircleIndicator();
                resetTimer();
            }

            function updateCircleIndicator() {
                for (let i = 0; i < indicator.children.length; i++) {
                    indicator.children[i].classList.remove("active");
                }
                indicator.children[index].classList.add("active");
            }

            function prevSlide() {
                index = (index === 0) ? slides.length - 1 : index - 1;
                changeSlide();
            }

            function nextSlide() {
                index = (index === slides.length - 1) ? 0 : index + 1;
                changeSlide();
            }

            function changeSlide() {
                slides.forEach(slide => slide.classList.remove("active"));
                slides[index].classList.add("active");
            }

            function resetTimer() {
                clearInterval(timer);
                timer = setInterval(autoPlay, 4000);
            }

            function autoPlay() {
                nextSlide();
                updateCircleIndicator();
            }

            timer = setInterval(autoPlay, 4000);
        }
    }


    // =========================================================================
    // SCROLL LOGIC (Header & Nút "Lên đầu trang")
    // =========================================================================
    const myHeader = document.querySelector("#myHeader");
    const upTop = document.querySelector("#upTop");

    // Chỉ gán sự kiện scroll nếu các phần tử này tồn tại
    if (myHeader && upTop) {
        window.onscroll = function() {
            // Lấy headerScroll bên trong hàm vì class 'scrolling' có thể bị thay đổi
            const headerScroll = document.querySelector('.header.scrolling');
            
            if (document.body.scrollTop > 400 || document.documentElement.scrollTop > 400) {
                if (headerScroll) {
                    headerScroll.classList.remove('scrolling');
                }
                upTop.style.display = 'block';
            } else {
                myHeader.classList.add('scrolling');
                upTop.style.display = 'none';
            }
        };
    }

});