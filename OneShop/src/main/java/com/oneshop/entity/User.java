package com.oneshop.entity; 

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonManagedReference; 

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity 
@Table(name = "users") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"addresses", "cart", "shop", "role"}) 
@ToString(exclude = {"addresses", "cart", "shop", "role"})     
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100) 
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 150) 
    private String email;

    // Đã xóa nvarchar, thay bằng length
    @Column(name = "full_name", length = 255) 
    private String fullName;

    @Column(name = "phone_number", length = 20) 
    private String phoneNumber;

    // Đã xóa nvarchar, thay bằng length
    @Column(length = 500) 
    private String address; 

    @Column(nullable = false)
    private boolean activated = false; 

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference 
    private Set<Address> addresses = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shop shop; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_company_id") 
    private ShippingCompany shippingCompany;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (this.role != null && this.role.getName() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.getName().name()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; 
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; 
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; 
    }

    @Override
    public boolean isEnabled() {
        return this.activated; 
    }

    public Long getUserId() {
        return id;
    }

    public void setUserId(Long id) {
        this.id = id;
    }
}