package com.oneshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FragmentController {

	@GetMapping("/fragments/guest/header") // URL mà JSP sẽ gọi
	public String getHeader() {
	    // Đường dẫn đến file trong /templates
	    return "fragments/guest/header"; 
	}

    @GetMapping("/fragments/footer")
    public String getFooter() {
        // Trả về đường dẫn đến file footer.html trong /templates
        return "fragments/footer";
    }
}