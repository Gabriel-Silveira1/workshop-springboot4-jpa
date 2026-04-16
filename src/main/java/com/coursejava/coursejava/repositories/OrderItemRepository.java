package com.coursejava.coursejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coursejava.coursejava.entities.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{
	
}
