package com.coursejava.coursejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coursejava.coursejava.entities.OrderItem;
import com.coursejava.coursejava.entities.pk.OrderItemPk;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemPk>{
	
}
