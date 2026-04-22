package com.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String status;
    private BigDecimal totalPrice;

    private boolean deleted;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("deleted = false")
    private List<OrderItem> orderItems = new ArrayList<>();
}
