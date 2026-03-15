// Giả sử trong package com.oneshop.dto
package com.oneshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisplayImage {
    private String url;
    private String alt; // Thuộc tính alt text (văn bản thay thế)
}